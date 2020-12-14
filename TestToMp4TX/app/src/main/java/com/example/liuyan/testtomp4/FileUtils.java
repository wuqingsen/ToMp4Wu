package com.example.liuyan.testtomp4;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 文件处理工具类
 * Created by renhui on 2017/9/25.
 */
public class FileUtils {
    public String getFilePath(){
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"myav.mp4";
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
