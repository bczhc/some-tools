package pers.zhc.tools.document;

import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.u.common.Documents;

public class NoteTakingActivity extends Document {
    @Override
    protected void onCreate(@Documents.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_note_activity);
        Intent intent = getIntent();
        long mills = intent.getLongExtra("mills", 0);
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        String bottom_btn_string = intent.getStringExtra("bottom_btn_string");
        boolean originCreate = intent.getBooleanExtra("origin", true);
        title = title == null ? getString(R.string.nul) : title;
        content = content == null ? getString(R.string.nul) : content;
        bottom_btn_string = bottom_btn_string == null || bottom_btn_string.equals("") ? getString(R.string.insert_record) : bottom_btn_string;
        SQLiteDatabase db = getDB(this);
        EditText content_et = findViewById(R.id.doc_content_et);
        EditText title_et = findViewById(R.id.doc_title_et);
        Button insertBtn = findViewById(R.id.insert_record);
        content_et.setText(String.format(getString(R.string.tv), content));
        title_et.setText(String.format(getString(R.string.tv), title));
        insertBtn.setText(String.format(getString(R.string.tv), bottom_btn_string));
        if (originCreate) {
            insertBtn.setOnClickListener(v -> {
                ContentValues cv = new ContentValues();
                cv.put("t", System.currentTimeMillis());
                cv.put("title", title_et.getText().toString());
                cv.put("content", content_et.getText().toString());
                try {
                    db.insertOrThrow("doc", null, cv);
                    Toast.makeText(this, R.string.recording_success, Toast.LENGTH_SHORT).show();
                } catch (SQLException e) {
                    Common.showException(e, this);
                }
            });
        } else {
            insertBtn.setOnClickListener(v -> {
                try {
                    ContentValues values = new ContentValues();
                    values.put("title", title_et.getText().toString());
                    values.put("content", content_et.getText().toString());
                    db.update("doc", values, "t=?", new String[]{String.valueOf(mills)});
                    Toast.makeText(this, R.string.updating_success, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Common.showException(e, this);
                }
            });
        }
    }
}
