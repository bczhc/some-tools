package pers.zhc.tools.utils;

import android.app.Activity;
import android.widget.Toast;

public class Common {
    public static void showException(Exception e, Activity activity) {
        e.printStackTrace();
        activity.runOnUiThread(() -> Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show());
    }
}