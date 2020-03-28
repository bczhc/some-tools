package pers.zhc.tools.utils;

import android.graphics.Bitmap;
import android.graphics.Point;

public class BitmapUtil {

    public static void getBitmapResolution(Bitmap bitmap, Point point) {
        point.x = bitmap.getWidth();
        point.y = bitmap.getHeight();
    }
}
