package com.wuqingsen.openglrecordvideowu.egl;

import android.opengl.EGL14;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * @author liuml
 * @explain
 * @time 2018/11/30 15:19
 */
public class EglHelper {

    //仿照源码 里面的EglHelper 写  具体看 GLSurfaceView 里面 从start()方法 开始
    private EGL10 mEgl;
    private EGLDisplay mEglDisplay;//默认的显示设备
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;

    public void initEgl(Surface surface, EGLContext eglContext) {
        //1.得到Egl实例
        mEgl = (EGL10) EGLContext.getEGL();

        //2.得到默认的显示设备（就是窗口）
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);


        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        //3.初始化默认显示设备
        int[] version = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed");
        }

        //4.配置属性自己写  设置显示设备的属性
        int[] attrbutes = new int[]{
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 8,
                EGL10.EGL_STENCIL_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE
        };
        //EGL10.EGL_NONE结尾
//        EGL_DEPTH_SIZE, 24, //请求深度缓冲区
//        EGL_STENCIL_SIZE, 8,//请求模版缓冲区
        int[] num_config = new int[1];
        if (!mEgl.eglChooseConfig(mEglDisplay, attrbutes, null, 1, num_config)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }


        //5.从系统中获取对应属性的配置
        int numConfigs = num_config[0];
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!mEgl.eglChooseConfig(mEglDisplay, attrbutes, configs, numConfigs,
                num_config)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }


        //6.创建EglContext

        //需要加参数 参数里面有版本号 如果不加参数
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        };
        if (eglContext != null) {
            mEglContext = mEgl.eglCreateContext(mEglDisplay, configs[0], eglContext, attrib_list);
        } else {//如果没有就创建
            mEglContext = mEgl.eglCreateContext(mEglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, attrib_list);
        }

        //7.创建渲染的Surface
        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, configs[0], surface, null);

        //8.绑定EglContext和Surface到显示设备中
        if(!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)){
            throw new RuntimeException("eglMakeCurrent fail");
        }



    }

    //9.刷新数据，显示渲染场景
    public boolean swapBuffers() {
        if (mEgl != null) {
           return mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
        } else {
            throw new RuntimeException("egl is null ");
        }
    }

    public EGLContext getmEglContext() {
        return mEglContext;
    }

    //回收s
    public void destoryEgl(){
        if (mEgl != null) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT);
            mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
            mEglSurface = null;

            mEgl.eglDestroyContext(mEglDisplay, mEglContext);
            mEglContext = null;

            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = null;
            mEgl = null;
        }
    }

}
