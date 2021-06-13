package pers.zhc.tools.test;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.MyApplication;
import pers.zhc.tools.R;
import pers.zhc.tools.bus.BusArrivalReminderNotificationReceiver;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.views.ScalableImageView;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ScalableImageView imageView = new ScalableImageView(this);
        setContentView(imageView);

        final InputStream inputStream = getResources().openRawResource(R.raw.db);
        imageView.setBitmap(BitmapFactory.decodeStream(inputStream));
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
