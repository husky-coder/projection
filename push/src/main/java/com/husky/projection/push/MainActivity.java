package com.husky.projection.push;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.husky.projection.push.service.ProjectionService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_PERMISSION_CODE = 100;
    private static final int REQUEST_PROJECTION_CODE = 200;

    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 动态授权
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
        } else {
            // TODO
        }

        findViewById(R.id.start_push).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                if (mediaProjectionManager != null) {
                    Log.d(TAG, "onClick>>mediaProjectionManager-->" + mediaProjectionManager);
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_PROJECTION_CODE);
                }
            }
        });

        findViewById(R.id.stop_push).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this, ProjectionService.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PROJECTION_CODE || resultCode != RESULT_OK || data == null)
            return;

        Intent projectionIntent = new Intent(this, ProjectionService.class);
        projectionIntent.putExtra("RESULT_CODE", resultCode);
        projectionIntent.putExtra("RESULT_DATA", data);
        startService(projectionIntent);
    }

    private void checkPermission(String[] permissions) {
        List<String> permissionReqs = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionReqs.add(permission);
            }
        }
        if (!permissionReqs.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionReqs.toArray(new String[permissionReqs.size()]), REQUEST_PERMISSION_CODE);
        } else {
            // TODO
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean hasPermission = true;
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            hasPermission = false;
                            break;
                        }
                    }
                    if (hasPermission) {
                        // TODO
                    } else {
                        // 如果用户没有授权，那么应该说明意图，引导用户去设置里面授权。
                        Toast.makeText(this, "应用缺少必要的权限！请点击\"权限\"，打开所需要的权限。", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        finish();
                    }
                }
                break;
            default:
        }
    }
}
