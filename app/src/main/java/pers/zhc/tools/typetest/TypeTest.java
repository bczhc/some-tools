package pers.zhc.tools.typetest;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ScrollEditText;

/**
 * @author bczhc
 */
public class TypeTest extends BaseActivity {
    private long start = -1;
    private boolean run = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.type_test_activity);
        EditText et = ((ScrollEditText) findViewById(R.id.type_et)).getEditText();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TypeTest.this.start == -1)
                    TypeTest.this.start = System.currentTimeMillis();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        TextView speedTV = findViewById(R.id.speed_tv);
        new Thread(() -> {
            for (; ; ) {
                if (run) {
                    int count = et.length();
                    long now = System.currentTimeMillis();
                    float rate = (float) count / ((now - start) / 1000F / 60F);
                    speedTV.setText(getString(R.string.type_speed, rate));
                    try {
                        //noinspection BusyWait
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        run = false;
        super.onDestroy();
    }
}
