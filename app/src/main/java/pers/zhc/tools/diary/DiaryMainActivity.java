package pers.zhc.tools.diary;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.FilePicker;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.*;
import pers.zhc.tools.utils.sqlite.SQLite;
import pers.zhc.tools.utils.sqlite.SQLite3;
import pers.zhc.tools.utils.sqlite.Statement;
import pers.zhc.u.FileU;
import pers.zhc.u.Latch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author bczhc
 */
public class DiaryMainActivity extends DiaryBaseActivity {
    private LinearLayout ll;
    @NonNull
    private String currentPasswordDigest = "";
    private boolean isUnlocked = false;
    private String[] weeks;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SQLite3 passwordDatabase = openPasswordDatabase();
        passwordDatabase.exec("SELECT digest FROM password where k='diary'", contents -> {
            currentPasswordDigest = contents[0];
            return 0;
        });
        passwordDatabase.close();

        this.weeks = getResources().getStringArray(R.array.weeks);
        if (currentPasswordDigest.isEmpty()) {
            load();
            return;
        }
        setContentView(R.layout.password_view);
        EditText passwordET = findViewById(R.id.password_et);
        String finalPassword = currentPasswordDigest;
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
                if (JNI.Diary.myDigest(text).equals(finalPassword)) {
                    passwordET.setEnabled(false);
                    load();
                }
            }
        });

        final ActionBar actionBar = this.getSupportActionBar();
        if (actionBar == null) throw new AssertionError();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void load() {
        isUnlocked = true;
        setContentView(R.layout.diary_activity);
        invalidateOptionsMenu();
        ll = findViewById(R.id.ll);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.diary);
            actionBar.show();
        }
        loadListViews();
    }


    @NotNull
    private SQLite3 openPasswordDatabase() {
        SQLite3 pwDB = SQLite3.open(Common.getInternalDatabaseDir(this, "passwords.db").getPath());
        pwDB.exec("CREATE TABLE IF NOT EXISTS password\n" +
                "(\n" +
                "    " +
                "-- " +
                "key\n" +
                "    k      TEXT NOT NULL PRIMARY KEY,\n" +
                "    " +
                "-- password digest\n" +
                "    digest TEXT NOT NULL\n" +
                ")");
        return pwDB;
    }

    private void setPassword(String password) {
        final SQLite3 passwordDatabase = openPasswordDatabase();
        final boolean exist = SQLite.checkRecordExistence(passwordDatabase, "password", "k", "diary");
        if (!exist) {
            passwordDatabase.exec("INSERT INTO password\n" +
                    "VALUES ('diary', '')");
        }
        String digest;
        // avoid my digest algorithm's calculation of an empty string
        if (password.isEmpty()) digest = "";
        else digest = JNI.Diary.myDigest(password);
        try {
            Statement statement = passwordDatabase.compileStatement("UPDATE password\n" +
                    "SET digest=?\n" +
                    "WHERE k = 'diary'");
            statement.reset();
            statement.bindText(1, digest);
            statement.step();
            statement.release();
        } catch (Exception e) {
            Common.showException(e, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isUnlocked) {
            final MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.diary_actionbar, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void createSpecificDateDiary() {
        final AlertDialog promptDialog = DialogUtil.createPromptDialog(this, R.string.enter_specific_date, (et, alertDialog) -> {
            final String s = et.getText().toString();
            try {
                final int dateInt = Integer.parseInt(s);
                createOrOpenDiary(new DiaryTakingActivity.MyDate(dateInt));
            } catch (NumberFormatException e) {
                ToastUtils.show(this, R.string.please_type_correct_value);
            }
        });
        promptDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.write_diary:
                createOrOpenDiary(null);
                break;
            case R.id.create:
                createSpecificDateDiary();
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
                break;
            case R.id.sort:
                final SQLite3 diaryDatabase = getDiaryDatabase();
                diaryDatabase.beginTransaction();
                diaryDatabase.exec("DROP TABLE IF EXISTS temp");
                diaryDatabase.exec("CREATE TABLE IF NOT EXISTS temp\n" +
                        "(\n" +
                        "    date          INTEGER,\n" +
                        "    content       TEXT NOT NULL,\n" +
                        "    attachment_id INTEGER\n" +
                        ")");
                diaryDatabase.exec("INSERT INTO temp SELECT * FROM diary ORDER BY date");
                diaryDatabase.exec("DROP TABLE diary");
                diaryDatabase.exec("ALTER TABLE temp RENAME TO diary");
                diaryDatabase.commit();
                refreshListViews();
                break;
            case R.id.attachment:
                startActivity(new Intent(this, DiaryAttachmentActivity.class));
                break;
            default:
                break;
        }
        super.onOptionsItemSelected(item);
        return true;
    }

    private void importDiary(File file) {
        final Latch latch = new Latch();
        latch.suspend();
        new Thread(() -> {
            FileUtil.copy(file, new File(internalDatabasePath));
            latch.stop();
        }).start();
        latch.await();
        ToastUtils.show(this, R.string.importing_succeeded);

        SQLite3 newDatabase = SQLite3.open(internalDatabasePath);
        if (newDatabase.checkIfCorrupt()) {
            newDatabase.close();
            if (!new File(internalDatabasePath).delete()) {
                throw new RuntimeException("Failed to delete corrupted database file.");
            }

            ToastUtils.show(this, R.string.corrupted_database_and_recreate_new_msg);
        }

        setDatabase(internalDatabasePath);
        refreshListViews();
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
            ToastUtils.show(this, R.string.exporting_succeeded);
        } else {
            ToastUtils.show(this, R.string.copying_failed);
        }
    }

    private void changePassword() {
        View view = View.inflate(this, R.layout.change_password_view, null);
        Dialog dialog = new Dialog(this);
        DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.WRAP_CONTENT, false);
        EditText oldPasswordET = ((SmartHintEditText) view.findViewById(R.id.old_password)).getEditText();
        EditText newPasswordET = ((SmartHintEditText) view.findViewById(R.id.new_password)).getEditText();
        Button confirm = view.findViewById(R.id.confirm);
        confirm.setOnClickListener(v -> {
            String old = oldPasswordET.getText().toString();
            if (currentPasswordDigest.isEmpty() || currentPasswordDigest.equals(JNI.Diary.myDigest(old))) {
                setPassword(newPasswordET.getText().toString());
                ToastUtils.show(this, R.string.change_succeeded);
                dialog.dismiss();
            } else ToastUtils.show(this, R.string.password_not_matching);
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void createOrOpenDiary(@Nullable DiaryTakingActivity.MyDate date) {
        Intent intent = new Intent(this, DiaryTakingActivity.class);
        final int[] dateInts;
        if (date == null) {
            final Calendar calendar = Calendar.getInstance();
            final int year = calendar.get(Calendar.YEAR);
            final int month = calendar.get(Calendar.MONTH) + 1;
            final int day = calendar.get(Calendar.DAY_OF_MONTH);
            dateInts = new int[]{year, month, day};
        } else {
            dateInts = new int[]{date.getYear(), date.getMonth(), date.getDay()};
        }
        intent.putExtra("date", dateInts);
        String dateString;
        if (date == null) dateString = new DiaryTakingActivity.MyDate(dateInts).getDateIntString();
        else dateString = date.getDateIntString();
        intent.putExtra("myDateStr", dateString);
        startActivityForResult(intent, RequestCode.START_ACTIVITY_0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCode.START_ACTIVITY_0:
                // on diary taking activity returned
                if (data == null) throw new AssertionError();
                String myDateStr = data.getStringExtra("myDateStr");
                boolean isNewDiary = data.getBooleanExtra("newRec", true);

                RelativeLayout childRL = getChildRLByDate(myDateStr);
                if (isNewDiary) {
                    ll.addView(childRL);
                } else {
                    int childCount = ll.getChildCount();
                    int index = -1;
                    for (int i = 0; i < childCount; i++) {
                        DiaryTakingActivity.MyDate date = ((TextViewWithDate) ((RelativeLayout) ll.getChildAt(i)).getChildAt(0)).date;
                        if (date.getDateIntString().equals(myDateStr)) {
                            index = i;
                            break;
                        }
                    }
                    Common.debugAssert(index != -1);
                    ll.removeViewAt(index);
                    ll.addView(childRL, index);
                }
                break;
            case RequestCode.START_ACTIVITY_1:
                // export
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
                // import
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

    private void refreshListViews() {
        ll.removeAllViews();
        loadListViews();
    }

    private RelativeLayout getChildRLByDate(String dateString) {
        RelativeLayout[] r = {null};
        diaryDatabase.exec("SELECT * FROM diary WHERE date is " + dateString, contents -> {
            r[0] = getChildRL(contents);
            return 0;
        });
        return r[0];
    }

    private static class TextViewWithDate extends AppCompatTextView {
        private final DiaryTakingActivity.MyDate date;

        public TextViewWithDate(Context context, @NotNull DiaryTakingActivity.MyDate date) {
            super(context);
            this.date = date;
        }
    }

    @NotNull
    @SuppressLint("SetTextI18n")
    private RelativeLayout getChildRL(DiaryTakingActivity.MyDate myDate, String content) {
        RelativeLayout childRL = new RelativeLayout(this);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        childRL.setLayoutParams(layoutParams);
        TextViewWithDate dateTV = new TextViewWithDate(this, myDate);
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
            final Calendar calendar = Calendar.getInstance();
            calendar.set(myDate.getYear(), myDate.getMonth() - 1, myDate.getDay());
            final int weekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            weekString = this.weeks[weekIndex];
        } catch (Exception e) {
            Common.showException(e, this);
        }
        dateTV.setText(myDate.toString() + " " + weekString);
        previewTV.setText(content.length() > 100 ? (content.substring(0, 100) + "...") : content);
        setChildRL(myDate, childRL);
        runOnUiThread(() -> {
            childRL.addView(dateTV);
            childRL.addView(previewTV);
        });
        return childRL;
    }

    @NotNull
    private RelativeLayout getChildRL(@NotNull String[] sqliteColumnContent) {
        final DiaryTakingActivity.MyDate myDate = new DiaryTakingActivity.MyDate(Integer.parseInt(sqliteColumnContent[0]));
        final String content = sqliteColumnContent[1];
        return getChildRL(myDate, content);
    }

    private void loadListViews() {
        new Thread(() -> diaryDatabase.exec("SELECT * FROM diary", contents -> {
            try {
                RelativeLayout childRL = getChildRL(contents);

                runOnUiThread(() -> ll.addView(childRL));
            } catch (Exception e) {
                Common.showException(e, this);
            }
            return 0;
        })).start();
    }

    private void setChildRL(DiaryTakingActivity.MyDate date, @NotNull RelativeLayout childRL) {
        childRL.setOnClickListener(v -> {
            Intent intent = new Intent(this, DiaryContentPreviewActivity.class);
            intent.putExtra("dateInt", date.getDateInt());
            startActivity(intent);
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
                            final String dateString = dateET.getText().toString();
                            final DiaryTakingActivity.MyDate newDate;
                            try {
                                newDate = new DiaryTakingActivity.MyDate(Integer.parseInt(dateString));
                            } catch (Exception e) {
                                ToastUtils.show(this, R.string.please_type_correct_value);
                                return;
                            }
                            changeDate(date.getDateIntString(), newDate);
                            dialog.dismiss();
                            // update view
                            RelativeLayout newChildRL = getChildRLByDate(newDate.getDateIntString());
                            int indexOfChild = this.ll.indexOfChild(childRL);
                            this.ll.removeViewAt(indexOfChild);
                            this.ll.addView(newChildRL, indexOfChild);
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
                        diaryDatabase.exec("DELETE FROM diary WHERE date='" + date.getDateIntString() + '\'');
                        dialog.dismiss();
                        // update view
                        this.ll.removeView(childRL);
                    }, (d, which) -> {
                    }, R.string.whether_to_delete, ViewGroup.LayoutParams.MATCH_PARENT
                    , ViewGroup.LayoutParams.WRAP_CONTENT, false).show());
            dialog.setContentView(ll);
            dialog.show();
            return true;
        });
    }

    private void changeDate(String oldDateString, @NotNull DiaryTakingActivity.MyDate newDate) {
        diaryDatabase.exec("UPDATE diary SET date=" + newDate.getDateIntString() + " WHERE date=" + oldDateString);
    }

    @NotNull
    static String computeFileIdentifier(File f) throws IOException, NoSuchAlgorithmException {
        InputStream is = new FileInputStream(f);
        MessageDigest md = MessageDigest.getInstance("SHA1");
        DigestUtil.updateInputStream(md, is);
        final long length = f.length();
        byte[] packed = new byte[8];
        JNI.Struct.packLong(length, packed, 0, JNI.Struct.MODE_LITTLE_ENDIAN);
        md.update(packed);
        return DigestUtil.bytesToHexString(md.digest());
    }
}
