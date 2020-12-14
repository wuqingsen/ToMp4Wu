package com.demo.texttomp4complete;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * 文件处理工具类
 * Created by renhui on 2017/9/25.
 */
public class FileUtils {
    public static String getFilePath(){
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"wqsComplete.mp4";
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
