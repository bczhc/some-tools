package pers.zhc.tools.floatingdrawing;

import android.content.Context;
import android.support.annotation.Size;

public class JNI {
    static {
        System.loadLibrary("a");
        System.loadLibrary("fb_tools");
    }

    public native int mG(Context mainActivity, String key);

    native void floatToByteArray(@Size(value = 4) byte[] dest, float f);

    native void intToByteArray(@Size(value = 4) byte[] dest, int i);

    native float byteArrayToFloat(@Size(value = 4) byte[] bytes);

    native int byteArrayToInt(@Size(value = 4) byte[] bytes);
}

