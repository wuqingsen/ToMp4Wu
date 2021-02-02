package com.wuqingsen.openglrecordvideowu.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import com.wuqingsen.openglrecordvideowu.R;
import com.wuqingsen.openglrecordvideowu.utils.Constants;
import com.wuqingsen.openglrecordvideowu.utils.LogUtil;
import com.wuqingsen.openglrecordvideowu.utils.XYShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author liuml
 * @explain 离屏的fboRender
 * @time 2018/12/4 15:06
 */
public class XYCameraFboRender {

    private int oldSrc = -1;
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
    private int textureid;
    private int sampler;

    private Bitmap bitmap;

    private int bitmapTextureid;
    private float r;//宽高比

    private boolean OpenMark = false; //是否开启水印
    private boolean dynamic = false; //是否开启水印

    public XYCameraFboRender(Context context) {
        this.context = context;
        //水印
//        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.img_1);
        bitmap = XYShaderUtil.getCommon(Constants.addMarkText);//添加水印
        initData();

    }

    public void initData() {


        //求出宽高比例
        r = 1.0f * bitmap.getWidth() / bitmap.getHeight();
        //高设置成0.1
        float w = r * 0.1f;
        //在opengl 坐标系中.0.8f是自己设置的起始点, 这里求出左下角X轴
        vertexData[8] = 0.8f - w;
        vertexData[9] = -0.8f;//左下角Y轴  这样左下角就求出来了
        //同理
        vertexData[10] = 0.8f;
        vertexData[11] = -0.8f;

        vertexData[12] = 0.8f - w;
        vertexData[13] = -0.7f;

        vertexData[14] = 0.8f;
        vertexData[15] = -0.7f;


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

    public void setWaterMarkBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;

        //bitmap 获取纹理id
        bitmapTextureid = XYShaderUtil.loadBitmapTexture(bitmap);
    }

    public void setWaterMarkBitmap(int imgsrc) {

        if (imgsrc != oldSrc) {
            oldSrc = imgsrc;
            //img 获取纹理id
//        bitmapTextureid = XYShaderUtil.loadTexrute(imgsrc,context);
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imgsrc);
            if (bitmap != null) {
                bitmapTextureid = XYShaderUtil.loadBitmapTexture(bitmap);
                LogUtil.d("img 获取纹理id = " + bitmapTextureid);
            }
        }
    }


    public void onCreate() {

        //用于透明   使用OpenGL的混合功能
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //===结束
        String vertexSource = XYShaderUtil.getRawResource(context, R.raw.vertex_shader_fbo);
        String fragmentSource = XYShaderUtil.getRawResource(context, R.raw.fragment_shader_fbo);

        program = XYShaderUtil.createProgram(vertexSource, fragmentSource);


        vPosition = GLES20.glGetAttribLocation(program, "v_Position");
        fPosition = GLES20.glGetAttribLocation(program, "f_Position");
        sampler = GLES20.glGetUniformLocation(program, "sTexture");

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
        LogUtil.d("onCreate 调用");
    }

    public void onChange(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        OpenMark = XYCameraView.isAddMark;
        dynamic = XYCameraView.isDyNamicMark;
    }



    public void onDraw(int textureId) {


        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1f, 0f, 0f, 1f);

        GLES20.glUseProgram(program);

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


        //bitmap
        if (OpenMark) {

            if(XYCameraView.isDyNamicMark){
            }
//            LogUtil.d("onDraw 调用  右下角小图  bitmapTextureid = " + bitmapTextureid);
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
    }

    public void onChangeRender(){

    }

}
