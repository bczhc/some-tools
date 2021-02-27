package pers.zhc.tools.pi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.ScrollEditText;
import pers.zhc.tools.utils.ToastUtils;

public class Pi extends BaseActivity {
    private boolean isGenerating = false;
    private StringBuilder sb = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pi_activity);
        Button btn = findViewById(R.id.gen_pi);
        EditText et = findViewById(R.id.pi_et);
        TextView timeTV = findViewById(R.id.time_tv);
        ScrollEditText outET = findViewById(R.id.pi_out_et);
        final ExecutorService[] es = {Executors.newFixedThreadPool(1)};
        TextView tv = findViewById(R.id.waitTV);
        btn.setOnClickListener(v -> {
            try {
                String s = et.getText().toString();
                int i = Integer.parseInt("".equals(s) ? "0" : s);
                try {
                    es[0].shutdownNow();
                } catch (Exception ignored) {
                }
                es[0] = Executors.newFixedThreadPool(1);
                es[0].execute(() -> {
                    JNICallback callback = new JNICallback(sb = new StringBuilder("3."));
                    runOnUiThread(() -> tv.setText(R.string.please_wait));
                    this.isGenerating = true;
                    long sM = System.currentTimeMillis();
                    JNI.Pi.gen(i, callback);
                    long eM = System.currentTimeMillis();
                    runOnUiThread(() -> {
                        tv.setText(R.string.nul);
                        this.isGenerating = false;
                        outET.setText(String.format(getString(R.string.str), sb.toString()));
                        timeTV.setText(String.format(getResources().getString(R.string.tv_millis), (eM - sM)));
                    });
                });
                es[0].shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtils.show(this, e.toString());
            }
        });
        btn.setOnLongClickListener(v -> {
            ClipboardManager cm = ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE));
            if (sb != null) {
                ClipData cd = ClipData.newPlainText("Pi", sb.toString());
                if (cm != null) {
                    cm.setPrimaryClip(cd);
                } else {
                    ToastUtils.show(this, "null");
                }
                ToastUtils.show(this, R.string.copying_succeeded);
            } else {
                ToastUtils.show(this, R.string.null_string);
            }
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (this.isGenerating) {
            this.moveTaskToBack(true);
        } else {
            finish();
        }
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}