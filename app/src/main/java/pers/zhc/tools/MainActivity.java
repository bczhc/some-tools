package pers.zhc.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import kotlin.Unit;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.bus.BusQueryMainActivity;
import pers.zhc.tools.clipboard.Clip;
import pers.zhc.tools.test.CrashTest;
import pers.zhc.tools.diary.DiaryMainActivity;
import pers.zhc.tools.document.Document;
import pers.zhc.tools.fdb.FdbMainActivity;
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardMainActivity;
import pers.zhc.tools.inputmethod.WubiInputMethodActivity;
import pers.zhc.tools.magic.FileListActivity;
import pers.zhc.tools.pi.Pi;
import pers.zhc.tools.stcflash.FlashMainActivity;
import pers.zhc.tools.test.*;
import pers.zhc.tools.test.malloctest.MAllocTest;
import pers.zhc.tools.test.toast.ToastTest;
import pers.zhc.tools.test.typetest.TypeTest;
import pers.zhc.tools.utils.*;
import pers.zhc.tools.words.WordsMainActivity;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author bczhc
 */
public class MainActivity extends BaseActivity {

    private ShortcutManager shortcutManager = null;
    private final ArrayList<ActivityItem> activities = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tools_activity_main);

        addActivities();
        loadRecyclerView();
    }

    /**
     * TODO: 7/15/21 now when a shortcut was added, it could be changed or cross-positioned after a change of the main list
     */
    private void shortcut(int texts, Class<?> theClass, int id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            ToastUtils.show(this, R.string.shortcut_unsupported);
            return;
        }
        if (shortcutManager == null) {
            if ((shortcutManager = getSystemService(ShortcutManager.class)) == null) {
                ToastUtils.show(this, R.string.create_shortcut_failed);
                return;
            }
        }
        final List<ShortcutInfo> dynamicShortcuts = shortcutManager.getDynamicShortcuts();
        int removedIndex = -1;
        for (int i = 0; i < dynamicShortcuts.size(); i++) {
            ShortcutInfo dynamicShortcut = dynamicShortcuts.get(i);
            if (dynamicShortcut.getId().equals("shortcut_id" + id)) {
                removedIndex = i;
            }
        }
        if (removedIndex != -1) {
            dynamicShortcuts.remove(removedIndex);
            ToastUtils.show(this, R.string.deleted_shortcut);
        } else {
            int shortcutSize = dynamicShortcuts.size();
            if (shortcutSize + 1 > shortcutManager.getMaxShortcutCountPerActivity()) {
                ToastUtils.show(this, R.string.over_quantity_limit);
                return;
            }

            //new
            ShortcutInfo.Builder builder = new ShortcutInfo.Builder(this, "shortcut_id" + id);
            Intent intent = new Intent(this, theClass);
            intent.putExtra("fromShortcut", true);
            intent.setAction(Intent.ACTION_VIEW);
            ShortcutInfo shortcutInfo = builder.setShortLabel(getString(texts))
                    .setLongLabel(getString(texts))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
                    .setIntent(intent).build();
            dynamicShortcuts.add(shortcutInfo);
            ToastUtils.show(this, R.string.create_shortcut_succeeded);
        }
        //rebuild
        List<ShortcutInfo> newShortCutInfoList = new ArrayList<>();
        for (ShortcutInfo shortcut : dynamicShortcuts) {
            ShortcutInfo.Builder builder1 = new ShortcutInfo.Builder(this, shortcut.getId());
            builder1.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
                    .setLongLabel(Objects.requireNonNull(shortcut.getLongLabel()))
                    .setIntent(Objects.requireNonNull(shortcut.getIntent()))
                    .setShortLabel(Objects.requireNonNull(shortcut.getShortLabel()));
            newShortCutInfoList.add(builder1.build());
        }
        shortcutManager.setDynamicShortcuts(newShortCutInfoList);
    }

    private void addActivities() {
        activities.add(new ActivityItem(R.string.generate_pi, Pi.class));
        activities.add(new ActivityItem(R.string.toast, ToastTest.class));
        activities.add(new ActivityItem(R.string.put_in_clipboard, Clip.class));
        activities.add(new ActivityItem(R.string.floating_drawing_board, FloatingDrawingBoardMainActivity.class));
        activities.add(new ActivityItem(R.string.floating_drawing_board, FdbMainActivity.class));
        activities.add(new ActivityItem(R.string.notes, Document.class));
        activities.add(new ActivityItem(R.string.test, Demo.class));
        activities.add(new ActivityItem(R.string.sensor_test, SensorTest.class));
        activities.add(new ActivityItem(R.string.crash_test, CrashTest.class));
        activities.add(new ActivityItem(R.string.m_alloc_test, MAllocTest.class));
        activities.add(new ActivityItem(R.string.diary, DiaryMainActivity.class));
        activities.add(new ActivityItem(R.string.type_test, TypeTest.class));
        activities.add(new ActivityItem(R.string.tts_test, TTS.class));
        activities.add(new ActivityItem(R.string.regular_expression_test, RegExpTest.class));
        activities.add(new ActivityItem(R.string.wubi_input_method, WubiInputMethodActivity.class));
        activities.add(new ActivityItem(R.string.stc_flash, FlashMainActivity.class));
        activities.add(new ActivityItem(R.string.drawing_board_test, DrawingBoardTest.class));
        activities.add(new ActivityItem(R.string.bus_query_label, BusQueryMainActivity.class));
        activities.add(new ActivityItem(R.string.sys_info_label, SysInfo.class));
        activities.add(new ActivityItem(R.string.magic_label, FileListActivity.class));
        activities.add(new ActivityItem(R.string.unicode_table_label, UnicodeTable.class));
        activities.add(new ActivityItem(R.string.words_label, WordsMainActivity.class));
    }

    private void loadRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        final MyAdapter adapter = new MyAdapter(this, activities);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter.setOnItemClickListener((position, view) -> {
            final ActivityItem item = activities.get(position);
            startActivity(new Intent(this, item.activityClass));
            return Unit.INSTANCE;
        });

        adapter.setOnItemLongClickListener((position, view) -> {
            final ActivityItem item = activities.get(position);
            shortcut(item.textRes, item.activityClass, position);
            return Unit.INSTANCE;
        });
    }

    private static class ActivityItem {
        private @StringRes
        final int textRes;
        private final Class<? extends Activity> activityClass;

        public ActivityItem(int textRes, Class<? extends Activity> activityClass) {
            this.textRes = textRes;
            this.activityClass = activityClass;
        }
    }

    private static class MyAdapter extends AdapterWithClickListener<MyAdapter.MyViewHolder> {
        private final Context context;
        private final ArrayList<ActivityItem> activities;

        public MyAdapter(Context context, ArrayList<ActivityItem> activities) {
            this.context = context;
            this.activities = activities;
        }

        @org.jetbrains.annotations.Nullable
        @Contract(pure = true)
        @Override
        public MyViewHolder onCreateViewHolder(@NotNull ViewGroup parent) {
            final View view = LayoutInflater.from(context).inflate(R.layout.main_activity_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {
            final View view = holder.itemView;
            TextView tv = view.findViewById(R.id.tv);
            tv.setText(activities.get(position).textRes);
        }

        @Override
        public int getItemCount() {
            return activities.size();
        }

        private static class MyViewHolder extends RecyclerView.ViewHolder {
            public MyViewHolder(@NonNull @NotNull View itemView) {
                super(itemView);
            }
        }
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
            checkAndUpdate();
        }
        return true;
    }

    private void checkAndUpdate() {
        final String storagePath = Common.getAppMainExternalStoragePath(this);
        final File updateDir = new File(storagePath, "update");
        if (!updateDir.exists()) {
            if (!updateDir.mkdirs()) {
                ToastUtils.show(this, R.string.mkdir_failed);
                return;
            }
        }

        // TODO: 7/16/21 check update

        DialogUtil.createConfirmationAlertDialog(this, (dialog, which) -> {

            try {
                final File apkFile = new File(updateDir, "some-tools.apk");
                Download.startDownloadWithDialog(
                        this, new URL(Common.getStaticResourceUrlString("apks/some-tools.apk")),
                        apkFile,
                        () -> Common.installApk(this, apkFile)
                );
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

        }, R.string.app_update_download_dialog).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }
}