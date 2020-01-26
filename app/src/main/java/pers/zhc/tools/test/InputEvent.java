package pers.zhc.tools.test;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.cjz.jnigetevent.NativeEventCallBack;
import pers.zhc.tools.BaseActivity;

import java.io.IOException;

public class InputEvent extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Point point = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(point);
        int width = point.x;
        int height = point.y;
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
        NativeEventCallBack nativeEventCallBack = new NativeEventCallBack(width, height);
        nativeEventCallBack.setEventCallBack(eventData -> {
            System.out.println("eventData.x = " + eventData.x);
            System.out.println("eventData.y = " + eventData.y);
            System.out.println("eventData.action = " + eventData.action);
            System.out.println("eventData.eventCode = " + eventData.eventCode);
            System.out.println("eventData.eventType = " + eventData.eventType);
            System.out.println("eventData.eventValue = " + eventData.eventValue);
            System.out.println("eventData.pressure = " + eventData.pressure);
        });
        nativeEventCallBack.startTrace("/dev/input/event5");
    }
}
