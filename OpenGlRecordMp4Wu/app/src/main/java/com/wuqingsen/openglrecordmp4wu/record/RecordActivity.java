package com.wuqingsen.openglrecordmp4wu.record;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wuqingsen.openglrecordmp4wu.MainActivity;
import com.wuqingsen.openglrecordmp4wu.R;
import com.wuqingsen.openglrecordmp4wu.common.Constants;
import com.wuqingsen.openglrecordmp4wu.utils.camera.CameraView;
import com.wuqingsen.openglrecordmp4wu.utils.camera.SensorControler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * wuqingsen on 2020/12/25
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class RecordActivity extends AppCompatActivity implements SensorControler.CameraFocusListener {
    Button btn_video;
    private CameraView mCameraView;
    private boolean recordFlag = false;//是否正在录制
    private ExecutorService executorService;
    private SensorControler mSensorControler;
    String savePath = Constants.getPath("record/",  "wqs.mp4");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        btn_video = findViewById(R.id.btn_video);
        mCameraView = findViewById(R.id.camera_view);

        executorService = Executors.newSingleThreadExecutor();
        mSensorControler = SensorControler.getInstance();
        mSensorControler.setCameraFocusListener(this);
        btn_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recordFlag) {
                    executorService.execute(recordRunnable);
                    recordFlag = true;
                    btn_video.setText("点击结束录制");
                } else {
                    mCameraView.pause(false);
                    recordFlag = false;
                    btn_video.setText("已停止录制");
                    mCameraView.stopRecord();
                    recordComplete(savePath);
                }
            }
        });
    }


    Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (recordFlag) {
                    Log.w("wqs", "run: 开始录制" );
                    mCameraView.setSavePath(savePath);
                    mCameraView.startRecord();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void recordComplete(final String path) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RecordActivity.this, "文件保存路径：" + path, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onFocus() {

    }


    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.onResume();
        if (recordFlag) {
            mCameraView.resume(true);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (recordFlag) {
            mCameraView.pause(true);
        }
        mCameraView.onPause();
    }
}