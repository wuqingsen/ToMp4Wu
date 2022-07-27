package com.example.liuyan.testtomp4;

import android.content.Context;
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
    public String getFilePath() {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "myav.mp4";
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            return filePath;
        } catch (IOException e) {
            Log.i("FileUtil", "file.createNewFile() error...");
            e.printStackTrace();
        }
        return filePath;
    }


    public String mp4FilePath() {
        //mp4文件路径
        String path = "";
        path = getMediaFolderPath(MainActivity.context);
        if (path != null) {
            File dir = new File(path + File.separator + "VideoRecord" + File.separator);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            path = dir + File.separator + "111.mp4";
        }
        return path;
    }

    public String getMediaFolderPath(Context mContext) {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = mContext.getExternalFilesDir(null);// 获取跟目录
            return sdDir.toString();
        }
        return null;
    }
}
