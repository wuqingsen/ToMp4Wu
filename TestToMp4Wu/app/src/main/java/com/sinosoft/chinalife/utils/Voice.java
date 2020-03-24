package com.sinosoft.chinalife.utils;

import android.app.Activity;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sinosoft.chinalife.R;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;

/**
 * wuqingsen on 2019-12-23
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class Voice extends Activity {
    String AUDIO_MIME = "audio/mp4a-latm";
    MediaCodec mAudiEncoder;
    MediaFormat mAudioFormat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMediaCodeC();

        startMediaCodeC();
    }


    private void initMediaCodeC() {
        try {
            mAudiEncoder = MediaCodec.createEncoderByType(AUDIO_MIME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAudioFormat = new MediaFormat();
        mAudioFormat.setString(MediaFormat.KEY_MIME, AUDIO_MIME);
        mAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 32000);
        mAudioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        mAudioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
        mAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024);//
        mAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mAudiEncoder.configure(mAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    private void startMediaCodeC() {
//        mAudiEncoder.start();
//        ByteBuffer[] inputBuffers = mAudiEncoder.getInputBuffers();
//        ByteBuffer[] outputBuffers = mAudiEncoder.getOutputBuffers();
//        int inputBufIndex = mAudiEncoder.dequeueInputBuffer(1000);
//        ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
//        inputBuffer.clear();
//        inputBuffer.put(bytes, 0, BUFFER_SIZE_IN_BYTES);
    }


    //PCM转为AAC
//    public void start() {
//
//        try {
//            fos = new FileOutputStream(filePath);
//            bos = new BufferedOutputStream(fos, 200 * 1024);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 16000, 1);//参数对应-> mime type、采样率、声道数
//            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 100);//比特率
//            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16*1024);
//            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
//            codec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if (codec == null) {
//            Log.e(TAG, "create mediaEncode failed");
//            return;
//        }
//
//        //调用MediaCodec的start()方法，此时MediaCodec处于Executing状态
//        codec.start();
//
//    }

    /**
     * 将数据写入文件。
     */
//    private void writeDateTOFile(String AudioName) {
//        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
//        String mFileName = "/sdcard/Android/data/com.example.gosleep/cache/audio.raw";
//        byte[] audiodata = new byte[bufferSizeInBytes];
//        FileOutputStream fos = null;
//        FileOutputStream fos1 = null;
//        OutputStreamWriter writer = null;
//        int readsize = 0;
//        int audiodata_len = 0;
//        byte bytes[] = new byte[2];
//        try {
//            File file = new File(AudioName);
//            if (file.exists()) {
//                file.delete();
//            }
//            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
//
//            File file1 = new File(mFileName);
//            fos1 = new FileOutputStream(file1);
//            writer = new OutputStreamWriter(fos1, "utf-8");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        while (isRecord == true) {
//            readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
//            if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
//                try {
//                    audiodata_len = audiodata.length;
//                    for (int i = 0; i < audiodata_len; i += 2) {
//                        bytes[0] = audiodata[i];
//                        bytes[1] = audiodata[i + 1];
//                        writer.write(byte4ToShortInt(bytes, 0) + ",");
//                    }
//                    fos.write(audiodata);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        try {
//            writer.close();
//            fos1.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            fos.close();// 关闭写入流
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * byte数组转换为short int整数
     *
     * @param bytes byte数组
     * @param off   开始位置
     * @return int整数
     */
    private short byte4ToShortInt(byte[] bytes, int off) {
        short s = 0;
        short b0 = (short) (bytes[off] & 0xff);
        short b1 = (short) (bytes[off + 1] & 0xff);
        b1 <<= 8;
        s = (short) (b0 | b1);
        return s;
    }
}
