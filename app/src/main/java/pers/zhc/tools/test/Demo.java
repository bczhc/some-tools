package pers.zhc.tools.test;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
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
            new Thread(() -> {
                Handler handler = new Handler(Looper.getMainLooper());
                for (int i = 0; ; ++i) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    int finalI = i;
//                    handler.post(() -> ToastUtils.show(this, String.valueOf(finalI)));

                }
            }).start();

            return START_REDELIVER_INTENT;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            Configuration configuration = this.getResources().getConfiguration();
            switch (configuration.orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    ToastUtils.show(this, "landscape");
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    ToastUtils.show(this, "portrait");
                    break;
                default:
            }
        }

        @Override
        public void onDestroy() {
            ToastUtils.show(this, "Service done.");
            Handler handler = new Handler();
            handler.post(() -> {
            });
            super.onDestroy();
        }
    }

    @Override
    public void finish() {
//        stopService(serviceIntent);
        super.finish();
    }
}
