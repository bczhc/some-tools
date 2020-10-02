package pers.zhc.tools.epicycles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.u.ComplexDefinite;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.ComplexValue;

import java.util.ArrayList;

/**
 * @author bczhc
 */
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
        Intent intent = getIntent();
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
                    int threadNum = intent.getIntExtra("thread_num", Runtime.getRuntime().availableProcessors());
                    ViewGroup.LayoutParams doubleWrapLP = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    TextView textView = new TextView(this);
                    textView.setTextSize(30F);
                    ll.addView(textView);
                    sv.addView(ll);
                    calcDialog.setContentView(sv, doubleWrapLP);
                    calcDialog.setCanceledOnTouchOutside(false);
                    calcDialog.setCancelable(false);
                    calcDialog.show();
                    int definite_n = intent.getIntExtra("definite_n", 10000);
                    int epicyclesCount = intent.getIntExtra("epicyclesCount", 100) - 1;//除去中间的0
                    EpicyclesView.setT(intent.getDoubleExtra("T", 2 * Math.PI));
                    ComplexDefinite complexDefinite = new ComplexDefinite();
                    complexDefinite.n = definite_n;
                    new Thread(() -> {
                        JNI.FourierSeries.calc(((ArrayList<ComplexValue>) ComplexGraphDrawingView.pointList), EpicyclesView.getT()
                                , epicyclesCount, (n, re, im) -> {
                                    System.out.println(n + " " + re + ' ' + im);
                                }, threadNum, 100000);
                        runOnUiThread(() -> {
                            calcDialog.dismiss();
                            finish();
                            overridePendingTransition(0, R.anim.fade_out);
                        });
                    }).start();
                }).setTitle(R.string.choose_calculate_or_exit).create();
        DialogUtil.setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT, false);
        alertDialog.show();
    }

    static class SynchronizedPut {
        private final EpicyclesSequence sequence;

        public SynchronizedPut(EpicyclesSequence sequence) {
            this.sequence = sequence;
        }

        synchronized void put(EpicyclesSequence.Epicycle o) {
            sequence.put(o);
        }
    }
}
