package pers.zhc.tools.test;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.floatingdrawing.FloatingDrawingBoardService;
import pers.zhc.tools.utils.DisplayUtil;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    private Intent serviceIntent;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);
    }

    public static class MyService extends Service {
        private final String TAG = this.getClass().getName();

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            WindowManager wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Button b = new Button(this);
            b.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.format = PixelFormat.RGBA_8888;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            }
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            wm.addView(b, lp);

            return START_REDELIVER_INTENT;
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            int screenWidth = DisplayUtil.dip2px(this, newConfig.screenWidthDp);
            int screenHeight = DisplayUtil.dip2px(this, newConfig.screenHeightDp);
            switch (newConfig.orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                case Configuration.ORIENTATION_PORTRAIT:
                    ToastUtils.show(this, String.format("w: %d\nh: %d", screenWidth, screenHeight));
                    break;
                default:
            }
        }

        @Override
        public void onDestroy() {
            ToastUtils.show(this, "Service done.");
            super.onDestroy();
        }
    }

    @Override
    public void finish() {
//        stopService(serviceIntent);
        super.finish();
    }
}