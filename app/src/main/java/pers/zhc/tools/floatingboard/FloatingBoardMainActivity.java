package pers.zhc.tools.floatingboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.Selection;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Expression;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.FilePickerRL;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.DialogUtil;
import pers.zhc.tools.utils.PermissionRequester;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.Digest;
import pers.zhc.u.FileU;
import pers.zhc.u.common.MultipartUploader;
import pers.zhc.u.common.ReadIS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static pers.zhc.tools.utils.ColorUtils.invertColor;
import static pers.zhc.tools.utils.DialogUtil.createConfirmationAD;
import static pers.zhc.tools.utils.DialogUtil.setDialogAttr;

public class FloatingBoardMainActivity extends BaseActivity {
    static Map<Long, Activity> longMainActivityMap;//memory leak??
    private WindowManager wm = null;
    private LinearLayout ll;
    private PaintView pv;
    private int width;
    private Bitmap icon;
    private int height;
    private int TVsColor = Color.WHITE, textsColor = Color.GRAY;
    private boolean invertColorChecked = false;
    private View globalOnTouchListenerFloatingView;
    private File currentInternalPathFile = null;
    private Runnable importPathFileDoneAction;
    private long currentInstanceMills;
    private TextView[] childTVs;
    private HSVAColorPickerRL.Position[] positions = new HSVAColorPickerRL.Position[3];
    private Button startFW;
    private String[] strings;
    private WindowManager.LayoutParams lp;
    private WindowManager.LayoutParams lp2;

