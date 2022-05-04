package com.husky.projection.push.service;

import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.husky.projection.push.manager.ProjectionPushManager;

public class ProjectionService extends Service {
    private static final String TAG = "ProjectionService";

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private boolean hasStart = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand>>");
        // 已经启动过了
        if (hasStart) return super.onStartCommand(intent, flags, startId);

        int resultCode = intent.getIntExtra("RESULT_CODE", 0);
        Intent resultData = intent.getParcelableExtra("RESULT_DATA");
        if (resultCode == 0 || resultData == null)
            return super.onStartCommand(intent, flags, startId);

        if (mediaProjectionManager != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
        }
        if (mediaProjection == null) return super.onStartCommand(intent, flags, startId);

        Log.d(TAG, "onStartCommand>>mediaProjectionManager-->" + mediaProjectionManager);
        Log.d(TAG, "onStartCommand>>mediaProjection-->" + mediaProjection);
        // 开启投屏
        ProjectionPushManager.getInstance()
                .context(this)
                .mediaProjection(mediaProjection)
                .start();
        // 设置启动状态
        hasStart = !hasStart;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy>>");
        ProjectionPushManager.getInstance().stop();
        mediaProjectionManager = null;
    }
}
