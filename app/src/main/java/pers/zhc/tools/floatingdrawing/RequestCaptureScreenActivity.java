package pers.zhc.tools.floatingdrawing;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

public class RequestCaptureScreenActivity extends AppCompatActivity {
    private long timeMillisecond;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        timeMillisecond = intent.getLongExtra("millisecond", 0L);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        boolean b = requestCaptureScreen();
        if (!b) {
            ToastUtils.show(this, R.string.request_permission_error);
        }
    }

    private boolean requestCaptureScreen() {
        MediaProjectionManager mpm = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mpm == null) {
            return false;
        }
        Intent screenCaptureIntent = mpm.createScreenCaptureIntent();
        startActivityForResult(screenCaptureIntent, BaseActivity.RequestCode.REQUEST_CAPTURE_SCREEN);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FloatingDrawingBoardMainActivity activity = (FloatingDrawingBoardMainActivity) FloatingDrawingBoardMainActivity.longActivityMap.get(timeMillisecond);
        if (activity != null) {
            RequestPermissionInterface requestPermissionInterface = activity.requestPermissionInterface;
            if (requestPermissionInterface != null) {
                requestPermissionInterface.onRequestCallback(requestCode, resultCode, data);
            }
        } else {
            ToastUtils.show(this, R.string.native_error);
        }
        finish();
    }
}
