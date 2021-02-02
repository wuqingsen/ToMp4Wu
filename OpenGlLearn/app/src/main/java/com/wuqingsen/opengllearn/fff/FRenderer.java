package com.wuqingsen.opengllearn.fff;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.wuqingsen.opengllearn.R;
import com.wuqingsen.opengllearn.fff.objects.Mallet;
import com.wuqingsen.opengllearn.fff.objects.Table;
import com.wuqingsen.opengllearn.fff.programs.ColorShaderProgram;
import com.wuqingsen.opengllearn.fff.programs.TextureShaderProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.translateM;

/**
 * wuqingsen on 2021/1/11
 * Mailbox:1243411677@qq.com
 * annotation:渲染器
 */
public class FRenderer implements GLSurfaceView.Renderer {

    private Context context;
    private final float[] projectionMatrix = new float[16];

    private final float[] modelMatrix = new float[16];

    private Table table;
    private Mallet mallet;

    private TextureShaderProgram textureProgram;
    private ColorShaderProgram colorProgram;

    private int texture;

    public FRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w("wqs", "onSurfaceCreated");
        //设置背景清除颜色为红色。
        //第一个分量是红色的，第二个是绿色的，第三个是蓝色的，最后一个分量是alpha。
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        table = new Table();
        mallet = new Mallet();

        textureProgram = new TextureShaderProgram(context);
        colorProgram = new ColorShaderProgram(context);

        texture = TextureHelper.loadTexture(context,R.drawable.air_hockey_surface);
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

        Matrix.setIdentityM(modelMatrix,0);
        translateM(modelMatrix,0,0f,0f,-2.5f);
        rotateM(modelMatrix,0,-60,1f,0f,0f);

        final float[] temp = new float[16];
        multiplyMM(temp,0,projectionMatrix,0,modelMatrix,0);
        System.arraycopy(temp,0,projectionMatrix,0,temp.length);
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

        textureProgram.userProgram();
        textureProgram.setUniforms(projectionMatrix,texture);
        table.bindData(textureProgram);
        table.draw();

        colorProgram.userProgram();
        colorProgram.setUniforms(projectionMatrix);
        mallet.bindData(colorProgram);
        mallet.draw();
    }

}
