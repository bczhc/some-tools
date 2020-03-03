package pers.zhc.tools.floatingdrawing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.requireNonNull(intent.getAction()).equals("pers.zhc.tools.START_FB")) {
            long millisecond = intent.getLongExtra("millisecond", 0);
            FloatingDrawingBoardMainActivity activity = (FloatingDrawingBoardMainActivity) FloatingDrawingBoardMainActivity.longActivityMap.get(millisecond);
            if (activity != null) {
                activity.recover();
            }
        }
    }
}