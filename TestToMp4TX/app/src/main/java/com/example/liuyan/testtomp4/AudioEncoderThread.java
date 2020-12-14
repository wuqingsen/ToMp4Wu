package com.example.liuyan.testtomp4;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * 音频编码线程
 * MediaCodec:安卓底层的多媒体编码，可以用来编码和解码;
 * 处理输入的数据生成输出数据。首先生成一个输入数据缓冲区，将数据填入缓冲区提供给 MediaCodec
 * ，MediaCodec 会采用异步的方式处理这些输入的数据，然后将填满输出缓冲区提供给消费者，
 * 消费者消费完后将缓冲区返还给 MediaCodec。
 *
 * MediaFormat:里面有各种参数提供使用
 *
 * AudioRecord:音频记录，录音
 *
 */
public class AudioEncoderThread extends Thread {

    public static final int SAMPLES_PER_FRAME = 1024;
    private static final int TIMEOUT_USEC = 10000;
    private static final String MIME_TYPE = "audio/mp4a-latm";

    //采样率 现在能够保证在所有设备上使用的采样率是44100Hz, 但是其他的采样率（22050, 16000, 11025）在一些设备上也可以使用。
    private static final int SAMPLE_RATE = 44100;
    private static final int BIT_RATE = 64000;


    private final Object lock = new Object();
    private MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    private volatile boolean isExit = false;
    private WeakReference<MediaMuxerThread> mediaMuxerRunnable;
    private AudioRecord audioRecord;
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    private volatile boolean isStart = false;//是否开始：true开始，false停止
    private volatile boolean isMuxerReady = false;//录制是否准备好
    private long prevOutputPTSUs = 0;
    private MediaFormat audioFormat;

    public AudioEncoderThread(WeakReference<MediaMuxerThread> mediaMuxerRunnable) {
        this.mediaMuxerRunnable = mediaMuxerRunnable;
        mBufferInfo = new MediaCodec.BufferInfo();
        prepare();
    }

