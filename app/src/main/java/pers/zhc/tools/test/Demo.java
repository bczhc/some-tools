package pers.zhc.tools.test;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_activity);

        final ListView listView = findViewById(R.id.list_view);
        final MyAdapter myAdapter = new MyAdapter(this);
        listView.setAdapter(myAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position == myAdapter.getCount() - 1) {
                myAdapter.addCount();
                myAdapter.notifyDataSetChanged();
            }
        });
    }

    private static class MyAdapter extends BaseAdapter {
        private int count = 100;
        private final Context context;

        public MyAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return this.count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final RelativeLayout relativeLayout = new RelativeLayout(context);
            relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            final TextView textView = new TextView(context);
            textView.setText(String.valueOf(position));
            relativeLayout.addView(textView);
            return relativeLayout;
        }

        public void addCount() {
            ++this.count;
        }
    }
}
