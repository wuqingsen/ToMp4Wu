package com.wuqingsen.openglrecordvideowu.endodec;


import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import com.wuqingsen.openglrecordvideowu.egl.EglHelper;
import com.wuqingsen.openglrecordvideowu.egl.XYEGLSurfaceView;
import com.wuqingsen.openglrecordvideowu.utils.LogUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;


/**
 * @author liuml
 * @explain 仿照XYEGLSurfaceView写 的编码类  编码的基类
 * @time 2018/12/11 13:36
 */
public abstract class XYBaseMediaEncoder {

    private Surface surface;
    private EGLContext eglContext;


    private int width;
    private int height;

    private MediaCodec videoEncodec;
    private MediaFormat videoFormat;
    private MediaCodec.BufferInfo videoBufferInfo;

    private MediaCodec audioEncodec;
    private MediaFormat audioFormat;
    private MediaCodec.BufferInfo audioBufferInfo;
    private long audioPts = 0;
    private int sampleRate;

    private MediaMuxer mediaMuxer;

    private boolean encodecStart;//判断编码器是否开启
    private boolean audioExit;
    private boolean videoExit;


    private XYEGLMediaThread xyeglMediaThread;
    private VideoEncodecThread videoEncodecThread;
    private AudioEncodecThread audioEncodecThread;

    private OnMediaInfoListener onMediaInfoListener;

    //控制手动刷新还是自动刷新
    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;
    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    private XYEGLSurfaceView.XYGLRender xyGLRender;

    private boolean encode;

    public XYBaseMediaEncoder(Context context) {
    }

    public void setRender(XYEGLSurfaceView.XYGLRender xyGLRender) {
        this.xyGLRender = xyGLRender;
    }

    public void setmRenderMode(int mRenderMode) {
        this.mRenderMode = mRenderMode;
    }


    /**
     * @param eglContext
     * @param savePath     保存的文件路径
     * @param width 宽度
     * @param height    高度
     * @param sampleRate    采样率
     * @param channelcount  声道数
     */
    public void initEncodec(EGLContext eglContext, String savePath, int width, int height, int sampleRate, int channelcount) {
        Log.w("wqs", "保存路径:" +savePath);
        this.width = width;
        this.height = height;
        this.eglContext = eglContext;
        initMediaEncodec(savePath, width, height, sampleRate, channelcount);
    }


    public void setOnMediaInfoListener(OnMediaInfoListener onMediaInfoListener) {
        this.onMediaInfoListener = onMediaInfoListener;
    }

    public void startRecord() {
        if (surface != null && eglContext != null) {

            audioPts = 0;
            audioExit = false;
            videoExit = false;
            encodecStart = false;


            xyeglMediaThread = new XYEGLMediaThread(new WeakReference<XYBaseMediaEncoder>(this));
            videoEncodecThread = new VideoEncodecThread(new WeakReference<XYBaseMediaEncoder>(this));
            audioEncodecThread = new AudioEncodecThread(new WeakReference<XYBaseMediaEncoder>(this));
            xyeglMediaThread.isCreate = true;
            xyeglMediaThread.isChange = true;
            xyeglMediaThread.start();
            videoEncodecThread.start();
            audioEncodecThread.start();
        }
    }

    public void stopRecord() {
        if (xyeglMediaThread != null && videoEncodecThread != null && audioEncodecThread != null) {
            videoEncodecThread.exit();
            audioEncodecThread.exit();
            xyeglMediaThread.onDestory();
            videoEncodecThread = null;
            xyeglMediaThread = null;
            audioEncodecThread = null;
        }
    }


    /**
     * 封装器
     *
     * @param savePath
     * @param width
     * @param height
     * @param sampleRate
     * @param channelcount
     */
    private void initMediaEncodec(String savePath, int width, int height, int sampleRate, int channelcount) {

        try {
            mediaMuxer = new MediaMuxer(savePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoEncodec(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            initAudioEncodec(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelcount);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 初始化视频编码器
     *
     * @param mimeType
     * @param width
     * @param height
     */
    private void initVideoEncodec(String mimeType, int width, int height) {
        try {
            videoBufferInfo = new MediaCodec.BufferInfo();
            LogUtil.d("mimeType = " + mimeType + " width = " + width + "  height = " + height);
            videoFormat = MediaFormat.createVideoFormat(mimeType, width, height);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);//Surface
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4);//码率
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);//帧率
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);//I帧 关键帧的间隔  设置为1秒

            //编码
            videoEncodec = MediaCodec.createEncoderByType(mimeType);
            videoEncodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            surface = videoEncodec.createInputSurface();


        } catch (IOException e) {
            LogUtil.e(e.getMessage());
            e.printStackTrace();
            videoEncodec = null;
            videoFormat = null;
            videoBufferInfo = null;
        }

    }

