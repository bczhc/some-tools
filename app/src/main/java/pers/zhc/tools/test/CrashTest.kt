package pers.zhc.tools.test;

import android.os.Bundle;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;

/**
 * @author bczhc
 */
public class CrashTest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        throw new RuntimeException("Crash test");
    }
}
