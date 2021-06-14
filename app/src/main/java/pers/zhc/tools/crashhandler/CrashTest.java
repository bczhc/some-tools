package pers.zhc.tools.crashhandler;

import android.os.Bundle;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class CrashTest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        throw new RuntimeException("crash test...");
    }
}
