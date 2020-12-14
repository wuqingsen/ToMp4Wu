package com.demo.texttomp4complete.muxer;

import android.annotation.SuppressLint;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Vector;

/**
 * wuqingsen on 2020-10-10
 * Mailbox:1243411677@qq.com
 * annotation:
 */
@SuppressLint("NewApi")
public class MuxerThread extends Thread {
    public static final String TRACK_AUDIO = "TRACK_AUDIO";
    public static final String TRACK_VIDEO = "TRACK_VIDEO";
    private Vector<MuxerData> muxerDatas;
    private MediaMuxer mediaMuxer;
    private boolean isAddAudioTrack;
    private boolean isAddVideoTrack;
    private int videoTrack;
    private int audioTrack;
    private VideoThread videoThread;
    private AudioThread audioThread;
    private volatile boolean isRun;
    private long preVideoTimeUs;
    private long preAudioTimeUs;
    private MuxerThread.MuxerCallBack mCallBack;
    private Handler mHandler;

    public MuxerThread() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
            Looper.loop();
        }

        this.mHandler = new Handler(Looper.myLooper()) {
            public void handleMessage(Message var1) {
                if (!MuxerThread.this.isStart()) {
                    MuxerThread.this.mCallBack.fail("超时初始化失败");
                } else {
                    Log.d("wqs", "没有超时");
                }
            }
        };
    }

    public synchronized void addTrackIndex(String var1, MediaFormat var2) {
        if (!this.isAddAudioTrack || !this.isAddVideoTrack) {
            if (!this.isAddVideoTrack && var1.equals("TRACK_VIDEO")) {
                Log.d("wqs", "添加视频轨");
                this.videoTrack = this.mediaMuxer.addTrack(var2);
                if (this.videoTrack >= 0) {
                    this.isAddVideoTrack = true;
                    Log.d("wqs", "添加视频轨完成");
                } else {
                    this.mCallBack.fail("添加视频轨失败");
                }
            }

            if (!this.isAddAudioTrack && var1.equals("TRACK_AUDIO")) {
                Log.d("wqs", "添加音频轨");
                this.audioTrack = this.mediaMuxer.addTrack(var2);
                if (this.audioTrack >= 0) {
                    this.isAddAudioTrack = true;
                    Log.d("wqs", "添加音频轨完成");
                } else {
                    this.mCallBack.fail("添加音频轨失败");
                }
            }

            if (this.isStart()) {
                this.mediaMuxer.start();
                Log.d("wqs", "mediaMuxer初始化完成");
                this.mCallBack.success();
            }

        }
    }

    public boolean isStart() {
        return this.isAddAudioTrack && this.isAddVideoTrack;
    }

    public void addMuxerData(MuxerData var1) {
        this.muxerDatas.add(var1);
    }

    public void addVideoData(byte[] var1) {
        if (this.isRun) {
            this.videoThread.add(var1);
        }
    }

    public void addAudioData(byte[] var1) {
        if (this.isRun && this.audioThread != null) {
            this.audioThread.addAudioData(var1);
        }
    }

    public void startMuxer(String var1, int var2, int var3, MuxerThread.MuxerCallBack var4) {
        this.mCallBack = var4;

        try {
            this.mediaMuxer = new MediaMuxer(var1, 0);
            this.muxerDatas = new Vector();
        } catch (IOException var5) {
            Log.e("wqs", "MuxerThread准备异常" + var5.getMessage());
            var5.printStackTrace();
            var4.fail("MuxerThread准备异常");
        }

        this.isRun = true;
        this.videoThread = new VideoThread(var2, var3, this, var4);
        this.videoThread.start();
        this.audioThread = new AudioThread(this, var4);
        this.audioThread.start();
        this.start();
        this.mHandler.sendEmptyMessageDelayed(1, 3000L);
        Log.d("wqs", "MuxerThread准备完成");
    }

    public void exit() {
        this.isRun = false;

        try {
            this.join();
            if (this.videoThread != null) {
                this.videoThread.stopvideo();
                this.videoThread.join();
            }

            if (this.audioThread != null) {
                this.audioThread.audioStop();
                this.audioThread.join();
            }

        } catch (InterruptedException var1) {
            var1.printStackTrace();
        }
    }

    public void run() {
        while(this.isRun) {
            if (!this.muxerDatas.isEmpty() && this.isStart()) {
                MuxerData var1;
                if ((var1 = (MuxerData)this.muxerDatas.remove(0)).trackIndex.equals("TRACK_VIDEO") && this.videoTrack >= 0 && var1.bufferInfo.presentationTimeUs > this.preVideoTimeUs) {
                    try {
                        this.mediaMuxer.writeSampleData(this.videoTrack, var1.byteBuf, var1.bufferInfo);
                        this.preVideoTimeUs = var1.bufferInfo.presentationTimeUs;
                    } catch (Exception var4) {
                        Log.e("wqs", "写入视频数据异常" + var1.bufferInfo.size + "/" + var1.bufferInfo.offset + "/" + var1.bufferInfo.presentationTimeUs);
                        var4.printStackTrace();
                        Log.e("wqs", var4.getMessage() + "/");
                    }
                } else if (var1.bufferInfo.presentationTimeUs < this.preVideoTimeUs) {
                    Log.e("wqs", "视频数据时间戳错误");
                }

                if (var1.trackIndex.equals("TRACK_AUDIO") && this.audioTrack >= 0 && var1.bufferInfo.presentationTimeUs > this.preAudioTimeUs) {
                    try {
                        this.mediaMuxer.writeSampleData(this.audioTrack, var1.byteBuf, var1.bufferInfo);
                        this.preAudioTimeUs = var1.bufferInfo.presentationTimeUs;
                    } catch (Exception var3) {
                        Log.e("wqs", "写入视音频据异常" + var1.bufferInfo.size + "/" + var1.bufferInfo.offset + "/" + var1.bufferInfo.presentationTimeUs);
                        var3.printStackTrace();
                        Log.e("wqs", var3.getMessage() + "/");
                    }
                } else if (var1.bufferInfo.presentationTimeUs < this.preAudioTimeUs) {
                    Log.e("wqs", "音频数据时间戳错误");
                }
            }
        }

        this.mediaMuxer.release();
        Log.d("wqs", "MuxerThreadStop");
    }

    public interface MuxerCallBack {
        void success();

        void fail(String var1);
    }

}
