package com.wuqingsen.opengllearn.ddd;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.wuqingsen.opengllearn.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * wuqingsen on 2021/1/11
 * Mailbox:1243411677@qq.com
 * annotation:渲染器
 */
public class DRenderer implements GLSurfaceView.Renderer {

    private static final int POSITION_COMOPNENT_COUNT = 2;
    private final FloatBuffer vertexData;//缓冲区
    private Context context;
    private static final int BYTES_PER_FLOAT = 4;
    private int program;
    private int aPositionLocation;
    private static final String A_COLOR = "a_Color";
    private static final int COLOR_COMPONENT_CONT = 3;
    private static final int STRIDE = (POSITION_COMOPNENT_COUNT + COLOR_COMPONENT_CONT) *
            BYTES_PER_FLOAT;
    private int aColorLocation;

    private static final String U_MATRIX = "u_Matrix";
    private final float[] projectionMatrix = new float[16];
    private int uMatrixLocation;


    /**
     * 1.添加坐标点; OpenGl中，只能绘制点，直线和三角形
     * 在定义三角形时，我们总是以逆时针的顺序排列顶点，这称为卷曲顺序。
     */
    float[] tableVertices = {
            // Order of coordinates: X, Y, R, G, B

            // Triangle Fan
            0f, 0f, 1f, 1f, 1f,
            -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
            0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
            0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
            -0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
            -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,

            // Line 1
            -0.5f, 0f, 1f, 0f, 0f,
            0.5f, 0f, 1f, 0f, 0f,

            // Mallets
            0f, -0.25f, 0f, 0f, 1f,
            0f, 0.25f, 1f, 0f, 0f
    };

    public DRenderer(Context context) {
        this.context = context;
        //ByteBuffer.allocateDirect 分配一块内存；参数是分配多少字节的内存块。
        //order 季节缓冲区按照本地字节序组织它的内容。
        vertexData = ByteBuffer.allocateDirect(tableVertices.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(tableVertices);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.w("wqs", "onSurfaceCreated");
        //设置背景清除颜色为红色。
        //第一个分量是红色的，第二个是绿色的，第三个是蓝色的，最后一个分量是alpha。
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        //1.读取顶点着色器
        String vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.d_vertex_shader);
        //读取片段着色器
        String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.d_fragment_shader);
        Log.w("wqs", "onSurfaceCreated，第1步");

        //2.创建顶点着色器
        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        //创建片段着色器
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
        Log.w("wqs", "onSurfaceCreated，第2步");

        //3.连接两个着色器
        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);
        Log.w("wqs", "onSurfaceCreated，第3步");

        //4.验证该对象 program 是否可用
        ShaderHelper.validateProgram(program);
        Log.w("wqs", "onSurfaceCreated，第4步");

        //5.使用自定义的程序来绘制
        GLES20.glUseProgram(program);

        /**
         * 6.获取着色器并且为着色器赋值
         */
        aColorLocation = glGetAttribLocation(program, A_COLOR);
        //获取顶点着色器 a_Position 对象的指针
        aPositionLocation = glGetAttribLocation(program, "a_Position");
        Log.w("wqs", "onSurfaceCreated，第5步");

        /**
         * 6.为着色器赋值
         */
        vertexData.position(0);//将位置设置在数据的开头处
        glVertexAttribPointer(aPositionLocation, POSITION_COMOPNENT_COUNT,
                GL_FLOAT, false, STRIDE, vertexData);
        //7.指定OpenGL在哪使用顶点数组
        glEnableVertexAttribArray(aPositionLocation);
        Log.w("wqs", "onSurfaceCreated，第6步");

        /**
         * 注释
         * 1.vertexData.position
         * vertexData将位置设置为2,因为读取颜色属性时，要从第一个颜色属性读取，而不是位置属性，
         * 而在原数据中，颜色属性的位置是2
         * 2.glVertexAttribPointer
         * 把颜色数据和着色器中的a_Color关联起来,STRIDE这个参数是跨距，这个值告诉OpenGL两个
         * 颜色属性质检的距离是多少，这样位置属性和颜色属性连接存储，就不会将位置属性当做颜色属性读
         * 3.glEnableVertexAttribArray
         * 指定OpenGL在哪使用顶点数组
         */
        vertexData.position(POSITION_COMOPNENT_COUNT);
        glVertexAttribPointer(aColorLocation, COLOR_COMPONENT_CONT, GL_FLOAT,
                false, STRIDE, vertexData);
        glEnableVertexAttribArray(aColorLocation);
        Log.w("wqs", "onSurfaceCreated，第7步");

        uMatrixLocation = glGetUniformLocation(program,U_MATRIX);

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
        glViewport(0, 0, width, height);

        //创建一个正交投影
        final float aspectRatio = width > height ? (float) width / (float) height : (float) height / (float) width;

        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        }
    }

    /**
     * 每当需要绘制一个新帧时，OnDrawFrame就会被调用。通常，这是在屏幕的刷新率下完成的。
     *
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        //清除渲染表面。
        glClear(GL_COLOR_BUFFER_BIT);

        //将刚定义的矩阵传递给着色器
        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        /**
         * 8.绘制页面,画桌子,画线,点;
         * glUniform4f：第一个参数指定Uniform变量的值,后面是红绿蓝和透明值
         * glDrawArrays：第一个参数绘制三角形，第二个参数从数组开头开始读取顶点，第三个参数读到第六个点
         */
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6);

        //绘制线
        glDrawArrays(GL_LINES, 6, 2);

        //画点
        glDrawArrays(GL_POINTS, 8, 1);

        glDrawArrays(GL_POINTS, 9, 1);
    }

}
