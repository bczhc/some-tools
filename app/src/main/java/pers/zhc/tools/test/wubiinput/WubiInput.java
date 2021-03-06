package pers.zhc.tools.test.wubiinput;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.inputmethod.WubiIME;
import pers.zhc.tools.views.ScrollEditText;
import pers.zhc.tools.utils.sqlite.SQLite3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class WubiInput extends BaseActivity {
    private SQLite3 dictDB;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dictDB = WubiIME.getWubiDictDatabase(this);
        setContentView(R.layout.wubi_input_activity);
        TextView candidateTV = findViewById(R.id.candidate);
        EditText wubiCodeET = findViewById(R.id.code);
        EditText textOutET = ((ScrollEditText) findViewById(R.id.scroll_et)).getEditText();
        AtomicReference<TextWatcher> textWatcher = new AtomicReference<>();
        textWatcher.set(new TextWatcher() {
            private List<String> candidates;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            private void setText(String text) {
                wubiCodeET.removeTextChangedListener(textWatcher.get());
                wubiCodeET.setText(text);
                wubiCodeET.addTextChangedListener(textWatcher.get());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count >= before) {
                    CharSequence changed = s.subSequence(start, start + count);
                    if (s.length() == 5) {
                        setText(getString(R.string.nul));
                        try {
                            appendText(candidates.get(0));
                        } catch (Exception ignored) {
                        }
                        setText(changed.toString());
                        wubiCodeET.setSelection(wubiCodeET.length());
                        s = wubiCodeET.getText().toString();
                    }
                    if (changed.toString().equals(" ")) {
                        setText(getString(R.string.nul));
                        try {
                            appendText(candidates.get(0));
                        } catch (Exception ignored) {
                        }
                    }
                }
                candidates = new ArrayList<>();
                WubiInput.this.dictDB.exec("SELECT char FROM wubi_dict WHERE code is '" + s + "' ORDER BY num DESC", contents -> {
                    String candidate = contents[0];
                    candidates.add(candidate);
                    return 0;
                });
                candidateTV.setText(Arrays.toString(candidates.toArray(new String[0])));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

            private void appendText(String text) {
                textOutET.getText().insert(textOutET.length(), text);
            }
        });
        wubiCodeET.addTextChangedListener(textWatcher.get());
    }

    @Override
    public void finish() {
        dictDB.close();
        super.finish();
    }
}
