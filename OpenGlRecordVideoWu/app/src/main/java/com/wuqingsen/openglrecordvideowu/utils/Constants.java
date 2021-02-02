package com.wuqingsen.openglrecordvideowu.utils;

import android.os.Environment;

import java.io.File;

/**
 * @author liuml
 * @explain
 * @time 2018/12/28 10:40
 */
public class Constants {

    public static final String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String fileDir = rootDir + "/wqs添加水印.mp4";
    public static final String rotateFile = rootDir + "/rotate_test_live_recoder.mp4";
    public static final String breakPointfile1 = rootDir + "/test_live_recoder1.mp4";
    public static final String breakPointfile2 = rootDir + "/test_live_recoder2.mp4";
    public static final String shortVideo = rootDir + "/MyshortVideo"+ File.separator;
    public static final int ScreenWidth = 720;
    public static final int ScreenHeight= 1080;
    public static final int handleSuccess= 0;//处理成功

    public static final String musicfileDir = rootDir + "/xjw.mp3";//加入背景的音乐
    public static String addMarkText= "这是文字水印";
    public static final String testDir = rootDir + "/药神MP4.mp4";
}
