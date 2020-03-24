package com.sinosoft.chinalife;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.baidu.speech.asr.SpeechConstant; 
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * wuqingsen on 2019-12-12
 * Mailbox:1243411677@qq.com
 * annotation:百度语音识别，语音视频合成，工具类
 */
public class RecordAiUtils {
    private MainActivity mActivity;
    private Context mContext;

    public RecordAiUtils(MainActivity mActivity, Context mContext) {
        this.mActivity = mActivity;
        this.mContext = mContext;
    }

    //开始录制
    public void startRecord() {
        startVoice();
    }

    //停止录制
    public void stopRecord() {
        stopVoice();
    }

    //音频录制开始
    private void startVoice() {
        // 基于SDK唤醒词集成第2.1 设置唤醒的输入参数
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");
        // "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下

        //保存录音文件
        params.put(SpeechConstant.ACCEPT_AUDIO_DATA, true);
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "guoshou.pcm";
        params.put(SpeechConstant.OUT_FILE, path);

        String json = null; // 这里可以替换成你需要测试的json
        json = new JSONObject(params).toString();
        mActivity.wakeup.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);
        Log.e("=====音频录制开始时间", getTime());
    }

    //音频录制停止
    private void stopVoice() {
        mActivity.wakeup.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0); //
        Log.e("=====音频录制停止时间", getTime());
    }

    public String getTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日   HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        //获取当前时间
        String str = formatter.format(curDate);
        return str;
    }
}
