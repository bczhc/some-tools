package pers.zhc.tools;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import org.json.JSONException;
import org.json.JSONObject;
import pers.zhc.tools.bus.BusQueryMainActivity;
import pers.zhc.tools.clipboard.Clip;
import pers.zhc.tools.crashhandler.CrashTest;
import pers.zhc.tools.document.Document;
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardMainActivity;
import pers.zhc.tools.inputmethod.WubiInputMethodActivity;
import pers.zhc.tools.pi.Pi;
import pers.zhc.tools.stcflash.FlashMainActivity;
import pers.zhc.tools.test.*;
import pers.zhc.tools.test.DrawingBoardTest;
import pers.zhc.tools.test.RegExpTest;
import pers.zhc.tools.test.SensorTest;
import pers.zhc.tools.test.TTS;
import pers.zhc.tools.test.Demo;
import pers.zhc.tools.test.malloctest.MAllocTest;
import pers.zhc.tools.test.toast.ToastTest;
import pers.zhc.tools.test.typetest.TypeTest;
import pers.zhc.tools.utils.IOUtilsKt;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.diary.DiaryMainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/**
 * @author bczhc
 */
public class MainActivity extends BaseActivity {

    private ShortcutManager shortcutManager = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            Thread.sleep(100);
            setTheme(R.style.Theme_Application);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tools_activity_main);
        init();

    }


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

    private static class AnActivity {
        private final @StringRes
        int textIntRes;
        private final Class<?> activityClass;

        public AnActivity(int textIntRes, Class<? extends Activity> activityClass) {
            this.textIntRes = textIntRes;
            this.activityClass = activityClass;
        }
    }

    private void init() {
        LinearLayout ll = findViewById(R.id.ll);

        AnActivity[] activities = {
                new AnActivity(R.string.generate_pi, Pi.class),
                new AnActivity(R.string.toast, ToastTest.class),
                new AnActivity(R.string.put_in_clipboard, Clip.class),
                new AnActivity(R.string.floating_drawing_board, FloatingDrawingBoardMainActivity.class),
                new AnActivity(R.string.notes, Document.class),
                new AnActivity(R.string.test, Demo.class),
                new AnActivity(R.string.sensor_test, SensorTest.class),
                new AnActivity(R.string.crash_test, CrashTest.class),
                new AnActivity(R.string.m_alloc_test, MAllocTest.class),
                new AnActivity(R.string.diary, DiaryMainActivity.class),
                new AnActivity(R.string.type_test, TypeTest.class),
                new AnActivity(R.string.tts_test, TTS.class),
                new AnActivity(R.string.regular_expression_test, RegExpTest.class),
                new AnActivity(R.string.wubi_input_method, WubiInputMethodActivity.class),
                new AnActivity(R.string.stc_flash, FlashMainActivity.class),
                new AnActivity(R.string.drawing_board_test, DrawingBoardTest.class),
                new AnActivity(R.string.bus_query_label, BusQueryMainActivity.class)
        };

        CountDownLatch mainTextLatch = new CountDownLatch(1);
        new Thread(() -> {
            JSONObject jsonObject = null;
            try {
                URL url = new URL(Infos.ZHC_URL_STRING + "/tools_app/i.zhc");
                InputStream inputStream = url.openStream();
                final String read = IOUtilsKt.readToString(inputStream, StandardCharsets.UTF_8);
                jsonObject = new JSONObject(read);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            try {
                mainTextLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (jsonObject != null) {
                JSONObject finalJsonObject = jsonObject;
                runOnUiThread(() -> {
                    try {
                        String mainActivityText = finalJsonObject.getString("MainActivityText");
                        TextView tv = new TextView(this);
                        tv.setText(getString(R.string.str, mainActivityText));
                        ll.addView(tv);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        }).start();
        new Thread(() -> {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            for (int i = 0; i < activities.length; i++) {
                AnActivity activity = activities[i];
                Button btn = new Button(this);
                btn.setText(activity.textIntRes);
                btn.setTextSize(25F);
                btn.setAllCaps(false);
                btn.setLayoutParams(lp);
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent();
                    intent.setClass(this, activity.activityClass);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_bottom, 0);
                });
                int finalI = i;
                btn.setOnLongClickListener(v -> {
                    shortcut(activity.textIntRes, activity.activityClass, finalI);
                    return true;
                });
                runOnUiThread(() -> ll.addView(btn));
            }
            mainTextLatch.countDown();
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }
}