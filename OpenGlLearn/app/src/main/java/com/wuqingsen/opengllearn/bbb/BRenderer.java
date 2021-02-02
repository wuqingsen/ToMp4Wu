package com.wuqingsen.opengllearn.bbb;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
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
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * wuqingsen on 2021/1/11
 * Mailbox:1243411677@qq.com
 * annotation:渲染器
 */
public class BRenderer implements GLSurfaceView.Renderer {

    private static final int POSITION_COMPONENT_COUNT = 2;
    private final FloatBuffer vertexData;//缓冲区
    private Context context;
    private static final int BYTES_PER_FLOAT = 4;
    private int program;
    private int uColorLocation;
    private int aPositionLocation;


    /**
     * 1.添加坐标点; OpenGl中，只能绘制点，直线和三角形
     * 在定义三角形时，我们总是以逆时针的顺序排列顶点，这称为卷曲顺序。
     */
    float[] tableVertices = {
            // Triangle 1
            -0.5f, -0.5f,
            0.5f, 0.5f,
            -0.5f, 0.5f,

            // Triangle 2
            -0.5f, -0.5f,
            0.5f, -0.5f,
            0.5f, 0.5f,

            // Line 1
            -0.5f, 0f,
            0.5f, 0f,

            // Mallets
            0f, -0.25f,
            0f, 0.25f
    };

    public BRenderer(Context context) {
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
        String vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.b_vertex_shader);
        //读取片段着色器
        String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.b_fragment_shader);
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
        //获取片段着色器 u_Color 对象的指针
        uColorLocation = glGetUniformLocation(program, "u_Color");
        //获取顶点着色器 a_Position 对象的指针
        aPositionLocation = glGetAttribLocation(program, "a_Position");
        Log.w("wqs", "onSurfaceCreated，第5步");

        /**
         * 6.为着色器赋值
         * glVertexAttribPointer 指定渲染时索引值为index的顶点属性数组的数据格式和位置(简单理解为定义顶点着色器的参数)
         * 参数说明:
         * index:指定要修改的顶点属性的索引值
         * size:指定每个顶点的组件数量，必须为1,2,3或4
         * type:数据中每个组件的数据类型
         * normalized:放呗访问时，固定点数据值是否被归一化(GL_TRUE)或直接转换为固定点值(GL_FALSE)
         * stride:指定连续顶点属性之间的偏移量，为0，则紧密排列在一起
         * pointer:第一个组件在数组的第一个顶点属性中的偏移量，该数组与GL_ARRAY_BUFFER绑定，在缓冲区中
         */
        vertexData.position(0);//将位置设置在数据的开头处
        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT,
                GL_FLOAT, false, 0, vertexData);
        Log.w("wqs", "onSurfaceCreated，第6步");

        //7.指定OpenGL在哪使用顶点数组
        glEnableVertexAttribArray(aPositionLocation);
        Log.w("wqs", "onSurfaceCreated，第7步");

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

        /**
         * 8.绘制页面,画桌子,画线,点;
         * glUniform4f：第一个参数指定Uniform变量的值,后面是红绿蓝和透明值
         * glDrawArrays：第一个参数绘制三角形，第二个参数从数组开头开始读取顶点，第三个参数读到第六个点
         */
        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        glDrawArrays(GL_TRIANGLES, 0, 6);

        //绘制线
        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        glDrawArrays(GL_LINES,6,2);

        //画点
        glUniform4f(uColorLocation,1.0f,1.0f,1.0f,0.0f);
        glDrawArrays(GL_POINTS,8,1);

        glUniform4f(uColorLocation,1.0f,0.0f,0.0f,1.0f);
        glDrawArrays(GL_POINTS,9,1);
    }

}
