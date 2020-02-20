package pers.zhc.tools.floatingdrawing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        /*if (Objects.requireNonNull(intent.getAction()).equals("pers.zhc.tools.START_FB")) {
            long mills = intent.getLongExtra("mills", 0);
            FloatingDrawingBoardMainActivity activity = (FloatingDrawingBoardMainActivity) FloatingDrawingBoardMainActivity.longMainActivityMap.get(mills);
            if (activity != null) {
                activity.recover();
            }
        }*/
        ((FloatingDrawingBoardMainActivity) context.getApplicationContext()).recover();
        Toast.makeText(context, "recover!", Toast.LENGTH_SHORT).show();
    }
}