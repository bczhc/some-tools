package pers.zhc.tools.test;

import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ToastUtils.show(this, String.valueOf(JNI.JniDemo.call()));

        SQLite3 db = SQLite3.open("/storage/emulated/0/dbTest");
        db.key("123");
        db.exec("CREATE TABLE IF NOT EXISTS a(a)");
        db.exec("INSERT INTO a VALUES (12345)");
        db.close();
    }
}