package com.zhc.tools.functiondrawing;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.zhc.tools.BaseActivity;
import com.zhc.tools.R;
import com.zhc.u.FourierSeries;
import com.zhc.utils.DisplayUtil;

public class FunctionDrawing extends BaseActivity {
    private RelativeLayout rl;
    private FourierSeries fs;
    private int nNum = 100;
    private TextView tv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.function_drawing_activity);
        rl = findViewById(R.id.rl_below);
        tv = findViewById(R.id.tv);
        init();
        tv.setOnClickListener(v -> {
            EditText et = new EditText(this);
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            AlertDialog ad = adb.setPositiveButton(R.string.ok, (dialog, which) -> {
                nNum = Integer.parseInt(et.getText().toString());
                init();
            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            }).setTitle(R.string.type_n_num).setView(et).create();
            ad.setCanceledOnTouchOutside(true);
            ad.show();
        });
    }

    private void init() {
        fs = new FourierSeries(20) {
            @Override
            public double f_f(double x) {
                /*if (x < 5) return 10;
                if (x >= 5 && x < 7.5F) return -2 * x + 20;
                if (x >= 7.5F && x < 17.5F) return x - 2.5;
                return -2 * x + 50;*/
                if (x < 10) return x;
                if (x >= 10 && x < 20) return -x + 20;
                return x - 20;
            }
        };
        fs.definite.n = 100000;
        new Thread(() -> {
            fs.initAB(nNum, s -> runOnUiThread(() -> tv.setText(s)));
            draw();
        }).start();
    }

    private void draw() {
        runOnUiThread(() -> rl.removeAllViews());
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        int width = point.x;
        int height = point.y - DisplayUtil.sp2px(this, 20);
        FunctionDrawingView functionDrawingView = new FunctionDrawingView(this, width, height);
        runOnUiThread(() -> rl.addView(functionDrawingView));
        functionDrawingView.drawFunction(x -> (float) fs.F(nNum, x), 4);
        runOnUiThread(() -> tv.setText(R.string.nul));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 检测屏幕的方向：纵向或横向
        if (this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            //当前为横屏， 在此处添加额外的处理代码
            draw();
        } else if (this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            //当前为竖屏， 在此处添加额外的处理代码
            draw();
        }
    }
}