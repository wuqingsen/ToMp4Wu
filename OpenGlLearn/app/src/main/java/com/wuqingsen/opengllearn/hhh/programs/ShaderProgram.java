package com.wuqingsen.opengllearn.hhh.programs;

import android.content.Context;
import android.opengl.GLES20;

import com.wuqingsen.opengllearn.hhh.utils.ShaderHelper;
import com.wuqingsen.opengllearn.hhh.utils.TextResourceReader;

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

    protected static final String U_COLOR = "u_Color";

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
