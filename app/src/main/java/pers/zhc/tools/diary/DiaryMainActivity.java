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
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.FilePicker;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.*;
import pers.zhc.tools.utils.sqlite.Cursor;
import pers.zhc.tools.utils.sqlite.SQLite;
import pers.zhc.tools.utils.sqlite.SQLite3;
import pers.zhc.tools.utils.sqlite.Statement;
import pers.zhc.tools.views.SmartHintEditText;
import pers.zhc.u.FileU;
import pers.zhc.u.Latch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
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
        Common.doAssertion(actionBar != null);
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

    private void showCreateSpecificDateDiaryDialog() {
        final AlertDialog promptDialog = DialogUtil.createPromptDialog(this, R.string.enter_specific_date, (et, alertDialog) -> {
            final String s = et.getText().toString();
            try {
                final int dateInt = Integer.parseInt(s);

                if (checkRecordExistence(dateInt)) {
                    showDuplicateConfirmDialog(dateInt);
                } else {
                    createDiary(dateInt);
                }
            } catch (NumberFormatException e) {
                ToastUtils.show(this, R.string.please_type_correct_value);
            }
        });
        promptDialog.show();
    }

    private void showDuplicateConfirmDialog(int dateInt) {
        Dialog[] dialog = {null};
        dialog[0] = DialogUtil.createConfirmationAlertDialog(this, (d, which) -> {
            openDiaryPreview(dateInt);
            dialog[0].dismiss();
        }, R.string.duplicated_diary_dialog_title);
        DialogUtil.setDialogAttr(dialog[0], false, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        dialog[0].show();
    }

    private boolean checkRecordExistence(int dateInt) {
        final Statement statement = diaryDatabase.compileStatement("SELECT \"date\"\n" +
                "FROM diary\n" +
                "WHERE \"date\" IS ?");
        statement.bind(1, dateInt);
        final boolean hasRecord = diaryDatabase.hasRecord(statement);
        statement.release();
        return hasRecord;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.write_diary:
                writeDiary();
                break;
            case R.id.create:
                showCreateSpecificDateDiaryDialog();
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
                sort();
                break;
            case R.id.attachment:
                startActivity(new Intent(this, DiaryAttachmentActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(this, DiaryAttachmentSettingsActivity.class));
                break;
            default:
                break;
        }
        super.onOptionsItemSelected(item);
        return true;
    }

    private void sort() {
        diaryDatabase.exec("DROP TABLE IF EXISTS tmp");

        diaryDatabase.beginTransaction();
        final Statement statement = diaryDatabase.compileStatement("SELECT sql\n" +
                "FROM sqlite_master\n" +
                "WHERE type IS 'table'\n" +
                "  AND tbl_name IS 'diary'");
        final Cursor cursor = statement.getCursor();
        Common.doAssertion(cursor.step());
        final String diaryTableSql = cursor.getText(statement.getIndexByColumnName("sql"));
        statement.release();

        final String tmpTableSql = diaryTableSql.replaceFirst("diary", "tmp");
        diaryDatabase.exec(tmpTableSql);

        diaryDatabase.exec("INSERT INTO tmp SELECT * FROM diary ORDER BY \"date\"");
        diaryDatabase.exec("DROP TABLE diary");
        diaryDatabase.exec("ALTER TABLE tmp RENAME TO diary");
        diaryDatabase.commit();
        refreshListViews();
    }

    private void writeDiary() {
        final boolean recordExistence = checkRecordExistence(getCurrentDateInt());
        if (recordExistence) {
            final Intent intent = new Intent(this, DiaryTakingActivity.class);
            intent.putExtra("dateInt", getCurrentDateInt());
            startActivityForResult(intent, RequestCode.START_ACTIVITY_4);
        } else {
            createDiary(getCurrentDateInt());
        }
    }

    private void importDiary(File file) {
        final DiaryDatabaseRef diaryDatabaseRef = DiaryBaseActivity.Companion.getDiaryDatabaseRef();

        final int refCount = diaryDatabaseRef.getRefCount();
        // the only one reference is for the current activity
        if (refCount > 1) {
            ToastUtils.show(this, getString(R.string.diary_import_ref_count_not_zero_msg, refCount));
            return;
        }
        diaryDatabaseRef.countDownRef();
        Common.doAssertion(diaryDatabaseRef.isAbandoned());

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
        diaryDatabaseRef.countRef();
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

    private int getCurrentDateInt() {
        DiaryTakingActivity.MyDate date = new DiaryTakingActivity.MyDate(new Date(System.currentTimeMillis()));
        return date.getDateInt();
    }

    private void createDiary(int dateInt) {
        Intent intent = new Intent(this, DiaryTakingActivity.class);
        // use the current time
        intent.putExtra("dateInt", dateInt);
        startActivityForResult(intent, RequestCode.START_ACTIVITY_0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCode.START_ACTIVITY_0:
                // on diary taking activity returned
                // it must be an activity which intends to create a new diary record
                Common.doAssertion(data != null);
                int dateInt = data.getIntExtra("dateInt", -1);

                RelativeLayout childRL = getChildRLByDate(dateInt);
                ll.addView(childRL);
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
            case RequestCode.START_ACTIVITY_3:
                // on preview activity returned
                // refresh the view in the LinearLayout
            case RequestCode.START_ACTIVITY_4:
                // "write diary" action: start a diary taking activity directly
                // on the activity above returned
                // refresh the corresponding view
                Common.doAssertion(data != null);
                dateInt = data.getIntExtra("dateInt", -1);
                Common.doAssertion(dateInt != -1);
                updateDiaryView(dateInt);
                break;
            default:
                break;
        }
    }

    private void updateDiaryView(int dateInt) {
        final int childCount = ll.getChildCount();
        RelativeLayoutWithDate target = null;
        for (int i = 0; i < childCount; i++) {
            final RelativeLayoutWithDate child = ((RelativeLayoutWithDate) ll.getChildAt(i));
            if (child.dateInt == dateInt) {
                target = child;
            }
        }
        Common.doAssertion(target != null);

        final Statement statement = diaryDatabase.compileStatement("SELECT content\n" +
                "FROM diary\n" +
                "WHERE \"date\" IS ?");
        statement.bind(1, dateInt);
        final Cursor cursor = statement.getCursor();
        final boolean step = cursor.step();
        Common.doAssertion(step);
        final String newContent = cursor.getText(statement.getIndexByColumnName("content"));
        statement.release();

        ((LimitCharacterTextView) target.getChildAt(1)).setTextLimited(newContent);
    }

    private void refreshListViews() {
        ll.removeAllViews();
        loadListViews();
    }

    @NotNull
    private RelativeLayout getChildRLByDate(int dateInt) {
        final Statement statement = diaryDatabase.compileStatement("SELECT *\n" +
                "FROM diary\n" +
                "WHERE \"date\" IS ?");
        statement.bind(1, dateInt);
        final Cursor cursor = statement.getCursor();
        String content;
        if (cursor.step()) {
            content = cursor.getText(statement.getIndexByColumnName("content"));
        } else content = "";
        statement.release();

        return getChildRL(new DiaryTakingActivity.MyDate(dateInt), content);
    }

    private static class RelativeLayoutWithDate extends RelativeLayout {
        private final int dateInt;

        public RelativeLayoutWithDate(Context context, int dateInt) {
            super(context);
            this.dateInt = dateInt;
        }
    }

    @NotNull
    @SuppressLint("SetTextI18n")
    private RelativeLayout getChildRL(@NotNull DiaryTakingActivity.MyDate myDate, String content) {
        RelativeLayoutWithDate childRL = new RelativeLayoutWithDate(this, myDate.getDateInt());
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        childRL.setLayoutParams(layoutParams);
        TextView dateTV = new TextView(this);
        dateTV.setId(R.id.tv1);
        RelativeLayout.LayoutParams dateLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateTV.setLayoutParams(dateLP);
        dateTV.setTextColor(Color.parseColor("#1565C0"));
        dateTV.setTextSize(30);
        LimitCharacterTextView previewTV = new LimitCharacterTextView(this);
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
        previewTV.setTextLimited(content);
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
        childRL.setOnClickListener(v -> openDiaryPreview(date.getDateInt()));
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
                            RelativeLayout newChildRL = getChildRLByDate(newDate.getDateInt());
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

    private void openDiaryPreview(int dateInt) {
        Intent intent = new Intent(this, DiaryContentPreviewActivity.class);
        intent.putExtra("dateInt", dateInt);
        startActivityForResult(intent, RequestCode.START_ACTIVITY_3);
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

    private static class LimitCharacterTextView extends androidx.appcompat.widget.AppCompatTextView {

        public LimitCharacterTextView(Context context) {
            super(context);
        }

        public void setTextLimited(@NotNull String s) {
            super.setText(s.length() > 100 ? (s.substring(0, 100) + "...") : s);
        }
    }
}
