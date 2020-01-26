package com.cjz.jnigetevent;

import android.view.MotionEvent;


/**
 * Created by cjz on 2018/4/26.
 */

public class NativeEventCallBack {
    static {
        System.loadLibrary("JNIGetEventCtrl");
    }

    private int width;
    private int height;
    private int lastAction = MotionEvent.ACTION_CANCEL;

    private int ratio;  //宽高比

    private NEventCallBack eventCallBack;

    public native void startTrace(String path);


    public NativeEventCallBack(int width, int height) {
        this.width = width;
        this.height = height;
        ratio = width / height;
    }

    //监控触摸设备，java被C语言回调
    private void callback(int x, int y, int pressure, int eventType, int eventCode, int eventValue) {
        //Log.i("NativeEventCallBack", String.format("x:%d, y:%d, pressure:%d, type:%d, code:%d, value:%d", x, y, pressure, eventType, eventCode, eventValue));
        if (eventCallBack != null && (eventType == 1 || eventType == 3)) {
            float fX = (x / 32767f) * width;
            float fY = (y / 32767f) * height;
//            Log.i("nativeEvent", String.format("x:%f, y:%f, width:%d, height:%d", fX, fY, width, height));
            EventData eventData = new EventData();
            eventData.x = fX;
            eventData.y = fY;
            eventData.pressure = pressure;
            eventData.eventType = eventType;
            eventData.eventCode = eventCode;
            eventData.eventValue = eventValue;
            if (eventCode == 0x014A && eventValue == 1) {
                if (lastAction == MotionEvent.ACTION_DOWN) {
                    eventData.action = MotionEvent.ACTION_MOVE;
                } else {
                    eventData.action = MotionEvent.ACTION_DOWN;
                }
                lastAction = eventData.action;
            } else if (eventCode == 0x014A && eventValue == 0) {
                eventData.action = MotionEvent.ACTION_UP;
                lastAction = eventData.action;
            } else {
                if (lastAction == MotionEvent.ACTION_DOWN) {
                    eventData.action = MotionEvent.ACTION_MOVE;
                } else if (lastAction == MotionEvent.ACTION_UP) {
                    eventData.action = MotionEvent.ACTION_UP;
                }
            }
            if (eventData.action != MotionEvent.ACTION_CANCEL) {
                eventCallBack.nativeEvent(eventData);
            }
        }
    }

    public void setEventCallBack(NEventCallBack eventCallBack) {
        this.eventCallBack = eventCallBack;
    }

    public interface NEventCallBack {
        void nativeEvent(EventData eventData);
    }

    public class EventData {
        public float x;
        public float y;
        public int pressure;
        public int eventType;
        public int eventCode;
        public int eventValue;
        public int action = MotionEvent.ACTION_CANCEL;
    }

}