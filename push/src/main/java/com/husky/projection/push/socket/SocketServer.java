package com.husky.projection.push.socket;

import android.content.Context;
import android.media.projection.MediaProjection;
import android.util.Log;

import com.husky.projection.push.encoder.MediaEncode;
import com.husky.projection.push.socket.listener.OnSocketCallback;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class SocketServer implements OnSocketCallback {
    private static final String TAG = "SocketServer";
    private static final int PORT = 8889;

    private int port = PORT;
    private WebSocket mWebSocket;
    private WebSocketServer mWebSocketServer;

    public SocketServer(Context context, MediaProjection mediaProjection) {
        // 创建编码
        final MediaEncode mediaEncode = new MediaEncode(context, mediaProjection, this);
        // 创建socket服务端
        mWebSocketServer = new WebSocketServer(new InetSocketAddress(port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                Log.d(TAG, "onOpen>>");
                mWebSocket = conn;
                mediaEncode.start();
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                Log.d(TAG, "onClose>>");
                mWebSocketServer = null;
                mediaEncode.stop();
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                Log.d(TAG, "onMessage>>");
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                Log.d(TAG, "onError>>" + ex.toString());
                mWebSocketServer = null;
                mediaEncode.stop();
            }

            @Override
            public void onStart() {
                Log.d(TAG, "onStart>>");
            }
        };
    }

    /**
     * 开启链接
     */
    public void start() {
        Log.d(TAG, "start>>");
        if (mWebSocketServer != null) {
            mWebSocketServer.start();
        }
    }

    /**
     * 关闭链接
     */
    public void stop() {
        Log.d(TAG, "stop>>");
        if (mWebSocketServer != null) {
            try {
                mWebSocketServer.stop();
            } catch (Exception ignored) {
                Log.w(TAG, "");
            }
        }
    }

    /**
     * 发送数据
     *
     * @param frame
     */
    @Override
    public void onSendMessage(byte[] frame) {
        Log.d(TAG, "onSendMessage>>");
        if (mWebSocket != null && mWebSocket.isOpen())
            mWebSocket.send(frame);
    }
}
