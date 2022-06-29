package com.sinosoft.chinalife.openglwatermarkmp4.codec;

import android.media.MediaCodec;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sinosoft.chinalife.openglwatermarkmp4.MainActivity;
import com.sinosoft.chinalife.openglwatermarkmp4.MediaCodecConstant;

import java.nio.ByteBuffer;

/**
 * 视频写入thread
 */
public class VideoCodecThread extends Thread {

    private static final String TAG = "VideoCodecThread.class";

    private MediaCodec videoCodec;
    private MediaCodec.BufferInfo bufferInfo;
    private MediaMuxer mediaMuxer;

    private boolean isStop;

    private long pts;

    private MediaMuxerChangeListener listener;


    public VideoCodecThread(MediaCodec mediaCodec, MediaCodec.BufferInfo bufferInfo, MediaMuxer mediaMuxer,
                            @NonNull MediaMuxerChangeListener listener) {
        this.videoCodec = mediaCodec;
        this.bufferInfo = bufferInfo;
        this.mediaMuxer = mediaMuxer;
        this.listener = listener;
        pts = 0;
        MediaCodecConstant.videoTrackIndex = -1;
    }

    @Override
    public void run() {
        super.run();
        isStop = false;
        videoCodec.start();
        while (true) {
            if (isStop) {
                videoCodec.stop();
                videoCodec.release();
                videoCodec = null;
                MediaCodecConstant.videoStop = true;

                if (MediaCodecConstant.audioStop) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    listener.onMediaMuxerChangeListener(MediaCodecConstant.MUXER_STOP);
                    break;
                }
            }

            if (videoCodec == null)
                break;
            int outputBufferIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0);
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaCodecConstant.videoTrackIndex = mediaMuxer.addTrack(videoCodec.getOutputFormat());
                if (MediaCodecConstant.audioTrackIndex != -1) {
                    mediaMuxer.start();
                    //标识编码开始
                    MediaCodecConstant.encodeStart = true;
                    listener.onMediaMuxerChangeListener(MediaCodecConstant.MUXER_START);
                }
            } else {
                while (outputBufferIndex >= 0) {
                    if (!MediaCodecConstant.encodeStart) {
                        Log.d(TAG, "run: 线程延迟");
                        SystemClock.sleep(10);
                        continue;
                    }

                    ByteBuffer outputBuffer = videoCodec.getOutputBuffers()[outputBufferIndex];
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    if (pts == 0) {
                        pts = bufferInfo.presentationTimeUs;
                    }
                    bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts;
                    mediaMuxer.writeSampleData(MediaCodecConstant.videoTrackIndex, outputBuffer, bufferInfo);
                    Log.d(TAG, "视频秒数时间戳 = " + bufferInfo.presentationTimeUs / 1000000.0f);
                    if (bufferInfo != null)
                        listener.onMediaInfoListener((int) (bufferInfo.presentationTimeUs / 1000000));


                    //视频流回调
//                    int offset = outputBuffer.position();
//                    int len = bufferInfo.size;
//                    int size = outputBuffer.limit();
//                    byte[] data = new byte[size];
//                    for (int i = offset; i < size; i++) {
//                        data[i] = outputBuffer.get(i);
//                    }
//                    if (recordListener != null) {
//                        recordListener.onVideoEncode(data, offset, len);
//                    }


                    videoCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            }
        }
    }

    public void stopVideoCodec() {
        isStop = true;
    }
}
