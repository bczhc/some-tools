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
import pers.zhc.tools.clipboard.Clip;
import pers.zhc.tools.codecs.CodecsActivity;
import pers.zhc.tools.crashhandler.CrashTest;
import pers.zhc.tools.diary.DiaryMainActivity;
import pers.zhc.tools.document.Document;
import pers.zhc.tools.epicycles.EpicyclesEdit;
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardMainActivity;
import pers.zhc.tools.functiondrawing.FunctionDrawingBoard;
import pers.zhc.tools.malloctest.MAllocTest;
import pers.zhc.tools.pi.Pi;
import pers.zhc.tools.pressuretest.PressureTest;
import pers.zhc.tools.test.DocumentProviderTest;
import pers.zhc.tools.test.InputEvent;
import pers.zhc.tools.test.MathExpressionEvaluationTest;
import pers.zhc.tools.test.SensorTest;
import pers.zhc.tools.test.SurfaceViewTest;
import pers.zhc.tools.test.jni.Test;
import pers.zhc.tools.test.service.ServiceActivity;
import pers.zhc.tools.theme.SetTheme;
import pers.zhc.tools.toast.ToastTest;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.youdaoapi.YouDaoTranslate;
import pers.zhc.u.common.ReadIS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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


    private boolean shortcut(int texts, Class<?> theClass, int id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false;
        }
        if (shortcutManager == null) {
            if ((shortcutManager = getSystemService(ShortcutManager.class)) == null) {
                return false;
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
        } else {
            int shortcutSize = dynamicShortcuts.size();
            if (shortcutSize > shortcutManager.getMaxShortcutCountPerActivity()) {
                return false;
            }
            ShortcutInfo.Builder builder = new ShortcutInfo.Builder(this, "shortcut_id" + id);
            Intent intent = new Intent(this, theClass);
            intent.putExtra("fromShortcut", true);
            intent.setAction(Intent.ACTION_VIEW);
            ShortcutInfo shortcutInfo = builder.setShortLabel(getString(texts))
                    .setLongLabel(getString(texts))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher_foreground))
                    .setIntent(intent).build();
            dynamicShortcuts.add(shortcutInfo);
        }
        shortcutManager.setDynamicShortcuts(dynamicShortcuts);
        return true;
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
                R.string.document_provider_test
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
                DocumentProviderTest.class
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
                        tv.setText(getString(R.string.tv, mainActivityText));
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
                    final boolean shortcut = shortcut(texts[finalI], classes[finalI], finalI);
                    ToastUtils.show(this, shortcut ? R.string.create_shortcut_success : R.string.create_shortcut_failure);
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