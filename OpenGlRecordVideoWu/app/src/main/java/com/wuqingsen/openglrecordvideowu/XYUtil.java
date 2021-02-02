package com.wuqingsen.openglrecordvideowu;

import android.content.Context;
import android.text.TextUtils;

import com.wuqingsen.openglrecordvideowu.camera.XYCameraView;
import com.wuqingsen.openglrecordvideowu.endodec.XYBaseMediaEncoder;
import com.wuqingsen.openglrecordvideowu.endodec.XYMediaEncodec;
import com.wuqingsen.openglrecordvideowu.listener.OnHandleListener;
import com.wuqingsen.openglrecordvideowu.utils.AudioRecordUtil;
import com.wuqingsen.openglrecordvideowu.utils.Constants;
import com.wuqingsen.openglrecordvideowu.utils.RecordState;
/**
 * @author liuml
 * @explain
 * @time 2018/12/26 10:27
 */
public class XYUtil {

    private int result = 0;

    private native static int handle(String[] commands);

    public static RecordState recordState = RecordState.DEFAULT;
    private AudioRecordUtil audioRecordUtil;
    private XYMediaEncodec xyMediaEncodec;

    private Context mContext;
    private XYCameraView imgvideoview;

    private static class staticXYUtil {
        private static XYUtil xyUtil = new XYUtil();
    }

    private XYUtil() {

    }

    public static XYUtil getInstance() {
        return staticXYUtil.xyUtil;
    }


    public void stopRecoder() {
        recordState = RecordState.RECORD_STOP;
        if (audioRecordUtil != null) {
            audioRecordUtil.stopRecord();
        }
        if (xyMediaEncodec != null) {
            xyMediaEncodec.stopRecord();
        }
    }

    public void startRecoder(Context context, XYCameraView xycamaryview) {

        startRecoder(context, xycamaryview, "");
    }


    public void startRecoder(Context context, XYCameraView xycamaryview, String url) {
        recordState = RecordState.RECORDING;
        audioRecordUtil = AudioRecordUtil.getInstance();
        xyMediaEncodec = XYMediaEncodec.getInstance(context, xycamaryview.getTextureId());
        if (TextUtils.isEmpty(url)) {
            xyMediaEncodec.initEncodec(xycamaryview.getEglContext(),
                    Constants.fileDir, Constants.ScreenWidth, Constants.ScreenHeight, 44100, 2);
        } else {
            xyMediaEncodec.initEncodec(xycamaryview.getEglContext(),
                    url, Constants.ScreenWidth, Constants.ScreenHeight, 44100, 2);
        }

        xyMediaEncodec.setOnMediaInfoListener(new XYBaseMediaEncoder.OnMediaInfoListener() {
            @Override
            public void onMediaTime(int times) {
//                LogUtil.d("time = " + times);
            }
        });

        audioRecordUtil.setOnRecordListener(new AudioRecordUtil.OnRecordListener() {
            @Override
            public void recordByte(byte[] audioData, int readSize) {

                if (xyMediaEncodec != null) {
                    xyMediaEncodec.putPCMData(audioData, readSize);
                }

            }
        });
        audioRecordUtil.startRecord();
        xyMediaEncodec.startRecord();
    }
}
