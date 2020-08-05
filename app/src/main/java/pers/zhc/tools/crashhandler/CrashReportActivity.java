package pers.zhc.tools.crashhandler;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.Button;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.BuildConfig;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.common.MultipartUploader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author bczhc
 */
public class CrashReportActivity extends Activity {
    private final String TAG = this.getClass().getName();
    private TextView uploadStateTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        setContentView(R.layout.uncaught_exception_report_activity);
        final String exception = intent.getStringExtra("exception");
        TextView textView = findViewById(R.id.content);
        textView.setText(exception);
        Button restartButton = findViewById(R.id.restart_btn);
        Button uploadReportButton = findViewById(R.id.upload_report_btn);
        Button copyBtn = findViewById(R.id.copy_btn);
        uploadStateTextView = findViewById(R.id.state);
        restartButton.setOnClickListener(v -> {
            Intent launchIntent = new Intent();
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setClass(this, BaseActivity.Infos.LAUNCHER_CLASS);
            startActivity(launchIntent);
            killProcess();
        });
        final String filename = intent.getStringExtra("filename");
        final Map<String, String> deviceInfo = collectDeviceInfo();
        StringBuilder content = new StringBuilder();
        final Set<String> keySet = deviceInfo.keySet();
        for (String key : keySet) {
            content.append(key).append(": ").append(deviceInfo.get(key)).append('\n');
        }
        content.append(exception);
        uploadReportButton.setOnClickListener(v -> upload(filename, content.toString()));
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData cd = ClipData.newPlainText("exception", exception);
                cm.setPrimaryClip(cd);
                ToastUtils.show(this, R.string.copying_success);
            }
            //for debugging
            System.err.println(exception);
        });
        CrashHandler.save2File(this, filename, exception);
    }

    private void killProcess() {
        Process.killProcess(Process.myPid());
    }

    /**
     * 收集设备参数信息
     */
    public Map<String, String> collectDeviceInfo() {
        Map<String, String> infos = new HashMap<>(16);
        List<Field> declaredFields = new ArrayList<>();
        declaredFields.addAll(Arrays.asList(BuildConfig.class.getDeclaredFields()));
        declaredFields.addAll(Arrays.asList(Build.class.getDeclaredFields()));
        for (Field field : declaredFields) {
            field.setAccessible(true);
            Object o = null;
            try {
                o = field.get(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            infos.put(field.getName(), o == null ? "null" : o.toString());
        }
        return infos;
    }

    private void upload(String filename, String information) {
        byte[] bytes = new byte[0];
        byte[] contentBytes = new byte[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            bytes = filename.getBytes(StandardCharsets.UTF_8);
            contentBytes = information.getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                //noinspection CharsetObjectCanBeUsed
                bytes = filename.getBytes("UTF-8");
                //noinspection CharsetObjectCanBeUsed
                contentBytes = information.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        uploadStateTextView.setTextColor(Color.BLUE);
        uploadStateTextView.setText(R.string.uploading);
        byte[] finalContentBytes = contentBytes;
        byte[] finalBytes = bytes;
        new Thread(() -> {
            try {
                final String crashUploadSite = BaseActivity.Infos.ZHC_URL_STRING + "/tools_app/crash_report.zhc";
                MultipartUploader.formUpload(crashUploadSite, finalBytes, finalContentBytes);
                runOnUiThread(() -> {
                    uploadStateTextView.setTextColor(ContextCompat.getColor(this, R.color.done_green));
                    uploadStateTextView.setText(R.string.upload_done);
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    ToastUtils.showError(this, R.string.upload_failure, e);
                    uploadStateTextView.setTextColor(Color.RED);
                    uploadStateTextView.setText(R.string.upload_failure);
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        killProcess();
        super.onBackPressed();
    }
}