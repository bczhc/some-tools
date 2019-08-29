package pers.zhc.tools.document;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.Picker;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.DisplayUtil;
import pers.zhc.tools.utils.ViewWithExtras;
import pers.zhc.u.FileU;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Document extends BaseActivity {
    private ScrollView sv;
    private SQLiteDatabase db;
    private File dbFile = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.document_activity);
        Button insertBtn = findViewById(R.id.note_take);
        Button importBtn = findViewById(R.id.import_btn);
        Button exportBtn = findViewById(R.id.export_btn);
        insertBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteTakingActivity.class);
            startActivityForResult(intent, 41);
            overridePendingTransition(R.anim.in_left_and_bottom, 0);
        });
        Button deleteBtn = findViewById(R.id.delete_btn);
        deleteBtn.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            EditText et = new EditText(this);
            adb.setTitle("请输入要删除的t_mills（*表示全部）")
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        String s = et.getText().toString();
                        if (s.matches(".*\\*.*")) {
                            try {
                                db.delete("doc", null, null);
                            } catch (Exception e) {
                                Common.showException(e, this);
                            }
                        } else {
                            try {
                                int i = Integer.parseInt(s);
                                db.delete("doc", "id=?", new String[]{String.valueOf(i)});
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
        importBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, Picker.class);
            intent.putExtra("option", Picker.PICK_FILE);
            startActivityForResult(intent, 51);
        });
        exportBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, Picker.class);
            intent.putExtra("option", Picker.PICK_FOLDER);
            startActivityForResult(intent, 61);
        });
        db = getDB(this);
        setSVViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 41:
                setSVViews();
                break;
            case 51:
                if (data != null) {
                    File file = new File(data.getStringExtra("result"));
                    try {
                        FileU.FileCopy(file, dbFile, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Common.showException(e, this);
                        return;
                    }
                    if (file.exists()) Toast.makeText(this, R.string.importing_cuccess, Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this, R.string.copying_failed, Toast.LENGTH_SHORT).show();
                    setSVViews();
                }
                break;
            case 61:
                if (data != null) {
                    String destFileDir = data.getStringExtra("result");
                    String dbPath = db.getPath();
                    File file = new File(dbPath);
                    String dbName = file.getName();
                    try {
                        File destFile = new File(destFileDir + File.separator + dbName);
                        FileU.FileCopy(file, destFile);
                        if (destFile.exists())
                            Toast.makeText(this, getString(R.string.exporting_success) + "\n" + destFile.getCanonicalPath(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Common.showException(e, this);
                    }
                }
                break;
        }
    }

    private void setSVViews() {
        db = getDB(this);
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
        String[] sqliteOptions = getResources().getStringArray(R.array.sqlite_options);
        if (cursor.moveToFirst()) {
            es.execute(() -> {
                do {
                    ViewWithExtras<LinearLayout, Long> ll = new ViewWithExtras<>();
                    ll.a = new LinearLayout(this);
                    ll.a.setOrientation(LinearLayout.HORIZONTAL);
                    ll_lp.setMargins(margin, margin, margin, margin);
                    ll.a.setLayoutParams(ll_lp);
                    {//i = 0
                        LinearLayout smallLL = new LinearLayout(this);
                        long mills = cursor.getLong(0);
                        ll.extra = mills;
                        ll.a.setOnClickListener(v -> {
                            Dialog dialog = new Dialog(this);
                            LinearLayout linearLayout1 = new LinearLayout(this);
                            linearLayout1.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            linearLayout1.setOrientation(LinearLayout.VERTICAL);
                            DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
                            Button[] buttons = new Button[2];
                            View.OnClickListener[] onClickListeners = new View.OnClickListener[]{
                                    v1 -> {
                                        Intent intent = new Intent(this, NoteTakingActivity.class);
                                        intent.putExtra("origin", false);
                                        Cursor c = db.rawQuery("SELECT * FROM doc WHERE t=" + mills, null);
                                        c.moveToFirst();
                                        intent.putExtra("title", c.getString(1));
                                        intent.putExtra("content", c.getString(2));
                                        c.close();
                                        intent.putExtra("bottom_btn_string", getString(R.string.modification_record));
                                        intent.putExtra("mills", mills);
                                        startActivityForResult(intent, 41);
                                        dialog.dismiss();
                                        overridePendingTransition(R.anim.in_left_and_bottom, 0);
                                    },
                                    v1 -> {
                                        android.support.v7.app.AlertDialog confirmationAD = DialogUtil.createConfirmationAD(this, (dialog1, which) -> {
                                            try {
                                                db.execSQL("DELETE FROM doc WHERE t=" + mills);
                                            } catch (Exception e) {
                                                Common.showException(e, this);
                                            }
                                            setSVViews();
                                            dialog.dismiss();
                                        }, (dialog1, which) -> {
                                        }, R.string.whether_to_delete, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
                                        confirmationAD.show();
                                    }
                            };
                            for (int i = 0; i < buttons.length; i++) {
                                buttons[i] = new Button(this);
                                buttons[i].setText(String.format(getString(R.string.tv), sqliteOptions[i]));
                                buttons[i].setOnClickListener(onClickListeners[i]);
                                linearLayout1.addView(buttons[i]);
                            }
                            dialog.setContentView(linearLayout1);
                            dialog.setCanceledOnTouchOutside(true);
                            dialog.show();
                        });
                        Date date = new Date(mills);
                        String formatDate = SimpleDateFormat.getDateTimeInstance().format(date);
                        setSmallTVExtracted(smallLL_LP4, ll.a, smallLL, formatDate);
                    }
                    for (int i = 1; i < 3; i++) {
                        LinearLayout smallLL = new LinearLayout(this);
                        String s = cursor.getString(i);
                        setSmallTVExtracted(smallLL_LP4, ll.a, smallLL, s);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ll.a.setBackground(getDrawable(R.drawable.view_stroke));
                    }
                    runOnUiThread(() -> linearLayout.addView(ll.a));
                } while (cursor.moveToNext());
                cursor.close();
            });
        }
    }

    private void setSmallTVExtracted(LinearLayout.LayoutParams smallLL_LP4, LinearLayout ll, LinearLayout smallLL, String s) {
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

    SQLiteDatabase getDB(Activity ctx) {
        /*DocDB db = new DocDB(ctx, "a", null, 1);
        return db.getWritableDatabase();*/
        SQLiteDatabase database = null;
        File dbPath = new File(getFilesDir().toString() + File.separator + "db");
        if (!dbPath.exists()) {
            System.out.println("dbPath.mkdirs() = " + dbPath.mkdirs());
        }
        try {
            dbFile = new File(dbPath.getPath() + File.separator + "doc.db");
            database = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            database.execSQL("CREATE TABLE IF NOT EXISTS doc(\n" +
                    "    t long,\n" +
                    "    title text not null,\n" +
                    "    content text not null\n" +
                    ");");
        } catch (Exception e) {
            e.printStackTrace();
            Common.showException(e, ctx);
        }
        return database;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}