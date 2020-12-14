package com.demo.texttomp4complete;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.demo.texttomp4complete.muxer.MuxerThread;

public class MainActivity extends AppCompatActivity {
    private PermissionsChecker mPermissionsChecker; // 权限检测器
    private final int RESULT_CODE_LOCATION = 666;
    //定位权限,获取app内常用权限
    String[] permsLocation = {"android.permission.READ_PHONE_STATE"
            , "android.permission.ACCESS_COARSE_LOCATION"
            , "android.permission.ACCESS_FINE_LOCATION"
            , "android.permission.READ_EXTERNAL_STORAGE"
            , "android.permission.WRITE_EXTERNAL_STORAGE"
            , "android.permission.RECORD_AUDIO"
            , "android.permission.CAMERA"};
    private MuxerThread mMuxerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPermissionsChecker = new PermissionsChecker(MainActivity.this);

        if (mPermissionsChecker.lacksPermissions(permsLocation)) {
            ActivityCompat.requestPermissions(MainActivity.this, permsLocation, RESULT_CODE_LOCATION);
        }else {
            mMuxerThread = new MuxerThread();
            mMuxerThread.startMuxer(FileUtils.getFilePath(), 1920, 1080, new MuxerThread.MuxerCallBack() {
                @Override
                public void success() {

                }

                @Override
                public void fail(String var1) {

                }
            });
        }

    }

    public void stopRecoder() {
        if (this.mMuxerThread != null) {
            this.mMuxerThread.exit();
        }

    }

    public void addAudioData(byte[] var1) {
        if (this.mMuxerThread != null) {
            this.mMuxerThread.addAudioData(var1);
        }

    }

    public void addVideoData(byte[] var1) {
        if (this.mMuxerThread != null) {
            this.mMuxerThread.addVideoData(var1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RESULT_CODE_LOCATION:
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

}
