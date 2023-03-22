package pers.zhc.tools.crashhandler;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import kotlin.Unit;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.BuildConfig;
import pers.zhc.tools.Info;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author bczhc
 */
public class CrashReportActivity extends BaseActivity {
    private TextView uploadStateTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ToastUtils.show(this, R.string.no_write_permission);
        }
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
                    .setClass(this, Info.LAUNCHER_CLASS);
            startActivity(launchIntent);
            killProcess();
        });
        final String filename = intent.getStringExtra("filename");
        final Map<String, String> deviceInfo = collectDeviceInfo();
        StringBuilder sb = new StringBuilder();
        final Set<String> keySet = deviceInfo.keySet();
        for (String key : keySet) {
            sb.append(key).append(": ").append(deviceInfo.get(key)).append('\n');
        }
        sb.append(exception);
        String content = sb.toString();
        uploadReportButton.setOnClickListener(v -> upload(filename, content));
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData cd = ClipData.newPlainText("exception", content);
                cm.setPrimaryClip(cd);
                ToastUtils.show(this, R.string.copying_succeeded);
            }
            // for debugging
            System.err.println(exception);
        });
        CrashHandler.save2File(this, filename, content);
    }

    private void killProcess() {
        Process.killProcess(Process.myPid());
    }

    /**
     * 收集设备参数信息
     */
    public Map<String, String> collectDeviceInfo() {
        Map<String, String> info = new HashMap<>(16);
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
            info.put(field.getName(), o == null ? "null" : o.toString());
        }
        return info;
    }

    private void upload(@SuppressWarnings("unused") String filename, String information) {
        uploadStateTextView.setTextColor(Color.BLUE);
        uploadStateTextView.setText(R.string.uploading);

        CrashReportUploader.INSTANCE.upload(this, information, () -> {
            uploadStateTextView.setTextColor(ContextCompat.getColor(this, R.color.done_green));
            uploadStateTextView.setText(R.string.upload_done);
            return Unit.INSTANCE;
        }, message -> {
            uploadStateTextView.setTextColor(ContextCompat.getColor(this, R.color.red));
            if (message == null) {
                uploadStateTextView.setText(getString(R.string.upload_failed_server_error_toast));
            } else {
                uploadStateTextView.setText(getString(R.string.upload_failed_toast, message));
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onBackPressed() {
        killProcess();
        super.onBackPressed();
    }
}
