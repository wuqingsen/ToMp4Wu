package com.wuqingsen.openglrecordvideowu.utils;

/**
 * @author liuml
 * @explain 录制状态
 * @time 2019/5/29 10:27
 */
public enum RecordState {
    //0 未开始状态 1 录制状态 2,暂停状态 3 结束状态
    DEFAULT(0),RECORDING(1),RECORD_PAUSE(2),RECORD_STOP(3);

    RecordState(int i) {

    }
}
