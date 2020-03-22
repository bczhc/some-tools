package pers.zhc.tools.epicycles;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.Random;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.ComplexValue;

public class EpicyclesEdit extends BaseActivity {

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
        EditText threadNum = findViewById(R.id.thread_num);
        drawGraphBtn.setOnClickListener(v -> {
            try {
                ll.removeAllViews();
                epicyclesSequence.epicycles.clear();
                String s1 = definite_n.getText().toString();
                String s2 = T.getText().toString();
                String s3 = epicycles_count.getText().toString();
                String s4 = threadNum.getText().toString();
                ck(s1, s2, s3, s4, definite_n, T, epicycles_count, threadNum);
                s1 = definite_n.getText().toString();
                s2 = T.getText().toString();
                s3 = epicycles_count.getText().toString();
                s4 = threadNum.getText().toString();
                Intent intent = new Intent(this, ComplexGraphDrawing.class);
                intent.putExtra("definite_n", Integer.parseInt(s1));
                intent.putExtra("T", Double.parseDouble(s2));
                intent.putExtra("epicycles_count", Integer.parseInt(s3));
                intent.putExtra("thread_num", Integer.parseInt(s4));
                startActivityForResult(intent, RequestCode.START_ACTIVITY);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                ToastUtils.show(this, R.string.please_type_correct_value);
            }
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
        ll = findViewById(R.id.sc_ll);
        btn.setOnClickListener(v -> {
            try {
                String s1 = et_n.getText().toString();
                s1 = "".equals(s1) ? "0" : s1;
                String s2 = et_c_re.getText().toString();
                s2 = "".equals(s2) ? "0" : s2;
                String s3 = et_c_im.getText().toString();
                s3 = "".equals(s3) ? "0" : s3;
                EpicyclesSequence.AEpicycle aEpicycle = new EpicyclesSequence.AEpicycle(Double.parseDouble(s1)
                        , new ComplexValue(Double.parseDouble(s2)
                        , Double.parseDouble(s3)));
                TextView tv = new TextView(this);
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
            } catch (NumberFormatException e) {
                e.printStackTrace();
                ToastUtils.show(this, R.string.please_type_correct_value);
            }
        });
        start_btn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, EpicyclesTest.class);
                String s = T.getText().toString();
                if ("".equals(s)) {
                    T.setText(getString(R.string.tv, "50"));
                }
                s = T.getText().toString();
                EpicyclesView.setT(Double.parseDouble(s));
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_bottom, 0);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                ToastUtils.show(this, R.string.please_type_correct_value);
            }
        });
        Button sortBtn = findViewById(R.id.sort);
        sortBtn.setOnClickListener(v -> {
            LinearLayout dialog_ll = new LinearLayout(this);
            dialog_ll.setOrientation(LinearLayout.VERTICAL);
            Dialog dialog = new Dialog(this);
            DialogUtil.setDialogAttr(dialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    , false);
            dialog_ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            dialog.setContentView(dialog_ll, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            int[] strRes = new int[]{
                    R.string.vector_module_ascending_order,
                    R.string.vector_module_descending_order,
                    R.string.velocity_ascending_order,
                    R.string.velocity_descending_order,
                    R.string.random_order
            };
            View.OnClickListener[] onClickListeners = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                onClickListeners = new View.OnClickListener[]{
                        v0 -> {
                            EpicyclesEdit.epicyclesSequence.epicycles.sort((o1, o2) -> {
                                double c = o1.c.getComplexModule() - o2.c.getComplexModule();
                                return c < 0 ? -1 : (c == 0 ? 0 : 1);
                            });
                            reListEpicycles();
                            dialog.dismiss();
                        },
                        v1 -> {
                            EpicyclesEdit.epicyclesSequence.epicycles.sort((o1, o2) -> {
                                double c = -o1.c.getComplexModule() + o2.c.getComplexModule();
                                return c < 0 ? -1 : (c == 0 ? 0 : 1);
                            });
                            reListEpicycles();
                            dialog.dismiss();
                        },
                        v2 -> {
                            EpicyclesEdit.epicyclesSequence.epicycles.sort((o1, o2) -> {
                                if (Math.abs(o1.n) == Math.abs(o2.n)) {
                                    return 0;
                                }
                                return Math.abs(o1.n) < Math.abs(o2.n) ? -1 : 1;
                            });
                            reListEpicycles();
                            dialog.dismiss();
                        },
                        v3 -> {
                            EpicyclesEdit.epicyclesSequence.epicycles.sort((o1, o2) -> {
                                if (Math.abs(o1.n) == Math.abs(o2.n)) {
                                    return 0;
                                }
                                return Math.abs(o1.n) < Math.abs(o2.n) ? 1 : -1;
                            });
                            reListEpicycles();
                            dialog.dismiss();
                        },
                        v4 -> {
                            //noinspection ComparatorMethodParameterNotUsed
                            EpicyclesEdit.epicyclesSequence.epicycles.sort((o1, o2) -> Random.ran_sc(-100, 100));
                            reListEpicycles();
                            dialog.dismiss();
                        }
                };
            }
            Button[] optionBtns = new Button[strRes.length];
            if (onClickListeners != null) {
                for (int i = 0; i < optionBtns.length; i++) {
                    optionBtns[i] = new Button(this);
                    optionBtns[i].setText(strRes[i]);
                    optionBtns[i].setOnClickListener(onClickListeners[i]);
                    dialog_ll.addView(optionBtns[i]);
                }
            }
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        });
    }

    private void ck(String s1, String s2, String s3, String s4, EditText definite_n, EditText T, EditText epicycles_count, EditText threadNum) {
        if ("".equals(s1)) {
            definite_n.setText(getString(R.string.tv, "10000"));
        }
        if ("".equals(s2)) {
            T.setText(getString(R.string.tv, "50"));
        }
        if ("".equals(s3)) {
            epicycles_count.setText(getString(R.string.tv, "150"));
        }
        if ("".equals(s4)) {
            threadNum.setText(getString(R.string.tv, String.valueOf(Runtime.getRuntime().availableProcessors())));
        }
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
        if (requestCode == RequestCode.START_ACTIVITY) {
            reListEpicycles();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void reListEpicycles() {
        this.ll.removeAllViews();
        for (EpicyclesSequence.AEpicycle epicycle : EpicyclesEdit.epicyclesSequence.epicycles) {
            String s = String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s",
                    getString(R.string.left_parenthesis)
                    , epicycle.c.re, getString(R.string.add)
                    , epicycle.c.im, getString(R.string.i)
                    , getString(R.string.right_parenthesis)
                    , getString(R.string.e)
                    , getString(R.string.caret)
                    , getString(R.string.left_parenthesis)
                    , epicycle.n, getString(R.string.i)
                    , getString(R.string.t)
                    , getString(R.string.right_parenthesis));
            TextView tv = new TextView(this);
            tv.setText(s);
            setTV(tv, new EpicyclesSequence.AEpicycle(epicycle.n, epicycle.c));
            ll.addView(tv);
        }
    }
}