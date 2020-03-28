package pers.zhc.tools.filepicker;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.WindowManager;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.PermissionRequester;

import java.io.File;

public class Picker extends BaseActivity {
    public static final int PICK_FILE = 1;
    public static final int PICK_FOLDER = 2;
    private FilePickerRelativeLayout filePickerRelativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        new PermissionRequester(this::Do).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 33 && grantResults[0] == 0) {
            Do();
        }
    }

    private void Do() {
        Intent intent = getIntent();
        String initialPath = intent.getStringExtra("initialPath");
        initialPath = initialPath == null ? Common.getExternalStoragePath(this) : initialPath;
        filePickerRelativeLayout = new FilePickerRelativeLayout(this
                , intent.getIntExtra("option", PICK_FILE)
                , new File(initialPath)
                , () -> {
            finish();
            overridePendingTransition(0, R.anim.fade_out);
        }, s -> {
            Intent r = new Intent();
            r.putExtra("result", s);
            setResult(3, r);
            finish();
            overridePendingTransition(0, R.anim.fade_out);
        }, null);
        setContentView(filePickerRelativeLayout);
    }

    @Override
    public void onBackPressed() {
        filePickerRelativeLayout.previous();
    }
}