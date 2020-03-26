package pers.zhc.tools.malloctest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class MAllocTest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m_alloc_test_activity);
        Button btn = findViewById(R.id.alloc_btn);
        EditText editText = findViewById(R.id.et);
        btn.setOnClickListener(v -> {
            final String s = editText.getText().toString();
            long size = 0L;
            try {
                size = Long.parseLong(s);
            } catch (NumberFormatException e) {
                Common.showException(e, this);
            }
            ToastUtils.show(this, R.string.allocing);
            long finalSize = size;
            new Thread(() -> {
                final long address = JNI.MAllocTest.alloc(finalSize);
                runOnUiThread(() -> ToastUtils.show(this, getString(R.string.alloc_done)
                        + "\n0x" + Long.toHexString(address)));
            }).start();
        });
    }
}
