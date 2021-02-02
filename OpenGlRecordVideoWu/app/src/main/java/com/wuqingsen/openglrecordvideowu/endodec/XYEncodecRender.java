package com.wuqingsen.openglrecordvideowu.endodec;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.wuqingsen.openglrecordvideowu.R;
import com.wuqingsen.openglrecordvideowu.camera.XYCameraView;
import com.wuqingsen.openglrecordvideowu.egl.XYEGLSurfaceView;
import com.wuqingsen.openglrecordvideowu.utils.Constants;
import com.wuqingsen.openglrecordvideowu.utils.LogUtil;
import com.wuqingsen.openglrecordvideowu.utils.XYShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author liuml
 * @explain 录制时渲染的Rendeer
 * @time 2018/12/11 16:43
 */
public class XYEncodecRender implements XYEGLSurfaceView.XYGLRender  {

    private boolean isAddMark = false;//是否添加水印

    private Context context;
    private float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,

            0f, 0f,
            0f, 0f,
            0f, 0f,
            0f, 0f
    };


    private float[] fragmentData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f

    };
    private FloatBuffer fragmentBuffer;
    private FloatBuffer vertexBuffer;

    private int vboId;

    private int program;
    private int vPosition;
    private int fPosition;
    private int textureId;


    private Bitmap bitmap;

    private int bitmapTextureid = 0;

    public XYEncodecRender(Context context, int textureId) {

        this.context = context;
        this.textureId = textureId;


        addWaterMark();

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        fragmentBuffer = ByteBuffer.allocateDirect(fragmentData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(fragmentData);
        fragmentBuffer.position(0);
    }

    private void addWaterMark() {
        //水印Lml水印搞起了录制的
        bitmap = XYShaderUtil.getCommon(Constants.addMarkText);
//        bitmap = X
        //求出宽高比例
        float r = 1.0f * bitmap.getWidth() / bitmap.getHeight();
        //高设置成0.1
        float w = r * 0.1f;
        //在opengl 坐标系中.0.8f是自己设置的起始点, 这里求出左下角X轴   后面需要传递进来改变
        vertexData[8] = 0.8f - w;
        vertexData[9] = -0.8f;//左下角Y轴  这样左下角就求出来了
        //同理
        vertexData[10] = 0.8f;
        vertexData[11] = -0.8f;

        vertexData[12] = 0.8f - w;
        vertexData[13] = -0.7f;

        vertexData[14] = 0.8f;
        vertexData[15] = -0.7f;
    }


    @Override
    public void onSurfaceCreated() {

        //用于透明
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        String vertexSource = XYShaderUtil.getRawResource(context, R.raw.vertex_shader_fbo);
        String fragmentSource = XYShaderUtil.getRawResource(context, R.raw.fragment_shader_fbo);

        program = XYShaderUtil.createProgram(vertexSource, fragmentSource);


        vPosition = GLES20.glGetAttribLocation(program, "v_Position");
        fPosition = GLES20.glGetAttribLocation(program, "f_Position");

        int[] vbos = new int[1];
//        1、创建VBO
        GLES20.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];

        //2.绑定VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

        //3. 分配VBO需要的缓冲大小
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20.GL_STATIC_DRAW);

        //4,为VBO设置顶点数据的值(这里想象内存区域 先偏移0用于存储顶点坐标,再偏移顶点坐标的大小,用于存储片元坐标) 大小是顶点坐标加上片元坐标大小
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
//        5、解绑VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        //bitmap 获取纹理id

        bitmapTextureid = XYShaderUtil.loadBitmapTexture(bitmap);
        LogUtil.d("onSurfaceCreated 水印的值 " + isAddMark);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        GLES20.glViewport(0, 0, width, height);

    }

    @Override
    public void onDrawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1f, 0f, 0f, 1f);

        GLES20.glUseProgram(program);

        //先设置vbo 再绑定纹理
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

        //fbo
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8,
                0);

        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8,
                vertexData.length * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 绘制bitmap
//        LogUtil.d("绘制的时候 ");
        if (isAddMark) {

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTextureid);

            GLES20.glEnableVertexAttribArray(vPosition);
            GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8,
                    32);//偏移32个位置   float  32字节

            GLES20.glEnableVertexAttribArray(fPosition);
            GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8,
                    vertexData.length * 4);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }
        isAddMark = XYCameraView.isAddMark;
    }

    @Override
    public void changeRender() {

    }

}
