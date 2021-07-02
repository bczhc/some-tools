package pers.zhc.tools.diary;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.diary.fragments.AttachmentFragment;
import pers.zhc.tools.diary.fragments.DiaryFragment;
import pers.zhc.tools.diary.fragments.FileLibraryFragment;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.Cursor;
import pers.zhc.tools.utils.sqlite.SQLite3;
import pers.zhc.tools.utils.sqlite.Statement;
import pers.zhc.tools.views.SmartHintEditText;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * @author bczhc
 */
public class DiaryMainActivity extends DiaryBaseActivity {
    private boolean isUnlocked = false;
    private ActionBarDrawerToggle drawerToggle;
    private MaterialToolbar toolbar;
    private @MenuRes
    int currentMenuRes = R.menu.diary_menu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SQLite3 passwordDatabase = openPasswordDatabase();
        initPasswordDatabase(passwordDatabase);
        final String passwordDigest = getPasswordDigest(passwordDatabase);
        passwordDatabase.close();

        if (passwordDigest.isEmpty()) {
            load();
            return;
        }
        setContentView(R.layout.password_view);

        EditText passwordET = findViewById(R.id.password_et);
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
                if (JNI.Diary.myDigest(text).equals(passwordDigest)) {
                    passwordET.setEnabled(false);
                    load();
                }
            }
        });
    }

    private String getPasswordDigest(@NotNull SQLite3 db) {
        final Statement statement = db.compileStatement("SELECT digest\n" +
                "FROM password\n" +
                "where \"key\" IS 'diary'");
        final Cursor cursor = statement.getCursor();
        Common.doAssertion(cursor.step());
        final String passwordDigest = cursor.getText(0);
        statement.release();
        return passwordDigest;
    }

    private void load() {
        isUnlocked = true;
        setContentView(R.layout.diary_main_activity);
        invalidateOptionsMenu();

        initDrawerLayout();
        initToolbar();
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        Common.doAssertion(actionBar != null);
        actionBar.setDisplayHomeAsUpEnabled(true);

        toolbar.setTitle(R.string.diary);

        drawerToggle = new ActionBarDrawerToggle(this, findViewById(R.id.drawer_layout), toolbar, 0, 0);
    }

    @Override
    protected void onPostCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull @NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    private void initDrawerLayout() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        navigationView.setCheckedItem(R.id.diary);

        // the initial diary view
        fragmentManager.beginTransaction().add(R.id.diary_fragment_container, new DiaryFragment()).commit();

        navigationView.setNavigationItemSelectedListener(item -> {
            final int itemId = item.getItemId();

            final MenuItem checkedItem = navigationView.getCheckedItem();
            if (checkedItem != null && checkedItem.getItemId() == itemId) {
                drawerLayout.closeDrawers();
                return true;
            }

            if (itemId == R.id.diary) {

                fragmentManager.beginTransaction().replace(R.id.diary_fragment_container, new DiaryFragment()).commit();
                toolbar.setTitle(R.string.diary);
                currentMenuRes = R.menu.diary_menu;
                invalidateOptionsMenu();

            } else if (itemId == R.id.attachment) {

                fragmentManager.beginTransaction().replace(R.id.diary_fragment_container,
                        new AttachmentFragment(false, false, -1)
                ).commit();
                toolbar.setTitle(R.string.attachment);
                currentMenuRes = R.menu.diary_attachment_actionbar;
                invalidateOptionsMenu();

            } else if (itemId == R.id.file_library) {

                fragmentManager.beginTransaction().replace(R.id.diary_fragment_container, new FileLibraryFragment()).commit();
                toolbar.setTitle(R.string.file_library);
                currentMenuRes = R.menu.diary_file_library_actionbar;
                invalidateOptionsMenu();

            } else if (itemId == R.id.settings) {

                startActivity(new Intent(this, DiaryAttachmentSettingsActivity.class));
                drawerLayout.closeDrawers();
                return false;

            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void initPasswordDatabase(@NotNull SQLite3 db) {
        db.exec("CREATE TABLE IF NOT EXISTS password\n" +
                "(\n" +
                "    " +
                "-- " +
                "key\n" +
                "    \"key\"  TEXT NOT NULL PRIMARY KEY,\n" +
                "    " +
                "-- password digest\n" +
                "    digest TEXT NOT NULL\n" +
                ")");

        final boolean hasRecord = db.hasRecord("SELECT digest\n" +
                "FROM password\n" +
                "WHERE \"key\" IS 'diary'");
        if (!hasRecord) {
            db.exec("INSERT INTO password (\"key\", digest)\n" +
                    "VALUES ('diary', '')");
        }
    }

    @NotNull
    private SQLite3 openPasswordDatabase() {
        return SQLite3.open(Common.getInternalDatabaseDir(this, "passwords.db").getPath());
    }

    private void updatePasswordRecord(@NotNull String password) {
        final SQLite3 passwordDatabase = openPasswordDatabase();

        String digest;
        // avoid my digest algorithm's calculation of an empty string
        if (password.isEmpty()) {
            digest = "";
        } else {
            digest = JNI.Diary.myDigest(password);
        }
        passwordDatabase.execBind("UPDATE password\n" +
                "SET digest=?\n" +
                "WHERE \"key\" IS 'diary'", new Object[]{digest});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isUnlocked) {
            final MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(currentMenuRes, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.password) {

            showChangePasswordDialog();
            return true;

        }

        // let it consumed by the fragments in this activity
        return false;
    }

    private void showChangePasswordDialog() {
        View view = View.inflate(this, R.layout.change_password_view, null);
        Dialog dialog = new Dialog(this);
        DialogUtil.setDialogAttr(dialog, false, MATCH_PARENT
                , ViewGroup.LayoutParams.WRAP_CONTENT, false);
        EditText oldPasswordET = ((SmartHintEditText) view.findViewById(R.id.old_password)).getEditText();
        EditText newPasswordET = ((SmartHintEditText) view.findViewById(R.id.new_password)).getEditText();
        Button confirm = view.findViewById(R.id.confirm);

        confirm.setOnClickListener(v -> {
            final String currentPasswordDigest = getPasswordDigest();

            String old = oldPasswordET.getText().toString();
            if (currentPasswordDigest.isEmpty() || currentPasswordDigest.equals(JNI.Diary.myDigest(old))) {
                updatePasswordRecord(newPasswordET.getText().toString());
                ToastUtils.show(this, R.string.change_succeeded);
                dialog.dismiss();
            } else ToastUtils.show(this, R.string.password_not_matching);
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private String getPasswordDigest() {
        final SQLite3 db = openPasswordDatabase();
        final String digest = getPasswordDigest(db);
        db.close();
        return digest;
    }
}