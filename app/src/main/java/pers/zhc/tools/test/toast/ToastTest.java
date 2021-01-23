package pers.zhc.tools.test.toast;

import android.os.Bundle;
import android.text.Editable;
import android.widget.Button;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ScrollEditText;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class ToastTest extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toast_activity);
        Button btn = findViewById(R.id.toast);
        ScrollEditText et = findViewById(R.id.toast_et);
        btn.setOnClickListener(v -> {
            Editable text = et.getText();
            ToastUtils.show(this, text);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}
