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

import com.tencent.aai.AAIClient;
import com.tencent.aai.audio.data.AudioRecordDataSource;
import com.tencent.aai.auth.AbsCredentialProvider;
import com.tencent.aai.auth.LocalCredentialProvider;
import com.tencent.aai.exception.ClientException;
import com.tencent.aai.listener.AudioRecognizeResultListener;
import com.tencent.aai.model.AudioRecognizeRequest;
import com.tencent.aai.model.AudioRecognizeResult;
import com.tencent.aai.model.type.AudioRecognizeConfiguration;
import com.tencent.aai.model.type.AudioRecognizeTemplate;
import com.tencent.aai.model.type.EngineModelType;

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
    AAIClient aaiClient;
    AbsCredentialProvider credentialProvider;

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
        initTX();

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

    //初始化腾讯
    private void initTX() {
        AudioRecognizeRequest.Builder builder = new AudioRecognizeRequest.Builder();
        // 初始化识别请求
        final AudioRecognizeRequest audioRecognizeRequest = builder
//                        .pcmAudioDataSource(new AudioRecordDataSource()) // 设置数据源
                .pcmAudioDataSource(new AudioRecordDataSource()) // 设置数据源
                //.templateName(templateName) // 设置模板
//                        .setHotWordId("")//热词 id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词 id 设置，自动生效默认热词；如果进行了单独的热词 id 设置，那么将生效单独设置的热词 id。
                .build();

        // 自定义识别配置
        final AudioRecognizeConfiguration audioRecognizeConfiguration = new AudioRecognizeConfiguration.Builder()
                .minAudioFlowSilenceTime(2000) // 语音流识别时的间隔时间
                .minVolumeCallbackTime(80) // 音量回调时间
                .sensitive(2.5f)
                .build();

        credentialProvider = new LocalCredentialProvider("SdXCagX8WKyRny3sXxB9uVq32kGSTYWF");
        try {
            aaiClient = new AAIClient(MainActivity.this, 1259627588, "SdXCagX8WKyRny3sXxB9uVq32kGSTYWF", credentialProvider);
        } catch (ClientException e) {
            e.printStackTrace();
            Log.e("wqs初始化", "initTX: " + e.getMessage());
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                aaiClient.startAudioRecognize(audioRecognizeRequest, audioRecognizeResultlistener,
                        audioRecognizeStateListener, audioRecognizeTimeoutListener,
                        audioRecognizeConfiguration);

            }
        }).start();

    }


    // 识别结果回调监听器
    final AudioRecognizeResultListener audioRecognizeResultlistener = new AudioRecognizeResultListener() {

        boolean dontHaveResult = true;

        /**
         * 返回分片的识别结果
         * @param request 相应的请求
         * @param result 识别结果
         * @param seq 该分片所在语音流的序号 (0, 1, 2...)
         */
        @Override
        public void onSliceSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {

            if (dontHaveResult && !TextUtils.isEmpty(result.getText())) {
                dontHaveResult = false;
                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, receive first response in %s, result is = %s", seq, time, result.getText());
                Log.i(PERFORMANCE_TAG, message);
            }

            AAILogger.info(logger, "分片on slice success..");
            AAILogger.info(logger, "分片slice seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
            resMap.put(String.valueOf(seq), result.getText());
            final String msg = buildMessage(resMap);
            AAILogger.info(logger, "分片slice msg="+msg);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    recognizeResult.setText(msg);
                }
            });

        }

        /**
         * 返回语音流的识别结果
         * @param request 相应的请求
         * @param result 识别结果
         * @param seq 该语音流的序号 (1, 2, 3...)
         */
        @Override
        public void onSegmentSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
            dontHaveResult = true;
            AAILogger.info(logger, "语音流on segment success");
            AAILogger.info(logger, "语音流segment seq = {}, voiceid = {}, result = {}", seq, result.getVoiceId(), result.getText());
            resMap.put(String.valueOf(seq), result.getText());
            final String msg = buildMessage(resMap);
            AAILogger.info(logger, "语音流segment msg="+msg);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    recognizeResult.setText(msg);
                }
            });
        }

        /**
         * 识别结束回调，返回所有的识别结果
         * @param request 相应的请求
         * @param result 识别结果
         */
        @Override
        public void onSuccess(AudioRecognizeRequest request, String result) {
            AAILogger.info(logger, "识别结束, onSuccess..");
            AAILogger.info(logger, "识别结束, result = {}", result);
        }

        /**
         * 识别失败
         * @param request 相应的请求
         * @param clientException 客户端异常
         * @param serverException 服务端异常
         */
        @Override
        public void onFailure(AudioRecognizeRequest request, final ClientException clientException, final ServerException serverException) {
            if (clientException!=null) {
                AAILogger.info(logger, "onFailure..:"+clientException.toString());
            }
            if (serverException!=null) {
                AAILogger.info(logger, "onFailure..:"+serverException.toString());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {

                    if (clientException!=null) {
                        recognizeState.setText("识别状态：失败,  "+clientException.toString());
                        AAILogger.info(logger, "识别状态：失败,  "+clientException.toString());
                    } else if (serverException!=null) {
                        recognizeState.setText("识别状态：失败,  "+serverException.toString());
                    }
                }
            });
        }
    };

}

