package pers.zhc.tools.filepicker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
    public static final int RESULT_CODE = 0;
    private FilePickerRL filePickerRL;

    public static final String EXTRA_OPTION = "option";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_ENABLE_FILENAME = "enableFilename";
    public static final String EXTRA_FILENAME_RESULT = "filenameResult";

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
        boolean enableEditText = intent.getBooleanExtra(EXTRA_ENABLE_FILENAME, false);
        initialPath = initialPath == null ? Common.getExternalStoragePath(this) : initialPath;
        filePickerRL = new FilePickerRL(this
                , intent.getIntExtra(EXTRA_OPTION, PICK_FILE)
                , new File(initialPath)
                , p -> {
            finish();
            overridePendingTransition(0, R.anim.fade_out);

        }, (picker, path) -> {

            Intent r = new Intent();
            r.putExtra(EXTRA_RESULT, path);
            if (enableEditText) r.putExtra(EXTRA_FILENAME_RESULT, picker.getFilenameText());
            setResult(RESULT_CODE, r);
            finish();
            overridePendingTransition(0, R.anim.fade_out);

        }, null, enableEditText);

        setContentView(filePickerRL);
    }

    @Override
    public void onBackPressed() {
        filePickerRL.previous();
    }

    @NotNull
    @Contract("_, _ -> new")
    public static ActivityResultLauncher<Integer> getLauncher(@NotNull BaseActivity activity, @NotNull OnPickedResultCallback callback) {
        return activity.registerForActivityResult(new ActivityResultContract<Integer, String>() {
            @NonNull
            @NotNull
            @Override
            public Intent createIntent(@NonNull @NotNull Context context, Integer input) {
                final Intent intent = new Intent(activity, FilePicker.class);
                intent.putExtra(EXTRA_OPTION, input);
                return intent;
            }

            @Override
            public String parseResult(int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent intent) {
                if (intent == null) return null;
                return intent.getStringExtra(EXTRA_RESULT);
            }
        }, callback::result);
    }

    @NotNull
    @Contract("_, _ -> new")
    public static ActivityResultLauncher<Integer> getLauncherWithFilename(@NotNull BaseActivity activity, @NotNull OnPickedResultWithFilenameCallback callback) {
        return activity.registerForActivityResult(new ActivityResultContract<Integer, Result>() {
            @NonNull
            @NotNull
            @Override
            public Intent createIntent(@NonNull @NotNull Context context, Integer input) {
                final Intent intent = new Intent(activity, FilePicker.class);
                intent.putExtra(EXTRA_OPTION, input);
                intent.putExtra(EXTRA_ENABLE_FILENAME, true);
                return intent;
            }

            @Override
            public Result parseResult(int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent intent) {
                if (intent == null) return null;
                return new Result(intent.getStringExtra(EXTRA_RESULT), intent.getStringExtra(EXTRA_FILENAME_RESULT));
            }
        }, result -> callback.result(result.path, result.filename));
    }

    public interface OnPickedResultCallback {
        void result(@org.jetbrains.annotations.Nullable @Nullable String path);
    }

    public interface OnPickedResultWithFilenameCallback {
        void result(@org.jetbrains.annotations.Nullable @Nullable String path, @NotNull String filename);
    }

    private static class Result {
        private final String path;
        private final String filename;

        @Contract(pure = true)
        public Result(String path, String filename) {
            this.path = path;
            this.filename = filename;
        }
    }
}