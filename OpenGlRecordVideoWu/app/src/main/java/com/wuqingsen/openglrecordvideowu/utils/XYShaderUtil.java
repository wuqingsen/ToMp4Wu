package com.wuqingsen.openglrecordvideowu.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

/**
 * @author liuml
 * @explain 着色器帮助类
 * @time 2018/11/15 16:54
 */
public class XYShaderUtil {

    /**
     * 从raw内读取GLSE的数据
     *
     * @param context
     * @param rawId
     * @return
     */
    public static String getRawResource(Context context, int rawId) {
        InputStream inputStream = context.getResources().openRawResource(rawId);
        BufferedReader reader = new BufferedReader((new InputStreamReader(inputStream)));
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {

        }
        return sb.toString();
    }

    /**
     * 加载shader  编译片段着色器
     *
     * @param shaderType 着色器类型
     * @param source     编译代码
     * @return 着色器对象ID
     */
    public static int loadShader(int shaderType, String source) {
        //1. 创建一个新的着色器对象
        int shader = GLES20.glCreateShader(shaderType);
        // 2.获取创建状态
        if (shader != 0) {
            // 3.将着色器代码上传到着色器对象中
            GLES20.glShaderSource(shader, source);
            // 4.编译着色器对象
            GLES20.glCompileShader(shader);
            // 5.获取编译状态：OpenGL将想要获取的值放入长度为1的数组的首位
            int[] compile = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compile, 0);
            // 打印编译的着色器信息
            LogUtil.d("Results of compiling source:" + " " + shaderType + " : "
                    + GLES20.glGetShaderInfoLog(shader));
            // 6.验证编译状态
            if (compile[0] != GLES20.GL_TRUE) {
                // 在OpenGL中，都是通过整型值去作为OpenGL对象的引用。之后进行操作的时候都是将这个整型值传回给OpenGL进行操作。
                // 返回值0代表着创建对象失败。
                LogUtil.e("shader compile error");
                // 如果编译失败，则删除创建的着色器对象
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /**
     * 创建OpenGL程序：通过链接顶点着色器、片段着色器
     *
     * @param vertexSource   顶点着色器ID
     * @param fragmentSource 片段着色器ID
     * @return OpenGL程序ID
     */
    public static int createProgram(String vertexSource, String fragmentSource) {

        // 步骤1：编译顶点着色器

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        // 步骤2：编译片段着色器

        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            return 0;
        }

        // 步骤3：将顶点着色器、片段着色器进行链接，组装成一个OpenGL程序

        // 1.创建一个OpenGL程序对象
        int program = GLES20.glCreateProgram();
        // 2.获取创建状态
        if (program != 0) {
            // 3.将顶点着色器依附到OpenGL程序对象
            GLES20.glAttachShader(program, vertexShader);
            // 3.将片段着色器依附到OpenGL程序对象
            GLES20.glAttachShader(program, fragmentShader);
            // 4.将两个着色器链接到OpenGL程序对象
            GLES20.glLinkProgram(program);
            // 5.获取链接状态：OpenGL将想要获取的值放入长度为1的数组的首位
            int[] lineSatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, lineSatus, 0);
            // 6.验证链接状态
            if (lineSatus[0] != GLES20.GL_TRUE) {
                LogUtil.e("link program error");
                // 链接失败则删除程序对象
                GLES20.glDeleteProgram(program);
                // 7.返回程序对象：失败，为0
                program = 0;
            }
        }
        //步骤4：通知OpenGL开始使用该程序
        return program;
    }


    /**
     * 验证OpenGL程序对象状态
     *
     * @param program
     * @return
     */
    public static boolean validateProgram(int program) {
        GLES20.glValidateProgram(program);

        int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
        LogUtil.d("Results of validating program: " + validateStatus[0]
                + "\nLog:" + GLES20.glGetProgramInfoLog(program));
        return validateStatus[0] != 0;

    }

    public static Bitmap getCommon(String str) {
        return XYShaderUtil.createTextImage(str, 50, "#ff00ff", "#00000000", 0);//生成图片
    }

    /**
     * 创建图片
     *
     * @param text
     * @param textSize
     * @param textColor
     * @param bgColor
     * @param padding
     * @return
     */
    public static Bitmap createTextImage(String text, int textSize, String textColor, String bgColor, int padding) {

        Paint paint = new Paint();
        paint.setColor(Color.parseColor(textColor));
        paint.setTextSize(textSize);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        float width = paint.measureText(text, 0, text.length());

        float top = paint.getFontMetrics().top;
        float bottom = paint.getFontMetrics().bottom;

        Bitmap bm = Bitmap.createBitmap((int) (width + padding * 2), (int) ((bottom - top) + padding * 2), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);

        canvas.drawColor(Color.parseColor(bgColor));
        canvas.drawText(text, padding, -top + padding, paint);
        return bm;
    }

    /**
     * 加载
     *
     * @param bitmap
     * @return
     */
    public static int loadBitmapTexture(Bitmap bitmap) {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        ByteBuffer bitmapBuffer = ByteBuffer.allocate(bitmap.getHeight() * bitmap.getWidth() * 4);
        bitmap.copyPixelsToBuffer(bitmapBuffer);
        bitmapBuffer.flip();

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth(),
                bitmap.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);
        return textureIds[0];
    }

    public static int loadTexrute(int src, Context context) {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), src);
        if (bitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
            bitmap = null;
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            return textureIds[0];
        } else {
            return 0;
        }

    }

}
