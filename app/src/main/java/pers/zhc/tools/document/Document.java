package pers.zhc.tools.document;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.*;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DisplayUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Document extends BaseActivity {
    private ScrollView sv;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.document_activity);
        Button insertBtn = findViewById(R.id.note_take);
        insertBtn.setOnClickListener(v -> startActivityForResult(new Intent(this, NoteTakingActivity.class), 41));
        Button deleteBtn = findViewById(R.id.delete_btn);
        deleteBtn.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            EditText et = new EditText(this);
            adb.setTitle("请输入要删除的id（*表示全部）")
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        String s = et.getText().toString();
                        if (s.matches(".*\\*.*")) {
                            try {
                                db.execSQL("DELETE FROM doc;");
                            } catch (Exception e) {
                                Common.showException(e, this);
                            }
                        } else {
                            try {
                                int i = Integer.parseInt(s);
                                db.execSQL("DELETE FROM doc WHERE id=" + i);
                            } catch (Exception e) {
                                Common.showException(e, this);
                            }
                        }
                        setSVViews();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    })
                    .setView(et)
                    .show();
        });
        sv = findViewById(R.id.sv);
        db = getDB(this);
        setSVViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 41) {
            setSVViews();
        }
    }

    private void setSVViews() {
        sv.removeAllViews();
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        sv.addView(linearLayout);
        Cursor cursor = db.rawQuery("SELECT * FROM doc", null);
        LinearLayout.LayoutParams ll_lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams smallLL_LP4 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 4F);
//        LinearLayout.LayoutParams smallLL_LP1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1F);
        int margin = DisplayUtil.px2sp(this, 10);
        ExecutorService es = Executors.newCachedThreadPool();
        if (cursor.moveToFirst()) {
            es.execute(() -> {
                do {
                    LinearLayout ll = new LinearLayout(this);
                    ll_lp.setMargins(margin, margin, margin, margin);
                    ll.setLayoutParams(ll_lp);
                    for (int i = 0; i < 3; i++) {
                        LinearLayout smallLL = new LinearLayout(this);
                        String s = cursor.getString(i);
                        smallLL.setLayoutParams(smallLL_LP4);
                        TextView tv = new TextView(this);
                        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        tv.setText(String.format(getString(R.string.tv), s));
                        runOnUiThread(() -> {
                            smallLL.addView(tv);
                            tv.setTextSize(15F);
                            ll.addView(smallLL);
                        });
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ll.setBackground(getDrawable(R.drawable.view_stroke));
                    }
                    runOnUiThread(() -> linearLayout.addView(ll));
                } while (cursor.moveToNext());
                cursor.close();
            });
        }
    }

    SQLiteDatabase getDB(Context ctx) {
        DocDB db = new DocDB(ctx, "a", null, 1);
        return db.getWritableDatabase();
    }
}
