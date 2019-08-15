package com.zhc.tools.floatingboard;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
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
    private int TVsColor = Color.WHITE, textsColor = Color.GRAY;
    private boolean whetherTextsColorIsInverted_isChecked = false;
    private boolean notBeKilled = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floating_board_activity);
        //noinspection deprecation
        width = this.getWindowManager().getDefaultDisplay().getWidth();
        //noinspection deprecation
        height = this.getWindowManager().getDefaultDisplay().getHeight();
        Switch notBeKilledSwitch = findViewById(R.id.not_be_killed);
        notBeKilledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> notBeKilled = isChecked);
//        RelativeLayout rl = findViewById(R.id.main);
        pv = new PaintView(this, width, height);
        pv.setStrokeWidth(10);
        pv.setPaintColor(Color.RED);
        wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setBtn();
        Intent intent = getIntent();
        int a = intent.getIntExtra("a", 0);
        if (a == 1) {
            startFloatingWindow();
        }
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
                    stopFloatingWindow();
                    startFW.setText(R.string.stop_floating_window);
                    startFW.setOnClickListener(v1 -> {
                        stopFloatingWindow();
                        setBtn();
                    });
                    startFloatingWindow();
//                    moveTaskToBack(true);
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
        TextView[] childTVs = new TextView[strings.length];
        iv.setOnClickListener(v -> {
            System.out.println("click");
            ll.removeAllViews();
            for (int i = 0; i < strings.length; i++) {
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1F));
                childTVs[i] = new TextView(this);
                childTVs[i].setLayoutParams(childTV_lp);
                childTVs[i].setText(strings[i]);
                childTVs[i].setBackgroundColor(TVsColor);
                childTVs[i].setTextColor(textsColor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    childTVs[i].setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                } else childTVs[i].setTextSize(20F);
                int finalI = i;
                childTVs[i].setOnClickListener(v1 -> {
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
                                childTVs[finalI].setText(R.string.drawing);
                                strings[1] = getString(R.string.drawing);
                            } else {
                                lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                                wm.updateViewLayout(pv, lp);
                                childTVs[finalI].setText(R.string.controlling);
                                strings[1] = getString(R.string.controlling);
                            }
                            break;
                        case 2:
                            Dialog dialog = new Dialog(this);
                            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.color.transparent);
                            /*dialog.getWindow().setAttributes(new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                                    , WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 0, PixelFormat.RGBX_8888));*/
                            setDialogAttr(dialog);
                            HSVColorPickerRL hsvColorPickerRL = new HSVColorPickerRL(this, pv.getColor(), ((int) (width * .8)), ((int) (height * .4))) {
                                @Override
                                void onPickedAction(int color) {
                                    pv.setPaintColor(color);
                                }
                            };
                            setDialogAttr(dialog);
                            dialog.setContentView(hsvColorPickerRL);
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
                            if (pv.isEraserMode) {
                                pv.setEraserMode(false);
                                childTVs[finalI].setText(R.string.drawing_mode);
                                strings[6] = getString(R.string.drawing_mode);
                            } else {
                                pv.setEraserMode(true);
                                childTVs[finalI].setText(R.string.eraser_mode);
                                strings[6] = getString(R.string.eraser_mode);
                            }
                            break;
                        case 7:
                            Dialog d1 = new Dialog(this);

                            pv.clearAll();
                            break;
                        case 8:
                            hide();
                            break;
                        case 9:
                            Dialog c = new Dialog(this);
                            setDialogAttr(c);
                            LinearLayout cLL = new LinearLayout(this);
                            cLL.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            cLL.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout[] linearLayouts = new LinearLayout[]{
                                    new LinearLayout(this),
                                    new LinearLayout(this),
                                    new LinearLayout(this)
                            };
                            LinearLayout.LayoutParams cLLChildLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            Button TVsColorBtn = new Button(this);
                            TVsColorBtn.setText(R.string.control_panel_color);
                            TVsColorBtn.setOnClickListener(v2 -> {
                                Dialog TVsColorDialog = new Dialog(MainActivity.this);
                                setDialogAttr(TVsColorDialog);
                                HSVColorPickerRL TVsColorPicker = new HSVColorPickerRL(this, TVsColor, ((int) (width * .8)), ((int) (height * .4))) {
                                    @Override
                                    void onPickedAction(int color) {
                                        for (TextView childTV : childTVs) {
                                            TVsColor = color;
                                            childTV.setBackgroundColor(TVsColor);
                                            if (whetherTextsColorIsInverted_isChecked)
                                                childTV.setTextColor(textsColor = invertColor(TVsColor));
                                        }
                                    }
                                };
                                TVsColorDialog.setContentView(TVsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
                                TVsColorDialog.show();
                            });
                            Button textsColorBtn = new Button(this);
                            textsColorBtn.setOnClickListener(v2 -> {
                                Dialog textsColorDialog = new Dialog(MainActivity.this);
                                setDialogAttr(textsColorDialog);
                                HSVColorPickerRL textsColorPicker = new HSVColorPickerRL(this, textsColor, ((int) (width * .8)), ((int) (height * .4))) {
                                    @Override
                                    void onPickedAction(int color) {
                                        for (TextView childTV : childTVs) {
                                            if (!whetherTextsColorIsInverted_isChecked) textsColor = color;
                                            childTV.setTextColor(textsColor);
                                        }
                                    }
                                };
                                textsColorDialog.setContentView(textsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
                                textsColorDialog.show();
                            });
                            textsColorBtn.setText(R.string.text_color);
                            Switch whetherTextColorIsInverted = new Switch(this);
                            whetherTextColorIsInverted.setChecked(whetherTextsColorIsInverted_isChecked);
                            whetherTextColorIsInverted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                textsColorBtn.setEnabled(!isChecked);
                                whetherTextsColorIsInverted_isChecked = isChecked;
                                for (TextView childTV : childTVs) {
                                    childTV.setTextColor(textsColor = HSVColorPickerRL.invertColor(TVsColor));
                                }
                            });
                            whetherTextColorIsInverted.setText(R.string.whether_text_color_is_inverted);
                            whetherTextColorIsInverted.setLayoutParams(cLLChildLP);
                            TVsColorBtn.setLayoutParams(cLLChildLP);
                            textsColorBtn.setLayoutParams(cLLChildLP);
                            LinearLayout.LayoutParams LLsLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1F);
                            for (LinearLayout layout : linearLayouts) {
                                layout.setLayoutParams(LLsLP);
                                cLL.addView(layout);
                            }
                            linearLayouts[0].addView(TVsColorBtn);
                            linearLayouts[1].addView(textsColorBtn);
                            linearLayouts[2].addView(whetherTextColorIsInverted);
                            c.setContentView(cLL);
                            c.show();
                            break;
                        case 10:
                            stopFloatingWindow();
                            finish();
                            break;
                    }
                    System.out.println("i = " + finalI);
                });
                childTVs[i].setOnTouchListener(smallViewOnTouchListener);
                linearLayout.addView(childTVs[i]);
                ll.addView(linearLayout);
            }
        });
        iv.setOnTouchListener(smallViewOnTouchListener);
        ll.addView(iv);
        wm.addView(ll, lp2);
        if (notBeKilled) {
            View view = new View(this) {
                @Override
                public boolean onTouchEvent(MotionEvent event) {
                    System.out.println(event.getAction() + "\t" + event.getX() + "\t" + event.getY());
                    return true;
                }
            };
            view.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wm.addView(view, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGB_888));
            } else                 //noinspection deprecation
                wm.addView(view, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGB_888));
        }
    }

    private void hide() {
        wm.removeViewImmediate(ll);
        NotificationManager nm = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel("channel1", "隐藏通知", NotificationManager.IMPORTANCE_DEFAULT);
            nc.setDescription("隐藏通知");
            nc.canBypassDnd();
            nc.setBypassDnd(true);
            nc.enableLights(false);
            nc.enableLights(false);
            nm.createNotificationChannel(nc);
            Notification.Builder nb = new Notification.Builder(this, "channel1");
            nb.setSmallIcon(Icon.createWithBitmap(icon))
                    .setContentTitle("画板")
                    .setContentText("点击取消隐藏控制悬浮窗");
//            PendingIntent pi = PendingIntent.getBroadcast(this, 1, new Intent(this, NotificationClickReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
            Intent intent = new Intent(this, this.getClass());
            intent.putExtra("a", 1);
            PendingIntent pi = PendingIntent.getActivity(this, 11, intent, PendingIntent.FLAG_CANCEL_CURRENT);
//            nb.setContentIntent(pi);
            nb.setContentIntent(pi);
            Notification build = nb.build();
            nm.notify(1, build);
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
                double a = Math.log(edit) / Math.log(1.07D);
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
//        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.color.transparent);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public class NotificationClickReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            startFloatingWindow();
            Toast.makeText(MainActivity.this, "a", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setDialogAttr(Dialog d) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Objects.requireNonNull(d.getWindow()).setAttributes(new WindowManager.LayoutParams(((int) (width * .8)), ((int) (height * .4)), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, 0, PixelFormat.RGB_888));
        } else                                 //noinspection deprecation
            Objects.requireNonNull(d.getWindow()).setAttributes(new WindowManager.LayoutParams(((int) (width * .8)), ((int) (height * .4)), WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, 0, PixelFormat.RGB_888));
    }
}