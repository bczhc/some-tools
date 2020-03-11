package pers.zhc.tools.test.viewtest;

import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;

public class MainActivity extends BaseActivity {

    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View inflate = View.inflate(this, R.layout.progress_bar, null);
        setContentView(inflate);
        ProgressBar pb = findViewById(R.id.progress_bar);
        Handler handler = new Handler();
        int[] i = new int[1];
        Runnable r = () -> pb.setProgress((int) (i[0] / 10000F * 100F));
        thread = new Thread(() -> {
            for (int j = 0; j < 10000; j++) {
                i[0] = j;
                if (j % 1000 == 0) {
                    handler.post(r);
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        thread.start();
        return true;
    }
}