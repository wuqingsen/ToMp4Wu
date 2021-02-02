package com.wuqingsen.openglrecordvideowu.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import com.wuqingsen.openglrecordvideowu.egl.XYEGLSurfaceView;
import com.wuqingsen.openglrecordvideowu.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * @author liuml
 * @explain
 * @time 2018/12/7 15:53
 */
public class XYCameraView extends XYEGLSurfaceView {

    private float oldX;
    private float oldY;
    private final float TOUCH_SCALE = 0.2f;        //Proved to be good for normal rotation ( NEW )


    private XYCameraRender xyCameraRender;
    private XYCamera xyCamera;

    private List<Bitmap> bitmapList = new ArrayList<>();

    public static boolean isAddMark = false;//是否添加水印
    public static boolean isDyNamicMark = false;//是否动态水印

    //摄像头
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int textureId = -1;

    private Context mContext;
    private int count = 0;

    private float mPreviousX;
    private float mPreviousY;


    public XYCameraView(Context context) {
        this(context, null);
    }

    public XYCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XYCameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        xyCameraRender = new XYCameraRender(context);
        xyCamera = new XYCamera(context);
        setRender(xyCameraRender);
        previewAngle(context);
        xyCameraRender.setOnSurfaceCreateListener(new XYCameraRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(SurfaceTexture surfaceTexture, int tid) {
                LogUtil.d("render 回调");
                xyCamera.initCamera(surfaceTexture, cameraId);
                textureId = tid;
            }
        });


    }


    /**
     * 预览旋转角度
     *
     * @param context
     */
    public void previewAngle(Context context) {
        int angle = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        xyCameraRender.resetMatrix();

        switch (angle) {
            case Surface.ROTATION_0:
                LogUtil.d("当前角度 0");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    //后置摄像头
                    xyCameraRender.setAngle(90, 0, 0, 1);
                    xyCameraRender.setAngle(180, 1, 0, 0);

                } else {
                    xyCameraRender.setAngle(90, 0, 0, 1);

                }

                break;
            case Surface.ROTATION_90:
                LogUtil.d("当前角度 90");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {

                    xyCameraRender.setAngle(180, 0, 0, 1);
                    xyCameraRender.setAngle(180, 0, 1, 0);
                } else {
                    xyCameraRender.setAngle(90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_180:
                LogUtil.d("当前角度 180");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    xyCameraRender.setAngle(90, 0, 0, 1);
                    xyCameraRender.setAngle(180, 0, 1, 0);
                } else {
                    xyCameraRender.setAngle(-90f, 0f, 0f, 1f);
                }
                break;
            case Surface.ROTATION_270:
                LogUtil.d("当前角度 270");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                    xyCameraRender.setAngle(180, 0, 0, 1);
                    xyCameraRender.setAngle(180, 0, 1, 0);
                } else {
                    xyCameraRender.setAngle(0f, 0f, 0f, 1f);
                }
                break;
        }

    }


    public int getTextureId() {
        return textureId;
    }

    public void updateCurrentBitmap(Bitmap bitmap) {
        if (xyCameraRender != null) {
            xyCameraRender.setUpdateBitmap(bitmap);
//            requestRender();//手动刷新 调用一次
        }
    }

    public void setCurrentImg(int imgsrc) {
        if (xyCameraRender != null) {
            xyCameraRender.setCurrentImgSrc(imgsrc);
//            requestRender();//手动刷新 调用一次
            LogUtil.d("当前 imgsrc =  "+imgsrc);
        }
    }



    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                LogUtil.d("按下 x = "+x);
                LogUtil.d("按下 y = "+y);
                break;
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX; // 从左往有滑动时: x 值增大，dx 为正；反之则否。
                float dy = y - mPreviousY; // 从上往下滑动时: y 值增大，dy 为正；反之则否。
//                LogUtil.d("移动时 x  = "+x);
//                LogUtil.d("移动时 y  = "+y);
                // OpenGL 绕 z 轴的旋转符合左手定则，即 z 轴朝屏幕里面为正。
                // 用户面对屏幕时，是从正面向里看（此时 camera 所处的 z 坐标位置为负数），当旋转度数增大时会进行逆时针旋转。

                // 逆时针旋转判断条件1：触摸点处于 view 水平中线以下时，x 坐标应该要符合从右往左移动，此时 x 是减小的，所以 dx 取负数。
                if (y > getHeight() / 2) {
                    dx = dx * -1 ;
                }

                // 逆时针旋转判断条件2：触摸点处于 view 竖直中线以左时，y 坐标应该要符合从下往上移动，此时 y 是减小的，所以 dy 取负数。
                if (x < getWidth() / 2) {
                    dy = dy * -1 ;
                }
//                LogUtil.d("dx = "+dx);
//                LogUtil.d("dy = "+dy);
//                mRenderer.setAngle(mRenderer.getAngle() + ((dx + dy) * TOUCH_SCALE_FACTOR));

                // 在计算旋转角度后，调用requestRender()来告诉渲染器现在可以进行渲染了
//                requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

    public void onDestory() {
        if (xyCamera != null) {
            xyCamera.stopPreview();
        }
    }
}
