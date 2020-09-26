package pers.zhc.tools.test.jni;

import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class Test extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);
        findViewById(R.id.b1).setOnClickListener(v -> {
            final long start = System.currentTimeMillis();
            int c = 0;
            while (System.currentTimeMillis() - start < 1000) {
                JNI.JniTest.call();
                ++c;
            }
            ToastUtils.show(this, String.valueOf(c));
        });

        findViewById(R.id.b2).setOnClickListener(v -> {
            final int times = JNI.JniTest.toCall();
            ToastUtils.show(this, String.valueOf(times));
        });
        findViewById(R.id.b3).setOnClickListener(v -> {
            final long start = System.currentTimeMillis();
            int c = 0;
            while (System.currentTimeMillis() - start < 1000) {
                myCall();
                ++c;
            }
            ToastUtils.show(this, String.valueOf(c));
        });

        findViewById(R.id.b4).setOnClickListener(v -> {
            final int times = JNI.JniTest.toCall2(this);
            ToastUtils.show(this, String.valueOf(times));
        });
    }

    public void myCall() {

    }
}
