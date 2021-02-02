package pers.zhc.tools.floatingdrawing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

import pers.zhc.tools.BaseActivity;

/**
 * @author bczhc
 */
public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BaseActivity.BroadcastIntent.START_FLOATING_BOARD.equals(Objects.requireNonNull(intent.getAction()))) {
            long millisecond = intent.getLongExtra("millisecond", 0);
            FloatingDrawingBoardMainActivity activity = (FloatingDrawingBoardMainActivity) FloatingDrawingBoardMainActivity.longActivityMap.get(millisecond);
            if (activity != null) {
                activity.recover();
            }
        }
    }
}