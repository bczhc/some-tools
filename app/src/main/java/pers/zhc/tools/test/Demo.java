package pers.zhc.tools.test;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new MaterialAlertDialogBuilder(this)
                .setTitle("aaa中文中文中文中文中文中文中文中文中文中文中文中文中文中文中文")
                .setPositiveButton("aa", (a, b) -> {
                })
                .show();
    }
}