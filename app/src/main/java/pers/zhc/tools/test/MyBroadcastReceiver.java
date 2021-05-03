package pers.zhc.tools.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.jetbrains.annotations.NotNull;

/**
 * @author bczhc
 */
class MyBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = this.getClass().getName();
    public static final String ACTION_A = "AAAAC1";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received: " + intent.getIntExtra("a", 0));
    }
}
