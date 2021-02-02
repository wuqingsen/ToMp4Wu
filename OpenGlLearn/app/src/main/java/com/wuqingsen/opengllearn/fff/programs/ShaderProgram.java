package com.wuqingsen.opengllearn.fff.programs;

import android.content.Context;
import android.opengl.GLES20;

import com.wuqingsen.opengllearn.fff.ShaderHelper;
import com.wuqingsen.opengllearn.fff.TextResourceReader;

/**
 * wuqingsen on 2021/1/26
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class ShaderProgram {
    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";

    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    protected final int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        program = ShaderHelper.buildProgram(
                TextResourceReader.readTextFileFromResource(context, vertexShaderResourceId),
                TextResourceReader.readTextFileFromResource(context, fragmentShaderResourceId)
        );
    }

    public void userProgram(){
        GLES20.glUseProgram(program);
    }
}
