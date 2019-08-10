package com.zhc.tools.floatingboard;

import android.annotation.SuppressLint;
import android.app.*;
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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.zhc.tools.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private WindowManager wm = null;
    private LinearLayout ll;
    private PaintView pv;
    private int width;
    @SuppressWarnings("FieldCanBeLocal")
    private Bitmap icon;
    private int height;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floating_board_activity);
//        RelativeLayout rl = findViewById(R.id.main);
        //noinspection deprecation
        width = this.getWindowManager().getDefaultDisplay().getWidth();
        //noinspection deprecation
        height = this.getWindowManager().getDefaultDisplay().getHeight();
        pv = new PaintView(this, width, height);
        wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setBtn();

    }


    private void setBtn() {
        Button startFW = findViewById(R.id.start_f_w);
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
                        setBtn();
                    });
                    startFloatingWindow();
                }
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void startFloatingWindow() {
        pv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        WindowManager.LayoutParams lp2 = new WindowManager.LayoutParams();
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
        String[] strings = getResources().getStringArray(R.array.btn_string);
        lp.width = width;
        lp.height = height;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wm.addView(pv, lp);
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp2.width = /*(int) (width * proportionX)*/WindowManager.LayoutParams.WRAP_CONTENT;
        lp2.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE/* | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL*/;
        ll = new LinearLayout(this);
        LinearLayout.LayoutParams ll_lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams childTV_lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, /*ViewGroup.LayoutParams.WRAP_CONTENT*/(int) (height / strings.length * .7));
        childTV_lp.setMargins(0, 0, 0, 5);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(ll_lp);
        ImageView iv = new ImageView(this);
        InputStream inputStream = getResources().openRawResource(R.raw.db);
        icon = BitmapFactory.decodeStream(inputStream);
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
        iv.setImageBitmap(icon);
        float proportionX = ((float) 75) / ((float) 720);
        float proportionY = ((float) 75) / ((float) 1360);
        iv.setLayoutParams(new ViewGroup.LayoutParams((int) (width * proportionX), (int) (height * proportionY)));
        View.OnTouchListener smallViewOnTouchListener = new View.OnTouchListener() {
            private int lastRawX, lastRawY, paramX, paramY;
            private float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
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
                        if (Math.abs(lastX - x) < 1 && Math.abs(lastY - y) < 1) v.performClick();
                        break;
                }
                return true;
            }
        };
        iv.setOnClickListener(v -> {
            System.out.println("click");
            ll.removeAllViews();
            for (int i = 0; i < strings.length; i++) {
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1F));
                TextView childTV = new TextView(this);
                childTV.setLayoutParams(childTV_lp);
                childTV.setText(strings[i]);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    childTV.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                } else childTV.setTextSize(20F);
                childTV.setBackgroundColor(Color.WHITE);
                int finalI = i;
                childTV.setOnClickListener(v1 -> {
//                    Toast.makeText(this, "click: " + finalI, Toast.LENGTH_SHORT).show();
                    switch (finalI) {
                        case 0:
                            ll.removeAllViews();
                            ll.addView(iv);
                            wm.updateViewLayout(ll, lp2);
                            break;
                        case 1:
                            if (lp.flags == (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)) {
                                lp.flags = 0;
                                wm.updateViewLayout(pv, lp);
                                childTV.setText(R.string.drawing);
                            } else {
                                lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                                wm.updateViewLayout(pv, lp);
                                childTV.setText(R.string.controlling);
                            }
                            break;
                        case 2:
                            Dialog dialog = new Dialog(this);

                            ColorPickerRL dialog_rl = new ColorPickerRL(this, ((int) (width * .6)), ((int) (height * .6)), pv.getColor()) {
                                @Override
                                void onPickedAction(int color) {
//                                    super.onPickedAction(pickedColor);
                                }

                                @Override
                                void onDoneBtnPressed(int color) {
//                                    super.onDoneBtnPressed();
                                    dialog.dismiss();
//                                    Toast.makeText(MainActivity.this, String.valueOf(color), Toast.LENGTH_SHORT).show();
                                    pv.setPaintColor(color);
                                }
                            };
                            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.color.transparent);
                            dialog.setContentView(dialog_rl);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Objects.requireNonNull(dialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                            } else //noinspection deprecation
                                Objects.requireNonNull(dialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                            dialog.show();
                            break;
                        case 3:
                            changeStrokeWidth();
                            break;
                        case 4:
                            pv.undo();
                            break;
                        case 5:
                            pv.redo();
                            break;
                        case 6:
                            if (pv.isEraserModel) {
                                pv.setEraserModel(false);
                                childTV.setText(R.string.drawing_mode);
                            } else {
                                pv.setEraserModel(true);
                                childTV.setText(R.string.eraser_mode);
                            }
                            break;
                        case 7:
                            pv.clearAll();
                            break;
                        case 8:
                            hide();
                            break;
                        case 9:
                            stopFloatingWindow();
                            finish();
                            break;
                    }
                    System.out.println("i = " + finalI);
                });
                childTV.setOnTouchListener(smallViewOnTouchListener);
                linearLayout.addView(childTV);
                ll.addView(linearLayout);
            }
        });
        iv.setOnTouchListener(smallViewOnTouchListener);
        ll.addView(iv);
        wm.addView(ll, lp2);
    }

    private void hide() {
        wm.removeViewImmediate(ll);
        NotificationManager nm = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder nb = new Notification.Builder(this, "channel1");
            nb.setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("画板")
                    .setContentText("点击取消隐藏控制悬浮窗")
                    .setOngoing(false)
                    .setAutoCancel(true);
            nm.notify(1, nb.build());
        } else {
            NotificationCompat.Builder ncb = new NotificationCompat.Builder(this, "channel1");
            ncb.setOngoing(false)
                    .setAutoCancel(true)
                    .setContentTitle("画板")
                    .setContentText("点击取消隐藏控制悬浮窗")
                    .setSmallIcon(R.mipmap.ic_launcher);
            nm.notify(1, ncb.build());
        }
    }

    private void changeStrokeWidth() {
        LinearLayout ll = new LinearLayout(this);
        TextView widthWatchView = new TextView(this);
        SeekBar sb = new SeekBar(this);
        widthWatchView.setBackgroundColor(pv.getColor());
        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setTextSize(20F);
        tv.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            adb.setPositiveButton("确定", (dialog, which) -> {
                double edit = Double.parseDouble(et.getText().toString());
                double a = Math.log(edit) / Math.log(1.07d);
                widthWatchView.setHeight((int) edit);
                sb.setProgress((int) a);
            }).setNegativeButton("取消", (dialog, which) -> {
            }).setTitle("输入画笔宽度(pixels)").setView(et);
            AlertDialog ad = adb.create();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            } else //noinspection deprecation
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            ad.show();
        });
        Dialog dialog = new Dialog(this);
        sb.setProgress(((int) (Math.log((double) pv.getStrokeWidth()) / Math.log(1.07D))));
        widthWatchView.setHeight((int) Math.pow(1.07D, (double) sb.getProgress()));
        tv.setText(String.valueOf((int) Math.pow(1.07D, (double) sb.getProgress())));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                System.out.println("progress = " + progress);
                double pow = Math.pow(1.07D, ((double) progress));
                pv.setStrokeWidth((float) ((int) pow));
                widthWatchView.setHeight((int) pow);
                tv.setText(String.valueOf((int) pow));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sb.setMax(100);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(tv);
        ll.addView(sb);
        ll.addView(widthWatchView);
        ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setContentView(ll, new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        dialog.setTitle("change stroke width");
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.color.transparent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Objects.requireNonNull(dialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else //noinspection deprecation
            Objects.requireNonNull(dialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();

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