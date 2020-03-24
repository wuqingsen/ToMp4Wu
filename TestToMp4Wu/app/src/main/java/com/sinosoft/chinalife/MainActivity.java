package com.sinosoft.chinalife;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.sinosoft.chinalife.utils.PcmToWavUtil;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * 音视频混合界面
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, EventListener, Camera.PreviewCallback {

    SurfaceView surfaceView;
    Button startStopButton, btnzm, btnSB;

    Camera camera;
    SurfaceHolder surfaceHolder;
    int width = 1920;
    int height = 1080;
    private DecimalFormat formatTwo = new DecimalFormat("0.00");
    private DecimalFormat formatTwo1 = new DecimalFormat("0");
    RecordAiUtils aiUtils;

    //语音识别开始
    public EventManager wakeup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aiUtils = new RecordAiUtils(MainActivity.this, MainActivity.this);
        //初始化唤醒
        // 基于SDK唤醒词集成1.1 初始化EventManager
        wakeup = EventManagerFactory.create(this, "wp");
        // 基于SDK唤醒词集成1.3 注册输出事件
        wakeup.registerListener(MainActivity.this);

        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        startStopButton = (Button) findViewById(R.id.startStop);
        btnzm = findViewById(R.id.btnzm);
        btnSB = findViewById(R.id.btnSB);

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag().toString().equalsIgnoreCase("stop")) {
                    view.setTag("start");
                    ((TextView) view).setText("开始");
                    aiUtils.stopRecord();
                    //停止混合任务
                    MediaMuxerThread.stopMuxer();
                    //关闭摄像头
                    stopCamera();
                    //音频pcw转wav
//                    setVoicePcwWav();
                } else {
                    //打开摄像头
                    startCamera();
                    view.setTag("stop");
                    ((TextView) view).setText("停止");
                    //开始混合任务
                    MediaMuxerThread.startMuxer();
                    aiUtils.startRecord();
                }
            }
        });

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        btnzm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWav();
            }
        });
        btnSB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aiUtils.startRecord();
            }
        });
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        //识别回调
        if (data != null) {

            String logTxt = "";
            Log.e("=====params", params + "？？？params");
            if (params != null && !params.isEmpty()) {
                logTxt += "识别成功：" + params;
                Log.e("=====识别成功:", logTxt);
                if (params.contains("听清楚了")) {
                    Toast.makeText(MainActivity.this, "听清楚了", Toast.LENGTH_LONG).show();
                } else if (params.contains("我同意了")) {
                    Toast.makeText(MainActivity.this, "听清楚了", Toast.LENGTH_LONG).show();
                } else if (params.contains("我已确认")) {
                    Toast.makeText(MainActivity.this, "我已确认", Toast.LENGTH_LONG).show();
                }
            }
            MediaMuxerThread.audioThread.encode(data);
        }
    }

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

    private void setWav() {
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "guoshou.pcm";
        String filePath1 = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "guoshou.wav";
        File pcmFile = new File(filePath);
        File wavFile = new File(filePath1);
        if (!wavFile.mkdirs()) {
            Log.e("=====", "wavFile Directory not created");
        }
        if (wavFile.exists()) {
            wavFile.delete();
        }
        pcmToWavUtil.pcmToWav(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath());
    }

}

