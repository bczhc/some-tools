package pers.zhc.tools.functiondrawing;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.u.FourierSeries;

public class FunctionDrawing extends BaseActivity {
    private FourierSeries fs;
    private int nNum = 10;
    private TextView tv;
    private FunctionDrawerView functionDrawingView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.function_drawing_activity);
        RelativeLayout rl = findViewById(R.id.rl_below);
        tv = findViewById(R.id.tv);
        init();
        tv.setOnClickListener(v -> {
            EditText et = new EditText(this);
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            AlertDialog ad = adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                nNum = Integer.parseInt(et.getText().toString());
                init();
            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            }).setTitle(R.string.type_n_num).setView(et).create();
            ad.setCanceledOnTouchOutside(true);
            ad.show();
        });
        rl.addView(functionDrawingView);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {
        Intent intent = getIntent();
        int xLen = intent.getIntExtra("xLen", 30);
        int yLen = intent.getIntExtra("yLen", 30);
        fs = new FourierSeries(intent.getIntExtra("FS_T", 30)) {
            @Override
            public double f_f(double x) {
                return FunctionDrawingBoard.functionInterface.f(((float) x));
            }
        };
        fs.definite.n = 20000;
        new Thread(() -> {
            fs.initAB(nNum, s -> {
                runOnUiThread(() -> tv.setText(s));
                draw();
            }, Runtime.getRuntime().availableProcessors());
            runOnUiThread(() -> tv.setText(R.string.nul));
        }).start();
        functionDrawingView = new FunctionDrawerView(this, xLen, yLen);
        /*TextView[] textViews = new TextView[]{
                findViewById(R.id.increase_tv),
                findViewById(R.id.decrease_tv)
        };
        textViews[0].setOnClickListener(v -> {
            if (functionDrawingView.xLength > 0 && functionDrawingView.yLength > 0)
                functionDrawingView.zoom(functionDrawingView.xLength - 5, functionDrawingView.yLength - 5);
        });
        for (TextView textView : textViews) {
            textView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setBackgroundColor(Color.parseColor("#FD9A44"));
                        break;
                    case MotionEvent.ACTION_UP:
                        v.setBackgroundColor(Color.WHITE);
                        v.performClick();
                        break;
                }
                return true;
            });
        }
        textViews[1].setOnClickListener(v -> functionDrawingView.zoom(functionDrawingView.xLength + 5, functionDrawingView.yLength + 5));*/
    }

    private void draw() {
        functionDrawingView.drawFunction(x -> fs.F(nNum, x));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 检测屏幕的
        // 方向：纵向或横向
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}