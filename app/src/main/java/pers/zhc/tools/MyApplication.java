package pers.zhc.tools;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;

/**
 * @author bczhc
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        BaseActivity.getAppInfo(this);

        createNotificationChannel1();
    }

    public void createNotificationChannel1() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel("c1", "channel name", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("description..");

            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
