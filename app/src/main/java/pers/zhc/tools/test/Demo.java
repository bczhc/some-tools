package pers.zhc.tools.test;

import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String hello = JNI.JniDemo.hello(this, "hello");
        ToastUtils.show(this, hello);
        setContentView(R.layout.demo_activity);
    }
}