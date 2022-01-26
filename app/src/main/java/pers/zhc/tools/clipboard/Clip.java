package pers.zhc.tools.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.util.Objects;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

public class Clip extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clip_activity);
        Button btn = findViewById(R.id.copy);
        EditText et = findViewById(R.id.copy_et);
        btn.setOnClickListener(v -> {
            String s = et.getText().toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                ClipData cd = ClipData.newHtmlText("", getString(R.string.html_content), s);
                try {
                    Objects.requireNonNull(cm).setPrimaryClip(cd);
                    ToastUtils.show(this, getString(R.string.copying_succeeded));
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtils.show(this, R.string.copying_failed);
                }
            } else {
                ToastUtils.show(this, getString(R.string.html_clipboard_unsupported));
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}