    //准备
    private void prepare() {
        /**
         * 创建一个最小的音频格式
         * MIME_TYPE:mime类型
         * SAMPLE_RATE：采样率
         * channelCount:内容中音频通道的数量。
         */
        audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
        //一个以位/秒为单位描述平均比特率的键。
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE); //64000
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1); //单通道
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE); //44100
    }

    //开始
    private void startMediaCodec() throws IOException {
        //设置编码器 MIME_TYPE:mime类型
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        /**
         * 配置组件
         * audioFormat:输入数据的格式(解码器)或输出数据的所需格式(编码器)
         * CONFIGURE_FLAG_ENCODE:指定 CONFIGURE_FLAG_ENCODE 将组件配置为编码器。
         */
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        //配置完成，开始
        mMediaCodec.start();

        //开始录音
        prepareAudioRecord();

        isStart = true;
    }

    //停止录音
    private void stopMediaCodec() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        isStart = false;
        Log.e("=====音频录制", "停止");
    }

    //开始录音
    private void prepareAudioRecord() {
        if (audioRecord != null) {
            //将之前的停止、重置
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        /**
         * 设置线程优先级
         * THREAD_PRIORITY_URGENT_AUDIO: 最重要的音频线程的标准优先级。应用程序通常不能更改为此优先级。
         */
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        /**
         * AudioRecord.getMinBufferSize方法:返回成功创建AudioRecord所需的最小缓冲区大小对象，单位为字节。
         * SAMPLE_RATE :采样率,现在能够保证在所有设备上使用的采样率是44100Hz
         * AudioFormat.CHANNEL_IN_MONO :声道数,CHANNEL_IN_MONO是可以保证在所有设备能够使用的。
         * AudioFormat.ENCODING_PCM_16BIT :返回的音频数据的格式。 ENCODING_PCM_8BIT, ENCODING_PCM_16BIT, and ENCODING_PCM_FLOAT.
         */
        final int min_buffer_size = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = null;

        /**
         * MediaRecorder.AudioSource.MIC: 录音来源
         * SAMPLE_RATE:采样率
         * AudioFormat.CHANNEL_IN_MONO: 声道数
         * AudioFormat.ENCODING_PCM_16BIT:返回的音频数据的格式
         * min_buffer_size:在录制期间写入音频数据的缓冲区的总大小(以字节为单位)
         */
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min_buffer_size);

        if (audioRecord != null) {
            //开始录音
            audioRecord.startRecording();
        }
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

    @Override
    public void run() {
        /**
         * allocateDirect:分配一个新的直接字节缓冲区。
         * SAMPLES_PER_FRAME:新缓冲区的容量，以字节为单位
         */
        final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
        int readBytes;
        //isExit:false还在录制,true已经退出
        while (!isExit) {
            /*启动或者重启*/
            if (!isStart) {
                stopMediaCodec();

                //混合器未准备好
                if (!isMuxerReady) {
                    synchronized (lock) {
                        try {
                            Log.e("=====音频录制", "audio -- 等待混合器准备...");
                            lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                if (isMuxerReady) {
                    try {
                        Log.e("=====音频录制", "audio -- startMediaCodec...");
                        startMediaCodec();
                    } catch (IOException e) {
                        e.printStackTrace();
                        isStart = false;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                        }
                    }
                }
            } else if (audioRecord != null) {
                //录音解码

                //clear:清除这个缓冲区
                buf.clear();

                /**
                 * read:从音频硬件中读取音频数据以便记录到直接缓冲区中
                 * buf:写入所记录的音频数据的直接缓冲区。数据被写入到audioBuffer.position()。
                 * SAMPLES_PER_FRAME:请求的字节数。建议但不强制要求所请求的字节数是帧大小的倍数(以字节为单位的样本大小乘以通道数)
                 */
                readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);

                if (readBytes > 0) {
                    /**
                     * buf.position:设置此缓冲区的位置。如果标记被定义并且大于新位置，那么它将被丢弃。
                     * readBytes:新位置值;必须是非负且不大于当前限制
                     */
                    buf.position(readBytes);

                    /**
                     * 翻转这个缓冲区。将极限设置为当前位置，然后将位置设置为零。如果标记被定义，那么它将被丢弃。
                     * 将缓存字节数组的指针设置为数组的开始序列即数组下标0。这样就可以从buffer开头，对该buffer进行遍历（读取）了。
                     */
                    buf.flip();

                    Log.e("=====音频录制", "解码音频数据:" + readBytes);
                    try {
                        //解码
                        encode(buf, readBytes, getPTSUs());
                    } catch (Exception e) {
                        Log.e("=====音频录制", "解码音频(Audio)数据 失败");
                        e.printStackTrace();
                    }
                }
            }

        }
        Log.e("=====音频录制", "Audio 录制线程 退出...");
    }

    //解码
    private void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        //isExit:false还在录制,true已经退出
        if (isExit) return;

        /**
         * getInputBuffers:检索输入缓冲区集。在start()返回之后调用它。
         * 调用此方法后，必须不再使用先前调用此方法返回的任何ByteBuffers。
         * 简单理解：得到输入缓存数组
         */
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();


        /**
         * 注：queueInputBuffer与dequeueInputBuffer配合使用
         * dequeueInputBuffer：返回要用有效数据填充的输入缓冲区的索引，如果当前没有可用的缓冲区，则返回-1。
         * 如果timeoutUs == 0，此方法将立即返回，如果timeoutUs <如果timeoutUs >，则等待到“timeoutUs”微秒0.
         * 简单理解：提取出要处理的部分，将这一部分放入缓冲区
         * TIMEOUT_USEC：超时(以微秒为单位)，负超时表示“无限”。
         */
        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);

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
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0);
            }
        }

        /*获取解码后的数据*/
        /**
         * 检索输出缓冲区集。在start()返回之后调用它，每当dequeueOutputBuffer通过返回
         * {@link #INFO_OUTPUT_BUFFERS_CHANGED}来发出输出缓冲区更改的信号时调用它。
         * 调用此方法后，必须不再使用先前调用此方法返回的任何ByteBuffers。
         * 简单理解：得到输出缓存数组
         */
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();

        /**
         * dequeueOutputBuffer:获取输出缓冲区
         * mBufferInfo:将填充缓冲区元数据。
         * TIMEOUT_USEC:超时(以微秒为单位)，负超时表示“无限”。
         */
        int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

        //FORMAT_CHANGEED < 0 所以需要一个 ||
        while (encoderStatus >= 0 || encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //添加轨道的好时机，只有一次

                //getOutputFormat获取音频输出格式
                final MediaFormat format = mMediaCodec.getOutputFormat();

                //获取弱引用 MediaMuxerThread 对象
                MediaMuxerThread mediaMuxerRunnable = this.mediaMuxerRunnable.get();

                if (mediaMuxerRunnable != null) {
                    Log.e("=====音频录制", "添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + format.toString());
                    mediaMuxerRunnable.addTrackIndex(MediaMuxerThread.TRACK_AUDIO, format);
                }
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];

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
                        mediaMuxer.addMuxerData(new MediaMuxerThread.MuxerData(MediaMuxerThread.TRACK_AUDIO, encodedData, mBufferInfo));

                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    }
                }

                //释放音频解码器
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
            }

            /**
             * dequeueOutputBuffer:获取输出缓冲区
             * mBufferInfo:将填充缓冲区元数据。
             * TIMEOUT_USEC:超时(以微秒为单位)，负超时表示“无限”。
             */
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
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
}
