package com.wuqingsen.opengllearn.ggg.data;

import android.opengl.GLES20;

import com.wuqingsen.opengllearn.ggg.data.Constands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * wuqingsen on 2021/2/2
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class VertexArray {
    private final FloatBuffer floatBuffer;

    public VertexArray(float[] vertexData) {
        floatBuffer = ByteBuffer.allocateDirect(vertexData.length * Constands.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
    }

    public void setVertexAttribPointer(int dataOffset, int attributeLocation
            , int componentCount, int stride) {
        //将位置设置在数据的开头处
        floatBuffer.position(dataOffset);
        //为着色器赋值
        GLES20.glVertexAttribPointer(attributeLocation, componentCount,
                GLES20.GL_FLOAT, false,stride,floatBuffer);
        //指定OpenGL在哪使用顶点数组
        GLES20.glEnableVertexAttribArray(attributeLocation);
        //将位置设置在数据的开头处
        floatBuffer.position(0);
    }
}
