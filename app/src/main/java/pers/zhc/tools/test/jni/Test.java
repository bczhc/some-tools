package pers.zhc.tools.test.jni;

import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.SQLite3;

/**
 * @author bczhc
 */
public class Test extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SQLite3 db = SQLite3.open("");
        db.exec("BEGIN TRANSACTION");
        db.exec("CREATE TABLE IF NOT EXISTS a(a,b,c)");
        for (int i = 0; i < 1000; i++) {
            db.exec("INSERT INTO a VALUES (123, 321, 'haha')");
        }
        db.exec("COMMIT");

        int[] n = {0};
        db.exec("SELECT * FROM a", contents -> {
            ++n[0];
            return 0;
        });
        System.out.println("n[0] = " + n[0]);
        ToastUtils.show(this, String.valueOf(n[0]));
        db.close();
    }
}
