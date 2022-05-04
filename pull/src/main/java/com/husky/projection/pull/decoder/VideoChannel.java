package com.husky.projection.pull.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.husky.projection.pull.decoder.listener.OnVideoDecoderCallback;
import com.husky.projection.pull.decoder.listener.OnVideoDecoderConfigListener;
import com.husky.projection.pull.socket.listener.OnSocketCallback;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class VideoChannel implements OnSocketCallback {
    private static final String TAG = "VideoChannel";
    private static final int WIDTH = 720;
    private static final int HEIGT = 1280;

    private Surface surface;
    private MediaCodec videoDecoder;
    private int width = WIDTH;
    private int heigt = HEIGT;
    // api >= 23 时使用，主要是将解码/编码放入子线程中
    private HandlerThread videoDecoderThread;
    private Handler videoDecoderHandler;

    private ArrayBlockingQueue<byte[]> blockingQueue;

    private volatile boolean decodeOver = true; // 是否解码结束，默认结束
    private OnVideoDecoderConfigListener onVideoDecoderConfigListener;
    private OnVideoDecoderCallback onVideoDecoderCallback;

    public VideoChannel(Surface surface) {
        this.surface = surface;
        this.blockingQueue = new ArrayBlockingQueue<byte[]>(3, true);
    }

    /**
     * 配置
     */
    public void config() {
        this.videoDecoderThread = new HandlerThread("videoDecoderThread");
        this.videoDecoderThread.start();
        this.videoDecoderHandler = new Handler(videoDecoderThread.getLooper());

        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, heigt);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * heigt);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            videoDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // api >= 23
                videoDecoder.setCallback(decodeCallback, videoDecoderHandler);
            } else {    // 21 =< api < 23
                videoDecoder.setCallback(decodeCallback);
            }
            // 需要在 setCallback 之后，配置 configure
            videoDecoder.configure(mediaFormat, surface, null, 0);
            // 成功回调
            if (onVideoDecoderConfigListener != null)
                onVideoDecoderConfigListener.onDecoderConfigSuccess();
        } catch (Exception e) {
            Log.w(TAG, "");
            // 配置解码器出现异常重置解码结束标志和释放资源
            stop();
            // 失败回调
            if (onVideoDecoderConfigListener != null)
                onVideoDecoderConfigListener.onDecoderConfigFailed();
        }
    }

    // 异步解码回调
    private MediaCodec.Callback decodeCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int index) {
            Log.d(TAG, mediaCodec + ">>decodeCallback>>onInputBufferAvailable-->index = " + index);
            if (index >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index); // api >= 21
                if (inputBuffer != null) {
                    inputBuffer.clear();

                    if (decodeOver) {   // 结束
                        mediaCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        Log.d(TAG, mediaCodec + ">>decodeCallback>>onInputBufferAvailable>>decodeOver = " + decodeOver);
                    } else {
                        try {
                            // 暂时使用当前时间戳
                            long presentationTimeUs = System.currentTimeMillis();
                            byte[] videoFrame = blockingQueue.take();
                            if (videoFrame.length == 0) return;

                            inputBuffer.put(videoFrame, 0, videoFrame.length);
                            Log.d(TAG, mediaCodec + ">>decodeCallback>>onInputBufferAvailable>>bufferSize = " + videoFrame.length);
                            Log.d(TAG, mediaCodec + ">>decodeCallback>>onInputBufferAvailable>>extractor.getSampleTime() = " + presentationTimeUs);
                            mediaCodec.queueInputBuffer(index, 0, videoFrame.length, presentationTimeUs, 0);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
            Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable-->index = " + index);
            if (index >= 0 && bufferInfo.size > 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index); // api >= 21
                if (outputBuffer != null) {
                    Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable>>bufferInfo.size-->" + bufferInfo.size);
                    Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable>>outputBuffer.remaining-->" + outputBuffer.remaining());
                    byte[] yuvFrame = new byte[outputBuffer.remaining()];
                    outputBuffer.get(yuvFrame);
                    outputBuffer.clear();

                    Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable-->yuvFrame.length-->" + yuvFrame.length);
                    Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable-->bufferInfo.offset-->" + bufferInfo.offset);
                    Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable-->bufferInfo.size-->" + bufferInfo.size);
                    Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable-->bufferInfo.flags-->" + bufferInfo.flags);
                    Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable-->bufferInfo.presentationTimeUs-->" + bufferInfo.presentationTimeUs);
                }

                // 释放outputBufferId上的数据
                mediaCodec.releaseOutputBuffer(index, true);
            }

            /**
             * 输入解码已经读到文件末尾以后，输出解码还不能马上执行到此处，需要将输出解码队列中解码完才能执行到此处
             * 如果加上解码是否结束标识符判断能马上停止解码（isDecodeOver）
             */
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 || decodeOver) {   // 表示到达文件末尾了
                Log.d(TAG, mediaCodec + ">>decodeCallback>>BUFFER_FLAG_END_OF_STREAM-->");
                // 停止并解码器释放资源
                stop();
                Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputBufferAvailable decodeOver = " + decodeOver);
                // 解码完成回调
                if (onVideoDecoderCallback != null)
                    onVideoDecoderCallback.onDecoderOver();
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d(TAG, mediaCodec + ">>decodeCallback>>onError-->" + e);
            // 解码器错误情况下重置解码结束标志和释放资源
            stop();
            // 解码失败回调
            if (onVideoDecoderCallback != null)
                onVideoDecoderCallback.onDecoderError();
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d(TAG, mediaCodec + ">>decodeCallback>>onOutputFormatChanged-->mediaFormat-->" + mediaFormat.toString());
            // mediaFormat回调
            if (onVideoDecoderCallback != null)
                onVideoDecoderCallback.onDecoderFormatChanged(mediaFormat);
        }
    };

    public void start() {
        Log.d(TAG, "start>>");
        decodeOver = false;
        videoDecoder.start();
    }

    public void stop() {
        Log.d(TAG, "stop>>");
        decodeOver = true;
        if (videoDecoder != null) {
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
        }
    }

    @Override
    public void onReceiveMessage(byte[] frame) {
        try {
            if (blockingQueue != null) {
                blockingQueue.put(frame);
            }
        } catch (InterruptedException ignored) {
            Log.w(TAG, "");
        }
    }

    public void setOnDecoderConfigListener(OnVideoDecoderConfigListener onVideoDecoderConfigListener) {
        this.onVideoDecoderConfigListener = onVideoDecoderConfigListener;
    }

    public void setOnDecoderCallback(OnVideoDecoderCallback onVideoDecoderCallback) {
        this.onVideoDecoderCallback = onVideoDecoderCallback;
    }
}
