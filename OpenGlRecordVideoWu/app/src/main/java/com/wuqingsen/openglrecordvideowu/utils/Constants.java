package com.wuqingsen.openglrecordvideowu.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * @author liuml
 * @explain
 * @time 2018/12/28 10:40
 */
public class Constants {

    public static final int ScreenWidth = 720;
    public static final int ScreenHeight = 1080;

    public static String addMarkText = "这是文字水印";


    public static String getFilePath(){
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"demo11.mp4";
        File file = new File(filePath);
        if (file.exists()){
            file.delete();
        }
        try {
            file.createNewFile();
            return filePath;
        } catch (IOException e) {
            Log.i("FileUtil","file.createNewFile() error...");
            e.printStackTrace();
        }
        return filePath;
    }
}
