package pers.zhc.tools.test.jni;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.jni.JNI;

import java.util.Random;

/**
 * @author bczhc
 */
public class Test extends BaseActivity {
    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int sql1 = JNI.Sqlite3.createHandler();
        JNI.Sqlite3.open(sql1, "/storage/emulated/0/test2.db");
        JNI.Sqlite3.exec(sql1, "CREATE TABLE a(a int,b int, c int)", null);
        JNI.Sqlite3.exec(sql1, "BEGIN TRANSACTION", null);
        final Random random = new Random();
        new Thread(() -> {
            int c = 0;
            final long startTime = System.currentTimeMillis();
            for (; ; ) {
                JNI.Sqlite3.exec(sql1,
                        String.format("INSERT INTO a VALUES(%d,%d,%d)",
                                random.nextInt(),
                                random.nextInt(),
                                random.nextInt()),
                        null);
                ++c;
                if (System.currentTimeMillis() - startTime >= 1000) {
                    System.out.println("c = " + c);
                    JNI.Sqlite3.exec(sql1, "COMMIT", null);
                    JNI.Sqlite3.close(sql1);
                    JNI.Sqlite3.releaseHandler(sql1);
                    break;
                }
            }
        }).start();
    }
}
