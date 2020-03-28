package pers.zhc.tools.toast;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.widget.Button;
import android.widget.EditText;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

public class AToast extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toast_activity);
        Button btn = findViewById(R.id.toast);
        EditText et = findViewById(R.id.toast_et);
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
