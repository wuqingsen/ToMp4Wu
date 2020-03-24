package com.sinosoft.chinalife;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * 音频编码线程
 * MediaCodec:安卓底层的多媒体编码，可以用来编码和解码;
 * 处理输入的数据生成输出数据。首先生成一个输入数据缓冲区，将数据填入缓冲区提供给 MediaCodec
 * ，MediaCodec 会采用异步的方式处理这些输入的数据，然后将填满输出缓冲区提供给消费者，
 * 消费者消费完后将缓冲区返还给 MediaCodec。
 * <p>
 * MediaFormat:里面有各种参数提供使用
 * <p>
 * AudioRecord:音频记录，录音
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AudioEncoderThread extends Thread {

    public static final int SAMPLES_PER_FRAME = 1024;
    private static final int TIMEOUT_USEC = 10000;

    private final Object lock = new Object();
    private MediaCodec mediaCodec;                // API >= 16(Android4.1.2)
    private volatile boolean isExit = false;
    private WeakReference<MediaMuxerThread> mediaMuxerRunnable;
    //    private AudioRecord audioRecord;
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    private volatile boolean isStart = false;//是否开始：true开始，false停止
    private volatile boolean isMuxerReady = false;//录制是否准备好
    private long prevOutputPTSUs = 0;
    private MediaFormat mediaFormat;
    private FileOutputStream outputStream = null;
    //    private ByteBuffer byteBufferLocal;
    private ByteBuffer byteBufferMixture;

    public static final int DEFAULT_BIT_RATE = 64000; //128kb

    public static final int DEFAULT_SIMPLE_RATE = 16000; //44100Hz

    public static final int DEFAULT_CHANNEL_COUNTS = 1;

    public static final int DEFAULT_MAX_INPUT_SIZE = 16384; //16k

    private DataOutputStream dataOutputStream;

    public AudioEncoderThread(WeakReference<MediaMuxerThread> mediaMuxerRunnable) {
        this.mediaMuxerRunnable = mediaMuxerRunnable;
        mBufferInfo = new MediaCodec.BufferInfo();
//        prepare();
        try {
            startMediaCodec();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //开始
    private void startMediaCodec() throws IOException {
        //设置编码器 MIME_TYPE:mime类型
        mediaCodec = createMediaCodec();
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //配置完成，开始
        mediaCodec.start();
        isStart = true;
    }

    //停止录音
    private void stopMediaCodec() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        isStart = false;
        Log.e("=====音频录制", "停止");
    }

    public void exit() {
        isExit = true;
    }

    //混合器已经准备好
    public void setMuxerReady(boolean muxerReady) {
        synchronized (lock) {
            Log.e("=====音频录制准备", Thread.currentThread().getId() + " audio -- setMuxerReady..." + muxerReady);
            isMuxerReady = muxerReady;
            lock.notifyAll();
        }
    }

//    @Override
//    public void run() {
//
//        /**
//         * allocateDirect:分配一个新的直接字节缓冲区。
//         * SAMPLES_PER_FRAME:新缓冲区的容量，以字节为单位
//         */
//        byteBufferMixture = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
////        byteBufferLocal = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
////        int readBytes;
//        //isExit:false还在录制,true已经退出
//        if (null != outputStream) {
//            while (!isExit) {
//                /*启动或者重启*/
//                if (!isStart) {
//                    //停止录制
//                    stopMediaCodec();
//
//                    //混合器未准备好
//                    if (!isMuxerReady) {
//                        synchronized (lock) {
//                            try {
//                                Log.e("=====音频录制", "audio -- 等待混合器准备...");
//                                lock.wait();
//                            } catch (InterruptedException e) {
//                            }
//                        }
//                    }
//
//                    if (isMuxerReady) {
//                        try {
//                            Log.e("=====音频录制", "audio -- startMediaCodec...");
//                            startMediaCodec();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            isStart = false;
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e1) {
//                            }
//                        }
//                    }
//                } else {
//                    //开始录制
//                    //clear:清除这个缓冲区
//                    byteBufferMixture.clear();
////                    byteBufferLocal.clear();
//
//                    /**
//                     * read:从音频硬件中读取音频数据以便记录到直接缓冲区中
//                     * buf:写入所记录的音频数据的直接缓冲区。数据被写入到audioBuffer.position()。
//                     * SAMPLES_PER_FRAME:请求的字节数。建议但不强制要求所请求的字节数是帧大小的倍数(以字节为单位的样本大小乘以通道数)
//                     */
////                    readBytes = audioRecord.read(byteBufferMixture, SAMPLES_PER_FRAME);
//
//                }
//
//            }
//        }
//        Log.e("=====音频录制", "Audio 录制线程 退出...");
//    }

    //解码
    private void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        //isExit:false还在录制,true已经退出
        if (isExit) return;

        /**
         * getInputBuffers:检索输入缓冲区集。在start()返回之后调用它。
         * 调用此方法后，必须不再使用先前调用此方法返回的任何ByteBuffers。
         * 简单理解：得到输入缓存数组
         */
        final ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();

        /**
         * 注：queueInputBuffer与dequeueInputBuffer配合使用
         * dequeueInputBuffer：返回要用有效数据填充的输入缓冲区的索引，如果当前没有可用的缓冲区，则返回-1。
         * 如果timeoutUs == 0，此方法将立即返回，如果timeoutUs <如果timeoutUs >，则等待到“timeoutUs”微秒0.
         * 简单理解：提取出要处理的部分，将这一部分放入缓冲区
         * TIMEOUT_USEC：超时(以微秒为单位)，负超时表示“无限”。
         */
        final int inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC);

        /*向编码器输入数据*/
        if (inputBufferIndex >= 0) {
            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];

            //清除这个缓冲区。位置被设置为零，极限被设置为容量，标记被丢弃。
            inputBuffer.clear();
            if (buffer != null) {

                /**
                 * 将给定源缓冲区中剩余的字节传输到此缓冲区；
                 * 也就是往buffer里写一个字节，并把postion移动一位
                 */
                inputBuffer.put(buffer);
            }
            if (length <= 0) {
                /**
                 * 注：queueInputBuffer与dequeueInputBuffer配合使用
                 * queueInputBuffer:将ByteBuffer放回到队列中，释放缓存区
                 * inputBufferIndex:输入缓冲区的索引
                 * offset:数据开始时输入缓冲区中的字节偏移量。
                 * size:有效输入数据的字节数。
                 * presentationTimeUs:此缓冲区的表示时间戳(以微秒为单位)。这通常是显示(呈现)此缓冲区的媒体时间
                 * flags:位掩码，一般为0；由BUFFER_FLAG_CODEC_CONFIG和BUFFER_FLAG_END_OF_STREAM组成
                 *
                 * MediaCodec.BUFFER_FLAG_CODEC_CONFIG:这表明标记为这样的缓冲区包含编解码器初始化/编解码器特定的数据，而不是媒体数据。
                 * MediaCodec.BUFFER_FLAG_END_OF_STREAM:这标志着流的结束，也就是说，在此之后没有缓冲区可用
                 */
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
            }
        }

        /*获取解码后的数据*/
        /**
         * 检索输出缓冲区集。在start()返回之后调用它，每当dequeueOutputBuffer通过返回
         * {@link #INFO_OUTPUT_BUFFERS_CHANGED}来发出输出缓冲区更改的信号时调用它。
         * 调用此方法后，必须不再使用先前调用此方法返回的任何ByteBuffers。
         * 简单理解：得到输出缓存数组
         */
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

        /**
         * dequeueOutputBuffer:获取输出缓冲区
         * mBufferInfo:将填充缓冲区元数据。
         * TIMEOUT_USEC:超时(以微秒为单位)，负超时表示“无限”。
         */
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

        //FORMAT_CHANGEED < 0 所以需要一个 ||
        while (outputBufferIndex >= 0 || outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //添加轨道的好时机，只有一次

                //getOutputFormat获取音频输出格式
                final MediaFormat format = mediaCodec.getOutputFormat();

                //获取弱引用 MediaMuxerThread 对象
                MediaMuxerThread mediaMuxerRunnable = this.mediaMuxerRunnable.get();

                if (mediaMuxerRunnable != null) {
                    Log.e("=====音频录制", "添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + format.toString());
                    mediaMuxerRunnable.addTrackIndex(MediaMuxerThread.TRACK_AUDIO, format);
                }
            } else {
                Log.e("=====音频录制", "添加音轨else");
                final ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                //生成新的音频文件，aac格式
//                outputBuffer.position(mBufferInfo.offset);
//                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
//                byte[] data = new byte[mBufferInfo.size + 7];
//                addADTStoPacket(dataData, dataData.length);
//                outputBuffer.get(dataData, 7, mBufferInfo.size);
//                outputBuffer.position(mBufferInfo.offset);
//                try {
//                    outputStream.write(dataData);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                /**
                 * flags:与缓冲区关联的缓冲区标志,BUFFER_FLAG_KEY_FRAME 和 BUFFER_FLAG_END_OF_STREAM组合
                 * MediaCodec.BUFFER_FLAG_KEY_FRAME =1:这表明(编码的)标记为这样的缓冲区包含关键帧的数据。
                 * MediaCodec.BUFFER_FLAG_CODEC_CONFIG =2:这表明标记为这样的缓冲区包含编解码器初始化/编解码器特定的数据，而不是媒体数据。
                 */
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {

                    //获取弱引用 MediaMuxerThread 对象
                    MediaMuxerThread mediaMuxer = this.mediaMuxerRunnable.get();

                    //isMuxerStart:当前音视频合成器是否运行了,轨道是否都添加了;true音视频合成器运行，轨道添加
                    if (mediaMuxer != null && mediaMuxer.isMuxerStart()) {

                        /**
                         * 设置时间戳
                         * presentationTimeUs:缓冲区的表示时间戳(以微秒为单位)
                         */
                        mBufferInfo.presentationTimeUs = getPTSUs();

                        //往混合器添加音频
                        mediaMuxer.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_AUDIO, outputBuffer, mBufferInfo));

                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    }
                }

                //释放音频解码器
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }

            /**
             * dequeueOutputBuffer:获取输出缓冲区
             * mBufferInfo:将填充缓冲区元数据。
             * TIMEOUT_USEC:超时(以微秒为单位)，负超时表示“无限”。
             */
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        }
    }

    /**
     * get next encoding presentationTimeUs
     */
    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    /**
     * 将采集得到的数据提交给MediaCodec
     */
    public synchronized void encode( byte[] data) {
        //是否在录制
        if (!isStart) {
            Log.e("=====", "停止");
            return;
        }
        //混合器未准备好
//        if (!isMuxerReady) {
//            synchronized (lock) {
//                try {
//                    Log.e("=====音频录制", "audio -- 等待混合器准备...");
//                    lock.wait();
//                } catch (InterruptedException e) {
//                }
//            }
//        }
//
//        if (isMuxerReady) {
//            try {
//                Log.e("=====音频录制", "audio -- startMediaCodec...");
//                startMediaCodec();
//            } catch (IOException e) {
//                e.printStackTrace();
//                isStart = false;
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e1) {
//                }
//            }
//        }
        if (data != null) {
            int bufferIndexId = mediaCodec.dequeueInputBuffer(10000);
            if (bufferIndexId >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(bufferIndexId);
                inputBuffer.clear();
                inputBuffer.put(data);
                mediaCodec.queueInputBuffer(bufferIndexId, 0, data.length, System.nanoTime() / 1000, 0);
            }
        }

        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

        /**
         * dequeueOutputBuffer:获取输出缓冲区
         * mBufferInfo:将填充缓冲区元数据。
         * TIMEOUT_USEC:超时(以微秒为单位)，负超时表示“无限”。
         */
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

        //FORMAT_CHANGEED < 0 所以需要一个 ||
        while (outputBufferIndex >= 0 || outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //添加轨道的好时机，只有一次

                //getOutputFormat获取音频输出格式
                final MediaFormat format = mediaCodec.getOutputFormat();

                //获取弱引用 MediaMuxerThread 对象
                MediaMuxerThread mediaMuxerRunnable = this.mediaMuxerRunnable.get();

                if (mediaMuxerRunnable != null) {
                    Log.e("=====音频录制", "添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + format.toString());
                    mediaMuxerRunnable.addTrackIndex(MediaMuxerThread.TRACK_AUDIO, format);
                }
            } else {
                Log.e("=====音频录制", "添加音轨else");
                final ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                //生成新的音频文件，aac格式
//                outputBuffer.position(mBufferInfo.offset);
//                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
//                byte[] data = new byte[mBufferInfo.size + 7];
//                addADTStoPacket(dataData, dataData.length);
//                outputBuffer.get(dataData, 7, mBufferInfo.size);
//                outputBuffer.position(mBufferInfo.offset);
//                try {
//                    outputStream.write(dataData);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                /**
                 * flags:与缓冲区关联的缓冲区标志,BUFFER_FLAG_KEY_FRAME 和 BUFFER_FLAG_END_OF_STREAM组合
                 * MediaCodec.BUFFER_FLAG_KEY_FRAME =1:这表明(编码的)标记为这样的缓冲区包含关键帧的数据。
                 * MediaCodec.BUFFER_FLAG_CODEC_CONFIG =2:这表明标记为这样的缓冲区包含编解码器初始化/编解码器特定的数据，而不是媒体数据。
                 */
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {

                    //获取弱引用 MediaMuxerThread 对象
                    MediaMuxerThread mediaMuxer = this.mediaMuxerRunnable.get();

                    //isMuxerStart:当前音视频合成器是否运行了,轨道是否都添加了;true音视频合成器运行，轨道添加
                    if (mediaMuxer != null && mediaMuxer.isMuxerStart()) {

                        /**
                         * 设置时间戳
                         * presentationTimeUs:缓冲区的表示时间戳(以微秒为单位)
                         */
                        mBufferInfo.presentationTimeUs = getPTSUs();

                        //往混合器添加音频
                        mediaMuxer.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_AUDIO, outputBuffer, mBufferInfo));

                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    }
                }

                //释放音频解码器
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            }

            /**
             * dequeueOutputBuffer:获取输出缓冲区
             * mBufferInfo:将填充缓冲区元数据。
             * TIMEOUT_USEC:超时(以微秒为单位)，负超时表示“无限”。
             */
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
        }
    }

    public void setDataOutputStream(DataOutputStream dataOutputStream) {
        this.dataOutputStream = dataOutputStream;
    }

    /**
     * 从MediaCodec中取得已经编码好的数据, 并写入文件
     */
    private synchronized void queryEncodedData(MediaCodec mediaCodec) {
        if (mediaCodec == null) {
            return;
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndexId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        if (outputBufferIndexId >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferIndexId);
            byteBuffer.position(bufferInfo.offset);
            byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
            byte[] frame = new byte[bufferInfo.size];
            byteBuffer.get(frame, 0, bufferInfo.size);
            writeToFile(frame);
            mediaCodec.releaseOutputBuffer(outputBufferIndexId, false);
        }
    }

    private void writeToFile(byte[] frame) {
        byte[] packetWithADTS = new byte[frame.length + 7];
        System.arraycopy(frame, 0, packetWithADTS, 7, frame.length);
        //必须在裸aac流上添加ADTS头, 不然播放器播放不了
        addADTStoPacket(packetWithADTS, packetWithADTS.length);
        if (dataOutputStream != null) {
            try {
                dataOutputStream.write(packetWithADTS, 0, packetWithADTS.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //添加头
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 1;  //CPE
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    public MediaCodec createMediaCodec() {
        mediaFormat = createMediaFormat();
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        String name = mediaCodecList.findEncoderForFormat(mediaFormat);
        if (name != null) {
            try {
                return MediaCodec.createByCodecName(name);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public MediaFormat createMediaFormat() {
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                DEFAULT_SIMPLE_RATE, DEFAULT_CHANNEL_COUNTS);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, DEFAULT_BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, DEFAULT_MAX_INPUT_SIZE);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, DEFAULT_SIMPLE_RATE); //44100
        return mediaFormat;
    }
}
