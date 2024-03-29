package com.wuqingsen.opengllearn.ggg.objects;

import android.opengl.GLES20;

import com.wuqingsen.opengllearn.ggg.data.VertexArray;
import com.wuqingsen.opengllearn.ggg.data.Constands;
import com.wuqingsen.opengllearn.ggg.programs.ColorShaderProgram;
import com.wuqingsen.opengllearn.ggg.utils.Geometry;

import java.util.List;

/**
 * wuqingsen on 2021/2/2
 * Mailbox:1243411677@qq.com
 * annotation:木槌
 */
public class Mallet {
    private static final int POSITION_COMPONENT_COUNT = 3;

    public final float radius;
    public final float height;

    private final VertexArray vertexArray;

    private final List<ObjectBuilder.DrawCommand> drawList;

    public Mallet(float radius, float height, int numPointsAroundMallet) {
        ObjectBuilder.GeneratedData generatedData =
                ObjectBuilder.createMallet(new Geometry.Point(0f, 0f, 0f)
                        , radius, height, numPointsAroundMallet);

        this.radius = radius;
        this.height = height;

        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
    }

    public void bindData(ColorShaderProgram colorShaderProgram) {
        vertexArray.setVertexAttribPointer(0, colorShaderProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT, 0);
    }

    public void draw() {
        for (ObjectBuilder.DrawCommand drawCommand : drawList) {
            drawCommand.draw();
        }
    }


}
