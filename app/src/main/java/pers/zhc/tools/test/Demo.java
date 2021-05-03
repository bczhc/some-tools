package pers.zhc.tools.test;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.MyApplication;
import pers.zhc.tools.R;
import pers.zhc.tools.bus.BusArrivalReminderNotificationReceiver;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Button button = new Button(this);
        setContentView(button);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastAction.ACTION_BUS_CANCEL_CLICK);
        registerReceiver(new BusArrivalReminderNotificationReceiver(), filter);

        button.setOnClickListener(v -> {
            final Intent intent = new Intent(BroadcastAction.ACTION_BUS_CANCEL_CLICK);
            intent.putExtra("aa", 1234);
            PendingIntent pi = PendingIntent.getBroadcast(this, 2, intent, 0);

            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MyApplication.NOTIFICATION_CHANNEL_ID_COMMON);
            final Notification notification = builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("aab")
                    .addAction(R.drawable.ic_launcher_foreground, "badsasdads", pi)
                    .build();

            final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(0, notification);
        });
    }
}
