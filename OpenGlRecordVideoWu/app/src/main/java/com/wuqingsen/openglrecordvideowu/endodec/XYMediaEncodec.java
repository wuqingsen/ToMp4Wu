package com.wuqingsen.openglrecordvideowu.endodec;

import android.content.Context;

/**
 * @author liuml
 * @explain
 * @time 2018/12/11 16:41
 */
public class XYMediaEncodec extends XYBaseMediaEncoder {

    private static volatile XYMediaEncodec instance;
    private XYEncodecRender xyEncodecRender;

    private XYMediaEncodec(Context context, int textureId) {
        super(context);
        xyEncodecRender = new XYEncodecRender(context, textureId);
        setRender(xyEncodecRender);
        setmRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public static XYMediaEncodec getInstance(Context context, int textureId) {
        if (instance == null) {
            synchronized (XYMediaEncodec.class) {
                if (instance == null) {
                    instance = new XYMediaEncodec(context, textureId);
                }
            }
        }
        return instance;
    }
}
