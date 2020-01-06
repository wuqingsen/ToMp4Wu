package com.example.liuyan.testtomp4;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

/**
 * 音视频混合界面
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    SurfaceView surfaceView;
    Button startStopButton;

    Camera camera;
    SurfaceHolder surfaceHolder;
    int width = 1920;
    int height = 1080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //申请权限
        applyPermission();
        //判断手机支持的编码格式
        checkAVFormat();
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        startStopButton = (Button) findViewById(R.id.startStop);

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag().toString().equalsIgnoreCase("stop")) {
                    view.setTag("start");
                    ((TextView) view).setText("开始");
                    //停止混合任务
                    MediaMuxerThread.stopMuxer();
                    //关闭摄像头
                    stopCamera();
                } else {
                    //打开摄像头
                    startCamera();
                    view.setTag("stop");
                    ((TextView) view).setText("停止");
                    //开始混合任务
                    MediaMuxerThread.startMuxer();
                }
            }
        });

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.i("=====Main", "enter surfaceCreated method");
        this.surfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.i("=====Main", "enter surfaceChanged method");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.i("=====Main", "enter surfaceDestroyed method");
        MediaMuxerThread.stopMuxer();
        stopCamera();

    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        MediaMuxerThread.addVideoFrameData(bytes);
    }


    //----------------------- 摄像头操作相关 --------------------------------------

    /**
     * 打开摄像头
     */
    private void startCamera() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);

        // 这个宽高的设置必须和后面编解码的设置一样，否则不能正常处理
        parameters.setPreviewSize(width, height);

        try {
            camera.setParameters(parameters);
            camera.setPreviewDisplay(surfaceHolder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭摄像头
     */
    private void stopCamera() {
        // 停止预览并释放资源
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera = null;
        }
    }


    //申请权限
    private void applyPermission() {
        String[] limits = new String[4];
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            limits[0] = Manifest.permission.CAMERA;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            limits[1] = Manifest.permission.RECORD_AUDIO;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            limits[2] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            limits[3] = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        ActivityCompat.requestPermissions(this, limits, 666);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 666:
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Log.i("=====Main", permissions[i] + "申请失败...");
                    } else {
                        Log.i("=====Main", permissions[i] + "申请成功...");
                    }
                }
                Log.i("=====Main", "权限申请结束...");
        }
    }

    private void checkAVFormat() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase("video/avc")) {
                    Log.i("=====Main", "支持视频H.264(avc)编码...");
                }
                if (types[j].equalsIgnoreCase("audio/mp4a-latm")) {
                    Log.i("=====Main", "支持音频aac编码...");
                }
            }
        }
    }

}

