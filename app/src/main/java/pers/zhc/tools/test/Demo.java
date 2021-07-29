package pers.zhc.tools.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import kotlin.Unit;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.fdb.ScreenColorPickerView;
import pers.zhc.tools.media.ScreenCapturePermissionRequestActivity;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {

    private Intent mediaProjectionData = null;
    private MediaProjection mediaProjection = null;
    private ImageReader imageReader = null;
    private int screenDensity;

    private final ActivityResultLauncher<Unit> requestScreenPermissionLauncher = ScreenCapturePermissionRequestActivity.Companion.getRequestLauncher(this, result -> {

        if (result.getResultCode() == RESULT_OK) {
            mediaProjectionData = result.getData();
            capture();
        } else {
            ToastUtils.show(this, "CANCELED");
        }

        return Unit.INSTANCE;
    });

    private void setupImageReader() {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        imageReader = ImageReader.newInstance(displayMetrics.widthPixels, displayMetrics.heightPixels, PixelFormat.RGBA_8888, 1);
        screenDensity = displayMetrics.densityDpi;

        imageReader.setOnImageAvailableListener(reader -> {
            final Image image = imageReader.acquireLatestImage();
            if (image != null) {
                ToastUtils.show(this, image.toString());
                image.close();
            } else {
                ToastUtils.show(this, "Null image");
            }
        }, null);
    }

    private void capture() {

        if (mediaProjection == null) {
            MediaProjectionManager mpm = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            this.mediaProjection = mpm.getMediaProjection(RESULT_OK, mediaProjectionData);

            setupImageReader();
            final VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("VirtualDisplay", imageReader.getWidth(), imageReader.getHeight(), screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ScreenColorPickerView view = new ScreenColorPickerView(this);
        setContentView(view);

        view.setOnTouchListener((v, event) -> {
            ToastUtils.show(this, event.toString());
            return true;
        });
    }
}