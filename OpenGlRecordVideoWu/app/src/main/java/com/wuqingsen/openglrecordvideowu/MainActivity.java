package com.wuqingsen.openglrecordvideowu;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.wuqingsen.openglrecordvideowu.camera.XYCameraView;

public class MainActivity extends AppCompatActivity {
    Button btn_start,btn_stop;
    private XYCameraView xycamaryview;
    private XYUtil xyUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);
        xycamaryview = findViewById(R.id.xycameraview);
        xyUtil = XYUtil.getInstance();

        xycamaryview.isAddMark = true;
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xyUtil.startRecoder(MainActivity.this, xycamaryview);
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xyUtil.stopRecoder();
            }
        });
    }
}