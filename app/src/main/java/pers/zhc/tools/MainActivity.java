package pers.zhc.tools;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.app.ActivityItem;
import pers.zhc.tools.app.AppMenuAdapter;
import pers.zhc.tools.app.SmallToolsListActivity;
import pers.zhc.tools.app.TestListActivity;
import pers.zhc.tools.bus.BusQueryMainActivity;
import pers.zhc.tools.diary.DiaryMainActivity;
import pers.zhc.tools.document.Document;
import pers.zhc.tools.email.EmailMainActivity;
import pers.zhc.tools.fdb.FdbMainActivity;
import pers.zhc.tools.fourierseries.FourierSeriesActivity;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.wubi.WubiInputMethodActivity;
import pers.zhc.tools.magic.FileListActivity;
import pers.zhc.tools.stcflash.FlashMainActivity;
import pers.zhc.tools.transfer.TransferMainActivity;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.words.WordsMainActivity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static pers.zhc.tools.MyApplication.wakeLock;

/**
 * @author bczhc
 */
public class MainActivity extends BaseActivity {

    private final ArrayList<ActivityItem> activities = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tools_activity_main);

        addActivities();
        loadRecyclerView();
    }

    private void addActivities() {
        activities.add(new ActivityItem(R.string.app_menu_test, TestListActivity.class));
        activities.add(new ActivityItem(R.string.app_menu_little_tools, SmallToolsListActivity.class));
        activities.add(new ActivityItem(R.string.floating_drawing_board, FdbMainActivity.class));
        activities.add(new ActivityItem(R.string.notes, Document.class));
        activities.add(new ActivityItem(R.string.diary, DiaryMainActivity.class));
        activities.add(new ActivityItem(R.string.wubi_input_method, WubiInputMethodActivity.class));
        activities.add(new ActivityItem(R.string.stc_flash, FlashMainActivity.class));
        activities.add(new ActivityItem(R.string.bus_query_label, BusQueryMainActivity.class));
        activities.add(new ActivityItem(R.string.magic_label, FileListActivity.class));
        activities.add(new ActivityItem(R.string.words_label, WordsMainActivity.class));
        activities.add(new ActivityItem(R.string.transfer_label, TransferMainActivity.class));
        activities.add(new ActivityItem(R.string.email_label, EmailMainActivity.class));
        activities.add(new ActivityItem(R.string.fourier_series_label, FourierSeriesActivity.class));
    }

    private void loadRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        final AppMenuAdapter adapter = new AppMenuAdapter(this, activities);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // TODO: 9/21/21 shortcut
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.settings) {
            startActivity(new Intent(this, Settings.class));
        } else if (itemId == R.id.update) {
            updateAction();
        } else if (itemId == R.id.git_log) {
            showGitLogDialog();
        } else if (itemId == R.id.switch_themes) {
            if (AppCompatDelegate.getDefaultNightMode() == MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
            } else if (AppCompatDelegate.getDefaultNightMode() == MODE_NIGHT_NO) {
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
            }
        } else if (itemId == R.id.wake_lock_acquire) {
            acquireWakeLockAction();
        } else if (itemId == R.id.wake_lock_release) {
            releaseWakeLockAction();
        }
        return true;
    }

    @SuppressLint("WakelockTimeout")
    private void acquireWakeLockAction() {
        if (wakeLock == null) {
            final PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (wakeLock.isHeld()) {
            ToastUtils.show(this, R.string.wake_lock_already_held_toast);
            return;
        }
        wakeLock.acquire();
        ToastUtils.show(this, R.string.wake_lock_held_success);
    }

    private void releaseWakeLockAction() {
        if (wakeLock == null || !wakeLock.isHeld()) {
            ToastUtils.show(this, R.string.wake_lock_no_held);
            return;
        }
        wakeLock.release();
        ToastUtils.show(this, R.string.wake_lock_release_success);
    }

    private void showGitLogDialog() {
        String[] commitLogSplit = BuildConfig.commitLogEncodedSplit;
        StringBuilder sb = new StringBuilder();
        for (String s : commitLogSplit) {
            sb.append(s);
        }
        String compressedGitLogEncoded = sb.toString();
        byte[] gitLogData = JNI.Lzma.decompress(Base64.decode(compressedGitLogEncoded, Base64.DEFAULT));
        String gitLog = new String(gitLogData, StandardCharsets.UTF_8);

        final View inflate = View.inflate(this, R.layout.git_log_view, null);
        TextView tv = inflate.findViewById(R.id.tv);
        tv.setText(gitLog);

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(inflate);
        dialog.show();
    }

    private void updateAction() {
        pers.zhc.tools.main.MainActivity.Companion.showGithubActionDownloadDialog(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }
}