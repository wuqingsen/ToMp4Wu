package com.wuqingsen.openglrecordmp4wu.utils.filter;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.wuqingsen.openglrecordmp4wu.utils.common.EasyGlUtils;
import com.wuqingsen.openglrecordmp4wu.utils.common.MatrixUtils;


/**
 * draw并不执行父类的draw方法,所以矩阵对它无效
 * Description:
 */
public class ProcessFilter extends AFilter {

    private AFilter mFilter;
    //创建离屏buffer
    private int[] fFrame = new int[1];
    private int[] fRender = new int[1];
    private int[] fTexture = new int[1];

    private int width;
    private int height;


    public ProcessFilter(Resources mRes) {
        super(mRes);
        mFilter=new NoFilter(mRes);
        float[]  OM= MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM,false,true);//矩阵上下翻转
        mFilter.setMatrix(OM);
    }

    @Override
    protected void initBuffer() {

    }

    @Override
    protected void onCreate() {
        mFilter.create();
    }

    @Override
    public int getOutputTexture() {
        return fTexture[0];
    }

    @Override
    public void draw() {
        boolean b= GLES20.glIsEnabled(GLES20.GL_CULL_FACE);
        if(b){
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }
        GLES20.glViewport(0,0,width,height);
        EasyGlUtils.bindFrameTexture(fFrame[0],fTexture[0]);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_RENDERBUFFER, fRender[0]);
        mFilter.setTextureId(getTextureId());
        mFilter.draw();
        EasyGlUtils.unBindFrameBuffer();
        if(b){
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        if(this.width!=width&&this.height!=height){
            this.width=width;
            this.height=height;
            mFilter.setSize(width,height);
            deleteFrameBuffer();
            GLES20.glGenFramebuffers(1,fFrame,0);
            GLES20.glGenRenderbuffers(1,fRender,0);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,fRender[0]);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                width, height);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER,0);
            EasyGlUtils.genTexturesWithParameter(1,fTexture,0, GLES20.GL_RGBA,width,height);
        }
    }

    private void deleteFrameBuffer() {
        GLES20.glDeleteRenderbuffers(1, fRender, 0);
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);
    }

}
