package com.wuqingsen.opengllearn.hhh.objects;

import android.opengl.GLES20;

import com.wuqingsen.opengllearn.hhh.data.Constands;
import com.wuqingsen.opengllearn.hhh.data.VertexArray;
import com.wuqingsen.opengllearn.hhh.programs.TextureShaderProgram;

/**
 * wuqingsen on 2021/2/2
 * Mailbox:1243411677@qq.com
 * annotation:桌子
 */
public class Table {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE =
            (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * Constands.BYTES_PER_FLOAT;

    private static final float[] VERTEX_DATA = {
            0f,  0f, 0.5f, 0.5f,
            -0.5f,-0.8f, 0f, 0.9f,
            0.5f,-0.8f, 1f, 0.9f,
            0.5f, 0.8f, 1f, 0.1f,
            -0.5f, 0.8f, 0f, 0.1f,
            -0.5f,-0.8f, 0f, 0.9f
    };

    private final VertexArray vertexArray;

    public Table(){
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    public void bindData(TextureShaderProgram textureProgram){
        vertexArray.setVertexAttribPointer(0,textureProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,STRIDE);
        vertexArray.setVertexAttribPointer(POSITION_COMPONENT_COUNT,
                textureProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT, STRIDE);

    }

    public void draw(){
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN,0,6);
    }
}
