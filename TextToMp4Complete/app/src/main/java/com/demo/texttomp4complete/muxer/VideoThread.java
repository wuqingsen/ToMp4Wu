package com.demo.texttomp4complete.muxer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * wuqingsen on 2020-10-10
 * Mailbox:1243411677@qq.com
 * annotation:
 */
@SuppressLint("NewApi")
public class VideoThread extends Thread {
    private final MuxerThread muxerThread;
    private final int mWidth;
    private final int mHeigth;
    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 30;
    private static final int IFRAME_INTERVAL = 1;
    private static final int TIMEOUT_USEC = 20000;
    private static final int BIT_RATE = 2048000;
    private final Vector<byte[]> frameBytes;
    private final byte[] mFrameData;
    private final MuxerThread.MuxerCallBack mCallback;
    private MediaFormat mediaFormat;
    private MediaCodec mMediaCodec;
    private volatile boolean isRun;
    private long prevOutputPTSUs;

    public VideoThread(int var1, int var2, MuxerThread var3, MuxerThread.MuxerCallBack var4) {
        this.mCallback = var4;
        this.muxerThread = var3;
        this.mWidth = var1;
        this.mHeigth = var2;
        this.mFrameData = new byte[this.mWidth * this.mHeigth * 3 / 2];
        this.frameBytes = new Vector();
        this.preper();
    }

    private void preper() {
        this.mediaFormat = MediaFormat.createVideoFormat("video/avc", this.mWidth, this.mHeigth);
        this.mediaFormat.setInteger("bitrate", 2048000);
        this.mediaFormat.setInteger("frame-rate", 30);
        this.mediaFormat.setInteger("color-format", 21);
        this.mediaFormat.setInteger("i-frame-interval", 1);

        try {
            this.mMediaCodec = MediaCodec.createEncoderByType("video/avc");
            this.mMediaCodec.configure(this.mediaFormat, (Surface) null, (MediaCrypto) null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            this.mMediaCodec.start();
            Log.d("wqs", "VideoThread准备完成");
        } catch (IOException var2) {
            Log.e("wqs", "VideoThread准备异常");
            var2.printStackTrace();
            this.mCallback.fail("VideoThread准备异常");
        }
    }

    public void add(byte[] var1) {
        if (this.isRun) {
            if (this.frameBytes.size() > 10) {
                this.frameBytes.remove(0);
            }

            this.frameBytes.add(var1);
        }
    }

    public void run() {
        this.isRun = true;

        while (true) {
            do {
                if (!this.isRun) {
                    this.mMediaCodec.stop();
                    this.mMediaCodec.release();
                    Log.d("wqs", "VideoThreadStop");
                    return;
                }
            } while (this.frameBytes.isEmpty());

            NV21toI420SemiPlanar((byte[]) this.frameBytes.remove(0), this.mFrameData, this.mWidth, this.mHeigth);
            ByteBuffer[] var1 = this.mMediaCodec.getOutputBuffers();
            ByteBuffer[] var2 = this.mMediaCodec.getInputBuffers();
            int var3;
            if ((var3 = this.mMediaCodec.dequeueInputBuffer(20000L)) > 0) {
                ByteBuffer var5;
                (var5 = var2[var3]).clear();
                var5.put(this.mFrameData);
                this.mMediaCodec.queueInputBuffer(var3, 0, this.mFrameData.length, System.nanoTime() / 1000L, 0);
            }

            MediaCodec.BufferInfo var6 = new MediaCodec.BufferInfo();
            var3 = this.mMediaCodec.dequeueOutputBuffer(var6, 20000L);

            while (true) {
                if (var3 != -1) {
                    if (var3 == -3) {
                        var1 = this.mMediaCodec.getOutputBuffers();
                    } else if (var3 == -2) {
                        Log.d("wqs", "videoINFO_OUTPUT_FORMAT_CHANGED");
                        MediaFormat var4 = this.mMediaCodec.getOutputFormat();
                        this.muxerThread.addTrackIndex("TRACK_VIDEO", var4);
                    } else if (var3 < 0) {
                        Log.d("wqs", "outputBufferIndex < 0");
                    } else {
                        ByteBuffer var7 = var1[var3];
                        if ((var6.flags & 2) != 0) {
                            Log.d("wqs", "视频ignoring BUFFER_FLAG_CODEC_CONFIG");
                            var6.size = 0;
                        }

                        if (var6.size > 0 && this.muxerThread.isStart() && var6.presentationTimeUs > 0L) {
                            var6.presentationTimeUs = this.getPTSUs();
                            this.muxerThread.addMuxerData(new MuxerData("TRACK_VIDEO", var7, var6));
                            this.prevOutputPTSUs = var6.presentationTimeUs;
                        }

                        this.mMediaCodec.releaseOutputBuffer(var3, false);
                    }
                }

                var3 = this.mMediaCodec.dequeueOutputBuffer(var6, 20000L);
                if ((var6.flags & 4) != 0) {
                    Log.e("wqs", "video end");
                    return;
                }

                if (var3 <= 0) {
                    break;
                }
            }
        }
    }

    public void stopvideo() {
        this.isRun = false;
    }

    private static void NV21toI420SemiPlanar(byte[] var0, byte[] var1, int var2, int var3) {
        System.arraycopy(var0, 0, var1, 0, var2 * var3);

        for (var2 *= var3; var2 < var0.length; var2 += 2) {
            var1[var2] = var0[var2 + 1];
            var1[var2 + 1] = var0[var2];
        }

    }

    private long getPTSUs() {
        long var1;
        if ((var1 = System.nanoTime() / 1000L) < this.prevOutputPTSUs) {
            var1 += this.prevOutputPTSUs - var1;
        }

        return var1;
    }
}
