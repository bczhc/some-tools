package pers.zhc.tools.test;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.DialogUtil;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        final ProgressDialog dialog = new ProgressDialog(this);
//        DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
//        dialog.show();
//        Dialog d = new Dialog(this);
//        final ProgressView view = new ProgressView(this);
//        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//        d.setContentView(view);
//        DialogUtil.setDialogAttr(d, false, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
//        d.show();

//        Dialog d = new Dialog(this);
//        d.setContentView(View.inflate(this, R.layout.edittext_view, null));
//        DialogUtil.setDialogAttr(d, false, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
//        d.show();
        setContentView(R.layout.demo_activity);
    }
}