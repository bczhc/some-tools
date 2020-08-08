package pers.zhc.tools.diary;

import android.app.ActionBar;
import android.app.AlertDialog;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import pers.zhc.tools.filepicker.FilePicker;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.SQLite;
import pers.zhc.u.FileU;
import pers.zhc.u.Latch;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author bczhc
 */
public class DiaryMainActivity extends BaseActivity {
    private SQLiteDatabase diaryDatabase;
    private LinearLayout ll;
    private String currentPassword;
    private boolean isUnlocked = false;
    private String[] week;

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
        this.week = getResources().getStringArray(R.array.weeks);
        if (currentPassword == null || currentPassword.isEmpty()) {
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
        isUnlocked = true;
        setContentView(R.layout.diary_activity);
        invalidateOptionsMenu();
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
                "    pw text not null\n" +
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
        if (isUnlocked) {
            final MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.diary_actionbar, menu);
        }
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
            case R.id.export:
                Intent intent1 = new Intent(this, FilePicker.class);
                intent1.putExtra("option", FilePicker.PICK_FOLDER);
                startActivityForResult(intent1, RequestCode.START_ACTIVITY_1);
                break;
            case R.id.import_:
                Intent intent2 = new Intent(this, FilePicker.class);
                intent2.putExtra("option", FilePicker.PICK_FILE);
                startActivityForResult(intent2, RequestCode.START_ACTIVITY_2);
            default:
                break;
        }
        return true;
    }

    private void importDiary(File file) {
        final File databaseFile = Common.getInternalDatabaseDir(this, "diary.db");
        final Latch latch = new Latch();
        latch.suspend();
        AtomicBoolean status = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                status.set(FileU.FileCopy(file, databaseFile));
            } catch (IOException e) {
                Common.showException(e, this);
            } finally {
                latch.stop();
            }
        }).start();
        latch.await();
        if (status.get()) {
            ToastUtils.show(this, R.string.importing_success);
        } else {
            ToastUtils.show(this, R.string.copying_failure);
        }
        diaryDatabase = getDiaryDatabase(this);
        refresh();
    }

    private void exportDiary(File dir) {
        final File databaseFile = Common.getInternalDatabaseDir(this, "diary.db");
        final Latch latch = new Latch();
        latch.suspend();
        AtomicBoolean status = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                status.set(FileU.FileCopy(databaseFile, new File(dir, "diary.db")));
            } catch (IOException e) {
                Common.showException(e, this);
            } finally {
                latch.stop();
            }
        }).start();
        latch.await();
        if (status.get()) {
            ToastUtils.show(this, R.string.exporting_success);
        } else {
            ToastUtils.show(this, R.string.copying_failure);
        }
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
        switch (requestCode) {
            case RequestCode.START_ACTIVITY_0:
                refresh();
                break;
            case RequestCode.START_ACTIVITY_1:
                if (data == null) {
                    break;
                }
                final String dir = data.getStringExtra("result");
                if (dir == null) {
                    break;
                }
                exportDiary(new File(dir));
                break;
            case RequestCode.START_ACTIVITY_2:
                if (data == null) {
                    break;
                }
                final String file = data.getStringExtra("result");
                if (file == null) {
                    break;
                }
                importDiary(new File(file));
                break;
            default:
                break;
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
                    String weekString = null;
                    try {
                        final DiaryTakingActivity.MyDate myDate = new DiaryTakingActivity.MyDate(dateString);
                        final Calendar calendar = Calendar.getInstance();
                        calendar.set(myDate.year, myDate.month, myDate.day);
                        final int weekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                        weekString = this.week[weekIndex];
                    } catch (Exception e) {
                        Common.showException(e, this);
                    }
                    dateTV.setText(dateString + " " + weekString);
                    previewTV.setText(content.length() > 100 ? (content.substring(0, 100) + "...") : content);
                    setChildRL(dateString, childRL);
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

    private void setChildRL(String dateString, RelativeLayout childRL) {
        childRL.setOnClickListener(v -> {
            try {
                createDiary(new DiaryTakingActivity.MyDate(dateString));
            } catch (Exception e) {
                Common.showException(e, this);
            }
        });
        childRL.setOnLongClickListener(v -> {
            Dialog dialog = new Dialog(this);
            DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
            LinearLayout ll = new LinearLayout(this);
            ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ll.setOrientation(LinearLayout.VERTICAL);
            final Button changeDateBtn = new Button(this);
            final Button deleteBtn = new Button(this);
            changeDateBtn.setText(R.string.change_date);
            deleteBtn.setText(R.string.delete);
            ll.addView(changeDateBtn);
            ll.addView(deleteBtn);
            changeDateBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            changeDateBtn.setOnClickListener(v1 -> {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                EditText dateET = new EditText(this);
                final AlertDialog d2 = adb.setTitle(R.string.enter_new_date)
                        .setPositiveButton(R.string.confirm, (dialog1, which) -> {
                            final String newDateString = dateET.getText().toString();
                            final DiaryTakingActivity.MyDate newDate;
                            try {
                                newDate = new DiaryTakingActivity.MyDate(newDateString);
                            } catch (Exception e) {
                                ToastUtils.show(this, R.string.please_type_correct_value);
                                return;
                            }
                            changeDate(dateString, newDate);
                            dialog.dismiss();
                            refresh();
                        })
                        .setNegativeButton(R.string.cancel, (dialog1, which) -> {
                        })
                        .setView(dateET)
                        .create();
                DialogUtil.setDialogAttr(d2, false, ViewGroup.LayoutParams.MATCH_PARENT
                        , ViewGroup.LayoutParams.WRAP_CONTENT, false);
                d2.show();
            });
            deleteBtn.setOnClickListener(v1 -> DialogUtil.createConfirmationAlertDialog(this, (d, which) -> {
                        diaryDatabase.delete("diary", "date=?", new String[]{dateString});
                        dialog.dismiss();
                        refresh();
                    }, (d, which) -> {
                    }, R.string.whether_to_delete, ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.WRAP_CONTENT, false).show());
            dialog.setContentView(ll);
            dialog.show();
            return true;
        });
    }

    private void changeDate(String oldDateString, DiaryTakingActivity.MyDate newDate) {
        ContentValues cv = new ContentValues();
        cv.put("date", newDate.toString());
        diaryDatabase.update("diary", cv, "date=?", new String[]{oldDateString});
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}