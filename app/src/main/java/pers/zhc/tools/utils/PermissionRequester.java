package pers.zhc.tools.utils;

import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionRequester {
    private final PermissionRequesterInterface requesterInterface;

    public PermissionRequester(PermissionRequesterInterface permissionRequesterInterface) {
        this.requesterInterface = permissionRequesterInterface;
    }

    public void requestPermission(AppCompatActivity activity, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{
                    permission
            }, requestCode);
        } else {
            requesterInterface.action();
        }
    }
}
