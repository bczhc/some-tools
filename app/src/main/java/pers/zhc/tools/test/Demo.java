package pers.zhc.tools.test;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    private MyBroadcastReceiver receiver;
    private int i = 0;
    private MyBroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastReceiver = new MyBroadcastReceiver();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MyBroadcastReceiver.ACTION_A);
        registerReceiver(broadcastReceiver, filter);

        final Button button = new Button(this);
        setContentView(button);
        button.setOnClickListener(v -> createNotification());
    }

    private void createNotification() {
        final Intent intent = new Intent(MyBroadcastReceiver.ACTION_A);
        intent.putExtra("a", 123);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final NotificationCompat.Builder nb = new NotificationCompat.Builder(this, "c1");
        final Notification notification = nb.setSmallIcon(R.drawable.ic_db)
                .setContentTitle("Test")
                .setContentText("...!!!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setChannelId("c1")
                .addAction(R.drawable.ic_zhc_logo, "a", pendingIntent)
                .build();

        i += 1;
        manager.notify(i, notification);
    }

    @Override
    public void finish() {
        unregisterReceiver(this.broadcastReceiver);
        super.finish();
    }
}
