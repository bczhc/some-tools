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
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * @author bczhc
 */
public class DiaryMainActivity extends DiaryBaseActivity {
    private RecyclerView recyclerView;
    @NonNull
    private String currentPasswordDigest = "";
    private boolean isUnlocked = false;
    private final List<DiaryItemData> diaryItemDataList = new ArrayList<>();
    private MyAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SQLite3 passwordDatabase = openPasswordDatabase();
        passwordDatabase.exec("SELECT digest FROM password where k='diary'", contents -> {
            currentPasswordDigest = contents[0];
            return 0;
        });
        passwordDatabase.close();

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
        recyclerView = findViewById(R.id.recycler_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.diary);
            actionBar.show();
        }
        loadRecyclerView();
    }

    private void loadRecyclerView() {
        new Thread(() -> {
            refreshDiaryItemDataList();

            runOnUiThread(() -> {
                recyclerViewAdapter = new MyAdapter(this, diaryItemDataList);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(recyclerViewAdapter);

                recyclerViewAdapter.setOnItemClickListener((position, view) -> openDiaryPreview(((DiaryItemRL) view).dateInt));
                recyclerViewAdapter.setOnItemLongClickListener((position, view) -> {
                    final PopupMenu popupMenu = PopupMenuUtil.createPopupMenu(this, view, R.menu.diary_popup_menu);
                    popupMenu.setOnMenuItemClickListener(item -> {
                        final int itemId = item.getItemId();
                        if (itemId == R.id.change_date_btn) {
                            popupMenuChangeDate(position);
                        } else if (itemId == R.id.delete_btn) {
                            popupMenuDelete(position);
                        }
                        return true;
                    });
                    popupMenu.show();
                });
            });
        }).start();
    }

    private void refreshDiaryItemDataList() {
        diaryItemDataList.clear();

        final Statement statement = diaryDatabase.compileStatement("SELECT *\n" +
                "FROM diary");
        final int dateColumn = statement.getIndexByColumnName("date");
        final int contentColumn = statement.getIndexByColumnName("content");

        final Cursor cursor = statement.getCursor();
        while (cursor.step()) {
            final int date = cursor.getInt(dateColumn);
            final String content = cursor.getText(contentColumn);

            diaryItemDataList.add(new DiaryItemData(date, content));
        }

        statement.release();
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
        DialogUtil.setDialogAttr(dialog[0], false, MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        dialog[0].show();
    }

    private boolean checkRecordExistence(int dateInt) {
        return this.diaryDatabase.hasRecord("SELECT \"date\"\n" +
                "FROM diary\n" +
                "WHERE \"date\" IS ?", new Object[]{dateInt});
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.write_diary) {

            writeDiary();

        } else if (itemId == R.id.create) {

            showCreateSpecificDateDiaryDialog();

        } else if (itemId == R.id.password) {

            changePassword();

        } else if (itemId == R.id.export) {

            Intent intent1 = new Intent(this, FilePicker.class);
            intent1.putExtra("option", FilePicker.PICK_FOLDER);
            startActivityForResult(intent1, RequestCode.START_ACTIVITY_1);

        } else if (itemId == R.id.import_) {

            Intent intent2 = new Intent(this, FilePicker.class);
            intent2.putExtra("option", FilePicker.PICK_FILE);
            startActivityForResult(intent2, RequestCode.START_ACTIVITY_2);

        } else if (itemId == R.id.sort) {

            sort();

        } else if (itemId == R.id.attachment) {

            startActivity(new Intent(this, DiaryAttachmentActivity.class));

        } else if (itemId == R.id.settings) {

            startActivity(new Intent(this, DiaryAttachmentSettingsActivity.class));

        }
        return true;
    }

    private int getForeignKeys() {
        final Statement fk = diaryDatabase.compileStatement("PRAGMA foreign_keys");
        final Cursor fkCursor = fk.getCursor();
        Common.doAssertion(fkCursor.step());
        final int foreignKey = fkCursor.getInt(0);
        fk.release();
        return foreignKey;
    }

    private void setForeignKeys(int foreignKeys) {
        final Statement fk = diaryDatabase.compileStatement("PRAGMA foreign_keys=" + foreignKeys);
        fk.step();
        fk.release();
    }

    private void sort() {
        final int foreignKeys = getForeignKeys();
        // disable foreign keys constraint
        setForeignKeys(0);

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

        refreshList();

        // restore foreign keys setting
        setForeignKeys(foreignKeys);
    }

    private void writeDiary() {
        final boolean recordExistence = checkRecordExistence(getCurrentDateInt());
        if (recordExistence) {
            final Intent intent = new Intent(this, DiaryTakingActivity.class);
            intent.putExtra(DiaryTakingActivity.EXTRA_DATE_INT, getCurrentDateInt());
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
        refreshList();
    }

    private void refreshList() {
        refreshDiaryItemDataList();
        recyclerViewAdapter.notifyDataSetChanged();
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
        DialogUtil.setDialogAttr(dialog, false, MATCH_PARENT
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
        intent.putExtra(DiaryTakingActivity.EXTRA_DATE_INT, dateInt);
        startActivityForResult(intent, RequestCode.START_ACTIVITY_0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCode.START_ACTIVITY_0:
                // "write diary" action: on diary taking activity returned
                // it must be an activity which intends to create a new diary record
                Common.doAssertion(data != null);
                int dateInt = data.getIntExtra(DiaryTakingActivity.EXTRA_DATE_INT, -1);

                // update view
                diaryItemDataList.add(new DiaryItemData(dateInt, queryDiaryContent(dateInt)));
                recyclerViewAdapter.notifyItemInserted(diaryItemDataList.size() - 1);
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
                Common.doAssertion(data != null);
                refreshDiaryItem(data);
                break;
            case RequestCode.START_ACTIVITY_4:
                // "write diary" action: start a diary taking activity directly when the diary of today's date already exists
                // refresh the corresponding view
                Common.doAssertion(data != null);
                refreshDiaryItem(data);
                break;
            default:
                break;
        }
    }

    private void refreshDiaryItem(int dateInt) {
        final String content = queryDiaryContent(dateInt);
        int position = getDiaryItemPosition(dateInt);
        diaryItemDataList.get(position).content = content;
        recyclerViewAdapter.notifyItemChanged(position);
    }

    /**
     * @param data intent with {@link DiaryTakingActivity#EXTRA_DATE_INT} extra
     */
    private void refreshDiaryItem(@NotNull Intent data) {
        Common.doAssertion(data.hasExtra(DiaryContentPreviewActivity.EXTRA_DATE_INT));
        int dateInt = data.getIntExtra(DiaryContentPreviewActivity.EXTRA_DATE_INT, -1);
        refreshDiaryItem(dateInt);
    }

    private int getDiaryItemPosition(int dateInt) {
        for (int i = 0; i < diaryItemDataList.size(); i++) {
            if (diaryItemDataList.get(i).dateInt == dateInt) {
                return i;
            }
        }
        return -1;
    }

    private String queryDiaryContent(int dateInt) {
        final Statement statement = diaryDatabase.compileStatement("SELECT content\n" +
                "FROM diary\n" +
                "WHERE \"date\" IS ?");
        statement.bind(1, dateInt);

        final Cursor cursor = statement.getCursor();
        Common.doAssertion(cursor.step());
        final String content = cursor.getText(0);

        statement.release();

        return content;
    }

    private static class DiaryItemRL extends RelativeLayout {
        private int dateInt = -1;

        public DiaryItemRL(Context context) {
            super(context);
        }
    }

    private void popupMenuChangeDate(int position) {
        final DiaryItemData diaryItemData = diaryItemDataList.get(position);
        final int oldDateInt = diaryItemData.dateInt;

        EditText dateET = new EditText(this);

        final AlertDialog dialog = DialogUtil.createConfirmationAlertDialog(this, (d, which) -> {

            final String dateString = dateET.getText().toString();
            int newDateInt;
            try {
                newDateInt = Integer.parseInt(dateString);
            } catch (Exception e) {
                ToastUtils.show(this, R.string.please_type_correct_value);
                return;
            }

            changeDate(oldDateInt, newDateInt);
            d.dismiss();

            // update view
            diaryItemData.dateInt = newDateInt;
            recyclerViewAdapter.notifyItemChanged(position);

        }, null, dateET, R.string.enter_new_date, MATCH_PARENT, WRAP_CONTENT, false);

        dialog.show();
    }

    private void popupMenuDelete(int position) {

        final int dateInt = diaryItemDataList.get(position).dateInt;
        DialogUtil.createConfirmationAlertDialog(this, (dialog, which) -> {

            final Statement statement = diaryDatabase.compileStatement("DELETE\n" +
                    "FROM diary\n" +
                    "WHERE \"date\" IS ?");
            statement.bind(1, dateInt);
            statement.step();
            statement.release();

            dialog.dismiss();

            // update view
            diaryItemDataList.remove(position);
            recyclerViewAdapter.notifyItemRemoved(position);
        }, R.string.whether_to_delete).show();
    }

    private void openDiaryPreview(int dateInt) {
        Intent intent = new Intent(this, DiaryContentPreviewActivity.class);
        intent.putExtra(DiaryContentPreviewActivity.EXTRA_DATE_INT, dateInt);
        startActivityForResult(intent, RequestCode.START_ACTIVITY_3);
    }

    private void changeDate(int oldDateString, int newDate) {
        diaryDatabase.execBind("UPDATE diary\n" +
                "SET \"date\"=?\n" +
                "WHERE \"date\" IS ?", new Object[]{newDate, oldDateString});
    }

    @NotNull
    static String computeIdentifier(File f) throws IOException {
        InputStream is = new FileInputStream(f);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        DigestUtil.updateInputStream(md, is);
        final long length = f.length();
        byte[] packed = new byte[8];
        JNI.Struct.packLong(length, packed, 0, JNI.Struct.MODE_LITTLE_ENDIAN);
        md.update(packed);
        return DigestUtil.bytesToHexString(md.digest());
    }

    @NotNull
    static String computeIdentifier(@NotNull byte[] data) {
        final long length = data.length;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        final byte[] packed = new byte[8];
        JNI.Struct.packLong(length, packed, 0, JNI.Struct.MODE_LITTLE_ENDIAN);
        md.update(data);
        md.update(packed);
        return DigestUtil.bytesToHexString(md.digest());
    }

    @NotNull
    static String computeIdentifier(@NotNull String s) {
        return computeIdentifier(s.getBytes(StandardCharsets.UTF_8));
    }

    private static class LimitCharacterTextView extends androidx.appcompat.widget.AppCompatTextView {

        public LimitCharacterTextView(Context context) {
            super(context);
        }

        public void setTextLimited(@NotNull String s) {
            super.setText(s.length() > 100 ? (s.substring(0, 100) + "...") : s);
        }
    }

    private static class DiaryItemData {
        private int dateInt;
        private String content;

        public DiaryItemData(int dateInt, String content) {
            this.dateInt = dateInt;
            this.content = content;
        }
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private final Context context;
        private final String[] weeks;
        private final List<DiaryItemData> data;
        private OnItemClickListener onItemClickListener = null;
        private OnItemLongClickListener onItemLongClickListener = null;

        private MyAdapter(@NotNull Context context, @NotNull List<DiaryItemData> data) {
            this.context = context;
            this.data = data;
            this.weeks = context.getResources().getStringArray(R.array.weeks);
        }

        private static class MyViewHolder extends RecyclerView.ViewHolder {
            public MyViewHolder(@NonNull @NotNull View itemView) {
                super(itemView);
            }

            private DiaryItemRL getDiaryItemRL() {
                return (DiaryItemRL) itemView;
            }
        }

        @NotNull
        private DiaryItemRL createDiaryItemRL() {
            DiaryItemRL childRL = new DiaryItemRL(context);
            final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            childRL.setLayoutParams(layoutParams);

            TextView dateTV = new TextView(context);
            dateTV.setId(R.id.tv1);
            RelativeLayout.LayoutParams dateLP = new RelativeLayout.LayoutParams(MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateTV.setLayoutParams(dateLP);
            dateTV.setTextColor(Color.parseColor("#1565C0"));
            dateTV.setTextSize(30);

            LimitCharacterTextView previewTV = new LimitCharacterTextView(context);
            previewTV.setId(R.id.tv2);
            RelativeLayout.LayoutParams previewLP = new RelativeLayout.LayoutParams(MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            previewLP.addRule(RelativeLayout.BELOW, R.id.tv1);
            previewTV.setLayoutParams(previewLP);

            childRL.addView(dateTV);
            childRL.addView(previewTV);
            return childRL;
        }

        @SuppressLint("SetTextI18n")
        private void bindDiaryItemRL(@NotNull DiaryItemRL item, @NotNull DiaryTakingActivity.MyDate myDate, String content) {
            final TextView dateTV = (TextView) item.getChildAt(0);
            final LimitCharacterTextView previewTV = (LimitCharacterTextView) item.getChildAt(1);
            item.dateInt = myDate.getDateInt();

            String weekString = null;
            try {
                final Calendar calendar = Calendar.getInstance();
                calendar.set(myDate.getYear(), myDate.getMonth() - 1, myDate.getDay());
                final int weekIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                weekString = this.weeks[weekIndex];
            } catch (Exception e) {
                Common.showException(e, context);
            }

            dateTV.setText(myDate + " " + weekString);
            previewTV.setTextLimited(content);
        }

        @NonNull
        @NotNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
            final DiaryItemRL diaryItemRL = createDiaryItemRL();
            final MyViewHolder holder = new MyViewHolder(diaryItemRL);

            diaryItemRL.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    final int position = holder.getLayoutPosition();
                    onItemClickListener.onClick(position, diaryItemRL);
                }
            });

            diaryItemRL.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    final int position = holder.getLayoutPosition();
                    onItemLongClickListener.onLongClick(position, diaryItemRL);
                }
                return true;
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {
            final DiaryItemData itemData = data.get(position);
            bindDiaryItemRL(holder.getDiaryItemRL(), new DiaryTakingActivity.MyDate(itemData.dateInt), itemData.content);
        }

        @Override
        public int getItemCount() {
            return this.data.size();
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.onItemClickListener = onItemClickListener;
        }

        public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
            this.onItemLongClickListener = onItemLongClickListener;
        }
    }

    private interface OnItemClickListener {
        void onClick(int position, View view);
    }

    private interface OnItemLongClickListener {
        void onLongClick(int position, View view);
    }
}

