package com.wuqingsen.openglrecordvideowu.egl;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGLContext;

/**
 * @author liuml
 * @explain 仿照GLSurface 写的
 * @time 2018/12/3 10:56
 */
public abstract class XYEGLSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private Surface surface;
    private EGLContext eglContext;

    private XYEGLThread xyEGLThread;

    private XYGLRender xyGLRender;

    //控制手动刷新还是自动刷新
    public final static int RENDERMODE_WHEN_DIRTY = 0;//手动
    public final static int RENDERMODE_CONTINUOUSLY = 1;

    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    public XYEGLSurfaceView(Context context) {
        this(context, null);
    }

    public XYEGLSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XYEGLSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }


    public void setRender(XYGLRender xyglRender) {
        this.xyGLRender = xyglRender;
    }

    public void setRenderMode(int mRenderMode) {
        if (xyGLRender == null) {
            throw new RuntimeException("must set render befo");
        }
        this.mRenderMode = mRenderMode;
    }

    /**
     * 继承XYGLsurfaceview 后调用这个方法 surface就不为空
     *
     * @param surface
     * @param eglContext
     */
    public void setSurfaceAndEglContext(Surface surface, EGLContext eglContext) {
        this.surface = surface;
        this.eglContext = eglContext;
    }

    public EGLContext getEglContext() {
        if (xyEGLThread != null) {
            return xyEGLThread.getEglContext();
        }
        return  null;
    }

    public void requestRender() {
        if (xyEGLThread != null) {
            xyEGLThread.requestRunder();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (surface == null) {
            surface = holder.getSurface();
        }
        xyEGLThread = new XYEGLThread(new WeakReference<XYEGLSurfaceView>(this));
        xyEGLThread.isCreate = true;
        xyEGLThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        xyEGLThread.width = width;
        xyEGLThread.height = height;
        xyEGLThread.isChange = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        xyEGLThread.onDestory();
        xyEGLThread = null;
        surface = null;
        eglContext = null;
    }

    //看GLSurface 源码 也有这个
    public interface XYGLRender {
        void onSurfaceCreated();

        void onSurfaceChanged(int width, int height);

        void onDrawFrame();

        void changeRender();
    }

    static class XYEGLThread extends Thread {
        private WeakReference<XYEGLSurfaceView> xyeglSurfaceViewWeakReference;
        private EglHelper eglHelper = null;
        private Object object = null;

        private boolean isExit = false;
        //记录是否创建
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;


        private int width;
        private int height;

        public XYEGLThread(WeakReference<XYEGLSurfaceView> xyeglSurfaceViewWeakReference) {
            this.xyeglSurfaceViewWeakReference = xyeglSurfaceViewWeakReference;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            isStart = false;
            object = new Object();
            eglHelper = new EglHelper();
            eglHelper.initEgl(xyeglSurfaceViewWeakReference.get().surface, xyeglSurfaceViewWeakReference.get().eglContext);

            while (true) {
                if (isExit) {
                    //释放资源
                    release();
                    break;
                }
                if (isStart) {
                    if (xyeglSurfaceViewWeakReference.get().mRenderMode == RENDERMODE_WHEN_DIRTY) {
                        //手动刷新
                        synchronized (object) {
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (xyeglSurfaceViewWeakReference.get().mRenderMode == RENDERMODE_CONTINUOUSLY) {
                        try {
                            Thread.sleep(1000 / 60);//每秒60帧
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new RuntimeException("mRenderMode is wrong value");
                    }
                }

                onCreate();
                onChange(width, height);
                onDraw();
                isStart = true;
            }
        }


        private void onCreate() {
            if (isCreate && xyeglSurfaceViewWeakReference.get().xyGLRender != null) {
                isCreate = false;
                xyeglSurfaceViewWeakReference.get().xyGLRender.onSurfaceCreated();
            }
        }

        private void onChange(int width, int height) {
            if (isChange && xyeglSurfaceViewWeakReference.get().xyGLRender != null) {
                isChange = false;
                xyeglSurfaceViewWeakReference.get().xyGLRender.onSurfaceChanged(width, height);
            }
        }

        private void onDraw() {
            if (xyeglSurfaceViewWeakReference.get().xyGLRender != null && eglHelper != null) {
                xyeglSurfaceViewWeakReference.get().xyGLRender.onDrawFrame();
                //必须调用两次 才能显示
                if (!isStart) {
                    xyeglSurfaceViewWeakReference.get().xyGLRender.onDrawFrame();
                }
                eglHelper.swapBuffers();
            }
        }

        private void requestRunder() {
            if (object != null) {
                synchronized (object) {
                    object.notifyAll();//解除
                }
            }
        }

        public void onDestory() {
            isExit = true;
            requestRunder();

        }

        public void release() {
            if (eglHelper != null) {
                eglHelper.destoryEgl();
                eglHelper = null;
                object = null;
                xyeglSurfaceViewWeakReference = null;
            }
        }

        public EGLContext getEglContext() {
            if (eglHelper != null) {
                if (eglHelper != null) {
                    return eglHelper.getmEglContext();
                }
            }
            return null;
        }
    }

}















