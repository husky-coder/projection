package com.husky.projection.push.encoder;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.husky.projection.push.encoder.listener.OnVideoEncoderCallback;
import com.husky.projection.push.encoder.listener.OnVideoEncoderConfigListener;
import com.husky.projection.push.socket.listener.OnSocketCallback;
import com.husky.projection.push.util.FileUtils;
import com.husky.projection.push.util.StorageUtil;

import java.io.File;
import java.nio.ByteBuffer;

public class VideoChannel {
    private static final String TAG = "VideoChannel";

    private static final int WIDTH = 720;
    private static final int HEIGT = 1280;
    private static final int NAL_I = 5;     // I帧
    private static final int NAL_SPS_PPS = 7;   // SPS、PPS帧

    private Context mContext;
    private MediaCodec videoEncoder;
    private int width = WIDTH;
    private int heigt = HEIGT;
    private byte[] spsPpsBuf;   // 缓存sps、pps的数组

    // api >= 23 时使用，主要是将解码/编码放入子线程中
    private HandlerThread videoEncoderThread;
    private Handler videoEncoderHandler;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;

    private volatile boolean encodeOver = true; // 是否结束，默认结束
    private OnVideoEncoderConfigListener onVideoEncoderConfigListener;
    private OnVideoEncoderCallback onVideoEncoderCallback;
    private OnSocketCallback onSocketCallback;

    public VideoChannel(Context context, MediaProjection mediaProjection, OnSocketCallback onSocketCallback) {
        this.mContext = context;
        this.mediaProjection = mediaProjection;
        this.onSocketCallback = onSocketCallback;
    }

