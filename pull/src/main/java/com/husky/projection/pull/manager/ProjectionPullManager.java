package com.husky.projection.pull.manager;

import android.util.Log;
import android.view.Surface;

import com.husky.projection.pull.socket.SocketClient;

public class ProjectionPullManager {
    private static final String TAG = "ProjectionPullManager";
    // socket客户端
    private SocketClient socketClient;
    // 解码用到的surface
    private Surface surface;

    private static class ProjectionPullManagerHolder {
        private static final ProjectionPullManager INSTANCE = new ProjectionPullManager();
    }

    private ProjectionPullManager() {
        Log.d(TAG, "ProjectionPullManager>>构造函数");
        // 防止反射破坏代理模式
        if (getInstance() != null) {
            throw new RuntimeException();
        }
    }

    public static ProjectionPullManager getInstance() {
        return ProjectionPullManagerHolder.INSTANCE;
    }

    /**
     * 设置Surface
     *
     * @param surface
     * @return
     */
    public ProjectionPullManager surface(Surface surface) {
        this.surface = surface;
        return ProjectionPullManagerHolder.INSTANCE;
    }

    public void start() {
        Log.d(TAG, "start>>");
        // 初始化网络
        socketClient = new SocketClient(surface);
        socketClient.start();
    }

    public void stop() {
        Log.d(TAG, "stop>>");
        if (socketClient != null)
            socketClient.stop();
    }
}
