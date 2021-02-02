package com.wuqingsen.opengllearn.ccc;

import android.opengl.GLES20;
import android.util.Log;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

/**
 * wuqingsen on 2021/1/11
 * Mailbox:1243411677@qq.com
 * annotation:创建顶点着色器和片段着色器
 */
public class ShaderHelper {
    private static final String TAG = "ShaderHelper";

    /** 加载和编译一个顶点着色器，返回OpenGL对象ID */
    public static int compileVertexShader(String shaderCode) {
        return compileShader(GL_VERTEX_SHADER, shaderCode);
    }

    /** 加载和编译一个片段着色器，返回OpenGL对象ID */
    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GL_FRAGMENT_SHADER, shaderCode);
    }

    /** 编译一个着色器，返回OpenGL对象ID */
    private static int compileShader(int type, String shaderCode) {

        //创建一个新的着色器对象
        final int shaderObjectId = glCreateShader(type);

        if (shaderObjectId == 0) {
            Log.w("wqs", "ShaderHelper: Could not create new shader.");
        }

        //传入着色器源
        glShaderSource(shaderObjectId, shaderCode);

        //编译着色器
        glCompileShader(shaderObjectId);

        //获取编译状态
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);

        //验证编译状态
        if (compileStatus[0] == 0) {
            //如果失败，删除着色器对象
            glDeleteShader(shaderObjectId);
            Log.w("wqs", "ShaderHelper: Compilation of shader failed");
        }

        //返回着色器对象ID
        return shaderObjectId;
    }

    /** 将顶点着色器和片段着色器连接到OpenGL中，返回OpenGL对象ID，连接失败返回0 */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {

        //创建一个新的程序对象
        final int programObjectId = glCreateProgram();

        if (programObjectId == 0) {
            Log.w("wqs", "ShaderHelper: Could not create new program");
        }

        //将顶点着色器附加到程序上
        glAttachShader(programObjectId, vertexShaderId);
        //将片段着色器附加到程序上
        glAttachShader(programObjectId, fragmentShaderId);

        //将两个着色器连接到一个程序中
        glLinkProgram(programObjectId);

        //获取连接状态
        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);

        //验证链接状态
        if (linkStatus[0] == 0) {
            //如果失败，删除程序对象
            glDeleteProgram(programObjectId);
            Log.v("wqs", "Results of linking program:\n" + GLES20.glGetProgramInfoLog(programObjectId));
        }

        //返回程序对象ID
        return programObjectId;
    }

    /** 验证OpenGL程序，应该只在开始应用程序调用 */
    public static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);

        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);

        return validateStatus[0] != 0;
    }
}
