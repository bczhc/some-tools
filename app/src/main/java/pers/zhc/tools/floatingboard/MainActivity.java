package pers.zhc.tools.floatingboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.*;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.view.*;
import android.widget.*;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.FilePickerRL;
import pers.zhc.tools.utils.PermissionRequester;
import pers.zhc.u.FileU;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static pers.zhc.tools.utils.ColorUtils.invertColor;
import static pers.zhc.tools.utils.DialogUtil.createConfirmationAD;
import static pers.zhc.tools.utils.DialogUtil.setDialogAttr;

public class MainActivity extends BaseActivity {
    private WindowManager wm = null;
    private LinearLayout ll;
    private PaintView pv;
    private int width;
    private Bitmap icon;
    private int height;
    private int TVsColor = Color.WHITE, textsColor = Color.GRAY;
    private boolean whetherTextsColorIsInverted_isChecked = false;
    private View globalOnTouchListenerFloatingView;
    private NotificationClickReceiver notificationClickReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floating_board_activity);
        init();
    }

    private void init() {
        notificationClickReceiver = new NotificationClickReceiver();
        Point point = new Point();
        /*//noinspection deprecation
        width = this.getWindowManager().getDefaultDisplay().getWidth();
        //noinspection deprecation
        height = this.getWindowManager().getDefaultDisplay().getHeight();*/
        getWindowManager().getDefaultDisplay().getSize(point);
        width = point.x;
        height = point.y;
        Switch notBeKilledSwitch = findViewById(R.id.not_be_killed);
        globalOnTouchListenerFloatingView = new View(this) {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                System.out.println("event.getAction() = " + event.getAction());
                System.out.println("event.getX() = " + event.getX());
                System.out.println("event.getY() = " + event.getY());
                return true;
            }
        };
        globalOnTouchListenerFloatingView.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        View keepNotBeingKilledView = new View(this);
        keepNotBeingKilledView.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        notBeKilledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            notBeKilled = isChecked;
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    wm.addView(keepNotBeingKilledView, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGB_888));
                } else
                    //noinspection deprecation
                    wm.addView(keepNotBeingKilledView, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGB_888));
            } else try {
                wm.removeViewImmediate(keepNotBeingKilledView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
        IntentFilter filter = new IntentFilter();
        registerReceiver(notificationClickReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(notificationClickReceiver);
        super.onDestroy();
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

    @SuppressLint({"ClickableViewAccessibility"})
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
                            setDialogAttr(dialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)));
                            HSVColorPickerRL hsvColorPickerRL = new HSVColorPickerRL(this, pv.getColor(), ((int) (width * .8)), ((int) (height * .4))) {
                                @Override
                                void onPickedAction(int color) {
                                    pv.setPaintColor(color);
                                }
                            };
                            setDialogAttr(dialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)));
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
                            createConfirmationAD(this, (dialog1, which) -> {
                                File internalPathFile = new File(getFilesDir() + File.separator + "fb.path");
                                System.out.println("internalPathFile.delete() = " + internalPathFile.delete());
                                pv.clearAll();
                            }, (dialog1, which) -> {
                            }, R.string.whether_to_clear, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).show();
                            break;
                        case 8:
                            createConfirmationAD(this, (dialog1, which) -> hide(), (dialog1, which) -> {
                            }, R.string.whether_to_hide, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).show();
                            break;
                        case 9:
                            Dialog c = new Dialog(this);
                            setDialogAttr(c, false, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)));
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
                                setDialogAttr(TVsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)));
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
                            textsColorBtn.setEnabled(!this.whetherTextsColorIsInverted_isChecked);
                            textsColorBtn.setOnClickListener(v2 -> {
                                Dialog textsColorDialog = new Dialog(MainActivity.this);
                                setDialogAttr(textsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)));
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
                                    childTV.setTextColor(textsColor = invertColor(TVsColor));
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
                            new PermissionRequester(this::saveAction).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 23);
                            break;
                        case 11:
                            createConfirmationAD(this, (dialog1, which) -> stopFloatingWindow(), (dialog1, which) -> {
                            }, R.string.whether_to_exit, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).show();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wm.addView(globalOnTouchListenerFloatingView, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.RGB_888));
        } else
            //noinspection deprecation
            wm.addView(globalOnTouchListenerFloatingView, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.RGB_888));
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
            Intent intent = new Intent(this, notificationClickReceiver.getClass());
            PendingIntent pi = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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
            Intent intent = new Intent(this, NotificationClickReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            ncb.setContentIntent(pi);
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
            adb.setPositiveButton(R.string.ok, (dialog, which) -> {
                double edit = Double.parseDouble(et.getText().toString());
                double a = Math.log(edit) / Math.log(1.07D);
                widthWatchView.setHeight((int) edit);
                sb.setProgress((int) a);
            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            }).setTitle(R.string.type_stroke_width__pixels).setView(et);
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
        try {
            wm.removeViewImmediate(globalOnTouchListenerFloatingView);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    private void saveAction() {
        Dialog moreOptionsDialog = new Dialog(this);
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setDialogAttr(moreOptionsDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout ll = new LinearLayout(this);
        Button[] buttons = new Button[3];
        int[] textsRes = new int[]{
                R.string.save_image,
                R.string.save_path,
                R.string.import_path
        };
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Button(this);
            buttons[i].setText(textsRes[i]);
            ll.addView(buttons[i]);
            File d = new File(Environment.getExternalStorageDirectory().toString() + File.separator + getString(R.string.drawing_board));
            if (!d.exists()) System.out.println("d.mkdir() = " + d.mkdir());
            File pathDir = new File(d.toString() + File.separator + "path");
            if (!pathDir.exists()) System.out.println("pathDir.mkdir() = " + pathDir.mkdir());
            File internalPathFile = new File(getFilesDir() + File.separator + "fb.path");
            View.OnClickListener[] onClickListeners = new View.OnClickListener[]{
                    v -> {
                        @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        AlertDialog.Builder adb = new AlertDialog.Builder(this);
                        EditText et = new EditText(this);
                        et.setText(String.format(getString(R.string.tv), format));
                        AlertDialog alertDialog = adb.setPositiveButton(R.string.ok, (dialog, which) -> {
                            if (pv.saveImg(d.toString(), format + ".png")) {
                                Toast.makeText(this, getString(R.string.saving_success) + "\n" + d.toString() + File.separator + et.getText().toString() + ".png", Toast.LENGTH_SHORT).show();
                            }
                            moreOptionsDialog.dismiss();
                        }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                        }).setTitle(R.string.type_file_name).setView(et).create();
                        setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    },
                    v -> {
                        AlertDialog.Builder adb = new AlertDialog.Builder(this);
                        EditText et = new EditText(this);
                        @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                        et.setText(String.format(getString(R.string.tv), format));
                        AlertDialog alertDialog = adb.setPositiveButton(R.string.ok, (dialog, which) -> {
                            File pathFile = new File(pathDir.toString() + File.separator + et.getText().toString() + ".path");
                            try {
                                FileU.FileCopy(internalPathFile, pathFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (pathFile.exists())
                                Toast.makeText(this, getString(R.string.saving_success) + "\n" + pathFile.toString(), Toast.LENGTH_SHORT).show();
                            else Toast.makeText(this, R.string.saving_failed, Toast.LENGTH_SHORT).show();
                            moreOptionsDialog.dismiss();
                        }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                        }).setTitle(R.string.type_file_name).setView(et).create();
                        setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    },
                    v -> {
                        /*LinearLayout ll1 = new LinearLayout(this);
                        ll1.setLayoutParams(layoutParams);
                        ll1.setOrientation(LinearLayout.HORIZONTAL);
                        EditText et = new EditText(this);
                        et.setLayoutParams(layoutParams);
                        et.setGravity(Gravity.TOP);
                        ll1.addView(et);
                        Button btn1 = new Button(this);
                        btn1.setText(R.string.pick_file);
                        btn1.setLayoutParams(layoutParams);
                        ll1.addView(btn1);
                        btn1.setOnClickListener(v1 -> {
                            Dialog dialog = new Dialog(this);
                            setDialogAttr(dialog, false, ((int) (((float) width) * .8)), ((int) (((float) height) * .8)));
                            dialog.show();
                        });
                        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                        AlertDialog ad = dialog.setPositiveButton(R.string.ok, (dialog1, which) -> {
                            File file = new File(getFilesDir() + File.separator + "FilePickerResult");
                            pv.importPathFile(new File(et.getText().toString()));
                        }).setNegativeButton(R.string.cancel, (dialog1, which) -> {
                        }).setTitle(R.string.choose_path_file)
                                .setView(ll1).create();
                        setDialogAttr(ad, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        ad.show();*/
                        /*Intent intent = new Intent();
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setClass(this, Picker.class);
                        intent.putExtra("option", Picker.PICK_FILE);
                        startActivityForResult(intent, 71);
                        overridePendingTransition(R.anim.in_left_and_bottom, 0);
                        moreOptionsDialog.dismiss();*/
                        Dialog dialog = new Dialog(this);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        FilePickerRL filePickerRL = new FilePickerRL(this, FilePickerRL.TYPE_PICK_FILE, null, dialog::dismiss, s -> {
                            dialog.dismiss();
                            pv.importPathFile(new File(s));
                            Toast.makeText(this, R.string.importing_cuccess, Toast.LENGTH_SHORT).show();
                            moreOptionsDialog.dismiss();
                        });
                        dialog.setOnKeyListener((dialog1, keyCode, event) -> {
                            if (event.getAction() == KeyEvent.ACTION_UP)
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    filePickerRL.previous();
                                }
                            return true;
                        });
                        setDialogAttr(dialog, false, ((int) (((float) width) * .8)), ((int) (((float) height) * .8)));
                        dialog.setContentView(filePickerRL);
                        dialog.show();
                    }
            };
            buttons[i].setOnClickListener(onClickListeners[i]);
        }
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutParams(layoutParams);
        ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.addView(ll);
        moreOptionsDialog.setContentView(sv);
        moreOptionsDialog.setCanceledOnTouchOutside(true);
        moreOptionsDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 23 && grantResults[0] == 0) saveAction();
    }

    public class NotificationClickReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("onReceiveClick!");
            startFloatingWindow();
            Toast.makeText(MainActivity.this, "a", Toast.LENGTH_SHORT).show();
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 71 && data != null) {
            String file = data.getStringExtra("result");
            pv.importPathFile(new File(file));
            Toast.makeText(this, R.string.importing_cuccess, Toast.LENGTH_SHORT).show();
        }
    }*/
}