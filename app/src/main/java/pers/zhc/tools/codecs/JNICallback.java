package pers.zhc.tools.codecs;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import pers.zhc.tools.jni.JNI;

public class JNICallback implements JNI.Codecs.Callback {
    private final TextView tv;
    private final AppCompatActivity a;

    JNICallback(TextView tv, AppCompatActivity a) {
        this.tv = tv;
        this.a = a;
    }

    @Override
    public void callback(String s, double b) {
        if (b == -1D) {
            a.runOnUiThread(() -> tv.setText(s));
        } else {
            System.out.println("s = " + s);
            System.out.println("codecs_activity_b = " + b);
            a.runOnUiThread(() -> tv.setText(String.format("%s", b + "%")));
        }
    }
}
