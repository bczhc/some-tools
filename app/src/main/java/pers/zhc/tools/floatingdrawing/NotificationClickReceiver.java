package pers.zhc.tools.floatingdrawing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import pers.zhc.tools.utils.Common;

import static pers.zhc.tools.BaseActivity.BroadcastAction.ACTION_START_FLOATING_BOARD;

/**
 * @author bczhc
 */
public class NotificationClickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Common.doAssertion(intent != null);
        final String action = intent.getAction();

        if (action.equals(ACTION_START_FLOATING_BOARD)) {
            long millisecond = intent.getLongExtra("millisecond", 0);
            FloatingDrawingBoardMainActivity activity = (FloatingDrawingBoardMainActivity) FloatingDrawingBoardMainActivity.longActivityMap.get(millisecond);
            if (activity != null) {
                activity.recover();
            }
        }
    }
}