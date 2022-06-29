package com.sinosoft.chinalife.openglwatermarkmp4;

import android.Manifest;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.MediaMuxer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.sinosoft.chinalife.openglwatermarkmp4.codec.AudioCapture;
import com.sinosoft.chinalife.openglwatermarkmp4.codec.MediaEncodeManager;
import com.sinosoft.chinalife.openglwatermarkmp4.codec.VideoEncodeRender;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 录像功能 -- 录音+录视频+编解码+合成视频
 */
public class MainActivity extends AppCompatActivity{

    private final String TAG = "MainActivity.class";
    private CameraSurfaceView cameraSurfaceView;
    private ImageView ivRecord, ivSwitch;

    //录音
    private AudioCapture audioCapture;

    private MediaEncodeManager mediaEncodeManager;

    //开启 -- 关闭录制
    private boolean isStartRecord = false;
    //开启 -- 关闭前后摄像头切换
    private boolean isSwitchCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        setContentView(R.layout.activity_main);

        ivRecord = findViewById(R.id.iv_record);
        ivSwitch = findViewById(R.id.iv_switch);

        cameraSurfaceView = findViewById(R.id.camera_surface_view);

        audioCapture = new AudioCapture();

        ivRecord.setOnClickListener(v -> {
            //录像
            if (!isStartRecord) {
                //开启录像时，不允许切换摄像头
                ivSwitch.setVisibility(View.GONE);

                initMediaCodec();
                mediaEncodeManager.startEncode();
                audioCapture.start();
                ivRecord.setImageResource(R.mipmap.ic_stop_record);
                isStartRecord = true;
            } else {
                ivSwitch.setVisibility(View.GONE);

                isStartRecord = false;
                mediaEncodeManager.stopEncode();
                audioCapture.stop();
                ivRecord.setImageResource(R.mipmap.ic_start_record);
            }
        });

        ivSwitch.setOnClickListener(v -> {
            //切换前后摄像头 -- 默认开启后置摄像头
            if (!isSwitchCamera) {

                cameraSurfaceView.switchCamera(true);
                isSwitchCamera = true;
            } else {

                cameraSurfaceView.switchCamera(false);
                isSwitchCamera = false;
            }
        });
    }

    private void initMediaCodec() {

        String currentDate = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA).format(new Date());
        String fileName = "/VID_".concat(currentDate).concat(".mp4");
        String filePath = FileUtils.getDiskCachePath(this) + fileName;
        int mediaFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
        //String audioType = MediaFormat.MIMETYPE_AUDIO_AAC;
        String audioType = "audio/mp4a-latm";
        //String videoType = MediaFormat.MIMETYPE_VIDEO_AVC;
        String videoType = "video/avc";
        int sampleRate = 44100;
        int channelCount = 2;//单声道 channelCount=1 , 双声道  channelCount=2
        //AudioCapture.class类中采集音频采用的位宽：AudioFormat.ENCODING_PCM_16BIT ，此处应传入16bit，
        // 用作计算pcm一帧的时间戳
        int audioFormat = 16;
        //预览
        int width = cameraSurfaceView.getCameraPreviewHeight();
        int height = cameraSurfaceView.getCameraPreviewWidth();

        mediaEncodeManager = new MediaEncodeManager(new VideoEncodeRender(this,
                cameraSurfaceView.getTextureId(), cameraSurfaceView.getType(), cameraSurfaceView.getColor()));

        mediaEncodeManager.initMediaCodec(filePath, mediaFormat, audioType, sampleRate,
                channelCount, audioFormat, videoType, width, height);

        mediaEncodeManager.initThread(new com.sinosoft.chinalife.openglwatermarkmp4.codec.MediaMuxerChangeListener() {
                                          @Override
                                          public void onMediaMuxerChangeListener(int type) {
                                              if (type == MediaCodecConstant.MUXER_START) {
                                                  Log.d(TAG, "onMediaMuxerChangeListener --- " + "视频录制开始了");
                                                  setPcmRecordListener();
                                              }
                                          }

                                          @Override
                                          public void onMediaInfoListener(int time) {
                                              Log.d(TAG, "视频录制时长 --- " + time);
                                          }
                                      }, cameraSurfaceView.getEglContext(),
                GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    //录音线程数据回调
    private void setPcmRecordListener() {
        if (audioCapture.getCaptureListener() == null)
            audioCapture.setCaptureListener((audioSource, audioReadSize) -> {
                if (MediaCodecConstant.audioStop || MediaCodecConstant.videoStop) {
                    return;
                }
                mediaEncodeManager.setPcmSource(audioSource, audioReadSize);
            });
    }

}
