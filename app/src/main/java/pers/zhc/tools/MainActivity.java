package pers.zhc.tools;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import pers.zhc.tools.characterscounter.CounterTest;
import pers.zhc.tools.clipboard.Clip;
import pers.zhc.tools.codecs.CodecsActivity;
import pers.zhc.tools.crashhandler.CrashTest;
import pers.zhc.tools.diary.DiaryMainActivity;
import pers.zhc.tools.document.Document;
import pers.zhc.tools.epicycles.EpicyclesEdit;
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardMainActivity;
import pers.zhc.tools.functiondrawing.FunctionDrawingBoard;
import pers.zhc.tools.inputmethod.WubiInputMethodActivity;
import pers.zhc.tools.pi.Pi;
import pers.zhc.tools.stcflash.FlashMainActivity;
import pers.zhc.tools.test.DocumentProviderTest;
import pers.zhc.tools.test.InputEvent;
import pers.zhc.tools.test.MathExpressionEvaluationTest;
import pers.zhc.tools.test.RegExpTest;
import pers.zhc.tools.test.SensorTest;
import pers.zhc.tools.test.SurfaceViewTest;
import pers.zhc.tools.test.TTS;
import pers.zhc.tools.test.UsbSerialTest;
import pers.zhc.tools.test.jni.Test;
import pers.zhc.tools.test.malloctest.MAllocTest;
import pers.zhc.tools.test.pressuretest.PressureTest;
import pers.zhc.tools.test.service.ServiceActivity;
import pers.zhc.tools.test.theme.SetTheme;
import pers.zhc.tools.test.toast.ToastTest;
import pers.zhc.tools.test.typetest.TypeTest;
import pers.zhc.tools.test.wubiinput.WubiInput;
import pers.zhc.tools.test.youdaoapi.YouDaoTranslate;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.common.ReadIS;

/**
 * @author bczhc
 */
public class MainActivity extends BaseActivity {

    private ShortcutManager shortcutManager = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tools_activity_main);
        init();
    }


    private void shortcut(int texts, Class<?> theClass, int id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            ToastUtils.show(this, R.string.shortcut_unsupport);
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

    private void init() {
        LinearLayout ll = findViewById(R.id.ll);
        final int[] texts = new int[]{
                R.string.some_codecs,
                R.string.generate_pi,
                R.string.toast,
                R.string.put_in_clipboard,
                R.string.floating_drawing_board,
                R.string.fourier_series_calc,
                R.string.notes,
                R.string.fourier_series_in_complex,
                R.string.s,
                R.string.view_test,
                R.string.set_theme,
                R.string.math_expression_evaluation_test,
                R.string.sensor_test,
                R.string.input_event,
                R.string.surface_view_test,
                R.string.serviceTest,
                R.string.you_dao_translate_interface_invoke,
                R.string.crash_test,
                R.string.m_alloc_test,
                R.string.diary,
                R.string.pressure_test,
                R.string.document_provider_test,
                R.string.characters_counter_test,
                R.string.type_test,
                R.string.tts_test,
                R.string.regular_expression_test,
                R.string.wubi_test,
                R.string.wubi_input_method,
                R.string.usb_serial_test,
                R.string.stc_flash
        };
        final Class<?>[] classes = new Class[]{
                CodecsActivity.class,
                Pi.class,
                ToastTest.class,
                Clip.class,
                FloatingDrawingBoardMainActivity.class,
                FunctionDrawingBoard.class,
                Document.class,
                EpicyclesEdit.class,
                Test.class,
                pers.zhc.tools.test.viewtest.MainActivity.class,
                SetTheme.class,
                MathExpressionEvaluationTest.class,
                SensorTest.class,
                InputEvent.class,
                SurfaceViewTest.class,
                ServiceActivity.class,
                YouDaoTranslate.class,
                CrashTest.class,
                MAllocTest.class,
                DiaryMainActivity.class,
                PressureTest.class,
                DocumentProviderTest.class,
                CounterTest.class,
                TypeTest.class,
                TTS.class,
                RegExpTest.class,
                WubiInput.class,
                WubiInputMethodActivity.class,
                UsbSerialTest.class,
                FlashMainActivity.class
        };
        CountDownLatch mainTextLatch = new CountDownLatch(1);
        new Thread(() -> {
            JSONObject jsonObject = null;
            try {
                URL url = new URL(Infos.ZHC_URL_STRING + "/tools_app/i.zhc");
                InputStream inputStream = url.openStream();
                StringBuilder sb = new StringBuilder();
                new ReadIS(inputStream, "utf-8").read(sb::append);
                inputStream.close();
                jsonObject = new JSONObject(sb.toString());
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
            for (int i = 0; i < texts.length; i++) {
                Button btn = new Button(this);
                btn.setText(texts[i]);
                btn.setTextSize(25F);
                btn.setAllCaps(false);
                btn.setLayoutParams(lp);
                int finalI = i;
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent();
                    intent.setClass(this, classes[finalI]);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_bottom, 0);
                });
                btn.setOnLongClickListener(v -> {
                    shortcut(texts[finalI], classes[finalI], finalI);
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