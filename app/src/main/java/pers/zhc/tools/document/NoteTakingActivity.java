package pers.zhc.tools.document;

import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.views.ScrollEditText;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.common.Documents;

/**
 * @author bczhc
 */
public class NoteTakingActivity extends Document {
    static String content = null;
    static String title = null;
    private boolean textChanged;
    private Button insertBtn;

    @Override
    protected void onCreate(@Documents.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_note_activity);
        Intent intent = getIntent();
        long millisecond = intent.getLongExtra("millisecond", 0);
        String bottom_btn_string = intent.getStringExtra("bottom_btn_string");
        boolean originCreate = intent.getBooleanExtra("origin", true);
        title = title == null ? getString(R.string.nul) : title;
        content = content == null ? getString(R.string.nul) : content;
        bottom_btn_string = bottom_btn_string == null || "".equals(bottom_btn_string) ? getString(R.string.add_record) : bottom_btn_string;
        SQLiteDatabase db = getDB(this);
        EditText title_et = findViewById(R.id.doc_title_et);
        ScrollEditText content_et = findViewById(R.id.doc_content_et);
        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) {
                    textChanged = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                textChanged = true;
            }
        };
        content_et.setText(String.format(getString(R.string.str), content));
        title_et.setText(String.format(getString(R.string.str), title));
        content_et.getEditText().addTextChangedListener(textWatcher);
        title_et.addTextChangedListener(textWatcher);
        insertBtn = findViewById(R.id.add_record);
        insertBtn.setText(String.format(getString(R.string.str), bottom_btn_string));
        insertBtn.setOnClickListener(v -> {
            if (originCreate) {
                ContentValues cv = new ContentValues();
                cv.put("t", System.currentTimeMillis());
                cv.put("title", title_et.getText().toString());
                cv.put("content", content_et.getText().toString());
                try {
                    db.insertOrThrow("doc", null, cv);
                    ToastUtils.show(this, R.string.recording_succeeded);
                } catch (SQLException e) {
                    Common.showException(e, this);
                }
            } else {
                try {
                    ContentValues values = new ContentValues();
                    values.put("title", title_et.getText().toString());
                    values.put("content", content_et.getText().toString());
                    db.update("doc", values, "t=?", new String[]{String.valueOf(millisecond)});
                    ToastUtils.show(this, R.string.updating_succeeded);
                } catch (Exception e) {
                    Common.showException(e, this);
                }
            }
            textChanged = false;
        });
    }

    @Override
    public void onBackPressed() {
        if (textChanged) {
            final AlertDialog confirmationAlertDialog = DialogUtil.createConfirmationAlertDialog(this, (dialog, which) -> {
                        insertBtn.performClick();
                        clearStr();
                        super.onBackPressed();
                    }, (dialog, which) -> {
                        clearStr();
                        super.onBackPressed();
                    }, R.string.whether_to_save_unsaved_content
                    , ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
            confirmationAlertDialog.show();
        } else {
            clearStr();
            super.onBackPressed();
        }
    }

    private void clearStr() {
        title = null;
        content = null;
        System.gc();
    }
}
