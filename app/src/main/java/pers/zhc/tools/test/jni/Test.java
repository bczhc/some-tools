package pers.zhc.tools.test.jni;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Objects;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

/**
 * @author bczhc
 */
public class Test extends BaseActivity {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EditText et = new EditText(this);
        et.setGravity(Gravity.TOP);
        SpannableString ss = new SpannableString(" ");
        Drawable d = getDrawable(R.drawable.ic_db);
        Objects.requireNonNull(d).setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
        ss.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        et.setText(ss);
        setContentView(et);
        et.setOnLongClickListener(v -> {
            ToastUtils.show(this, et.getText());
            return true;
        });
    }
}
