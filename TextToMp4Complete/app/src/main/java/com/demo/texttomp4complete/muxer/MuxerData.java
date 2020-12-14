package com.demo.texttomp4complete.muxer;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * wuqingsen on 2020-10-10
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class MuxerData {

    String trackIndex;
    ByteBuffer byteBuf;
    MediaCodec.BufferInfo bufferInfo;

    public MuxerData(String var1, ByteBuffer var2, MediaCodec.BufferInfo var3) {
        this.trackIndex = var1;
        this.byteBuf = var2;
        this.bufferInfo = var3;
    }
}
