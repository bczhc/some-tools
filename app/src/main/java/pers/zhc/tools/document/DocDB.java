package pers.zhc.tools.document;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import pers.zhc.u.common.Documents;

class DocDB extends SQLiteOpenHelper {
    DocDB(@Documents.Nullable Context context, @Documents.Nullable String name, @Documents.Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE doc(\n" +
                "    date varchar(13) not null,\n" +
                "    title varchar(1048576) not null,\n" +
                "    content varchar(10485760) not null\n" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("ALTER TABLE doc add sex varchar(8);");
    }
}