    private static void uploadPaths(Context context) {
        try {
            InputStream is = new URL(Infos.zhcUrlString + "/upload/list.zhc?can=").openStream();
            if (is.read() != 1) {
                is.close();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> stringList = new ArrayList<>();
        try {
            URL getListURL = new URL(Infos.zhcUrlString + "/upload/list.zhc");
            InputStream is = getListURL.openStream();
            StringBuilder sb = new StringBuilder();
            new ReadIS(is, "UTF-8").read(sb::append);
            String s = sb.toString();
            System.out.println("sb.toString() = " + s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                JSONArray jsonArray = jsonObject.getJSONArray("files");
                int length = jsonArray.length();
                for (int i = 0; i < length; i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    stringList.add(object.getString("md5"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        File pathDir = new File(Common.getExternalStoragePath(context) + File.separator + context.getString(R.string.drawing_board) + File.separator + "path");
        File[] listFiles = pathDir.listFiles();
        if (listFiles != null)
            for (File file : listFiles) {
                if (file.isDirectory()) continue;
                try {
                    String fileMd5String = Digest.getFileMd5String(file);
                    if (!listStringContain(stringList, fileMd5String)) {
                        InputStream is = new FileInputStream(file);
                        byte[] headInformation = null;
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("name", file.getName());
                            headInformation = jsonObject.toString().getBytes();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (headInformation == null)
                            headInformation = ("unknown" + System.currentTimeMillis()).getBytes();
                        MultipartUploader.formUpload(Infos.zhcUrlString + "/upload/upload.zhc", headInformation, is);
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    private static boolean listStringContain(List<String> list, String s) {
        for (String s1 : list) {
            if (s.equals(s1)) {
                return true;
            }
        }
        return false;
    }

    public static EditText getSelectedET_currentMills(Context ctx, EditText et) {
        @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        et.setText(String.format(ctx.getString(R.string.tv), format));
        Selection.selectAll(et.getText());
        return et;
    }

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floating_board_activity);
        init();
        if (longMainActivityMap == null) {
            longMainActivityMap = new HashMap<>();
        }
        longMainActivityMap.put(currentInstanceMills, this);
    }

    @Override
    protected byte ckV() {
        new Thread(() -> {
            byte b = super.ckV();
            if (b == 0) {
                try {
                    runOnUiThread(this::stopFloatingWindow);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
        }).start();
        return 1;
    }

    private void init() {
        ckV();
        //        NotificationClickReceiver notificationClickReceiver = new NotificationClickReceiver();
//        registerReceiver(this.notificationClickReceiver, new IntentFilter("pers.zhc.tools.START_SERVICE"));
        currentInstanceMills = System.currentTimeMillis();
        File currentInternalPathDir = new File(getFilesDir().getPath() + File.separatorChar + "path");
        if (!currentInternalPathDir.exists()) {
            System.out.println("currentInternalPathDir.mkdirs() = " + currentInternalPathDir.mkdirs());
        }
        currentInternalPathFile = new File(currentInternalPathDir.getPath() + File.separatorChar + currentInstanceMills + ".path");
        Button clearPathBtn = findViewById(R.id.clear_path_btn);
        final float[] cachesSize = {0F};
        if (currentInternalPathDir.exists()) {
            File[] listFiles = currentInternalPathDir.listFiles();
            for (int i = 0, listFilesLength = listFiles != null ? listFiles.length : 0; i < listFilesLength; i++) {
                File file = listFiles[i];
                cachesSize[0] += file.length();
            }
        }
        clearPathBtn.setText(getString(R.string.clear_application_caches, cachesSize[0] / 1024F));
        clearPathBtn.setOnClickListener(v -> {
            FileU fileU = new FileU();
            if (currentInternalPathDir.exists()) {
                File[] listFiles = currentInternalPathDir.listFiles();
                for (File file : listFiles != null ? listFiles : new File[0]) {
                    if (!FloatingBoardMainActivity.longMainActivityMap.containsKey(Long.parseLong(fileU.getFileName(file.getName())))) {
                        System.out.println("file.delete() = " + file.delete());
                    }
                }
            }
            cachesSize[0] = 0;
            if (currentInternalPathDir.exists()) {
                File[] listFiles = currentInternalPathDir.listFiles();
                for (File file : listFiles != null ? listFiles : new File[0]) {
                    cachesSize[0] += file.length();
                }
            }
            clearPathBtn.setText(getString(R.string.clear_application_caches, cachesSize[0] / 1024F));
        });
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
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(FloatingBoardMainActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 4444);
                } else {
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
                }
            }
        });
//        RelativeLayout rl = findViewById(R.id.main);
        pv = new PaintView(this, width, height, currentInternalPathFile);
        pv.setStrokeWidth(10F);
        pv.setEraserStrokeWidth(10F);
        pv.setPaintColor(Color.RED);
        wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setBtn();
        Intent intent = getIntent();
        int a = intent.getIntExtra("a", 0);
        if (a == 1) {
            startFloatingWindow(true, true);
        }
        new Thread(() -> uploadPaths(this)).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setBtn() {
        startFW = findViewById(R.id.start_f_w);
        startFW.setText(R.string.start_floating_window);
        setStartBtn();
    }

    private void setStartBtn() {
        startFW.setOnClickListener(v -> {
            ToastUtils.show(this, R.string.floating_board);
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(FloatingBoardMainActivity.this)) {
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
                    startFloatingWindow(true, true);
//                    moveTaskToBack(true);
                }
            }
        });
    }

    @SuppressLint({"ClickableViewAccessibility"})
    void startFloatingWindow(boolean addPV, boolean addGlobalTL) {
        pv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        lp = new WindowManager.LayoutParams();
        lp2 = new WindowManager.LayoutParams();
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
        strings = getResources().getStringArray(R.array.btn_string);
        lp.width = width;
        lp.height = height;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (addPV) wm.addView(pv, lp);
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp2.width = /*(int) (width * proportionX)*/WindowManager.LayoutParams.WRAP_CONTENT;
        lp2.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE/* | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL*/;
        ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
        childTVs = new TextView[strings.length];
        importPathFileDoneAction = () -> {
            if (pv.isEraserMode) {
                childTVs[6].setText(R.string.eraser_mode);
                strings[6] = getString(R.string.eraser_mode);
            } else {
                childTVs[6].setText(R.string.drawing_mode);
                strings[6] = getString(R.string.drawing_mode);
            }
            ToastUtils.show(this, R.string.importing_success);
        };
        iv.setOnClickListener(v -> {
            System.out.println("click");
            ll.removeAllViews();
            if (pv.isEraserMode) {
                strings[6] = getString(R.string.eraser_mode);
            } else strings[6] = getString(R.string.drawing_mode);
            for (int i = 0; i < strings.length; i++) {
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1F));
//                int finalI1 = i;
                childTVs[i] = new TextView(this);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (height / strings.length * .7));
                layoutParams.setMargins(0, 0, 0, 5);
                childTVs[i].setLayoutParams(layoutParams);
                childTVs[i].setText(strings[i]);
                childTVs[i].setBackgroundColor(TVsColor);
                childTVs[i].setTextColor(textsColor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    childTVs[i].setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                    childTVs[i].setAutoSizeTextTypeUniformWithConfiguration(1, 200, 1, TypedValue.COMPLEX_UNIT_SP);
                    childTVs[i].setGravity(Gravity.CENTER);
                } else childTVs[i].setTextSize(20F);
                int finalI = i;
                childTVs[i].setOnClickListener(v1 -> {
                    ckV();
                    switch (finalI) {
                        case 0:
                            ll.removeAllViews();
                            ll.addView(iv);
                            wm.updateViewLayout(ll, lp2);
                            break;
                        case 1:
                            toggleDrawAndControlMode();
                            break;
                        case 2:
                            Dialog dialog = new Dialog(this);
                            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.color.transparent);
                            /*dialog.getWindow().setAttributes(new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                                    , WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 0, PixelFormat.RGBX_8888));*/
                            setDialogAttr(dialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
                            HSVAColorPickerRL hsvColorPickerRL = new HSVAColorPickerRL(this, pv.getColor(), ((int) (width * .8)), ((int) (height * .4)), positions[0]) {
                                @Override
                                void onPickedAction(int color, Position position) {
                                    pv.setPaintColor(color);
                                    positions[0] = position;
                                }
                            };
                            setDialogAttr(dialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
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
                                pv.clearAll();
                                pv.clearTouchRecordOSContent();
                            }, (dialog1, which) -> {
                            }, R.string.whether_to_clear, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).show();
                            break;
                        case 8:
                            createConfirmationAD(this, (dialog1, which) -> hide(), (dialog1, which) -> {
                            }, R.string.whether_to_hide, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).show();
                            break;
                        case 9:
                            Dialog c = new Dialog(this);
                            setDialogAttr(c, false, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
                            LinearLayout cLL = new LinearLayout(this);
                            cLL.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            cLL.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout[] linearLayouts = new LinearLayout[]{
                                    new LinearLayout(this),
                                    new LinearLayout(this),
                                    new LinearLayout(this)
                            };
                            Button TVsColorBtn = new Button(this);
                            TVsColorBtn.setText(R.string.control_panel_color);
                            TVsColorBtn.setOnClickListener(v2 -> {
                                Dialog TVsColorDialog = new Dialog(FloatingBoardMainActivity.this);
                                setDialogAttr(TVsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
                                HSVAColorPickerRL TVsColorPicker = new HSVAColorPickerRL(this, TVsColor, ((int) (width * .8)), ((int) (height * .4)), positions[1]) {
                                    @Override
                                    void onPickedAction(int color, Position position) {
                                        for (TextView childTV : childTVs) {
                                            TVsColor = color;
                                            childTV.setBackgroundColor(TVsColor);
                                            if (invertColorChecked)
                                                childTV.setTextColor(textsColor = invertColor(TVsColor));
                                        }
                                        positions[1] = position;
                                    }
                                };
                                TVsColorDialog.setContentView(TVsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
                                TVsColorDialog.show();
                            });
                            Button textsColorBtn = new Button(this);
                            textsColorBtn.setEnabled(!this.invertColorChecked);
                            textsColorBtn.setOnClickListener(v2 -> {
                                Dialog textsColorDialog = new Dialog(FloatingBoardMainActivity.this);
                                setDialogAttr(textsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
                                HSVAColorPickerRL textsColorPicker = new HSVAColorPickerRL(this, textsColor, ((int) (width * .8)), ((int) (height * .4)), positions[2]) {
                                    @Override
                                    void onPickedAction(int color, Position position) {
                                        for (TextView childTV : childTVs) {
                                            if (!invertColorChecked) textsColor = color;
                                            childTV.setTextColor(textsColor);
                                        }
                                        positions[2] = position;
                                    }
                                };
                                textsColorDialog.setContentView(textsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
                                textsColorDialog.show();
                            });
                            textsColorBtn.setText(R.string.text_color);
                            Switch whetherTextColorIsInverted = new Switch(this);
                            whetherTextColorIsInverted.setChecked(invertColorChecked);
                            whetherTextColorIsInverted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                textsColorBtn.setEnabled(!isChecked);
                                invertColorChecked = isChecked;
                                for (TextView childTV : childTVs) {
                                    childTV.setTextColor(textsColor = invertColor(TVsColor));
                                }
                            });
                            whetherTextColorIsInverted.setText(R.string.whether_text_color_is_inverted);
                            whetherTextColorIsInverted.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            TVsColorBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            textsColorBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            for (LinearLayout layout : linearLayouts) {
                                layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1F));
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
                            createConfirmationAD(this, (dialog1, which) -> exit(), (dialog1, which) -> {
                            }, R.string.whether_to_exit, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).show();
//                            pv.scaleCanvas((float) (width * 2), ((float) (height * 2)));
                            break;
                    }
                    System.out.println("i = " + finalI);
                });
                childTVs[i].setOnTouchListener(smallViewOnTouchListener);
                linearLayout.setGravity(Gravity.CENTER);
                linearLayout.addView(childTVs[i]);
                ll.addView(linearLayout);
            }
        });
        iv.setOnTouchListener(smallViewOnTouchListener);
        ll.addView(iv);
        wm.addView(ll, lp2);
        if (addGlobalTL) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wm.addView(globalOnTouchListenerFloatingView, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.RGB_888));
        } else
            //noinspection deprecation
            wm.addView(globalOnTouchListenerFloatingView, new WindowManager.LayoutParams(0, 0, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.RGB_888));
    }

    void toggleDrawAndControlMode() {
        if (lp.flags == (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)) {
            lp.flags = 0;
            wm.updateViewLayout(pv, lp);
            childTVs[1].setText(R.string.drawing);
            strings[1] = getString(R.string.drawing);
        } else {
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            wm.updateViewLayout(pv, lp);
            childTVs[1].setText(R.string.controlling);
            strings[1] = getString(R.string.controlling);
        }
    }

    private void exit() {
        stopFloatingWindow();
//                                unregisterReceiver(notificationClickReceiver);
        new Thread(() -> uploadPaths(this)).start();
        FloatingBoardMainActivity.longMainActivityMap.remove(currentInstanceMills);
        this.startFW.setText(R.string.start_floating_window);
        setStartBtn();
    }

    private void hide() {
        wm.removeViewImmediate(ll);
        NotificationManager nm = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        String date = SimpleDateFormat.getDateTimeInstance().format(new Date(this.currentInstanceMills));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel("channel1", getString(R.string.hide_notification), NotificationManager.IMPORTANCE_DEFAULT);
            nc.setDescription(getString(R.string.hide_notification));
            nc.canBypassDnd();
            nc.setBypassDnd(true);
            nc.enableLights(false);
            nc.enableLights(false);
            Objects.requireNonNull(nm).createNotificationChannel(nc);
            Notification.Builder nb = new Notification.Builder(this, "channel1");
            nb.setSmallIcon(Icon.createWithBitmap(icon))
                    .setContentTitle(getString(R.string.drawing_board))
                    .setContentText(getString(R.string.appear_f_b, date));
//            Intent intent = new Intent(this, NotificationClickReceiver.class);
            Intent intent = new Intent();
            intent.setAction("pers.zhc.tools.START_FB");
            intent.setPackage(getPackageName());
            intent.putExtra("mills", currentInstanceMills);
            boolean isDrawMode = this.childTVs[1].getText().equals(getString(R.string.drawing_mode));
            intent.putExtra("isDrawMode", isDrawMode);
            System.out.println("isDrawMode = " + isDrawMode);
            PendingIntent pi = getPendingIntent(intent);
            nb.setContentIntent(pi);
            Notification build = nb.build();
            build.flags = Notification.FLAG_AUTO_CANCEL;
            nm.notify(((int) (System.currentTimeMillis() - this.currentInstanceMills)), build);
        } else {
            NotificationCompat.Builder ncb = new NotificationCompat.Builder(this, "channel1");
            ncb.setOngoing(false)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.drawing_board))
                    .setContentText(getString(R.string.appear_f_b, date))
                    .setSmallIcon(R.mipmap.ic_launcher);
            Intent intent = new Intent();
            boolean isDrawMode = this.childTVs[1].getText().equals(getString(R.string.drawing_mode));
            intent.putExtra("isDrawMode", isDrawMode);
            System.out.println("isDrawMode = " + isDrawMode);
            intent.setAction("pers.zhc.tools.START_FB");
            intent.putExtra("mills", currentInstanceMills);
            intent.setPackage(getPackageName());
            PendingIntent pi = getPendingIntent(intent);
            ncb.setContentIntent(pi);
            Notification build = ncb.build();
            build.flags = Notification.FLAG_AUTO_CANCEL;
            Objects.requireNonNull(nm).notify(((int) (System.currentTimeMillis() - this.currentInstanceMills)), build);
        }
    }

    private PendingIntent getPendingIntent(Intent intent) {
        return PendingIntent.getBroadcast(this, ((int) (System.currentTimeMillis() - this.currentInstanceMills)), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void changeStrokeWidth() {
        Dialog mainDialog = new Dialog(this);
        LinearLayout mainLL = new LinearLayout(this);
        mainLL.setOrientation(LinearLayout.VERTICAL);
        mainLL.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout barLL = new LinearLayout(this);
        RadioGroup rg = new RadioGroup(this);
        barLL.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        barLL.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton[] radioButtons = new RadioButton[2];
        int[] strRes = new int[]{
                R.string.drawing_paint_stroke_width,
                R.string.eraser_paint_stroke_width
        };
        for (int i = 0; i < radioButtons.length; i++) {
            radioButtons[i] = new RadioButton(this);
            if (i == 0) radioButtons[0].setChecked(true);
            radioButtons[i].setText(strRes[i]);
            radioButtons[i].setId(i + 1);
            rg.addView(radioButtons[i]);
        }
        rg.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        barLL.addView(rg);
        LinearLayout ll = new LinearLayout(this);
        StrokeWatchView strokeWatchView = new StrokeWatchView(this, width, height);
        SeekBar sb = new SeekBar(this);
        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setTextSize(20F);
        final int[] checked = {1};
        tv.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            adb.setPositiveButton(R.string.ok, (dialog, which) -> {
                try {
                    int edit = Integer.parseInt(et.getText().toString());
                    double a = Math.log(edit) / Math.log(1.07D);
                    strokeWatchView.change(((float) edit), pv.getColor());
                    sb.setProgress((int) a);
                    if (checked[0] == 1)
                        pv.setStrokeWidth(((float) edit));
                    else pv.setEraserStrokeWidth(((float) edit));
                    tv.setText(getString(R.string.tv, String.valueOf(edit)));
                } catch (Exception e) {
                    Common.showException(e, this);
                }
            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            }).setTitle(R.string.type_stroke_width__pixels).setView(et);
            AlertDialog ad = adb.create();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            } else //noinspection deprecation
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            ad.show();
        });
        /*sb.setProgress(((int) (Math.log((double) pv.getStrokeWidth()) / Math.log(1.07D))));
        strokeWatchView.setLayoutParams(new LinearLayout.LayoutParams(sb.getProgress(), sb.getProgress()));
        strokeWatchView.change((float) Math.pow(1.07D, (double) sb.getProgress()), pv.getColor());
        tv.setText(String.valueOf((int) Math.pow(1.07D, (double) sb.getProgress())));*/
        double pow = pv.getStrokeWidth();
        pv.setStrokeWidth((float) ((int) pow));
        strokeWatchView.setLayoutParams(new LinearLayout.LayoutParams(((int) pow), ((int) pow)));
        strokeWatchView.change(((float) pow), pv.getColor());
        tv.setText(String.valueOf((int) pow));
        sb.setProgress((int) (Math.log(pow) / Math.log(1.07D)));
        sb.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sb.setMax(100);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(tv);
        ll.addView(sb);
        ll.addView(strokeWatchView);
        ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.color.transparent);
        setDialogAttr(mainDialog, false, width, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double pow = Math.pow(1.07D, progress);
                if (checked[0] == 1) {
                    pv.setStrokeWidth((float) ((int) pow));
                } else {
                    pv.setEraserStrokeWidth((float) pow);
                }
                tv.setText(String.valueOf(((int) pow)));
                strokeWatchView.setLayoutParams(new LinearLayout.LayoutParams(((int) pow), ((int) pow)));
                strokeWatchView.change(((float) pow), pv.getColor());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            checked[0] = checkedId;
            float w = checkedId == 1 ? pv.getStrokeWidth() : pv.getEraserStrokeWidth();
            strokeWatchView.setLayoutParams(new LinearLayout.LayoutParams(((int) w), ((int) w)));
            strokeWatchView.change(w, pv.getColor());
            tv.setText(String.format(getString(R.string.tv), (int) w));
            sb.setProgress((int) (Math.log(w) / Math.log(1.07D)));
        });
        mainLL.addView(barLL);
        mainLL.addView(ll);
        mainDialog.setContentView(mainLL);
        mainDialog.show();
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
        setDialogAttr(moreOptionsDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        LinearLayout ll = new LinearLayout(this);
        int[] textsRes = new int[]{
                R.string.import_image,
                R.string.export_image,
                R.string.import_path,
                R.string.export_path,
                R.string.reset_transform
        };
        Button[] buttons = new Button[textsRes.length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Button(this);
            buttons[i].setText(textsRes[i]);
            ll.addView(buttons[i]);
            File d = new File(Common.getExternalStoragePath(this) + File.separator + getString(R.string.drawing_board));
            if (!d.exists()) System.out.println("d.mkdir() = " + d.mkdir());
            File pathDir = new File(d.toString() + File.separator + "path");
            if (!pathDir.exists()) System.out.println("pathDir.mkdir() = " + pathDir.mkdir());
            File imageDir = new File(d.toString() + File.separator + "image");
            if (!imageDir.exists()) System.out.println("imageDir.mkdir() = " + imageDir.mkdir());
            View.OnClickListener[] onClickListeners = new View.OnClickListener[]{
                    v -> {
                        Dialog dialog = new Dialog(this);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        FilePickerRL filePickerRL = new FilePickerRL(this, FilePickerRL.TYPE_PICK_FILE, imageDir, dialog::dismiss, s -> {
                            dialog.dismiss();
                            AlertDialog.Builder importImageOptionsDialogBuilder = new AlertDialog.Builder(this);
                            LinearLayout linearLayout = new LinearLayout(this);
                            linearLayout.setOrientation(LinearLayout.VERTICAL);
                            TextView infoTV = new TextView(this);
                            EditText[] editTexts = new EditText[4];
                            Bitmap imageBitmap;
                            if ((imageBitmap = BitmapFactory.decodeFile(s)) == null) {
                                ToastUtils.show(this, R.string.importing_failed);
                                return;
                            }
                            int imageBitmapWidth = imageBitmap.getWidth();
                            int imageBitmapHeight = imageBitmap.getHeight();
                            infoTV.setText(String.format(getString(R.string.board_w_h_info_and_bitmap_w_h_info), width, height, "\n", imageBitmapWidth, imageBitmapHeight));
                            int[] hintRes = new int[]{
                                    R.string.left_starting_point,
                                    R.string.top_starting_point,
                                    R.string.scaled_width,
                                    R.string.scaled_height,
                            };
                            for (int j = 0; j < editTexts.length; j++) {
                                editTexts[j] = new EditText(this);
                                editTexts[j].setHint(hintRes[j]);
                                linearLayout.addView(editTexts[j]);
                            }
                            editTexts[0].setText(getString(R.string.tv, "0"));
                            editTexts[1].setText(getString(R.string.tv, "0"));
                            editTexts[2].setText(getString(R.string.tv, String.valueOf(imageBitmapWidth)));
                            editTexts[3].setText(getString(R.string.tv, String.valueOf(imageBitmapHeight)));
                            linearLayout.addView(infoTV);
                            AlertDialog importImageOptionsDialog = importImageOptionsDialogBuilder.setView(linearLayout)
                                    .setTitle(R.string.set__top__left__scaled_width__scaled_height)
                                    .setPositiveButton(R.string.ok, (dialog1, which) -> {
                                        try {
                                            double c1 = (float) new Expression(editTexts[0].getText().toString()).calculate();
                                            double c2 = (float) new Expression(editTexts[1].getText().toString()).calculate();
                                            double c3 = new Expression(editTexts[2].getText().toString()).calculate();
                                            double c4 = new Expression(editTexts[3].getText().toString()).calculate();
                                            if (Double.isNaN(c1) || Double.isNaN(c2) || Double.isNaN(c3) | Double.isNaN(c4))
                                                throw new Exception("math expression invalid");
                                            pv.importImage(imageBitmap, ((float) c1), ((float) c2), ((int) c3), ((int) c4));
                                            ToastUtils.show(this, R.string.importing_success);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            ToastUtils.show(this, R.string.type_error);
                                        }
                                        moreOptionsDialog.dismiss();
                                    })
                                    .setNegativeButton(R.string.cancel, (dialog1, which) -> {
                                    })
                                    .create();
                            setDialogAttr(importImageOptionsDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                            importImageOptionsDialog.show();
                        });
                        setFilePickerDialog(dialog, filePickerRL);
                    },
                    v -> {
                        EditText et = getSelectedET_currentMills(this, new EditText(this));
                        AlertDialog.Builder adb = new AlertDialog.Builder(this);
                        AlertDialog alertDialog = adb.setPositiveButton(R.string.ok, (dialog, which) -> {
                            pv.closePathRecoderOS();
                            File imageFile = new File(imageDir.toString() + File.separator + et.getText().toString() + ".png");
                            pv.saveImg(imageFile);
                            if (imageFile.exists())
                                ToastUtils.show(this, getString(R.string.saving_success) + "\n" + imageDir.toString() + File.separator + et.getText().toString() + ".png");
                            else ToastUtils.show(this, R.string.saving_failed);
                            pv.setOS(currentInternalPathFile, true);
                            moreOptionsDialog.dismiss();
                        }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                        }).setTitle(R.string.type_file_name).setView(et).create();
                        setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                        DialogUtil.setADWithET_autoShowSoftKeyboard(et, alertDialog);
                        alertDialog.show();
                    },
                    v -> {
                        Dialog dialog = new Dialog(this);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        FilePickerRL filePickerRL = new FilePickerRL(this, FilePickerRL.TYPE_PICK_FILE, pathDir, dialog::dismiss, s -> {
                            dialog.dismiss();
                            Dialog importPathFileProgressDialog = new Dialog(this);
                            setDialogAttr(importPathFileProgressDialog, false, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                            importPathFileProgressDialog.setCanceledOnTouchOutside(false);
                            RelativeLayout progressRL = View.inflate(this, R.layout.progress_bar, null).findViewById(R.id.rl);
                            progressRL.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            importPathFileProgressDialog.show();
                            importPathFileProgressDialog.setContentView(progressRL, new ViewGroup.LayoutParams(((int) (((float) width) * .95F)), ViewGroup.LayoutParams.WRAP_CONTENT));
                            TextView tv = progressRL.findViewById(R.id.progress_tv);
                            tv.setText(R.string.importing);
//                            ProgressBar progressBar = progressRL.findViewById(R.id.progress_bar);
                            TextView pTV = progressRL.findViewById(R.id.progress_bar_title);
                            pv.importPathFile(new File(s), () -> {
                                runOnUiThread(importPathFileDoneAction);
                                importPathFileProgressDialog.dismiss();
                            }, aFloat -> runOnUiThread(() -> {
//                                progressBar.setProgress(aFloat.intValue());
                                pTV.setText(getString(R.string.progress_tv, aFloat));
                            }));
                            moreOptionsDialog.dismiss();
                        });
                        setFilePickerDialog(dialog, filePickerRL);
                    },
                    v -> {
                        EditText et = getSelectedET_currentMills(this, new EditText(this));
                        AlertDialog.Builder adb = new AlertDialog.Builder(this);
                        AlertDialog alertDialog = adb.setPositiveButton(R.string.ok, (dialog, which) -> {
                            File pathFile = new File(pathDir.toString() + File.separator + et.getText().toString() + ".path");
                            try {
                                FileU.FileCopy(currentInternalPathFile, pathFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (pathFile.exists()) {
                                ToastUtils.show(this, getString(R.string.saving_success) + "\n" + pathFile.toString());
                                new Thread(() -> uploadPaths(this)).start();
                            } else ToastUtils.show(this, R.string.saving_failed);
                            moreOptionsDialog.dismiss();
                        }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                        }).setTitle(R.string.type_file_name).setView(et).create();
                        setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                        DialogUtil.setADWithET_autoShowSoftKeyboard(et, alertDialog);
                        alertDialog.show();
                    },
                    v -> pv.resetTransform()
            };
            buttons[i].setOnClickListener(onClickListeners[i]);
        }
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.addView(ll);
        moreOptionsDialog.setContentView(sv);
        moreOptionsDialog.setCanceledOnTouchOutside(true);
        moreOptionsDialog.show();
    }

    private void setFilePickerDialog(Dialog dialog, FilePickerRL filePickerRL) {
        dialog.setOnKeyListener((dialog1, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP)
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    filePickerRL.previous();
                }
            return true;
        });
        setDialogAttr(dialog, false, ((int) (((float) width) * .8)), ((int) (((float) height) * .8)), true);
        dialog.setContentView(filePickerRL);
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 23 && grantResults[0] == 0) saveAction();
    }
}