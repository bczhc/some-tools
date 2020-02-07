package pers.zhc.tools.floatingdrawing;

import android.content.Context;
import android.support.annotation.Size;

public class JNI {
    static {
        System.loadLibrary("a");
        System.loadLibrary("fb_tools");
    }

    public native int mG(Context mainActivity, String key);

    public native void floatToByteArray(@Size(min = 4) byte[] dest, float f, int offset);

    public native void intToByteArray(@Size(min = 4) byte[] dest, int i, int offset);

    public native float byteArrayToFloat(@Size(min = 4) byte[] bytes, int offset);

    public native int byteArrayToInt(@Size(min = 4) byte[] bytes, int offset);
}

