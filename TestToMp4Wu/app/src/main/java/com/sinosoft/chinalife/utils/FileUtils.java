package com.sinosoft.chinalife.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * 文件处理工具类
 */
public class FileUtils {
    public String getFilePath(){
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"wuqingsen.mp4";
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
