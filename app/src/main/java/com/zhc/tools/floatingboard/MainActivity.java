package com.zhc.tools.floatingboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.zhc.tools.R;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private WindowManager wm = null;
    private LinearLayout ll;
    private PaintView pv;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floating_board_activity);
//        RelativeLayout rl = findViewById(R.id.main);

        pv = new PaintView(this);
        Button colorPickerBtn = findViewById(R.id.color_picker_button);
        colorPickerBtn.setOnClickListener(v -> {
            ColorPickerDialog dialog = new ColorPickerDialog(this, Color.RED, "Color picker", color -> pv.color = color);
            dialog.show();
        });
        wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setBtn(colorPickerBtn.getHeight());

    }

    private void setBtn(int colorPickerBtnHeight) {
        Button startFW = findViewById(R.id.start_f_w);
        startFW.setHeight(colorPickerBtnHeight);
        startFW.setText(R.string.start_floating_window);
        startFW.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 4444);
                } else {
                    startFW.setText(R.string.stop_floating_window);
                    startFW.setOnClickListener(v1 -> {
                        stopFloatingWindow();
                        setBtn(colorPickerBtnHeight);
                    });
                    startFloatingWindow();
                    moveTaskToBack(true);
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void startFloatingWindow() {
        pv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        WindowManager.LayoutParams lp2 = new WindowManager.LayoutParams();
        //noinspection deprecation
        int width = this.getWindowManager().getDefaultDisplay().getWidth();
        //noinspection deprecation
        int height = this.getWindowManager().getDefaultDisplay().getHeight();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            lp2.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            //noinspection deprecation
            lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            //noinspection deprecation
            lp2.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        lp.format = PixelFormat.RGBA_8888;
        lp2.format = PixelFormat.RGBA_8888;

//        btn.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//        PaintView paintView = new PaintView(this);
//        wm.addView(paintView, lp);
        String[] strings = getResources().getStringArray(R.array.btn_string);
        lp.width = width;
        lp.height = height;
        wm.addView(pv, lp);
        lp2.width = 80;
        lp2.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        ll = new LinearLayout(this);
        LinearLayout.LayoutParams ll_lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(ll_lp);
//        Button btn = new Button(this);
        ImageView iv = new ImageView(this);
        InputStream inputStream = getResources().openRawResource(R.raw.db);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
        iv.setImageBitmap(bitmap);
        ll.addView(iv);
        iv.setOnClickListener(v -> {
            System.out.println("click");
            ll.removeAllViews();
            for (int i = 0; i < 3; i++) {
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1F));
                Button childBtn = new Button(this);
                childBtn.setLayoutParams(ll_lp);
                childBtn.setText(strings[i]);
                int finalI = i;
                childBtn.setOnClickListener(v1 -> {
//                    Toast.makeText(this, "click: " + finalI, Toast.LENGTH_SHORT).show();
                    switch (finalI) {
                        case 0:
                            ll.removeAllViews();
                            ll.addView(iv);
                            wm.updateViewLayout(ll, lp2);
                            break;
                        case 1:
                            if (lp.flags == WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) {
                                lp.flags = 0;
                                wm.updateViewLayout(pv, lp);
                            } else {
                                lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                                wm.updateViewLayout(pv, lp);
                                /*ll.removeAllViews();
                                ll.addView(iv);
                                lp2.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                                wm.updateViewLayout(ll, lp2);*/
                            }
                            break;
                        case 2:
                            stopFloatingWindow();
                            finish();
                            break;
                    }
                });
                linearLayout.addView(childBtn);
                ll.addView(linearLayout);
            }
        });
        iv.setOnTouchListener(new View.OnTouchListener() {
            private int lastRawX, lastRawY, paramX, paramY;
            private float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
            /*int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    System.out.println("event.getX() = " + event.getX());
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            return true;*/
                float x = event.getX();
                float y = event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastRawX = (int) event.getRawX();
                        lastRawY = (int) event.getRawY();
                        lastX = event.getX();
                        lastY = event.getY();
                        paramX = lp2.x;
                        paramY = lp2.y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastRawX;
                        int dy = (int) event.getRawY() - lastRawY;
                        lp2.x = paramX + dx;
                        lp2.y = paramY + dy;
                        // 更新悬浮窗位置
                        wm.updateViewLayout(ll, lp2);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (lastX == x && lastY == y) v.performClick();
                        break;
                }
                return true;
            }
        });
        wm.addView(ll, lp2);
    }

    private void stopFloatingWindow() {
        try {
            wm.removeViewImmediate(ll);
        } catch (Exception ignored) {
        }
        try {
            wm.removeViewImmediate(pv);
        } catch (Exception ignored) {
        }
    }
}