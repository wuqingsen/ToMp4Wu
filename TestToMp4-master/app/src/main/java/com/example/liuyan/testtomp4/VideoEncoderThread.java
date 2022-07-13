package com.example.liuyan.testtomp4;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * 视频编码线程
 */
public class VideoEncoderThread extends Thread {

    public static final int IMAGE_HEIGHT = 720;
    public static final int IMAGE_WIDTH = 1280;


    // 编码相关参数
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private static final int FRAME_RATE = 25; // 帧率
    private static final int IFRAME_INTERVAL = 10; // I帧间隔（GOP）
    private static final int TIMEOUT_USEC = 10000; // 编码超时时间

    // 视频宽高参数
    private int mWidth;
    private int mHeight;

    // 存储每一帧的数据 Vector 自增数组
    private Vector<byte[]> frameBytes;
    private byte[] mFrameData;

    private static final int COMPRESS_RATIO = 256;
    private static final int BIT_RATE = IMAGE_HEIGHT * IMAGE_WIDTH * 3 * 8 * FRAME_RATE / COMPRESS_RATIO; // bit rate CameraWrapper.

    private final Object lock = new Object();

    private MediaCodec mMediaCodec;  // Android硬编解码器
    private MediaCodec.BufferInfo mBufferInfo; //  编解码Buffer相关信息

    private WeakReference<MediaMuxerThread> mediaMuxer; // 音视频混合器
    private MediaFormat mediaFormat; // 音视频格式

    private volatile boolean isStart = false;
    private volatile boolean isExit = false;
    private volatile boolean isMuxerReady = false;


    public VideoEncoderThread(int mWidth, int mHeight, WeakReference<MediaMuxerThread> mediaMuxer) {
        // 初始化相关对象和参数
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mediaMuxer = mediaMuxer;
        frameBytes = new Vector<byte[]>();
        prepare();
    }

    // 执行相关准备工作
    private void prepare() {
        Log.i("=====视频录制", "VideoEncoderThread().prepare");
        mFrameData = new byte[this.mWidth * this.mHeight * 3 / 2];
        mBufferInfo = new MediaCodec.BufferInfo();

        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, this.mWidth, this.mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
//        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);

//        mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
    }

    /**
     * 开始视频编码
     */
    private void startMediaCodec() throws IOException {
        mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        isStart = true;
    }

    //混合器已经初始化，等待添加轨道
    public void setMuxerReady(boolean muxerReady) {
        synchronized (lock) {
            Log.e("=====视频录制", Thread.currentThread().getId() + " video -- setMuxerReady..." + muxerReady);
            isMuxerReady = muxerReady;
            lock.notifyAll();
        }
    }

    public void add(byte[] data) {
        if (frameBytes != null && isMuxerReady) {
            frameBytes.add(data);
        }
    }

    @Override
    public void run() {
        super.run();
        while (!isExit) {
            if (!isStart) {
                stopMediaCodec();
                if (!isMuxerReady) {
                    synchronized (lock) {
                        try {
                            Log.e("=====视频录制", "video -- 等待混合器准备...");
                            lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                if (isMuxerReady) {
                    try {
                        Log.e("=====视频录制", "video -- startMediaCodec...");
                        startMediaCodec();
                    } catch (IOException e) {
                        isStart = false;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                        }
                    }
                }

            } else if (!frameBytes.isEmpty()) {
                byte[] bytes = this.frameBytes.remove(0);
                Log.e("=====视频录制", "解码视频数据:" + bytes.length);
                try {
                    encodeFrame(dealByte(bytes));
                } catch (Exception e) {
                    Log.e("=====视频录制", "解码视频(Video)数据 失败");
                    e.printStackTrace();
                }
            }
        }
        Log.e("=====视频录制", "Video 录制线程 退出...");
    }

    public void exit() {
        isExit = true;
    }

    /**
     * 将拿到的预览帧数据转为bitmap添加水印 再讲bitmap转为帧数据
     * @param dst 预览的帧数据
     * @return
     */
    private byte[] dealByte(byte[] dst) {
//        YuvImage image = new YuvImage(dst, ImageFormat.NV21, CameraSettings.SRC_IMAGE_WIDTH,CameraSettings.SRC_IMAGE_HEIGHT, null)
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        image.compressToJpeg(new Rect(0, 0, CameraSettings.SRC_IMAGE_WIDTH,CameraSettings.SRC_IMAGE_HEIGHT), 100, stream);
//        Bitmap bitmapAll = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

        Bitmap bitmapAll = MainActivity.myClass.nv21ToBitmap(dst, mWidth,
                mHeight);

        Bitmap bitmapAllNew = bitmapAll.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmapAllNew);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        long a = System.currentTimeMillis();
        String b = "北京市东城区雍和宫";
        canvas.drawText(a + "", 100, 100, paint);
        canvas.drawText(b, 100, 300, paint);
        byte[] newBytes = SYUtils.bitmapToNv21(bitmapAllNew, mWidth, mHeight);
        if (newBytes != null) {
            return newBytes;
        } else {
            return null;
        }
    }

    /**
     * 编码每一帧的数据
     *
     * @param input 每一帧的数据
     */
    private void encodeFrame(byte[] input) {
        Log.w("=====视频录制", "VideoEncoderThread.encodeFrame()");

        // 将原始的N21数据转为I420
        NV21toI420SemiPlanar(input, mFrameData, this.mWidth, this.mHeight);

        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();

        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(mFrameData);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mFrameData.length, System.nanoTime() / 1000, 0);
        } else {
            Log.e("=====视频录制", "input buffer not available");
        }

        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

        //FORMAT_CHANGEED < 0 所以需要一个 ||
        while (outputBufferIndex >= 0 || outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //添加轨道的好时机，只有一次
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                MediaMuxerThread mediaMuxerRunnable = this.mediaMuxer.get();
                if (mediaMuxerRunnable != null) {
                    mediaMuxerRunnable.addTrackIndex(MediaMuxerThread.TRACK_VIDEO, newFormat);
                }
            } else {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    MediaMuxerThread mediaMuxer = this.mediaMuxer.get();
                    if (mediaMuxer != null && mediaMuxer.isMuxerStart()) {
                        mediaMuxer.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_VIDEO, outputBuffer, mBufferInfo));
                    }
                }
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
            outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        }
    }

    /**
     * 停止视频编码
     */
    private void stopMediaCodec() {
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        isStart = false;
        Log.e("=====视频录制", "stop video 录制...");
    }


    private static void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
        for (int i = width * height; i < nv21bytes.length; i += 2) {
            i420bytes[i] = nv21bytes[i + 1];
            i420bytes[i + 1] = nv21bytes[i];
        }
    }

}
