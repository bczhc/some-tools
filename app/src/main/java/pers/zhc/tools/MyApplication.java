package pers.zhc.tools;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import pers.zhc.tools.diary.DiaryDatabase;

/**
 * @author bczhc
 */
public class MyApplication extends Application {
    public static String NOTIFICATION_CHANNEL_ID_COMMON = "c1";

    @Override
    public void onCreate() {
        super.onCreate();

        registerNotificationChannel();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);

        init();
    }

    private void init() {
        DiaryDatabase.init(this);
    }

    private void registerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_COMMON, getString(R.string.notification_chnnel_name_common), NotificationManager.IMPORTANCE_DEFAULT);
            final NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
