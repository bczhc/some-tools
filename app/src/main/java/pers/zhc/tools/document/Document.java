package pers.zhc.tools.document;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Button;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;

public class Document extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.document_activity);
        Button btn = findViewById(R.id.note_take);
        DocDB db = new DocDB(this, "a", null, 1);
        SQLiteDatabase writableDatabase = db.getWritableDatabase();
        btn.setOnClickListener(v -> {
            ContentValues cv = new ContentValues();
            cv.put("date", "date");
            cv.put("title", "t");
            cv.put("content", "c");
            writableDatabase.insert("doc", null, cv);
            cv.put("date", 6);
            cv.put("title", "no!!!!!!");
            cv.put("content", "完美的一天就是这样度过的，我真的，我感觉我要凉了。");
            writableDatabase.insert("doc", null, cv);
            Cursor cursor = null;
            try {
                cursor = writableDatabase.rawQuery("SELECT * FROM doc", null);
            } catch (Exception e) {
                Common.showException(e, this);
                e.printStackTrace();
            }
            if (cursor != null) {
                cursor.moveToFirst();
                for (int i = 0; i < 3; i++) {
                    Log.d("sql", cursor.getString(i));
                }
                while (cursor.moveToNext()) {
                    for (int i = 0; i < 3; i++) {
                        Log.d("sql", cursor.getString(i));
                    }
                }
                cursor.close();
            }
        });
    }
}