    /**
     * 配置
     *
     * @return
     */
    public void config() {
        Log.d(TAG, "config>>");
        videoEncoderThread = new HandlerThread("videoEncoderThread");
        videoEncoderThread.start();
        videoEncoderHandler = new Handler(videoEncoderThread.getLooper());

        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, heigt);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * heigt);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // api >= 23
                videoEncoder.setCallback(encodeCallback, videoEncoderHandler);
            } else {    // 21 =< api < 23
                videoEncoder.setCallback(encodeCallback);
            }
            // 需要在 setCallback 之后，配置 configure
            videoEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // 成功回调
            if (onVideoEncoderConfigListener != null)
                onVideoEncoderConfigListener.onEncoderConfigSuccess();
        } catch (Exception e) {
            Log.w(TAG, "");
            stop();
            // 失败回调
            if (onVideoEncoderConfigListener != null)
                onVideoEncoderConfigListener.onEncoderConfigFailed();
        }
    }

    // 异步编码回调
    private MediaCodec.Callback encodeCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            // 通过projection输入，无需自己实现
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
            Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable-->index = " + index);
            Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable-->bufferInfo.size = " + bufferInfo.size);
            if (index >= 0 && bufferInfo.size > 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index); // api >= 21
                if (outputBuffer != null) {
                    Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable>>bufferInfo.size-->" + bufferInfo.size);
                    Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable>>outputBuffer.remaining-->" + outputBuffer.remaining());
                    // 处理帧数据并发送至传输层
                    pushFrame(outputBuffer, bufferInfo);

                    byte[] h264Frame = new byte[outputBuffer.remaining()];
                    outputBuffer.get(h264Frame);
                    outputBuffer.clear();
                    // 调试用
                    FileUtils.writeContent(StorageUtil.getExternalFilesDir(mContext, null) + File.separator + "projection.h264", h264Frame);

                    Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable-->h264Frame.length-->" + h264Frame.length);
                    Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable-->bufferInfo.offset-->" + bufferInfo.offset);
                    Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable-->bufferInfo.size-->" + bufferInfo.size);
                    Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable-->bufferInfo.flags-->" + bufferInfo.flags);
                    Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable-->bufferInfo.presentationTimeUs-->" + bufferInfo.presentationTimeUs);
                }
                // 释放outputBufferId上的数据
                mediaCodec.releaseOutputBuffer(index, false);
            }

            /**
             * 输入编码已经没有数据时，输出编码还不能马上执行到此处，需要将输出编码队列中编码完才能执行到此处
             * 如果加上编码是否结束标识符判断能马上停止编码（isEncodeOver）
             */
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || encodeOver) {   // 表示到达文件末尾了
                Log.d(TAG, mediaCodec + ">>encodeCallback>>BUFFER_FLAG_END_OF_STREAM-->");
                // 停止编码器释放资源
                stop();
                Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputBufferAvailable encodeOver = " + encodeOver);
                // 编码结束回调
                if (onVideoEncoderCallback != null)
                    onVideoEncoderCallback.onEncoderOver();
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, mediaCodec + ">>encodeCallback>>onError-->" + e);
            // 解码器错误情况下重置解码结束标志和释放资源
            stop();
            // 编码失败回调
            if (onVideoEncoderCallback != null)
                onVideoEncoderCallback.onEncoderError();
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, mediaCodec + ">>encodeCallback>>onOutputFormatChanged-->mediaFormat-->" + mediaFormat.toString());
            // mediaFormat回调
            if (onVideoEncoderCallback != null)
                onVideoEncoderCallback.onEncoderFormatChanged(mediaFormat);
        }
    };

    /**
     * 开启
     */
    public void start() {
        Log.d(TAG, "start>>");
        Surface surface = videoEncoder.createInputSurface();
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "-display",
                width,
                heigt,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                null,
                null);

        encodeOver = false;
        videoEncoder.start();
    }

    /**
     * 关闭
     */
    public void stop() {
        Log.d(TAG, "stop>>");
        encodeOver = true;
        if (videoEncoder != null) {
            videoEncoder.stop();
            videoEncoder.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    /**
     * 帧处理并推送
     *
     * @param byteBuffer 一帧数据
     * @param bufferInfo
     */
    private void pushFrame(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        Log.d(TAG, "pushFrame>>");
        int offset = 4; // 默认分隔符为00 00 00 01
        if (byteBuffer.get(2) == 0x01) {    // 分隔符为00 00 01 的情况
            offset = 3;
        }

        int frameType = byteBuffer.get(offset) & 0x1F;  // h264格式需要&0x1F得到帧类型
        byte[] newBytes = null; // 存储要发送的数据
        Log.d(TAG, "pushFrame>>" + frameType);
        switch (frameType) {
            case NAL_SPS_PPS:   // sps、pps 帧
                spsPpsBuf = new byte[bufferInfo.size];
                byteBuffer.get(spsPpsBuf);  // 将spspps帧数据缓存起来
                break;
            case NAL_I:     // I 帧
                // 获取I帧数据
                byte[] iFrame = new byte[bufferInfo.size];
                byteBuffer.get(iFrame);
                // sps、pps + I帧的数据的 字节数组
                newBytes = new byte[spsPpsBuf.length + iFrame.length];
                // 将sps、pps拷贝到新字节数组
                System.arraycopy(spsPpsBuf, 0, newBytes, 0, spsPpsBuf.length);
                // 将I帧数据拷贝到新字节数组
                System.arraycopy(iFrame, 0, newBytes, spsPpsBuf.length, iFrame.length);
                break;
            default:    // P、B 帧
                // sps、pps + I帧的数据的 字节数组
                newBytes = new byte[bufferInfo.size];
                byteBuffer.get(newBytes);
        }

        // 回调编码好的数据
        if (onSocketCallback != null && newBytes != null) {
            onSocketCallback.onSendMessage(newBytes);
        }
    }

    public void setOnVideoEncoderConfigListener(OnVideoEncoderConfigListener onVideoEncoderConfigListener) {
        this.onVideoEncoderConfigListener = onVideoEncoderConfigListener;
    }

    public void setOnVideoEncoderCallback(OnVideoEncoderCallback onVideoEncoderCallback) {
        this.onVideoEncoderCallback = onVideoEncoderCallback;
    }
}
