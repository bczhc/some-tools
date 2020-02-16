package pers.zhc.tools.pi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.ToastUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pi extends BaseActivity {
    private boolean isGenerating = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pi_activity);
        PiJNI piJNI = new PiJNI();
        Button btn = findViewById(R.id.gen_pi);
        EditText et = findViewById(R.id.pi_et);
        TextView timeTV = findViewById(R.id.time_tv);
        piJNI.o = findViewById(R.id.pi_out_et);
        final ExecutorService[] es = {Executors.newFixedThreadPool(1)};
        TextView tv = findViewById(R.id.waitTV);
        btn.setOnClickListener(v -> {
            try {
                String s = et.getText().toString();
                int i = Integer.parseInt(s.equals("") ? "0" : s);
                try {
                    es[0].shutdownNow();
                } catch (Exception ignored) {
                }
                es[0] = Executors.newFixedThreadPool(1);
                es[0].execute(() -> {
                    piJNI.sb = new StringBuilder();
                    runOnUiThread(() -> tv.setText(R.string.please_wait));
                    this.isGenerating = true;
                    long sM = System.currentTimeMillis();
                    piJNI.gen(i);
                    long eM = System.currentTimeMillis();
                    runOnUiThread(() -> {
                        tv.setText(R.string.nul);
                        this.isGenerating = false;
                        piJNI.o.setText(String.format(this.getResources().getString(R.string.piTV), piJNI.sb.toString()));
//                        System.out.println("PiJNI.ints = " + Arrays.toString(PiJNI.ints));
                        timeTV.setText(String.format(getResources().getString(R.string.tv_millis), (eM - sM)));
                    });
                });
                es[0].shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtils.show(this, e.toString());
            }
            /*CountDownLatch[] latches = new CountDownLatch[]{
                    new CountDownLatch(1),
                    new CountDownLatch(1)
            };
            es[0].shutdownNow();
            es[0] = Executors.newFixedThreadPool(1);
            es[0].execute(() -> {
                final String[] s = new String[1];
                runOnUiThread(() -> {
                    piJNI.o.setText(R.string.nul);
                    piJNI.sb = new StringBuilder();
                    s[0] = et.getText().toString();
                    latches[0].countDown();
                });
                try {
                    latches[0].await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int i = Integer.parseInt(s[0].equals("") ? "0" : s[0]);
                piJNI.gen(i);
                latches[1].countDown();
            });
            try {
                latches[1].await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            piJNI.o.setText(piJNI.sb.toString());*/
        });
        btn.setOnLongClickListener(v -> {
            ClipboardManager cm = ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE));
            ClipData cd = ClipData.newPlainText("Pi", piJNI.sb.toString());
            if (cm != null) {
                cm.setPrimaryClip(cd);
            } else ToastUtils.show(this, "null");
            ToastUtils.show(this, R.string.copying_success);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (this.isGenerating) this.moveTaskToBack(true);
//        else super.onBackPressed();
        else finish();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }
}