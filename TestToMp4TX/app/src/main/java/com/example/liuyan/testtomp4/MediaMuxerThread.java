package com.example.liuyan.testtomp4;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * 音视频混合线程
 */
public class MediaMuxerThread extends Thread {

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;

    // 音轨添加状态
    private volatile boolean isVideoTrackAdd;
    private volatile boolean isAudioTrackAdd;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    int width = 1920;
    int height = 1080;
    private final Object lock = new Object();
    //单例
    private static MediaMuxerThread mediaMuxerThread;
    //音频 和 视频 线程单独处理
    private AudioEncoderThread audioThread;
    private VideoEncoderThread videoThread;
    //混合器 和 需要被混合的音视频数据
    private MediaMuxer mediaMuxer;
    private Vector<MuxerData> muxerDatas;

    private FileUtils fileSwapHelper;

    private volatile boolean isExit = false;

    private MediaMuxerThread() {
        // 构造函数
    }

    // 开始音视频混合任务
    public static void startMuxer() {
        if (mediaMuxerThread == null) {
            synchronized (MediaMuxerThread.class) {
                if (mediaMuxerThread == null) {
                    mediaMuxerThread = new MediaMuxerThread();
                    Log.e("=====混合线程", "mediaMuxerThread.run();");
                    mediaMuxerThread.start();
                }
            }
        }
    }

    // 停止音视频混合任务
    public static void stopMuxer() {
        if (mediaMuxerThread != null) {
            mediaMuxerThread.exit();
            try {
                mediaMuxerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mediaMuxerThread = null;
        }
    }

    //混合器初始化，但是未开始混合
    private void readyStart() throws IOException {
        readyStart(fileSwapHelper.getFilePath());
    }
    private void readyStart(String filePath) throws IOException {
        isExit = false;
        isVideoTrackAdd = false;
        isAudioTrackAdd = false;
        muxerDatas.clear();
//初始化
        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        if (audioThread != null) {
            audioThread.setMuxerReady(true);
        }
        if (videoThread != null) {
            videoThread.setMuxerReady(true);
        }
        Log.e("=====混合线程", "readyStart(String filePath, boolean restart) 保存至:" + filePath);
    }

    // 添加视频帧数据
    public static void addVideoFrameData(byte[] data) {
        if (mediaMuxerThread != null) {
            mediaMuxerThread.addVideoData(data);
        }
    }
//添加混合数据
    public void addMuxerData(MuxerData data) {
        if (!isMuxerStart()) {
            return;
        }
        muxerDatas.add(data);
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 添加视频／音频轨,如果都添加了，会自动开始混合
     */
    public synchronized void addTrackIndex(int index, MediaFormat mediaFormat) {
        if (isMuxerStart()) {
            return;
        }

        /* 如果已经添加了，就不做处理了 */
        if ((index == TRACK_AUDIO && isAudioTrackAdd()) || (index == TRACK_VIDEO && isVideoTrackAdd())) {
            return;
        }

        if (mediaMuxer != null) {
            int track = 0;
            try {
                track = mediaMuxer.addTrack(mediaFormat);
            } catch (Exception e) {
                Log.e("=====混合线程", "addTrack 异常:" + e.toString());
                return;
            }

            if (index == TRACK_VIDEO) {
                videoTrackIndex = track;
                isVideoTrackAdd = true;
                Log.e("=====混合线程", "添加视频轨完成");
            } else {
                audioTrackIndex = track;
                isAudioTrackAdd = true;
                Log.e("=====混合线程", "添加音轨完成");
            }
            requestStart();
        }
    }

    /**
     * 请求混合器开始启动
     */
    private void requestStart() {
        synchronized (lock) {
            if (isMuxerStart()) {
                mediaMuxer.start();
                Log.e("=====混合线程", "requestStart启动混合器..开始等待数据输入...");
                lock.notify();
            }
        }
    }

    /**
     * 当前是否添加了音轨
     */
    public boolean isAudioTrackAdd() {
        return isAudioTrackAdd;
    }

    /**
     * 当前是否添加了视频轨
     */
    public boolean isVideoTrackAdd() {
        return isVideoTrackAdd;
    }

    /**
     * 当前音视频合成器是否运行了,轨道是否都添加了
     */
    public boolean isMuxerStart() {
        return isAudioTrackAdd && isVideoTrackAdd;
    }


    // 添加视频数据
    private void addVideoData(byte[] data) {
        if (videoThread != null) {
            videoThread.add(data);
        }
    }
    //初始化混合器,还未开始混合,等两个通道添加了，才能开始混合
    private void initMuxer() {
        muxerDatas = new Vector<>();
        fileSwapHelper = new FileUtils();
        audioThread = new AudioEncoderThread((new WeakReference<MediaMuxerThread>(this)));
        videoThread = new VideoEncoderThread(width, height, new WeakReference<MediaMuxerThread>(this));
        audioThread.start();
        videoThread.start();
        try {
            readyStart();
        } catch (IOException e) {
            Log.e("=====混合线程", "initMuxer 异常:" + e.toString());
        }
    }

    @Override
    public void run() {
        super.run();
        //初始化混合器,还未开始混合,等两个通道添加了，才能开始混合
        initMuxer();
        while (!isExit) {
            //音/视轨道是否已经添加,添加了才能开始混合
            //如果两个轨道都添加了，自己写的添加轨道方法中会自动执行 mediaMuxer.start(),之后才能writeData()
            if (isMuxerStart()) {
                //需要被混合的音视频数据 是否为空
                if (muxerDatas.isEmpty()) {
                    synchronized (lock) {
                        try {
                            Log.e("=====混合线程", "等待混合数据...");
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    //不为空，开始混合
                        MuxerData data = muxerDatas.remove(0);
                        int track;
                        if (data.trackIndex == TRACK_VIDEO) {
                            track = videoTrackIndex;
                        } else {
                            track = audioTrackIndex;
                        }
                        Log.e("=====混合线程", "写入混合数据 " + data.bufferInfo.size);
                        try {
                            mediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
                        } catch (Exception e) {
                            Log.e("=====混合线程", "写入混合数据失败!" + e.toString());
                        }
                }
            } else {
                synchronized (lock) {
                    try {
                        Log.e("=====混合线程", "等待音视轨添加...");
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("=====混合线程", "addTrack 异常:" + e.toString());
                    }
                }
            }
        }
        readyStop();
        Log.e("=====混合线程", "混合器退出...");
    }

    private void readyStop() {
        if (mediaMuxer != null) {
            try {
                mediaMuxer.stop();
            } catch (Exception e) {
                Log.e("=====混合线程", "mediaMuxer.stop() 异常:" + e.toString());
            }
            try {
                mediaMuxer.release();
            } catch (Exception e) {
                Log.e("=====混合线程", "mediaMuxer.release() 异常:" + e.toString());

            }
            mediaMuxer = null;
        }
    }

    //停止
    private void exit() {
        if (videoThread != null) {
            videoThread.exit();
            try {
                videoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (audioThread != null) {
            audioThread.exit();
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {

        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }


}
