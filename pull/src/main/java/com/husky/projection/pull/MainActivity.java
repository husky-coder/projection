package com.husky.projection.pull;

import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.husky.projection.pull.manager.ProjectionPullManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private SurfaceView surfaceView;
    private Surface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceview);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceCreated>>");
                surface = surfaceHolder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged>>");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed>>");
            }
        });

        findViewById(R.id.start_pull).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ProjectionPullManager.getInstance()
                        .surface(surface)
                        .start();
            }
        });

        findViewById(R.id.stop_pull).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                ProjectionPullManager.getInstance().stop();
            }
        });
    }
}
