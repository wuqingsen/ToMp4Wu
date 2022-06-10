package com.wuqingsen.opengllearn.hhh;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.wuqingsen.opengllearn.R;
import com.wuqingsen.opengllearn.hhh.objects.Mallet;
import com.wuqingsen.opengllearn.hhh.objects.Puck;
import com.wuqingsen.opengllearn.hhh.objects.Table;
import com.wuqingsen.opengllearn.hhh.programs.ColorShaderProgram;
import com.wuqingsen.opengllearn.hhh.programs.TextureShaderProgram;
import com.wuqingsen.opengllearn.hhh.utils.Geometry;
import com.wuqingsen.opengllearn.hhh.utils.MatrixHelper;
import com.wuqingsen.opengllearn.hhh.utils.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;

/**
 * wuqingsen on 2021/1/11
 * Mailbox:1243411677@qq.com
 * annotation:渲染器
 */
public class HRenderer implements GLSurfaceView.Renderer {

    private Context context;
    private final float[] projectionMatrix = new float[16];

    private final float[] modelMatrix = new float[16];

    private Table table;
    private Mallet mallet;

    private TextureShaderProgram textureProgram;
    private ColorShaderProgram colorProgram;

    private int texture;

    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    private Puck puck;

    private boolean malletPressed = false;
    private Geometry.Point blueMalletPosition;
    private final float[] invertedViewProjectionMatrix = new float[16];

    private final float leftBound = -0.5f;
    private final float rightBound = 0.5f;
    private final float farBound = -0.8f;
    private final float nearBound = 0.8f;

    private Geometry.Point previousBlueMalletPosition;

    private Geometry.Point puckPosition;
    private Geometry.Vector puckVector;

    public HRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w("wqs", "onSurfaceCreated");
        //设置背景清除颜色为红色。
        //第一个分量是红色的，第二个是绿色的，第三个是蓝色的，最后一个分量是alpha。
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        table = new Table();
        mallet = new Mallet(0.08f, 0.15f, 32);
        puck = new Puck(0.06f, 0.02f, 32);

        textureProgram = new TextureShaderProgram(context);
        colorProgram = new ColorShaderProgram(context);

        texture = TextureHelper.loadTexture(context, R.drawable.air_hockey_surface);

        blueMalletPosition = new Geometry.Point(0f,mallet.height / 2f, 0.4f);
        puckPosition = new Geometry.Point(0f,puck.height / 2f,0f);
        puckVector = new Geometry.Vector(0f,0f,0f);
    }

    /**
     * 当表面发生变化时，onSurfaceChanged被调用。
     * 这个函数在曲面初始化时至少被调用一次。
     * 请记住，Android通常会在旋转时重启一个活动，在这种情况下，渲染器将被销毁并创建一个新的。
     *
     * @param gl
     * @param width  新的宽度，以像素为单位。
     * @param height 新的高度，以像素为单位。
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.w("wqs", "onSurfaceChanged");
        //设置OpenGL视口填充整个表面。
        GLES20.glViewport(0, 0, width, height);

        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, 1f, 10f);

        Matrix.setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    /**
     * 每当需要绘制一个新帧时，OnDrawFrame就会被调用。通常，这是在屏幕的刷新率下完成的。
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        //清除渲染表面。
        GLES20.glClear(GL_COLOR_BUFFER_BIT);

        puckPosition = puckPosition.translate(puckVector);

        if (puckPosition.x < leftBound + puck.radius
                || puckPosition.x > rightBound - puck.radius) {
            puckVector = new Geometry.Vector(-puckVector.x, puckVector.y, puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }
        if (puckPosition.z < farBound + puck.radius
                || puckPosition.z > nearBound - puck.radius) {
            puckVector = new Geometry.Vector(puckVector.x, puckVector.y, -puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }
        // Clamp the puck position.
        puckPosition = new Geometry.Point(
                clamp(puckPosition.x, leftBound + puck.radius, rightBound - puck.radius),
                puckPosition.y,
                clamp(puckPosition.z, farBound + puck.radius, nearBound - puck.radius)
        );

        puckVector = puckVector.scale(0.99f);

        //将视图矩阵和投影矩阵相乘。
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        Matrix.invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        //画桌子
        positionTableInScene();
        textureProgram.userProgram();
        textureProgram.setUniforms(modelViewProjectionMatrix, texture);
        table.bindData(textureProgram);
        table.draw();

        //画木槌
        positionObjectInScene(0f, mallet.height / 2f, -0.4f);
        colorProgram.userProgram();
        colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 0f, 0f);
        mallet.bindData(colorProgram);
        mallet.draw();

        positionObjectInScene(0f, mallet.height / 2f, 0.4f);
        //注意，我们不需要定义对象数据两次——我们只是在不同的位置用不同的颜色再次绘制相同的木槌。
        colorProgram.setUniforms(modelViewProjectionMatrix, 0f, 0f, 1f);
        mallet.draw();

        //画冰球
        positionObjectInScene(0f, puck.height / 2f, 0f);
        colorProgram.setUniforms(modelViewProjectionMatrix, 0.8f, 0.8f, 1f);
        puck.bindData(colorProgram);
        puck.draw();
    }

    private void positionTableInScene() {
        //这个表是用X和Y坐标定义的，所以我们将它旋转90度，使它平躺在XZ平面上。
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    private void positionObjectInScene(float x, float y, float z) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);

        Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(
                new Geometry.Point(blueMalletPosition.x,blueMalletPosition.y,blueMalletPosition.z),
                mallet.height / 2f);

        malletPressed = Geometry.intersects(malletBoundingSphere, ray);
    }

    private Geometry.Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY) {
        final float[] nearPointNdc = {
                normalizedX, normalizedY, -1, 1
        };
        final float[] farPointNdc = {
                normalizedX, normalizedY, 1, 1
        };

        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];

        Matrix.multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);

        Matrix.multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);

        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        Geometry.Point nearPointRay =
                new Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
        Geometry.Point farPointRay =
                new Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

        return new Geometry.Ray(nearPointRay, Geometry.vectorBetween(nearPointRay,farPointRay));
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    public void handleTouchDrag(float normalizedX, float normalizedY){
        if (malletPressed){
            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
            Geometry.Plane plane =
                    new Geometry.Plane(new Geometry.Point(0,0,0),new Geometry.Vector(0,1,0));
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            previousBlueMalletPosition = blueMalletPosition;
            blueMalletPosition = new Geometry.Point(
                    clamp(touchedPoint.x, leftBound + mallet.radius, rightBound - mallet.radius),
                    mallet.height / 2f,clamp(touchedPoint.z,0f+mallet.radius,nearBound - mallet.radius));

            float distance = Geometry.vectorBetween(blueMalletPosition, puckPosition).length();

            if (distance<(puck.radius + mallet.radius)){
                puckVector = Geometry.vectorBetween(previousBlueMalletPosition,blueMalletPosition);
            }
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }
}
