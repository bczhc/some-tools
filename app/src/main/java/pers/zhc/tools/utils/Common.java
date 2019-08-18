package pers.zhc.tools.utils;

import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class Common {
    public static void showException(Exception e, AppCompatActivity activity) {
        e.printStackTrace();
        activity.runOnUiThread(() -> Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show());
    }
}
