package pers.zhc.tools.filepicker;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.PermissionRequester;
import pers.zhc.tools.utils.ToastUtils;

import java.io.File;

/**
 * @author bczhc
 * <p>Put a string extra whose key is {@link FilePicker#EXTRA_OPTION} and value is {@link FilePicker#PICK_FILE} or {@link FilePicker#PICK_FOLDER}
 * to the intent before starting this activity by invoking {@link AppCompatActivity#startActivityForResult(Intent, int)}</p>
 * <p>The data it returns in {@link AppCompatActivity#onActivityResult(int, int, Intent)} has a string extra
 * whose key is {@link FilePicker#EXTRA_RESULT} and value is the picked file or folder.</p><br/>
 */
public class FilePicker extends BaseActivity {
    public static final int PICK_FILE = 1;
    public static final int PICK_FOLDER = 2;
    public static final int RESULT_CODE = 3;
    private FilePickerRL filePickerRL;

    public static final String EXTRA_OPTION = "option";
    public static final String EXTRA_RESULT = "result";

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
        if (requestCode == RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == 0) Do();
            else {
                ToastUtils.show(this, R.string.please_grant_permission);
                finish();
            }
        }
    }

    private void Do() {
        Intent intent = getIntent();
        String initialPath = intent.getStringExtra("initialPath");
        initialPath = initialPath == null ? Common.getExternalStoragePath(this) : initialPath;
        filePickerRL = new FilePickerRL(this
                , intent.getIntExtra(EXTRA_OPTION, PICK_FILE)
                , new File(initialPath)
                , p -> {
            Intent r = new Intent();
            r.putExtra(EXTRA_RESULT, (String) null);
            setResult(RESULT_CODE, r);
            finish();
            overridePendingTransition(0, R.anim.fade_out);
        }, (picker, path) -> {
            Intent r = new Intent();
            r.putExtra(EXTRA_RESULT, path);
            setResult(RESULT_CODE, r);
            finish();
            overridePendingTransition(0, R.anim.fade_out);
        }, null);
        setContentView(filePickerRL);
    }

    @Override
    public void onBackPressed() {
        filePickerRL.previous();
    }
}