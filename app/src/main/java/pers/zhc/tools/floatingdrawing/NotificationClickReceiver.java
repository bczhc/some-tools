package pers.zhc.tools.floatingdrawing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.requireNonNull(intent.getAction()).equals("pers.zhc.tools.START_FB")) {
            long mills = intent.getLongExtra("mills", 0);
            Runnable run = FloatingDrawingBoardMainActivity.longRunnableMap.get(mills);
            if (run != null) {
                run.run();
            }
        }
    }
}