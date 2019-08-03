package com.zhc.tools.pi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.zhc.codecs.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pi extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pi_activity);
        PiJNI piJNI = new PiJNI();
        Button btn = findViewById(R.id.gen_pi);
        EditText et = findViewById(R.id.pi_et);
        piJNI.o = findViewById(R.id.pi_out_et);
        final ExecutorService[] es = {Executors.newFixedThreadPool(1)};
        TextView tv = findViewById(R.id.waitTV);
        btn.setOnClickListener(v -> {
            try {
                String s = et.getText().toString();
                int i = Integer.parseInt(s.equals("") ? "0" : s);
                if (!es[0].isShutdown()) {
                    es[0].shutdownNow();
                }
                es[0] = Executors.newFixedThreadPool(1);
                es[0].execute(() -> {
                    piJNI.sb = new StringBuilder();
                    runOnUiThread(() -> tv.setText(R.string.wait));
                    piJNI.gen(i);
                    runOnUiThread(() -> {
                        tv.setText(R.string.nul);
                        piJNI.o.setText(piJNI.sb.toString());
                    });
                });
                es[0].shutdown();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                ClipData c = ClipData.newHtmlText("", "text", "<p style='color: red;'>ha3</p>");
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(c);
                Toast.makeText(this, "复制成功", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }
}