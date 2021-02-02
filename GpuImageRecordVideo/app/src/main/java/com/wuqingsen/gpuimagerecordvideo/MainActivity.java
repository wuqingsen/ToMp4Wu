package com.wuqingsen.gpuimagerecordvideo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "wqs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        if (PermissionHelper.hasCameraPermission(this)) {
            Intent intent = new Intent(MainActivity.this,CameraCaptureActivity.class);
            startActivity(intent);

        } else {
            PermissionHelper.requestCameraPermission(this, false);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!PermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            PermissionHelper.launchPermissionSettings(this);
            finish();
        } else {
            Intent intent = new Intent(MainActivity.this,CameraCaptureActivity.class);
            startActivity(intent);
        }
    }
}