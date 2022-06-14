package pers.zhc.tools.document;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.FilePicker;
import pers.zhc.tools.utils.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pers.zhc.tools.utils.DialogUtil.setDialogAttr;

/**
 * @author bczhc
 */
public class Document extends BaseActivity {
    private ScrollView sv;
    private SQLiteDatabase db;
    private File dbFile = null;
    private String state = "normal";
    private int chooseNum = 0;
    private RelativeLayout topView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.document_activity);
        Button insertBtn = findViewById(R.id.note_take);
        Button importBtn = findViewById(R.id.import_btn);
        Button exportBtn = findViewById(R.id.export_btn);
        insertBtn.setOnClickListener(v -> {
            Intent takingIntent = new Intent(this, NoteTakingActivity.class);
            startActivityForResult(takingIntent, RequestCode.START_ACTIVITY_1);
            overridePendingTransition(R.anim.in_left_and_bottom, 0);
        });
        Button deleteBtn = findViewById(R.id.delete_btn);
        deleteBtn.setOnClickListener(v -> {
            if (state.equals("del")) {
                try {
                    SQLiteStatement deleteStmt = db.compileStatement("DELETE FROM doc WHERE t=?");
                    db.beginTransaction();
                    for (int i = 0; i < ((LinearLayout) sv.getChildAt(0)).getChildCount(); i++) {
                        LinearLayout childLL = (LinearLayout) ((LinearLayout) sv.getChildAt(0)).getChildAt(i);

                        if (((TextView) (((LinearLayout) childLL.getChildAt(0))).getChildAt(0)).getCurrentTextColor() == 0xFFFF0000) {
                            long timestamp = ((LinearLayoutWithTimestamp) childLL).timestamp;
                            deleteStmt.bindLong(1, timestamp);
                            deleteStmt.execute();
                        }
                    }
                    deleteStmt.close();
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    ToastUtils.show(this, getString(R.string.deleted_notes, chooseNum));

                    setSVViews();
                    topView = findViewById(R.id.note_top_view);
                    topView.removeAllViews();
                } catch (Exception e) {
                    Common.showException(e, this);
                }
                View inflate = View.inflate(this, R.layout.note_top_view, null);
                topView = findViewById(R.id.note_top_view);
                topView.removeView(inflate);
                state = "normal";
            } else {
                state = "del";
                chooseNum = 0;
                View inflate = View.inflate(this, R.layout.note_top_view, null);
                topView = findViewById(R.id.note_top_view);
                topView.addView(inflate);
                ImageView close = findViewById(R.id.cancel_deletion);
                close.setOnClickListener(v1 -> {
                    topView = findViewById(R.id.note_top_view);
                    topView.removeAllViews();
                    state = "normal";
                    for (int i = 0; i < ((LinearLayout) sv.getChildAt(0)).getChildCount(); i++) {
                        LinearLayout childLL = (LinearLayout) ((LinearLayout) sv.getChildAt(0)).getChildAt(i);
                        if (((TextView) (((LinearLayout) childLL.getChildAt(0))).getChildAt(0)).getCurrentTextColor() == 0xFFFF0000) {
                            childLL.setBackground(getDrawable(R.drawable.view_stroke));
                            for (int i1 = 0; i1 < childLL.getChildCount(); i1++) {
                                ((TextView) ((LinearLayout) childLL.getChildAt(i1)).getChildAt(0)).setTextColor(0xFF808080);
                            }

                        }
                    }
                });
                final CheckBox chooseAll = findViewById(R.id.choose_all);
                chooseAll.setOnClickListener(v2 -> {
                    //使用setOnClickListener以防止setChecked触发setOnCheckedChangeListener
                    TextView ttv = topView.findViewById(R.id.top_tv);
                    if (chooseAll.isChecked()) {
                        for (int i = 0; i < ((LinearLayout) sv.getChildAt(0)).getChildCount(); i++) {
                            LinearLayout childLL = (LinearLayout) ((LinearLayout) sv.getChildAt(0)).getChildAt(i);
                            if (((TextView) (((LinearLayout) childLL.getChildAt(0))).getChildAt(0)).getCurrentTextColor() == 0xFF808080) {
                                childLL.setBackground(getDrawable(R.drawable.view_stroke_red));
                                for (int i1 = 0; i1 < childLL.getChildCount(); i1++) {
                                    ((TextView) ((LinearLayout) childLL.getChildAt(i1)).getChildAt(0)).setTextColor(0xFFFF0000);
                                }
                            }
                        }
                        chooseNum = ((LinearLayout) sv.getChildAt(0)).getChildCount();
                        ttv.setText(getString(R.string.selected_notes, chooseNum));

                    } else {
                        for (int i = 0; i < ((LinearLayout) sv.getChildAt(0)).getChildCount(); i++) {
                            LinearLayout childLL = (LinearLayout) ((LinearLayout) sv.getChildAt(0)).getChildAt(i);
                            if (((TextView) (((LinearLayout) childLL.getChildAt(0))).getChildAt(0)).getCurrentTextColor() == 0xFFFF0000) {
                                childLL.setBackground(getDrawable(R.drawable.view_stroke));
                                for (int i1 = 0; i1 < childLL.getChildCount(); i1++) {
                                    ((TextView) ((LinearLayout) childLL.getChildAt(i1)).getChildAt(0)).setTextColor(0xFF808080);
                                }
                            }
                        }
                        chooseNum = 0;
                        ttv.setText(R.string.no_notes_were_selected);
                    }
                });
                ToastUtils.show(this, R.string.note_deletion_tip);
            }
        });
        sv = findViewById(R.id.sv);
        importBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, FilePicker.class);
            intent.putExtra("option", FilePicker.PICK_FILE);
            startActivityForResult(intent, RequestCode.START_ACTIVITY_2);
        });
        exportBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, FilePicker.class);
            intent.putExtra("option", FilePicker.PICK_FOLDER);
            startActivityForResult(intent, RequestCode.START_ACTIVITY_3);
        });
        db = getDB(this);
        setSVViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCode.START_ACTIVITY_1:
                setSVViews();
                break;
            case RequestCode.START_ACTIVITY_2:
                if (data != null) {
                    String result = data.getStringExtra("result");
                    if (result == null) {
                        // cancelled
                        break;
                    }
                    File file = new File(result);
                    FileUtil.copy(file, dbFile);
                    final AlertDialog confirmationAlertDialog = DialogUtil.createConfirmationAlertDialog(this, (dialog, which) -> {
                                setSVViews();
                                ToastUtils.show(this, R.string.importing_succeeded);
                            }, (dialog, which) -> {
                                ToastUtils.show(this, R.string.importing_canceled);
                            }, R.string.whether_to_import_notes

                            , ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
                    if (file.exists()) {
                        if (((LinearLayout) sv.getChildAt(0)).getChildCount() != 0) {
                            confirmationAlertDialog.show();
                        } else {
                            setSVViews();
                            ToastUtils.show(this, R.string.importing_succeeded);
                        }
                    } else {
                        ToastUtils.show(this, R.string.copying_failed);
                    }
                }
                break;
            case RequestCode.START_ACTIVITY_3:
                if (data != null) {
                    final String destFileDir = data.getStringExtra("result");
                    if (destFileDir == null) {
                        // cancelled
                        break;
                    }
                    String dbPath = db.getPath();
                    File file = new File(dbPath);
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    View inflate = View.inflate(this, R.layout.export_notes, null);
                    final EditText filename = inflate.findViewById(R.id.filename);
                    filename.setText("doc");
                    final TextView tv = inflate.findViewById(R.id.export_notesTextview2);
                    if (!(new File(destFileDir + File.separator + "doc.db")).exists()) {
                        tv.setVisibility(View.INVISIBLE);
                    }
                    filename.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            if ((new File(destFileDir + File.separator + filename.getText() + ".db")).exists()) {
                                tv.setVisibility(View.VISIBLE);
                            } else {
                                tv.setVisibility(View.INVISIBLE);
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });
                    adb.setView(inflate);
                    adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                        try {
                            File destFile = new File(destFileDir + File.separator + filename.getText() + ".db");
                            FileUtil.copy(file, destFile);
                            if (destFile.exists()) {
                                ToastUtils.show(this, getString(R.string.exporting_succeeded) + "\n" + destFile.getCanonicalPath());
                            }
                        } catch (IOException e) {
                            Common.showException(e, this);
                        }
                    }).setNegativeButton(R.string.cancel, (dialog, which) -> {

                    });
                    Dialog ad = adb.create();
                    setDialogAttr(ad, false, ViewGroup.LayoutParams.WRAP_CONTENT
                            , ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    ad.show();
                }
                break;
            default:
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
        int margin = DisplayUtil.px2sp(this, 10);
        ExecutorService es = Executors.newCachedThreadPool();
        String[] sqliteOptions = getResources().getStringArray(R.array.sqlite_options);
        if (cursor.moveToFirst()) {
            es.execute(() -> {
                do {
                    LinearLayoutWithTimestamp llWithTimestamp = new LinearLayoutWithTimestamp(this);
                    llWithTimestamp = new LinearLayoutWithTimestamp(this);
                    llWithTimestamp.setOrientation(LinearLayout.HORIZONTAL);
                    ll_lp.setMargins(margin, margin, margin, margin);
                    llWithTimestamp.setLayoutParams(ll_lp);
                    {//i = 0
                        LinearLayout smallLL = new LinearLayout(this);
                        long millisecond = cursor.getLong(0);
                        llWithTimestamp.timestamp = millisecond;
                        LinearLayoutWithTimestamp finalLlWithTimestamp = llWithTimestamp;
                        llWithTimestamp.setOnClickListener(v -> {
                            topView = findViewById(R.id.note_top_view);
                            TextView ttv = topView.findViewById(R.id.top_tv);
                            if (state.equals("del")) {
                                if (((TextView) ((LinearLayout) finalLlWithTimestamp.getChildAt(0)).getChildAt(0)).getCurrentTextColor() != 0xFFFF0000) {
                                    finalLlWithTimestamp.setBackground(getDrawable(R.drawable.view_stroke_red));
                                    chooseNum++;
                                    ttv.setText(getString(R.string.selected_notes, chooseNum));
                                    for (int i = 0; i < finalLlWithTimestamp.getChildCount(); i++) {
                                        ((TextView) ((LinearLayout) finalLlWithTimestamp.getChildAt(i)).getChildAt(0)).setTextColor(0xFFFF0000);
                                    }
                                } else {
                                    finalLlWithTimestamp.setBackground(getDrawable(R.drawable.view_stroke));
                                    chooseNum--;
                                    CheckBox chooseAll = findViewById(R.id.choose_all);
                                    if (chooseAll.isChecked()) {
                                        chooseAll.setChecked(false);
                                    }
                                    if (chooseNum == 0) {
                                        ttv.setText(R.string.no_notes_were_selected);
                                    } else {
                                        ttv.setText(getString(R.string.selected_notes, chooseNum));
                                    }
                                    for (int i = 0; i < finalLlWithTimestamp.getChildCount(); i++) {
                                        ((TextView) ((LinearLayout) finalLlWithTimestamp.getChildAt(i)).getChildAt(0)).setTextColor(0xFF808080);
                                    }

                                }
                            } else {
                                Dialog dialog = new Dialog(this);
                                LinearLayout linearLayout1 = new LinearLayout(this);
                                linearLayout1.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                linearLayout1.setOrientation(LinearLayout.VERTICAL);
                                DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
                                Button[] buttons = new Button[2];
                                View.OnClickListener[] onClickListeners = new View.OnClickListener[]{
                                        v1 -> {
                                            try {
                                                Intent intent = new Intent(this, NoteTakingActivity.class);
                                                intent.putExtra("origin", false);
                                                Cursor c = db.rawQuery("SELECT * FROM doc WHERE t=" + millisecond, null);
                                                c.moveToFirst();
                                                NoteTakingActivity.title = c.getString(1);
                                                NoteTakingActivity.content = c.getString(2);
                                                c.close();
                                                intent.putExtra("bottom_btn_string", getString(R.string.modification_record));
                                                intent.putExtra("millisecond", millisecond);
                                                startActivityForResult(intent, RequestCode.START_ACTIVITY_1);
                                                dialog.dismiss();
                                                overridePendingTransition(R.anim.in_left_and_bottom, 0);
                                            } catch (IndexOutOfBoundsException e) {
                                                e.printStackTrace();
                                                ToastUtils.show(this, e.toString());
                                            }
                                        },
                                        v1 -> {
                                            AlertDialog confirmationAD = DialogUtil.createConfirmationAlertDialog(this, (dialog1, which) -> {
                                                try {
                                                    db.execSQL("DELETE FROM doc WHERE t=" + millisecond);
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
                                    buttons[i].setText(String.format(getString(R.string.str), sqliteOptions[i]));
                                    buttons[i].setOnClickListener(onClickListeners[i]);
                                    linearLayout1.addView(buttons[i]);
                                }
                                dialog.setContentView(linearLayout1);
                                dialog.setCanceledOnTouchOutside(true);
                                dialog.show();
                            }
                        });
                        Date date = new Date(millisecond);
                        String formatDate = SimpleDateFormat.getDateTimeInstance().format(date);
                        setSmallTVExtracted(smallLL_LP4, llWithTimestamp, smallLL, formatDate);
                    }
                    for (int i = 1; i < 3; i++) {
                        LinearLayout smallLL = new LinearLayout(this);
                        String s = cursor.getString(i);
                        int length = s.length();
                        setSmallTVExtracted(smallLL_LP4, llWithTimestamp, smallLL, length > 100 ? (s.substring(0, 100) + "\n...") : s);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        llWithTimestamp.setBackground(getDrawable(R.drawable.view_stroke));
                    }
                    LinearLayoutWithTimestamp finalLlWithTimestamp1 = llWithTimestamp;
                    runOnUiThread(() -> linearLayout.addView(finalLlWithTimestamp1));
                } while (cursor.moveToNext());
                cursor.close();
            });
        }
    }

    private void setSmallTVExtracted(LinearLayout.LayoutParams smallLL_LP4, LinearLayout ll, LinearLayout smallLL, String s) {
        smallLL.setLayoutParams(smallLL_LP4);
        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setText(String.format(getString(R.string.str), s));
        runOnUiThread(() -> {
            smallLL.addView(tv);
            tv.setTextSize(15F);
            tv.setTextColor(0xFF808080);
            ll.addView(smallLL);
        });
    }

    SQLiteDatabase getDB(AppCompatActivity ctx) {
        /*DocDB db = new DocDB(ctx, "a", null, 1);
        return db.getWritableDatabase();*/
        SQLiteDatabase database = null;
        File dbPath = Common.getInternalDatabaseDir(this);
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
        if (database != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                database.disableWriteAheadLogging();
            }
        }
        return database;
    }

    @Override
    public void onBackPressed() {
        if (state.equals("del")) {
            topView = findViewById(R.id.note_top_view);
            topView.removeAllViews();
            state = "normal";
            for (int i = 0; i < ((LinearLayout) sv.getChildAt(0)).getChildCount(); i++) {
                LinearLayout childLL = (LinearLayout) ((LinearLayout) sv.getChildAt(0)).getChildAt(i);
                if (((TextView) (((LinearLayout) childLL.getChildAt(0))).getChildAt(0)).getCurrentTextColor() == 0xFFFF0000) {
                    childLL.setBackground(getDrawable(R.drawable.view_stroke));
                    for (int i1 = 0; i1 < childLL.getChildCount(); i1++) {
                        ((TextView) ((LinearLayout) childLL.getChildAt(i1)).getChildAt(0)).setTextColor(0xFF808080);
                    }
                }
            }
        } else {
            super.onBackPressed();
            overridePendingTransition(0, R.anim.slide_out_bottom);
        }
    }

    private static class LinearLayoutWithTimestamp extends LinearLayout {

        private long timestamp;

        public LinearLayoutWithTimestamp(Context context) {
            super(context);
        }
    }
}
