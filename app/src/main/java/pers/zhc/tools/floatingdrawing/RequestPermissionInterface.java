package pers.zhc.tools.floatingdrawing;

import android.content.Intent;

public interface RequestPermissionInterface {
    void onRequestCallback(int requestCode, int resultCode, Intent data);
}
