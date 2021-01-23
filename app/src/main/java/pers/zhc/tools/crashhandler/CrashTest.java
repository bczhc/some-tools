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
        //noinspection NumericOverflow,divzero
        final int r = 1 / 0;
        ToastUtils.show(this, String.valueOf(r));
    }
}
