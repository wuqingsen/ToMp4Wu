package com.wuqingsen.opengllearn.aaa;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * wuqingsen on 2021/1/11
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class ARenderer implements GLSurfaceView.Renderer {

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //设置背景清除颜色为红色。
        //第一个分量是红色的，第二个是绿色的，第三个是蓝色的，最后一个分量是alpha。
        glClearColor(1.0f,0.0f,0.0f,0.0f);
    }

    /**
     * 当表面发生变化时，onSurfaceChanged被调用。
     * 这个函数在曲面初始化时至少被调用一次。
     * 请记住，Android通常会在旋转时重启一个活动，在这种情况下，渲染器将被销毁并创建一个新的。
     * @param gl
     * @param width 新的宽度，以像素为单位。
     * @param height 新的高度，以像素为单位。
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置OpenGL视口填充整个表面。
        glViewport(0,0,width,height);
    }

    /**
     * 每当需要绘制一个新帧时，OnDrawFrame就会被调用。通常，这是在屏幕的刷新率下完成的。
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        //清除渲染表面。
        glClear(GL_COLOR_BUFFER_BIT);
    }
}
