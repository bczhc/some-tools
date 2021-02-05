package pers.zhc.tools;

import android.app.Application;
import android.os.Handler;

/**
 * @author bczhc
 */
public class BaseApplication extends Application {
    public static Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        /*new Handler(Looper.getMainLooper()).post(() -> {
            while (true) {
                try {
                    Looper.loop();
                } catch (Throwable e) {
                    ToastUtils.show(this, e.toString());
                    e.printStackTrace();
                }
            }
        });*/
    }
}
