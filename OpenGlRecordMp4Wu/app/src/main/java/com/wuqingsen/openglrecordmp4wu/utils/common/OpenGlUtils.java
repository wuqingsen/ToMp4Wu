package com.wuqingsen.openglrecordmp4wu.utils.common;

import android.content.res.Resources;

import com.wuqingsen.openglrecordmp4wu.common.MyApplication;

import java.io.InputStream;

/**
 * wuqingsen on 2020/12/25
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class OpenGlUtils {
    //通过资源路径加载shader脚本文件
    public static String uRes(String path) {
        Resources resources = MyApplication.getContext().getResources();
        StringBuilder result = new StringBuilder();
        try {
            InputStream is = resources.getAssets().open(path);
            int ch;
            byte[] buffer = new byte[1024];
            while (-1 != (ch = is.read(buffer))) {
                result.append(new String(buffer, 0, ch));
            }
        } catch (Exception e) {
            return null;
        }
        return result.toString().replaceAll("\\r\\n", "\n");
    }
}
