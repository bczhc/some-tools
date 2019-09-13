package pers.zhc.tools;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import pers.zhc.tools.clipboard.Clip;
import pers.zhc.tools.codecs.CodecsActivity;
import pers.zhc.tools.document.Document;
import pers.zhc.tools.floatingboard.JNI;
import pers.zhc.tools.functiondrawing.FunctionDrawingBoard;
import pers.zhc.tools.pi.Pi;
import pers.zhc.tools.toast.AToast;

import java.io.*;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File vFile = getVFile(this);
        String vS = "";
        try {
            InputStream is = new FileInputStream(vFile);
            InputStreamReader isr = new InputStreamReader(is, "GBK");
            BufferedReader br = new BufferedReader(isr);
            vS = br.readLine();
            br.close();
            isr.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        vS = vS == null ? "" : vS;
        System.out.println("vS = " + vS);
        int vI = new JNI().mG(this, vS);
//        System.out.println("vI = " + vI);
        if (vI != 0) {
            RelativeLayout rl = findViewById(R.id.v_rl);
            if (rl == null) setContentView(R.layout.v_f_activity);
            rl = findViewById(R.id.v_rl);
            rl.setOnLongClickListener(v -> {
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                EditText et = new EditText(this);
                et.setOnLongClickListener(v1 -> {
                    String s = et.getText().toString();
                    try {
                        OutputStream os = new FileOutputStream(vFile, false);
                        os.write(s.getBytes());
                        os.flush();
                        os.close();
                        onCreate(savedInstanceState);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                });
                adb.setView(et)
                        .show();
                return false;
            });
        } else init();
    }

    private void init() {
        LinearLayout ll = findViewById(R.id.ll);
        final int[] texts = new int[]{
                R.string.some_codecs,
                R.string.generate_pi,
                R.string.toast,
                R.string.put_in_clipboard,
                R.string.overlaid_drawing_board,
                R.string.fourier_series_calc,
                R.string.notes

        };
        final Class<?>[] classes = new Class[]{
                CodecsActivity.class,
                Pi.class,
                AToast.class,
                Clip.class,
                pers.zhc.tools.floatingboard.MainActivity.class,
                FunctionDrawingBoard.class,
                Document.class
        };
        new Thread(() -> {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            for (int i = 0; i < texts.length; i++) {
                Button btn = new Button(this);
                btn.setText(texts[i]);
//                btn.setTextSize(sp2px(this, 15F));
                btn.setTextSize(25F);
                btn.setAllCaps(false);
                btn.setLayoutParams(lp);
                int finalI = i;
                btn.setOnClickListener(v -> {
                    Intent intent = new Intent();
                    intent.setClass(this, classes[finalI]);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_bottom, 0);
                });
                CountDownLatch latch = new CountDownLatch(1);
                runOnUiThread(() -> {
                    ll.addView(btn);
                    latch.countDown();
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        /*Button btn1 = findViewById(R.id.gen_pi);
        btn1.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, Pi.class);
            startActivity(intent);
        });
        Button btn2 = findViewById(R.id.toast);
        btn2.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, AToast.class);
            startActivity(intent);
        });
        Button btn3 = findViewById(R.id.clipboard);
        btn3.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(this, Clip.class);
            startActivity(intent);
        });*/

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.fade_out);
    }
}