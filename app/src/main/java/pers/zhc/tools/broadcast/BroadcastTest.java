package pers.zhc.tools.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class BroadcastTest extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
            final int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
            final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale;
            ToastUtils.show(context, String.valueOf(level));
            Log.d("broadcast", String.valueOf(level));
        } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            Log.d("broadcast", "airplane mode ...");
        }
    }
}
