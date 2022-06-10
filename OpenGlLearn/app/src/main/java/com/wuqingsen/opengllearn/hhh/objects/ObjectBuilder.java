package com.wuqingsen.opengllearn.hhh.objects;

import android.opengl.GLES20;

import com.wuqingsen.opengllearn.hhh.utils.Geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * wuqingsen on 2021/2/4
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class ObjectBuilder {
    private static final int FLOATS_PER_VERTEX = 3;
    private final float[] vertexData;
    private int offset = 0;
    private final List<DrawCommand> drawList = new ArrayList<>();

    private ObjectBuilder(int sizeInVertices) {
        vertexData = new float[FLOATS_PER_VERTEX * sizeInVertices];
    }

    private static int sizeOfCircleInVertices(int numPoints) {
        return (numPoints + 1) + 1;
    }

    private static int sizeOfOpenCylinderInVertices(int numPoints) {
        return (numPoints + 1) * 2;
    }

    //创建冰球
    static GeneratedData createPuck(Geometry.Cylinder puck, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints) + sizeOfOpenCylinderInVertices(numPoints);

        ObjectBuilder builder = new ObjectBuilder(size);
        Geometry.Circle puckTop =
                new Geometry.Circle(puck.center.translateY(puck.height / 2f), puck.radius);

        builder.appendCircle(puckTop, numPoints);
        builder.appendOpenCylinder(puck, numPoints);

        return builder.build();
    }

    //创建木槌
    static GeneratedData createMallet(Geometry.Point center, float radius, float height,
                                      int numPoints) {
        int size = sizeOfCircleInVertices(numPoints) * 2 + sizeOfOpenCylinderInVertices(numPoints) * 2;
        ObjectBuilder builder = new ObjectBuilder(size);
        float baseHeight = height * 0.25f;
        Geometry.Circle baseCircle = new Geometry.Circle(center.translateY(-baseHeight), radius);
        Geometry.Cylinder baseCylinder =
                new Geometry.Cylinder(baseCircle.center.translateY(-baseHeight / 2f), radius, baseHeight);
        builder.appendCircle(baseCircle, numPoints);
        builder.appendOpenCylinder(baseCylinder, numPoints);

        float handleHeight = height * 0.75f;
        float handleRadius = radius / 3f;

        Geometry.Circle handleCircle =
                new Geometry.Circle(center.translateY(height * 0.5f), handleRadius);
        Geometry.Cylinder handleCylinder =
                new Geometry.Cylinder(handleCircle.center.translateY(-handleHeight / 2f)
                        , handleRadius, handleHeight);

        builder.appendCircle(handleCircle, numPoints);
        builder.appendOpenCylinder(handleCylinder, numPoints);

        return builder.build();
    }

    //创建冰球顶部
    private void appendCircle(Geometry.Circle circle, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfCircleInVertices(numPoints);

        //风扇中心点
        vertexData[offset++] = circle.center.x;
        vertexData[offset++] = circle.center.y;
        vertexData[offset++] = circle.center.z;

        //绕中心点扇形。使用<=是因为我们想在起始角度生成两个点来完成风扇。
        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians = ((float) i / (float) numPoints) * ((float) Math.PI * 2f);

            vertexData[offset++] =
                    circle.center.x + circle.radius * (float) Math.cos(((double) angleInRadians));
            vertexData[offset++] = circle.center.y;
            vertexData[offset++] =
                    circle.center.z + circle.radius * (float) Math.sin(((double) angleInRadians));
        }

        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, startVertex, numVertices);
            }
        });
    }

    //冰球的侧面
    private void appendOpenCylinder(Geometry.Cylinder cylinder, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfOpenCylinderInVertices(numPoints);
        final float yStart = cylinder.center.y - (cylinder.height / 2f);
        final float yEnd = cylinder.center.y + (cylinder.height / 2f);

        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians = ((float) i / (float) numPoints) * ((float) Math.PI * 2F);
            float xPosition =
                    cylinder.center.x + cylinder.radius * (float) Math.cos((double) angleInRadians);
            float zPosition =
                    cylinder.center.z + cylinder.radius * (float) Math.sin((double) angleInRadians);

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yStart;
            vertexData[offset++] = zPosition;

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yEnd;
            vertexData[offset++] = zPosition;
        }

        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, startVertex, numVertices);
            }
        });
    }

    static interface DrawCommand {
        void draw();
    }

    public static class GeneratedData {
        final float[] vertexData;
        final List<DrawCommand> drawList;

        GeneratedData(float[] vertexData, List<DrawCommand> drawList) {
            this.vertexData = vertexData;
            this.drawList = drawList;
        }
    }

    private GeneratedData build() {
        return new GeneratedData(vertexData, drawList);
    }
}
