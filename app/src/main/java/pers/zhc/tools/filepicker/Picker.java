package pers.zhc.tools.filepicker;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.PermissionRequester;

import java.io.File;

public class Picker extends BaseActivity {
    public static int PICK_FILE = 1;
    public static int PICK_FOLDER = 2;
    private FilePickerRL filePickerRL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PermissionRequester(this::Do).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 33);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 33 && grantResults[0] == 0) Do();
    }

    private void Do() {
        Intent intent = getIntent();
        String initialPath = intent.getStringExtra("initialPath");
        initialPath = initialPath == null ? Common.getExternalStoragePath(this) : initialPath;
        filePickerRL = new FilePickerRL(this
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
        });
        setContentView(filePickerRL);
    }

    @Override
    public void onBackPressed() {
        filePickerRL.previous();
    }
}