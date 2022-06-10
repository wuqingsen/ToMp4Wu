package com.wuqingsen.opengllearn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.wuqingsen.opengllearn.aaa.AActivity;
import com.wuqingsen.opengllearn.bbb.BActivity;
import com.wuqingsen.opengllearn.ccc.CActivity;
import com.wuqingsen.opengllearn.ddd.DActivity;
import com.wuqingsen.opengllearn.eee.EActivity;
import com.wuqingsen.opengllearn.fff.FActivity;
import com.wuqingsen.opengllearn.ggg.GActivity;
import com.wuqingsen.opengllearn.hhh.HActivity;

public class MainActivity extends AppCompatActivity {

    Button btn_a, btn_b, btn_c, btn_d, btn_e,btn_f,btn_g,btn_h;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_a = findViewById(R.id.btn_a);
        btn_b = findViewById(R.id.btn_b);
        btn_c = findViewById(R.id.btn_c);
        btn_d = findViewById(R.id.btn_d);
        btn_e = findViewById(R.id.btn_e);
        btn_f = findViewById(R.id.btn_f);
        btn_g = findViewById(R.id.btn_g);
        btn_h = findViewById(R.id.btn_h);

        //简单实用
        btn_a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AActivity.class));
            }
        });

        //顶点和着色器，编译着色器及在屏幕上绘图
        btn_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BActivity.class));
            }
        });

        //增加颜色和着色
        btn_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CActivity.class));
            }
        });

        //调整屏幕宽高比
        btn_d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DActivity.class));
            }
        });

        //三维世界
        btn_e.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EActivity.class));
            }
        });

        //纹理增加细节
        btn_f.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, FActivity.class));
            }
        });

        //构建简单物体
        btn_g.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GActivity.class));
            }
        });

        //增加触控
        btn_h.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HActivity.class));
            }
        });
    }
}