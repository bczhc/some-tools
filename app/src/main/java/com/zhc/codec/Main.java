package com.zhc.codec;

import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

public class Main {
    @SuppressWarnings("Duplicates")
    int JNI_Decode(String f, String dF, int dT, TextView tv) {
        switch (dT) {
            case 0:
                return new JNI(tv).qmcDecode(f, dF);
            case 1:
                return new JNI(tv).kwmDecode(f, dF);
        }
        return -2;
    }

    public void showException(Exception e, AppCompatActivity activity) {
        e.printStackTrace();
        activity.runOnUiThread(() -> Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show());
    }
}