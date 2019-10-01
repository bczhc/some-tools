package pers.zhc.tools.test.epicycles_test;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.ComplexValue;

public class EpicyclesEdit extends AppCompatActivity {

    static EpicyclesSequence epicyclesSequence;
    private LinearLayout ll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        epicyclesSequence = new EpicyclesSequence();
        setContentView(R.layout.epicycles_edit_activity);
        EditText et_c_re = findViewById(R.id.c_re_tv);
        EditText et_c_im = findViewById(R.id.c_im_tv);
        EditText et_n = findViewById(R.id.et_n);
        Button btn = findViewById(R.id.add_btn);
        Button start_btn = findViewById(R.id.start);
        Button randomBtn = findViewById(R.id.random);
        Button drawGraphBtn = findViewById(R.id.drawing_graph);
        EditText definite_n = findViewById(R.id.definite_n_et);
        EditText T = findViewById(R.id.t_et);
        EditText epicycles_count = findViewById(R.id.epicycles_count);
        drawGraphBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComplexGraphDrawing.class);
            intent.putExtra("definite_n", Integer.valueOf(definite_n.getText().toString()));
            intent.putExtra("T", Integer.valueOf(T.getText().toString()));
            intent.putExtra("epicycles_count", Double.valueOf(epicycles_count.getText().toString()));
            startActivityForResult(intent, 71);
        });
        randomBtn.setOnClickListener(v -> {
            EpicyclesSequence.AEpicycle aEpicycle = new EpicyclesSequence.AEpicycle(Math.random() * 30, new ComplexValue(Math.random() * 10, Math.random() * 10));
            EpicyclesEdit.epicyclesSequence.put(aEpicycle);
            TextView tv = new TextView(this);
            setTV(tv, aEpicycle);
            String s = getString(R.string.left_parenthesis)
                    + aEpicycle.c.re + getString(R.string.add)
                    + aEpicycle.c.im + getString(R.string.i)
                    + getString(R.string.right_parenthesis)
                    + getString(R.string.e)
                    + getString(R.string.caret)
                    + getString(R.string.left_parenthesis)
                    + aEpicycle.n
                    + getString(R.string.i)
                    + getString(R.string.t)
                    + getString(R.string.right_parenthesis);
            tv.setText(getString(R.string.tv, s));
            ll.addView(tv);

        });
        ll = findViewById(R.id.ll);
        btn.setOnClickListener(v -> {
            String s1 = et_n.getText().toString();
            s1 = s1.equals("") ? "0" : s1;
            String s2 = et_c_re.getText().toString();
            s2 = s2.equals("") ? "0" : s2;
            String s3 = et_c_im.getText().toString();
            s3 = s3.equals("") ? "0" : s3;
            EpicyclesSequence.AEpicycle aEpicycle = new EpicyclesSequence.AEpicycle(Double.valueOf(s1)
                    , new ComplexValue(Double.valueOf(s2)
                    , Double.valueOf(s3)));
            AppCompatTextView tv = new AppCompatTextView(this);
            setTV(tv, aEpicycle);
            String s = String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s",
                    getString(R.string.left_parenthesis)
                    , s2, getString(R.string.add)
                    , s3, getString(R.string.i)
                    , getString(R.string.right_parenthesis)
                    , getString(R.string.e)
                    , getString(R.string.caret)
                    , getString(R.string.left_parenthesis)
                    , s1, getString(R.string.i)
                    , getString(R.string.t)
                    , getString(R.string.right_parenthesis));
            tv.setText(getString(R.string.tv, s));
            ll.addView(tv);
            epicyclesSequence.put(aEpicycle);
        });
        start_btn.setOnClickListener(v -> {
            startActivity(new Intent(this, EpicyclesTest.class));
            overridePendingTransition(R.anim.slide_in_bottom, 0);
        });

    }

    private void setTV(TextView tv, EpicyclesSequence.AEpicycle aEpicycle) {
        tv.setTextSize(20);
        tv.setOnLongClickListener(v1 -> {
            DialogUtil.createConfirmationAD(this, (dialog, which) -> {
                        ll.removeView(tv);
                        epicyclesSequence.epicycles.remove(aEpicycle);
                    }, (dialog, which) -> {
                    }, R.string.whether_to_delete, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    , false).show();
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 71) {
            for (EpicyclesSequence.AEpicycle epicycle : EpicyclesEdit.epicyclesSequence.epicycles) {
                String s = String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s",
                        getString(R.string.left_parenthesis)
                        , String.valueOf(epicycle.c.re), getString(R.string.add)
                        , String.valueOf(epicycle.c.im), getString(R.string.i)
                        , getString(R.string.right_parenthesis)
                        , getString(R.string.e)
                        , getString(R.string.caret)
                        , getString(R.string.left_parenthesis)
                        , String.valueOf(epicycle.n), getString(R.string.i)
                        , getString(R.string.t)
                        , getString(R.string.right_parenthesis));
                TextView tv = new TextView(this);
                tv.setText(s);
                setTV(tv, new EpicyclesSequence.AEpicycle(epicycle.n, epicycle.c));
                ll.addView(tv);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}