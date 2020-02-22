package pers.zhc.tools.test.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import pers.zhc.tools.utils.ToastUtils;

public class MyService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        ToastUtils.show(this, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        ToastUtils.show(this, "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ToastUtils.show(this, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("onUnbind");
        return super.onUnbind(intent);
    }
}
