package pers.zhc.tools.test.jni;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.utils.sqlite.MySQLite3;

import java.util.Random;

/**
 * @author bczhc
 */
public class Test extends BaseActivity {
    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MySQLite3 db = MySQLite3.open("/storage/emulated/0/test2.db");
        db.exec("CREATE TABLE a(a int,b int, c int)", null);
        db.exec("begin", null);
        final Random random = new Random();
        new Thread(() -> {
            int c = 0;
            final long startTime = System.currentTimeMillis();
            for (; ; ) {
                db.exec("insert into a values(" + random.nextInt() + "," + random.nextInt() + "," + random.nextInt() + ")", null);
                ++c;
                if (System.currentTimeMillis() - startTime >= 1000) {
                    System.out.println("c = " + c);
                    final long a = System.currentTimeMillis();
                    db.exec("commit", null);
                    final long b = System.currentTimeMillis();
                    System.out.println("(b - a) = " + (b - a));
                    db.close();
                    break;
                }
            }
        }).start();
    }
}
