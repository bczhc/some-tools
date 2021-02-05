package pers.zhc.tools.test.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;

public class ServiceActivity extends BaseActivity {
    private Intent serviceIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                System.out.println("onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                System.out.println("onServiceDisconnected");
            }
        };

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(serviceIntent);
    }
}
