package com.wuqingsen.opengllearn.ggg.programs;

import android.content.Context;
import android.opengl.GLES20;

import com.wuqingsen.opengllearn.R;

/**
 * wuqingsen on 2021/1/26
 * Mailbox:1243411677@qq.com
 * annotation:纹理着色器
 */
public class TextureShaderProgram extends ShaderProgram {
    private final int uMatrixLocation;
    private final int uTextureUnitLocation;

    private final int aPositionLocation;
    private final int aTextureCoordnatesLocation;

    public TextureShaderProgram(Context context) {
        super(context, R.raw.g_texture_vertex_shader, R.raw.g_texture_fragment_shader);
        uMatrixLocation = GLES20.glGetUniformLocation(program, U_MATRIX);
        uTextureUnitLocation = GLES20.glGetUniformLocation(program, U_TEXTURE_UNIT);

        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);
        aTextureCoordnatesLocation = GLES20.glGetAttribLocation(program, A_TEXTURE_COORDINATES);
    }

    @Override
    public void userProgram() {
        super.userProgram();
    }

    public void setUniforms(float[] matrix, int textureId) {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureUnitLocation,0);
    }

    public int getPositionAttributeLocation(){
        return aPositionLocation;
    }

    public int getTextureCoordinatesAttributeLocation(){
        return aTextureCoordnatesLocation;
    }
}
