package pers.zhc.tools.test.youdaoapi;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SpinnerAdapter;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ScrollEditText;

import java.util.ArrayList;
import java.util.List;

public class YouDaoTranslate extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.you_dao_activity);
        EditText et = ((ScrollEditText) findViewById(R.id.content)).getEditText();
        ScrollEditText out = findViewById(R.id.out);
        List<String> data = new ArrayList<>();
        SpinnerAdapter adapter = new ArrayAdapter<>(this, R.layout.adapter_layout, data);
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                /*String text = s.toString();
                Handler handler = new Handler();
                new Thread(() -> {
                    try {
                        String translate = YouDao.translate(text, null, null);
                        handler.post(() -> out.setText(translate));
                    } catch (IOException e) {
                        handler.post(() -> ToastUtils.showError(YouDaoTranslate.this, R.string.translate_error, e));
                    }
                }).start();*/
            }
        });
    }
}