    /**
     * 初始化音频编码器
     *
     * @param mimeType     格式
     * @param sampleRate   采样率
     * @param channelCount 声道数
     */
    private void initAudioEncodec(String mimeType, int sampleRate, int channelCount) {
        try {
            this.sampleRate = sampleRate;
            audioBufferInfo = new MediaCodec.BufferInfo();
            audioFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);//等级
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            LogUtil.d("设置audioFormat 的时候最小的 bufferSizeInBytes = "+bufferSizeInBytes);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSizeInBytes);

            audioEncodec = MediaCodec.createEncoderByType(mimeType);
            audioEncodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            audioBufferInfo = null;
            audioFormat = null;
            audioEncodec = null;
        }
    }

    /**
     * EGL线程 渲染线程
     */
    static class XYEGLMediaThread extends Thread {
        private WeakReference<XYBaseMediaEncoder> encoder;
        private EglHelper eglHelper;
        private Object object;

        private boolean isExit = false;
        //记录是否创建
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;

        public XYEGLMediaThread(WeakReference<XYBaseMediaEncoder> encoder) {
            this.encoder = encoder;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            isStart = false;
            object = new Object();
            eglHelper = new EglHelper();
//            LogUtil.d("encoder = " + encoder);
            eglHelper.initEgl(encoder.get().surface, encoder.get().eglContext);
            while (true) {
                if (isExit) {
                    release();
                    break;
                }
                if (isStart) {
//                    LogUtil.d("encoder.get().mRenderMode = " + encoder.get().mRenderMode);
                    if (encoder.get().mRenderMode == RENDERMODE_WHEN_DIRTY) {
                        //手动刷新
                        synchronized (object) {
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (encoder.get().mRenderMode == RENDERMODE_CONTINUOUSLY) {
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
                onChange(encoder.get().width, encoder.get().height);
                onDraw();
                isStart = true;
            }
        }


        private void onCreate() {
            if (isCreate && encoder.get().xyGLRender != null) {
                isCreate = false;
                encoder.get().xyGLRender.onSurfaceCreated();
            }
        }

        private void onChange(int width, int height) {
            if (isChange && encoder.get().xyGLRender != null) {
                isChange = false;
                encoder.get().xyGLRender.onSurfaceChanged(width, height);
            }
        }

        private void onDraw() {
            if (encoder.get().xyGLRender != null && eglHelper != null) {
                encoder.get().xyGLRender.onDrawFrame();
                //必须调用两次 才能显示
                if (!isStart) {
                    encoder.get().xyGLRender.onDrawFrame();
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
                encoder = null;
            }
        }

    }

    /**
     * 视频录制编码的线程
     */
    static class VideoEncodecThread extends Thread {

        private WeakReference<XYBaseMediaEncoder> encoder;
        private boolean isExit;

        private MediaCodec videoEncodec;
        private MediaCodec.BufferInfo videoBufferInfo;
        private MediaMuxer mediaMuxer;

        private int videoTrackIndex = -1;//视频轨道

        private long pts;

        public VideoEncodecThread(WeakReference<XYBaseMediaEncoder> encoder) {
            this.encoder = encoder;
            videoEncodec = encoder.get().videoEncodec;
            videoBufferInfo = encoder.get().videoBufferInfo;
            mediaMuxer = encoder.get().mediaMuxer;
            videoTrackIndex = -1;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            videoEncodec.start();
            videoTrackIndex = -1;
            pts = 0;
            while (true) {
                if (isExit) {
                    //先把编码器停止
                    videoEncodec.stop();
                    videoEncodec.release();
                    videoEncodec = null;

                    encoder.get().videoExit = true;
                    if (encoder.get().audioExit) {
                        //用medioMuxer 停止的时候才会把头信息写入视频
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        mediaMuxer = null;
                        LogUtil.d("视频录制完成 ");
                    }


                    break;
                }
                //得到队列中可用的输出的索引
                int outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    videoTrackIndex = mediaMuxer.addTrack(videoEncodec.getOutputFormat());
                    //判断音频是否开启编码
                    if (encoder.get().audioEncodecThread.audioTrackIndex != -1) {
                        mediaMuxer.start();
                        encoder.get().encodecStart = true;
                    }
                } else {
                    while (outputBufferIndex >= 0) {

                        if (encoder.get().encodecStart) {
                            ByteBuffer outputBuffer = videoEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(videoBufferInfo.offset);
                            outputBuffer.limit(videoBufferInfo.offset + videoBufferInfo.size);
                            //outputBuffer 编码
                            if (pts == 0) {
                                pts = videoBufferInfo.presentationTimeUs;
                            }
                            videoBufferInfo.presentationTimeUs = videoBufferInfo.presentationTimeUs - pts;//实现pts递增
                            mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, videoBufferInfo);

                            if (encoder.get().onMediaInfoListener != null) {
                                encoder.get().onMediaInfoListener.onMediaTime((int) videoBufferInfo.presentationTimeUs / 1000000);
                            }
                        }

                        //编码完了释放
                        videoEncodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = videoEncodec.dequeueOutputBuffer(videoBufferInfo, 0);
                    }
                }

            }
        }


        public void exit() {
            isExit = true;
        }
    }

    /**
     * put PCM 的数据
     *
     * @param buffer
     * @param size
     */
    public void putPCMData(byte[] buffer, int size) {
        if (audioEncodecThread != null && !audioEncodecThread.isExit && buffer != null && size > 0) {
            int inputBufferindex = audioEncodec.dequeueInputBuffer(0);
            if (inputBufferindex >= 0) {
                ByteBuffer byteBuffer = audioEncodec.getInputBuffers()[inputBufferindex];
                byteBuffer.clear();
                byteBuffer.put(buffer);
                long pts = getAudioPts(size, sampleRate);
                audioEncodec.queueInputBuffer(inputBufferindex, 0, size, pts, 0);
            }
        }
    }

    /**
     * 音频编码的线程
     */
    static class AudioEncodecThread extends Thread {

        private WeakReference<XYBaseMediaEncoder> encoder;
        private boolean isExit;

        private MediaCodec audioEncodec;
        private MediaCodec.BufferInfo AudioBufferInfo;

        private MediaMuxer mediaMuxer;

        //轨道
        private int audioTrackIndex = -1;

        private long pts;

        public AudioEncodecThread(WeakReference<XYBaseMediaEncoder> encoder) {
            this.encoder = encoder;

            audioEncodec = encoder.get().audioEncodec;
            AudioBufferInfo = encoder.get().audioBufferInfo;
            mediaMuxer = encoder.get().mediaMuxer;

            audioTrackIndex = -1;
        }

        @Override
        public void run() {
            super.run();

            pts = 0;
            isExit = false;

            audioEncodec.start();

            while (true) {
                if (isExit) {
                    //回收资源
                    audioEncodec.stop();
                    audioEncodec.release();
                    audioEncodec = null;
                    encoder.get().audioExit = true;
                    if (encoder.get().videoExit) {
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        mediaMuxer = null;
                        LogUtil.d("音频录制完成 ");
                    }

                    break;
                }

                int outputBufferIndex = audioEncodec.dequeueOutputBuffer(AudioBufferInfo, 0);
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (mediaMuxer != null) {
                        audioTrackIndex = mediaMuxer.addTrack(audioEncodec.getOutputFormat());
                        //判断视频线程中是否开启编码
                        if (encoder.get().videoEncodecThread.videoTrackIndex != -1) {
                            mediaMuxer.start();
                            encoder.get().encodecStart = true;
                        }
                    }
                } else {

                    while (outputBufferIndex >= 0) {
                        if (encoder.get().encodecStart) {
                            ByteBuffer outputBuffer = audioEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(AudioBufferInfo.offset);
                            outputBuffer.limit(AudioBufferInfo.offset + AudioBufferInfo.size);
                            //outputBuffer 编码
                            if (pts == 0) {
                                pts = AudioBufferInfo.presentationTimeUs;
                            }
                            AudioBufferInfo.presentationTimeUs = AudioBufferInfo.presentationTimeUs - pts;//实现pts递增
                            mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, AudioBufferInfo);

//                            if (encoder.get().onMediaInfoListener != null) {
//                                encoder.get().onMediaInfoListener.onMediaTime((int) AudioBufferInfo.presentationTimeUs / 1000000);
//                            }
                        }

                        audioEncodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = audioEncodec.dequeueOutputBuffer(AudioBufferInfo, 0);


                    }
                }


            }
        }

        public void exit() {
            isExit = true;
        }
    }

    public interface OnMediaInfoListener {
        void onMediaTime(int times);
    }

    private long getAudioPts(int size, int sampleRate) {
        audioPts = audioPts + (long) ((1.0 * size) / (sampleRate * 2 * 2) * 1000000.0);
        return audioPts;
    }
}














