package com.demo.texttomp4complete.muxer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * wuqingsen on 2020-10-10
 * Mailbox:1243411677@qq.com
 * annotation:
 */
@SuppressLint("NewApi")
public class AudioThread extends Thread {
    private static final int TIMEOUT_USEC = 0;
    private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 16000;
    private static final int BIT_RATE = 64000;
    private final MuxerThread.MuxerCallBack mCallback;
    private MediaFormat audioFormat;
    private MediaCodec mMediaCodec;
    private final AudioQueue queue;
    private volatile boolean isRun;
    private long prevOutputPTSUs;
    private MuxerThread muxerThread;

    public AudioThread(MuxerThread var1, MuxerThread.MuxerCallBack var2) {
        this.mCallback = var2;
        this.muxerThread = var1;
        this.queue = new AudioQueue();
        this.queue.init(102400);
        this.preper();
    }

    private void preper() {
        this.audioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 16000, 1);
        this.audioFormat.setInteger("bitrate", 64000);
        this.audioFormat.setInteger("channel-count", 1);
        this.audioFormat.setInteger("sample-rate", 16000);

        try {
            this.mMediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
            this.mMediaCodec.configure(this.audioFormat, (Surface) null, (MediaCrypto) null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            this.mMediaCodec.start();
            Log.d("wqs", "AudioThread准备完成");
        } catch (IOException var2) {
            Log.e("wqs", "AudioThread准备异常");
            var2.printStackTrace();
            this.mCallback.fail("AudioThread准备异常");
        }

        this.isRun = true;
    }

    public void addAudioData(byte[] var1) {
        if (this.isRun) {
            this.queue.addAll(var1);
        }
    }

    public void audioStop() {
        this.isRun = false;
    }

    public void run() {
        while (this.isRun) {
            byte[] var1 = new byte[640];
            if (this.queue.getAll(var1, 640) < 0) {
                try {
                    Thread.sleep(50L);
                    continue;
                } catch (InterruptedException var5) {
                    var5.printStackTrace();
                }
            }

            ByteBuffer[] var2 = this.mMediaCodec.getInputBuffers();
            ByteBuffer[] var3 = this.mMediaCodec.getOutputBuffers();
            int var4;
            if ((var4 = this.mMediaCodec.dequeueInputBuffer(0L)) > 0) {
                ByteBuffer var7;
                (var7 = var2[var4]).clear();
                var7.put(var1);
                this.mMediaCodec.queueInputBuffer(var4, 0, 640, this.getPTSUs(), 0);
            }

            MediaCodec.BufferInfo var8 = new MediaCodec.BufferInfo();
            int var6 = this.mMediaCodec.dequeueOutputBuffer(var8, 0L);

            while (true) {
                if (var6 != -1) {
                    if (var6 == -3) {
                        var3 = this.mMediaCodec.getOutputBuffers();
                    } else if (var6 == -2) {
                        Log.d("wqs", "audioINFO_OUTPUT_FORMAT_CHANGED");
                        MediaFormat var9 = this.mMediaCodec.getOutputFormat();
                        if (this.muxerThread != null) {
                            Log.d("wqs", "添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + var9.toString());
                            this.muxerThread.addTrackIndex("TRACK_AUDIO", var9);
                        }
                    } else if (var6 < 0) {
                        Log.e("wqs", "encoderStatus < 0");
                    } else {
                        ByteBuffer var10;
                        (var10 = var3[var6]).position(var8.offset);
                        if ((var8.flags & 2) != 0) {
                            Log.d("wqs", "音频BUFFER_FLAG_CODEC_CONFIG)");
                            var8.size = 0;
                        }

                        if (var8.size > 0 && this.muxerThread != null && this.muxerThread.isStart() && var8.presentationTimeUs > 0L) {
                            var8.presentationTimeUs = this.getPTSUs();
                            this.muxerThread.addMuxerData(new MuxerData("TRACK_AUDIO", var10, var8));
                            this.prevOutputPTSUs = var8.presentationTimeUs;
                        }

                        this.mMediaCodec.releaseOutputBuffer(var6, false);
                    }
                }

                if ((var6 = this.mMediaCodec.dequeueOutputBuffer(var8, 0L)) < 0) {
                    break;
                }
            }
        }

        this.mMediaCodec.stop();
        this.mMediaCodec.release();
        Log.d("wqs", "AudioThreadStop");
    }

    private long getPTSUs() {
        long var1;
        if ((var1 = System.nanoTime() / 1000L) < this.prevOutputPTSUs) {
            var1 += this.prevOutputPTSUs - var1;
        }

        return var1;
    }
}
