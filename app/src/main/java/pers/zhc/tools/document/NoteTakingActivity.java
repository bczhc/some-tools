package pers.zhc.tools.document;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.widget.Button;
import android.widget.EditText;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.u.common.Documents;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NoteTakingActivity extends Document {
    @Override
    protected void onCreate(@Documents.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.take_note_activity);
        SQLiteDatabase db = getDB(this);
        EditText content_et = findViewById(R.id.doc_content_et);
        EditText title_et = findViewById(R.id.doc_title_et);
        Button insertBtn = findViewById(R.id.insert_record);
        insertBtn.setOnClickListener(v -> {
            Date date = new Date();
            String formatDate = SimpleDateFormat.getDateTimeInstance().format(date);
            ContentValues cv = new ContentValues();
            cv.put("date", formatDate);
            cv.put("title", title_et.getText().toString());
            cv.put("content", content_et.getText().toString());
            try {
                db.insertOrThrow("doc", null, cv);
                Snackbar snackbar = Snackbar.make(insertBtn, "记录成功", Snackbar.LENGTH_SHORT);
                snackbar.setAction(R.string.dismiss_x, v1 -> snackbar.dismiss());
                snackbar.show();
            } catch (SQLException e) {
                Common.showException(e, this);
            }
        });
    }
}
