package com.husky.projection.push.manager;

import android.content.Context;
import android.media.projection.MediaProjection;
import android.util.Log;

import com.husky.projection.push.socket.SocketServer;

public class ProjectionPushManager {
    private static final String TAG = "ProjectionPushManager";

    private Context context;
    // socket服务端
    private SocketServer socketServer;
    // 录屏
    private MediaProjection mediaProjection;

    // 静态内部类
    private static class ProjectionPushManagerHolder {
        private static final ProjectionPushManager INSTANCE = new ProjectionPushManager();
    }

    private ProjectionPushManager() {
        Log.d(TAG, "ProjectionPushManager>>构造函数");
        // 防止反射破坏代理模式
        if (getInstance() != null) {
            throw new RuntimeException();
        }
    }

    public static ProjectionPushManager getInstance() {
        return ProjectionPushManagerHolder.INSTANCE;
    }

    public ProjectionPushManager context(Context context) {
        this.context = context;
        return ProjectionPushManagerHolder.INSTANCE;
    }

    public ProjectionPushManager mediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        return ProjectionPushManagerHolder.INSTANCE;
    }

    public void start() {
        Log.d(TAG, "start>>");
        // 初始化网络
        socketServer = new SocketServer(context, mediaProjection);
        socketServer.start();
    }

    public void stop() {
        Log.d(TAG, "stop>>");
        if (socketServer != null)
            socketServer.stop();
    }
}
