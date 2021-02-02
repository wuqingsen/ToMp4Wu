package com.wuqingsen.opengllearn.ggg.programs;

import android.content.Context;
import android.opengl.GLES20;

import com.wuqingsen.opengllearn.R;

/**
 * wuqingsen on 2021/2/2
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class ColorShaderProgram extends ShaderProgram {
    private final int uMatrixLocation;
    private final int aPositionLocation;
    private final int aColorLocation;

    public ColorShaderProgram(Context context){
        super(context, R.raw.g_vertex_shader,R.raw.g_fragment_shader);
        uMatrixLocation = GLES20.glGetUniformLocation(program,U_MATRIX);

        aPositionLocation = GLES20.glGetAttribLocation(program,A_POSITION);
        aColorLocation = GLES20.glGetAttribLocation(program,A_COLOR);

    }

    @Override
    public void userProgram() {
        super.userProgram();
    }

    public void setUniforms(float[] matrix){
        GLES20.glUniformMatrix4fv(uMatrixLocation,1,false,matrix,0);
    }

    public int getPositionAttributeLocation(){
        return aPositionLocation;
    }

    public int getColorAttributeLocation(){
        return aColorLocation;
    }
}
