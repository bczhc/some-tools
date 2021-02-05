package pers.zhc.tools.epicycles;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.u.ComplexDefinite;
import pers.zhc.u.math.util.ComplexValue;

/**
 * @author bczhc
 */
public class ComplexGraphDrawing extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComplexGraphDrawingView complexGraphDrawingView = new ComplexGraphDrawingView(this);
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
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
                    TextView textView = new TextView(this);
                    textView.setTextSize(30F);

                    Dialog progressDialog = new Dialog(this);
                    DialogUtil.setDialogAttr(progressDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
                    progressDialog.setContentView(textView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    int threadNum = intent.getIntExtra("thread_num", Runtime.getRuntime().availableProcessors());
                    int integralN = intent.getIntExtra("integralN", 10000);
                    int epicyclesCount = intent.getIntExtra("epicyclesCount", 100);
                    EpicyclesView.setT(intent.getDoubleExtra("T", 2 * Math.PI));
                    Log.d(TAG, String.format("Fourier series calculation info:" +
                                    "integralN: %d; period: %f; epicycles count: %d; thread number: %d",
                            integralN, EpicyclesView.getT(), epicyclesCount, threadNum));
                    ComplexDefinite complexDefinite = new ComplexDefinite();
                    complexDefinite.n = integralN;
                    Object lock = new Object();
                    new Thread(() -> {
                        final int[] c = {0};
                        JNI.FourierSeries.calc(((ArrayList<ComplexValue>) ComplexGraphDrawingView.pointList), EpicyclesView.getT()
                                , epicyclesCount, (n, re, im) -> {
                                    synchronized (lock) {
                                        EpicyclesEdit.epicyclesSequence.put(n, re, im);
                                        Log.d(TAG, n + " " + re + ' ' + im);
                                        runOnUiThread(() -> textView.setText(getString(R.string.progress_tv, ((float) ++c[0]) / ((float) epicyclesCount) * 100)));
                                    }
                                }, threadNum, integralN);
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            finish();
                            overridePendingTransition(0, R.anim.fade_out);
                        });
                    }).start();
                }).setTitle(R.string.choose_calculate_or_exit).create();
        DialogUtil.setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT, false);
        alertDialog.show();
    }
}
