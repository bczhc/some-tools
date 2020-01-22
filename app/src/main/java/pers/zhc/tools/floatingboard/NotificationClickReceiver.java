package pers.zhc.tools.floatingboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.requireNonNull(intent.getAction()).equals("pers.zhc.tools.START_FB")) {
            long mills = intent.getLongExtra("mills", 0);
            FloatingBoardMainActivity activity = (FloatingBoardMainActivity) FloatingBoardMainActivity.longMainActivityMap.get(mills);
            if (activity != null) {
                boolean isDrawMode = intent.getBooleanExtra("isDrawMode", false);
                if (isDrawMode) {
                    activity.toggleDrawAndControlMode();
                }
                activity.startFloatingWindow(false);
                activity.toggleDrawAndControlMode();
            }
        }
    }
}