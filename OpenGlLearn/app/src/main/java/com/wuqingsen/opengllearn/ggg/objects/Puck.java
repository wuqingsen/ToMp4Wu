package com.wuqingsen.opengllearn.ggg.objects;

import com.wuqingsen.opengllearn.ggg.data.VertexArray;
import com.wuqingsen.opengllearn.ggg.programs.ColorShaderProgram;
import com.wuqingsen.opengllearn.ggg.utils.Geometry;

import java.util.List;

/**
 * wuqingsen on 2021/2/7
 * Mailbox:1243411677@qq.com
 * annotation:冰球
 */
public class Puck {
    private static final int POSITION_COMPONENT_COUNT = 3;
    public final float radius, height;
    private final VertexArray vertexArray;
    private final List<ObjectBuilder.DrawCommand> drawList;

    public Puck(float radius, float height, int numPointsAroundPuck) {
        ObjectBuilder.GeneratedData generatedData = ObjectBuilder.createPuck(
                new Geometry.Cylinder(new Geometry.Point(0f, 0f, 0f), radius, height), numPointsAroundPuck);
        this.radius = radius;
        this.height = height;

        vertexArray = new VertexArray((generatedData.vertexData));
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
