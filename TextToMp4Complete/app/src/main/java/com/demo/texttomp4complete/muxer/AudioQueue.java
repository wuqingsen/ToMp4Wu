package com.demo.texttomp4complete.muxer;

import android.util.Log;

/**
 * wuqingsen on 2020-10-10
 * Mailbox:1243411677@qq.com
 * annotation:
 */
public class AudioQueue {
    private byte[] buffer;
    private int head;
    private int tail;
    private int count;
    private int size;

    public AudioQueue() {
    }

    public void init(int var1) {
        this.buffer = new byte[var1];
        this.size = var1;
        this.head = 0;
        this.tail = 0;
        this.count = 0;
    }

    public void add(byte var1) {
        if (this.size == this.count) {
            this.get();
        }

        if (this.tail == this.size) {
            this.tail = 0;
        }

        this.buffer[this.tail] = var1;
        ++this.tail;
        ++this.count;
    }

    public byte get() {
        if (this.count == 0) {
            Log.d("wqs", "队列为空");
            return -1;
        } else {
            if (this.head == this.size) {
                this.head = 0;
            }

            byte var1 = this.buffer[this.head];
            ++this.head;
            --this.count;
            return var1;
        }
    }

    public void addAll(byte[] var1) {
        synchronized(this) {
            int var3 = var1.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                byte var5 = var1[var4];
                this.add(var5);
            }

        }
    }

    public int getAll(byte[] var1, int var2) {
        synchronized(this) {
            if (this.count < var2) {
                return -1;
            } else {
                int var4 = 0;

                for(int var5 = 0; var5 < var2; ++var5) {
                    byte var6 = this.get();
                    var1[var5] = var6;
                    ++var4;
                }

                return var4;
            }
        }
    }
}
