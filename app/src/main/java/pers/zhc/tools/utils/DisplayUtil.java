package pers.zhc.tools.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.EditText;
import org.jetbrains.annotations.NotNull;

public class DisplayUtil {

    public static int px2dip(@NotNull Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int dip2px(@NotNull Context context, float dipValue) {
        return (int) (dipValue * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static float mm2px(@NotNull Context context, float mm) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, context.getResources().getDisplayMetrics());
    }

    public static int px2sp(@NotNull Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    public static int sp2px(@NotNull Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    public static int getStatusBarHeight(@NotNull Activity activity) {
        Rect rectangle = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
        return rectangle.top;
    }

    public static @NotNull DisplayMetrics getMetrics(@NotNull Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    public static @NotNull Point getScreenSize(@NotNull Context context) {
        Point point = new Point();
        final WindowManager windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(point);
        return point;
    }

    public static float cm2px(Context context, float cm) {
        return mm2px(context, cm * 10);
    }

    public static float getDefaultEditTextTextSize(Context context) {
        return ((float) DisplayUtil.px2sp(context, new EditText(context).getTextSize()));
    }
}