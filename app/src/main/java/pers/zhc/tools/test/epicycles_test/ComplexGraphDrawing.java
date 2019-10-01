package pers.zhc.tools.test.epicycles_test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.u.ComplexDefinite;
import pers.zhc.u.ThreadSequence;
import pers.zhc.u.math.util.ComplexFunctionInterface;
import pers.zhc.u.math.util.ComplexValue;

public class ComplexGraphDrawing extends BaseActivity {

    private int width;
    private int height;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComplexGraphDrawingView complexGraphDrawingView = new ComplexGraphDrawingView(this);
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        width = point.x;
        height = point.y;
        setContentView(complexGraphDrawingView);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        AlertDialog alertDialog = adb.setPositiveButton(R.string.exit, (dialog, which) -> {
            dialog.dismiss();
            super.onBackPressed();
        })
                .setNegativeButton(R.string.calc, (dialog, which) -> {
                    Dialog calcDialog = new Dialog(this);
                    DialogUtil.setDialogAttr(calcDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
                    ScrollView sv = new ScrollView(this);
                    sv.setLayoutParams(new ViewGroup.LayoutParams(((int) (((float) width) * .8F)), ((int) (((float) height) * .8F))));
                    LinearLayout ll = new LinearLayout(this);
                    ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    ll.setOrientation(LinearLayout.VERTICAL);
                    int threadNum = Runtime.getRuntime().availableProcessors();
                    ViewGroup.LayoutParams doubleWrap = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    TextView[] textViews = new TextView[threadNum];
                    for (int i = 0; i < threadNum; i++) {
                        textViews[i] = new TextView(this);
                        textViews[i].setTextSize(textViews[i].getTextSize() * 1.2F);
                        textViews[i].setLayoutParams(doubleWrap);
                        ll.addView(textViews[i]);
                    }
                    sv.addView(ll);
                    calcDialog.setContentView(sv, doubleWrap);
                    calcDialog.setCanceledOnTouchOutside(false);
                    calcDialog.setCancelable(false);
                    calcDialog.show();
                    ComplexFunctionInterface function = ComplexGraphDrawingView.complexFunction.getFunction(0, EpicyclesView.T);
                    System.out.println("function = " + function);
                    Intent intent = getIntent();
                    int definite_n = intent.getIntExtra("definite_n", 10000);
                    int epicycles_count = intent.getIntExtra("epicycles_count", 100);
                    EpicyclesView.T = intent.getDoubleExtra("T", 2 * Math.PI);
                    new Thread(() -> {
                        ThreadSequence threadSequence = new ThreadSequence(threadNum);
                        ComplexDefinite complexDefinite = new ComplexDefinite();
                        complexDefinite.n = definite_n;
                        ComplexValue complexValue = new ComplexValue(EpicyclesView.T, 0);
                        ComplexValue complexValue1 = new ComplexValue(0, 0);
                        for (int n = -epicycles_count / 2; n < -epicycles_count / 2 + epicycles_count; n++) {
                            int finalN = n;
                            threadSequence.execute((j, ro) -> {
                                ComplexValue definiteIntegralByTrapezium = complexDefinite.getDefiniteIntegralByTrapezium(0, EpicyclesView.T,
                                        t -> function.x(t).selfMultiply(complexValue1.setValue(Math.cos(-finalN * t * EpicyclesView.omega), Math.sin(-finalN * t * EpicyclesView.omega))));
                                EpicyclesEdit.epicyclesSequence.put(finalN, definiteIntegralByTrapezium.selfDivide(complexValue));
                                runOnUiThread(() -> textViews[j % threadNum].setText(getString(R.string.progress_tv, ((float) j) / ((float) epicycles_count) * 100F)));
                            });
                        }
                        threadSequence.start(() -> {
                            runOnUiThread(() -> {
                                calcDialog.dismiss();
                                super.onBackPressed();
                            });
                            overridePendingTransition(0, R.anim.fade_out);
                        });
                    }).start();
                }).setTitle(R.string.choose_calculate_or_exit)
                .create();
        DialogUtil.setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT, false);
        alertDialog.show();
    }
}