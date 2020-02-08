package pers.zhc.tools.utils;

import android.graphics.Bitmap;
import android.graphics.Point;

public class BitmapUtil {
    static {
        System.loadLibrary("bitmap");
    }

    public static native int getBitmapResolution(Bitmap bitmap, Point point);
}
