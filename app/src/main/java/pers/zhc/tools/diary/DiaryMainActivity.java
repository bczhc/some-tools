package pers.zhc.tools.diary;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.SQLite;

import java.util.Calendar;

/**
 * @author bczhc
 */
public class DiaryMainActivity extends BaseActivity {
    private SQLiteDatabase diaryDatabase;
    private LinearLayout ll;
    private String currentPassword;

    static SQLiteDatabase getDiaryDatabase(Context ctx) {
        final SQLiteDatabase diaryDatabase = SQLiteDatabase.openOrCreateDatabase(Common.getInternalDatabaseDir(ctx, "diary.db"), null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            diaryDatabase.disableWriteAheadLogging();
        }
        diaryDatabase.execSQL("CREATE TABLE IF NOT EXISTS diary(\n" +
                "    date text not null,\n" +
                "    content text not null\n" +
                ")");
        return diaryDatabase;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SQLiteDatabase passwordDatabase = getPasswordDatabase();
        final Cursor cursor = passwordDatabase.rawQuery("SELECT pw FROM passwords where k=?", new String[]{"diary"});
        if (cursor.moveToFirst()) {
            currentPassword = cursor.getString(cursor.getColumnIndex("pw"));
        }
        cursor.close();
        if (currentPassword == null) {
            load();
            return;
        }
        setContentView(R.layout.password_view);
        EditText passwordET = findViewById(R.id.password_et);
        String finalPassword = currentPassword;
        passwordET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String text = s.toString();
                if (finalPassword.equals(text)) {
                    passwordET.setEnabled(false);
                    load();
                }
            }
        });
    }

    private void load() {
        setContentView(R.layout.diary_activity);
        ll = findViewById(R.id.ll);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.diary);
            actionBar.show();
        }
        diaryDatabase = getDiaryDatabase(this);
        refresh();
    }


    private SQLiteDatabase getPasswordDatabase() {
        final SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(
                Common.getInternalDatabaseDir(this, "passwords.db"), null);
        db.disableWriteAheadLogging();
        db.execSQL("CREATE TABLE IF NOT EXISTS passwords(\n" +
                "    k text not null,\n" +
                "    pw text\n" +
                ")");
        return db;
    }

    private void setPassword(String password) {
        final SQLiteDatabase passwordDatabase = getPasswordDatabase();
        final boolean exist = SQLite.checkRecordExistence(passwordDatabase, "passwords", "k", "diary");
        ContentValues cv = new ContentValues();
        cv.put("k", "diary");
        cv.put("pw", password);
        if (exist) {
            passwordDatabase.update("passwords", cv, "k=?", new String[]{"diary"});
        } else {
            try {
                passwordDatabase.insertOrThrow("passwords", null, cv);
            } catch (SQLException e) {
                Common.showException(e, this);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.diary_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.write:
                createDiary(null);
                break;
            case R.id.password:
                changePassword();
                break;
            default:
                break;
        }
        return true;
    }

    private void changePassword() {
        View view = View.inflate(this, R.layout.change_password_view, null);
        Dialog dialog = new Dialog(this);
        DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.WRAP_CONTENT, false);
        EditText oldPasswordET = view.findViewById(R.id.old_password);
        EditText newPasswordET = view.findViewById(R.id.new_password);
        Button confirm = view.findViewById(R.id.confirm);
        confirm.setOnClickListener(v -> {
            if (oldPasswordET.getText().toString().equals(currentPassword == null ? "" : currentPassword)) {
                setPassword(newPasswordET.getText().toString());
                ToastUtils.show(this, R.string.change_success);
                dialog.dismiss();
            } else {
                ToastUtils.show(this, R.string.password_not_matching);
            }
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void createDiary(@Nullable DiaryTakingActivity.MyDate date) {
        Intent intent = new Intent(this, DiaryTakingActivity.class);
        final int[] dateInts;
        if (date == null) {
            final Calendar calendar = Calendar.getInstance();
            final int year = calendar.get(Calendar.YEAR);
            final int month = calendar.get(Calendar.MONTH);
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            dateInts = new int[]{year, month, day};
        } else {
            dateInts = new int[]{date.year, date.month, date.day};
        }
        intent.putExtra("date", dateInts);
        startActivityForResult(intent, RequestCode.START_ACTIVITY_0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCode.START_ACTIVITY_0) {
            refresh();
        }
    }

    private void refresh() {
        ll.removeAllViews();
        new Thread(() -> {
            final Cursor cursor = diaryDatabase.rawQuery("SELECT * FROM diary", null);
            final int dateColIndex = cursor.getColumnIndex("date");
            final int contentColIndex = cursor.getColumnIndex("content");
            if (cursor.moveToFirst()) {
                do {
                    final String dateString = cursor.getString(dateColIndex);
                    final String content = cursor.getString(contentColIndex);
                    RelativeLayout childRL = new RelativeLayout(this);
                    final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    childRL.setLayoutParams(layoutParams);
                    TextView dateTV = new TextView(this);
                    dateTV.setId(R.id.tv1);
                    RelativeLayout.LayoutParams dateLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    dateTV.setLayoutParams(dateLP);
                    dateTV.setTextColor(Color.parseColor("#1565C0"));
                    dateTV.setTextSize(30);
                    TextView previewTV = new TextView(this);
                    previewTV.setId(R.id.tv2);
                    RelativeLayout.LayoutParams previewLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    previewLP.addRule(RelativeLayout.BELOW, R.id.tv1);
                    previewTV.setLayoutParams(previewLP);
                    dateTV.setText(dateString);
                    previewTV.setText(content);
                    childRL.setOnClickListener(v -> createDiary(new DiaryTakingActivity.MyDate(dateString)));
                    childRL.setOnLongClickListener(v -> {
                        DialogUtil.createConfirmationAlertDialog(this, (dialog, which) -> {
                                    diaryDatabase.delete("diary", "date=?", new String[]{dateString});
                                    refresh();
                                }, (dialog, which) -> {
                                }, R.string.whether_to_delete, ViewGroup.LayoutParams.MATCH_PARENT
                                , ViewGroup.LayoutParams.WRAP_CONTENT, false).show();
                        return true;
                    });
                    runOnUiThread(() -> {
                        childRL.addView(dateTV);
                        childRL.addView(previewTV);
                        ll.addView(childRL);
                    });
                } while (cursor.moveToNext());
            }
            cursor.close();
        }).start();
    }
}
