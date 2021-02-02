package com.wuqingsen.openglrecordvideowu.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;


import com.wuqingsen.openglrecordvideowu.utils.DisplayUtil;

import java.io.IOException;
import java.util.List;

/**
 * @author liuml
 * @explain
 * @time 2018/12/10 11:08
 */
public class XYCamera {

    private SurfaceTexture surfaceTexture;
    //导包注意用硬件的
    private Camera camera;

    private int width;
    private int height;

    public XYCamera(Context context) {
        this.width = DisplayUtil.getScreenWidth(context);
        this.height = DisplayUtil.getScreenHeight(context);
    }

    /**
     * @param surfaceTexture
     * @param cameraId       前置还是后置
     */
    public void initCamera(SurfaceTexture surfaceTexture, int cameraId) {
        this.surfaceTexture = surfaceTexture;

        setCameraParm(cameraId);
    }

    public void initCamera(int cameraId) {
        camera = Camera.open(cameraId);
        try {
            camera.setPreviewTexture(surfaceTexture);
            Camera.Parameters parameters = camera.getParameters();

            parameters.setFlashMode("off");//闪光灯
            parameters.setPreviewFormat(ImageFormat.NV21);

            Camera.Size size = getFitSize(parameters.getSupportedPictureSizes());
            parameters.setPictureSize(size.width, size.height);

            size = getFitSize(parameters.getSupportedPreviewSizes());
            parameters.setPreviewSize(size.width, size.height);

            camera.setParameters(parameters);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void initFilterCamera(int cameraId) {
        camera = Camera.open(cameraId);
//            camera.setPreviewTexture(surfaceTexture);
        Camera.Parameters parameters = camera.getParameters();

        parameters.setFlashMode("off");//闪光灯
        parameters.setPreviewFormat(ImageFormat.NV21);

        Camera.Size size = getFitSize(parameters.getSupportedPictureSizes());
        parameters.setPictureSize(size.width, size.height);

        size = getFitSize(parameters.getSupportedPreviewSizes());
        parameters.setPreviewSize(size.width, size.height);

        camera.setParameters(parameters);
        camera.startPreview();


    }

    private void setCameraParm(int cameraId) {
        try {
            camera = Camera.open(cameraId);
            camera.setPreviewTexture(surfaceTexture);
            Camera.Parameters parameters = camera.getParameters();

            parameters.setFlashMode("off");//闪光灯
            parameters.setPreviewFormat(ImageFormat.NV21);

            Camera.Size size = getFitSize(parameters.getSupportedPictureSizes());
            parameters.setPictureSize(size.width, size.height);

            size = getFitSize(parameters.getSupportedPreviewSizes());
            parameters.setPreviewSize(size.width, size.height);

            camera.setParameters(parameters);
            camera.startPreview();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void changeCamera(int cameraId) {

        if (camera != null) {
            stopPreview();
        }
        setCameraParm(cameraId);

    }

    public Camera getCamera() {
        return camera;
    }

    /**
     * 动态的设置宽高
     *
     * @param sizes
     * @return
     */
    private Camera.Size getFitSize(List<Camera.Size> sizes) {
        if (width < height) {
            int t = height;
            height = width;
            width = t;
        }

        for (Camera.Size size : sizes) {
            if (1.0f * size.width / size.height == 1.0f * width / height) {
                return size;
            }
        }
        return sizes.get(0);
    }

}






