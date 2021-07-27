package pers.zhc.tools.floatingdrawing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.jetbrains.annotations.NotNull;
import org.mariuszgromada.math.mxparser.Expression;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.FilePickerRL;
import pers.zhc.tools.utils.*;
import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.views.HSVAColorPickerRL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static pers.zhc.tools.utils.DialogUtil.setDialogAttr;

/**
 * @author bczhc
 */
public class FloatingDrawingBoardMainActivity extends BaseActivity {
    /**
     * 静态存储Context。。用于隐藏恢复画板
     * 不知会不会内存泄露 (memory leak)...
     * 我已经看不懂我自己写的都是什么东西了，太乱了。
     */
    static Map<Long, Context> longActivityMap;
    final ColorUtils.HSVAColor strokeColorHSVA = new ColorUtils.HSVAColor();
    private final ColorUtils.HSVAColor panelColorHSVA = new ColorUtils.HSVAColor();
    private final ColorUtils.HSVAColor panelTextColorHSVA = new ColorUtils.HSVAColor();
    RequestPermissionInterface requestPermissionInterface = null;
    boolean mainDrawingBoardNotDisplay = false;
    private WindowManager wm = null;
    private LinearLayout fbLinearLayout;
    private PaintView pv;
    private int width;
    private Bitmap icon;
    private int height;
    private int panelColor = Color.WHITE, panelTextColor = Color.GRAY;
    private boolean invertColorChecked = false;
    private File currentInternalPathFile = null;
    private Runnable importPathFileDoneAction;
    private long currentInstanceMillisecond;
    private TextView[] childTextViews;
    private SwitchCompat fbSwitch;
    private String[] strings;
    private WindowManager.LayoutParams lp;
    private WindowManager.LayoutParams lp2;
    private ImageView iv;
    private LinearLayout optionsLinearLayout;
    private FloatingViewOnTouchListener.ViewDimension fbMeasuredSpec;
    private String internalPathDir = null;
    private Handler backgroundHandler;
    private boolean drawMode = false;
    private MediaProjectionManager mediaProjectionManager = null;
    private MediaProjection mediaProjection = null;
    private int dpi;
    private VirtualDisplay virtualDisplay = null;
    private boolean isCapturing = false;
    private Intent captureScreenResultData = null;
    private boolean panelColorFollowPainting;
    private FloatingViewOnTouchListener floatingViewOnTouchListener;

    private PaintView.PathSaver pathSaver;

    public static void setSelectedEditTextWithCurrentTimeMillisecond(@NotNull EditText et) {
        @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        et.setText(String.format(et.getContext().getString(R.string.str), format));
        Selection.selectAll(et.getText());
    }

    public static String completeParentheses(String s) {
        StringBuilder sb = new StringBuilder(s);
        Stack<Character> stack = new Stack<>();
        final int length = s.length();
        for (int i = 0; i < length; i++) {
            final char c = s.charAt(i);
            if (stack.empty() && c == ')') sb.insert(0, '(');
            if (c == '(') stack.push(c);
            else if (!stack.empty() && c == ')') stack.pop();
        }
        while (!stack.empty()) {
            sb.append(')');
            stack.pop();
        }
        return sb.toString();
    }

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.floating_board_activity);
        if (longActivityMap == null) {
            longActivityMap = new HashMap<>();
        }
        if (savedInstanceState != null) {
            final String savedOneKey = "internalPathFile";
            if (savedInstanceState.containsKey(savedOneKey)) {
                String lastInternalPath = savedInstanceState.getString("internalPathFile");
                ToastUtils.show(this, getString(R.string.str, lastInternalPath));
            }
        }
        new PermissionRequester(() -> {
        }).requestPermission(this, Manifest.permission.INTERNET, RequestCode.REQUEST_PERMISSION_INTERNET);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init() {

        currentInstanceMillisecond = System.currentTimeMillis();
        internalPathDir = getFilesDir().getPath() + File.separatorChar + "path";
        File currentInternalPathDir = new File(internalPathDir);
        if (!currentInternalPathDir.exists()) {
            System.out.println("currentInternalPathDir.mkdirs() = " + currentInternalPathDir.mkdirs());
        }
        currentInternalPathFile = new File(currentInternalPathDir.getPath() + File.separatorChar + currentInstanceMillisecond + ".path");
        pathSaver = new PaintView.PathSaver(currentInternalPathFile.getPath());

        Button clearPathBtn = findViewById(R.id.clear_path_btn);
        final float[] cacheSize = {0F};
        ArrayList<File> fileList = new ArrayList<>();
        Runnable showAndSetCacheSize = () -> {
            fileList.clear();
            cacheSize[0] = getCacheFilesSize(fileList);
            clearPathBtn.setText(getString(R.string.clear_application_caches, cacheSize[0] / 1024F));
        };
        clearPathBtn.setOnClickListener(v -> {
            showAndSetCacheSize.run();
            for (File file : fileList) {
                System.out.println("file.delete() = " + file.delete());
            }
            showAndSetCacheSize.run();
        });
        clearPathBtn.setOnLongClickListener(v -> {
            showAndSetCacheSize.run();
            return true;
        });
        clearPathBtn.performLongClick();
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        width = point.x;
        height = point.y;
        dpi = (int) getResources().getDisplayMetrics().density;
        View keepNotBeingKilledView = new View(this);
        keepNotBeingKilledView.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        pv = new PaintView(this);
        pv.setOnColorChangedCallback(color -> {
            if (this.panelColorFollowPainting) {
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                setPanelColor(color);
            }
        });
        pv.setDrawingStrokeWidth(10F);
        pv.setEraserStrokeWidth(10F);
        pv.setDrawingColor(Color.RED);
        wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setSwitch();
        strings = getResources().getStringArray(R.array.btn_string);
        childTextViews = new TextView[strings.length];
        // 更新悬浮窗位置
        pv.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        lp = new WindowManager.LayoutParams();
        lp2 = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            lp2.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            //noinspection deprecation
            lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            //noinspection deprecation
            lp2.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        }
        lp.format = PixelFormat.RGBA_8888;
        lp2.format = PixelFormat.RGBA_8888;
        lp.width = MATCH_PARENT;
        lp.height = MATCH_PARENT;
        //noinspection deprecation
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        lp2.width = /*(int) (width * proportionX)*/WRAP_CONTENT;
        lp2.height = WRAP_CONTENT;
        //noinspection deprecation
        lp2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        fbLinearLayout = new LinearLayout(this);
        fbLinearLayout.setOrientation(LinearLayout.VERTICAL);
        fbLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        System.gc();
        InputStream inputStream = getResources().openRawResource(R.raw.db);
        icon = BitmapFactory.decodeStream(inputStream);
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
        importPathFileDoneAction = () -> {
            if (pv.eraserMode) {
                childTextViews[6].setText(R.string.eraser_mode);
                strings[6] = getString(R.string.eraser_mode);
            } else {
                childTextViews[6].setText(R.string.drawing_mode);
                strings[6] = getString(R.string.drawing_mode);
            }
            ToastUtils.show(this, R.string.importing_succeeded);
        };
        this.optionsLinearLayout = new LinearLayout(this);
        optionsLinearLayout.setOrientation(LinearLayout.VERTICAL);
        optionsLinearLayout.setGravity(Gravity.CENTER);
        optionsLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        iv = new ImageView(this);
        float proportionX = ((float) 75) / ((float) 720);
        float proportionY = ((float) 75) / ((float) 1360);
        this.iv.setLayoutParams(new ViewGroup.LayoutParams((int) (width * proportionX), (int) (height * proportionY)));
        iv.setImageResource(R.drawable.ic_db);
        iv.setOnClickListener(v -> {
            fbLinearLayout.removeAllViews();
            fbLinearLayout.addView(optionsLinearLayout);
            measureFB_LL();
        });
        this.fbMeasuredSpec = new FloatingViewOnTouchListener.ViewDimension();
        floatingViewOnTouchListener = new FloatingViewOnTouchListener(lp2, wm, fbLinearLayout, width, height, fbMeasuredSpec);
        iv.setOnTouchListener(floatingViewOnTouchListener);
        for (int i = 0; i < strings.length; i++) {
            childTextViews[i] = new TextView(this);
            LinearLayout smallLL = new LinearLayout(this);
            smallLL.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, 0, 1F));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, (int) (height / strings.length * .7));
            layoutParams.setMargins(0, 0, 0, 5);
            childTextViews[i].setLayoutParams(layoutParams);
            childTextViews[i].setText(strings[i]);
            childTextViews[i].setBackgroundColor(panelColor);
            childTextViews[i].setTextColor(panelTextColor);
            childTextViews[i].setOnTouchListener((v, e) -> {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    // commit path temporary database in time, preventing data lose
                    pv.flushPathSaver();
                }
                return floatingViewOnTouchListener.onTouch(v, e);
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                childTextViews[i].setAutoSizeTextTypeUniformWithConfiguration(1, 200, 1, TypedValue.COMPLEX_UNIT_SP);
                childTextViews[i].setGravity(Gravity.CENTER);
            } else {
                childTextViews[i].setTextSize(20F);
            }
            int finalI = i;
            childTextViews[i].setOnClickListener(v1 -> {
                switch (finalI) {
                    case 0:
                        fbLinearLayout.removeAllViews();
                        fbLinearLayout.addView(iv);
                        measureFB_LL();
                        wm.updateViewLayout(fbLinearLayout, lp2);
                        break;
                    case 1:
                        toggleDrawAndControlMode();
                        break;
                    case 2:
                        setColor();
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
                        if (pv.eraserMode) {
                            pv.setEraserMode(false);
                            childTextViews[finalI].setText(R.string.drawing_mode);
                            strings[finalI] = getString(R.string.drawing_mode);
                        } else {
                            pv.setEraserMode(true);
                            childTextViews[finalI].setText(R.string.eraser_mode);
                            strings[finalI] = getString(R.string.eraser_mode);
                        }
                        break;
                    case 7:
                        DialogUtil.createConfirmationAlertDialog(this, (dialog1, which) -> {
                            pv.clearAll();
                            System.gc();
                        }, (dialog1, which) -> {
                        }, R.string.whether_to_clear, WRAP_CONTENT, WRAP_CONTENT, true).show();
                        break;
                    case 8:
                        pickScreenColor();
                        break;
                    case 9:
                        setPanel();
                        break;
                    case 10:
                        moreOptions();
                        break;
                    case 11:
                        DialogUtil.createConfirmationAlertDialog(this, (dialog1, which) -> exit(), (dialog1, which) -> {
                        }, R.string.whether_to_exit, WRAP_CONTENT, WRAP_CONTENT, true).show();
                        break;
                    default:
                        break;
                }
            });
            smallLL.setGravity(Gravity.CENTER);
            smallLL.addView(childTextViews[i]);
            optionsLinearLayout.addView(smallLL);
        }
        // 添加撤销，恢复按钮长按事件
        final boolean[] onUndo = {false};
        final boolean[] onRedo = {false};
        // for undo and redo motion event resolving...
        final float[][] prevXY = {{Float.NaN, Float.NaN}, {Float.NaN, Float.NaN}};
        // undo and redo
        final boolean[][] interruptLongClickAction = {{false}, {false}};
        LongClickResolver[] longClickResolver = new LongClickResolver[]{
                new LongClickResolver(this, () -> {
                    if (interruptLongClickAction[0][0]) return;
                    floatingViewOnTouchListener.cancelPerformClick();
                    final int[] time = {470};//执行间隔
                    final Thread thread = new Thread() {
                        @Override
                        public void run() {
                            Vibrator vibrator = (Vibrator) FloatingDrawingBoardMainActivity.this.getSystemService(VIBRATOR_SERVICE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                //noinspection deprecation
                                vibrator.vibrate(100);
                            }
                            while (onUndo[0]) {
                                if (time[0] > 70) {
                                    time[0] -= 36;
                                }
                                pv.undo();
                                try {
                                    //noinspection BusyWait
                                    sleep(time[0]);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    thread.start();
                    onUndo[0] = true;
                }),
                new LongClickResolver(this, () -> {
                    if (interruptLongClickAction[1][0]) return;
                    floatingViewOnTouchListener.cancelPerformClick();
                    final int[] time = {470};//执行间隔
                    final Thread thread = new Thread() {
                        @Override
                        public void run() {
                            Vibrator vibrator = (Vibrator) FloatingDrawingBoardMainActivity.this.getSystemService(VIBRATOR_SERVICE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                //noinspection deprecation
                                vibrator.vibrate(100);
                            }
                            while (onRedo[0]) {
                                if (time[0] > 70) {
                                    time[0] -= 36;
                                }
                                pv.redo();
                                try {
                                    //noinspection BusyWait
                                    sleep(time[0]);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    thread.start();
                    onRedo[0] = true;
                })
        };

        childTextViews[4].setOnTouchListener((v, event) -> {
            floatingViewOnTouchListener.onTouch(v, event, true);
            int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            longClickResolver[0].onTouch(event);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // initialize
                    prevXY[0][0] = x;
                    prevXY[0][1] = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (x - prevXY[0][0] > 1 || y - prevXY[0][1] > 1) {
                        onUndo[0] = false;
                        interruptLongClickAction[0][0] = true;
                    }
                    prevXY[0][0] = x;
                    prevXY[0][1] = y;
                    break;
                case MotionEvent.ACTION_UP:
                    onUndo[0] = false;
                    interruptLongClickAction[0][0] = false;
                    break;
                default:
            }
            return true;
        });
        childTextViews[5].setOnTouchListener((v, event) -> {
            floatingViewOnTouchListener.onTouch(v, event, true);
            int action = event.getAction();
            float x = event.getX();
            float y = event.getY();
            longClickResolver[1].onTouch(event);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // initialize
                    prevXY[1][0] = x;
                    prevXY[1][1] = y;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (x - prevXY[1][0] > 1 || y - prevXY[1][1] > 1) {
                        onRedo[0] = false;
                        interruptLongClickAction[0][0] = true;
                    }
                    prevXY[1][0] = x;
                    prevXY[1][1] = y;
                    break;
                case MotionEvent.ACTION_UP:
                    onRedo[0] = false;
                    interruptLongClickAction[1][0] = false;
                    break;
                default:
            }
            return true;
        });

        Button readCacheFileBtn = findViewById(R.id.open_cache_path_file);
        readCacheFileBtn.setOnClickListener(v -> {
            if (!fbSwitch.isChecked()) {
                fbSwitch.setChecked(true);
            }
            importPath(null, new File(internalPathDir));
        });
        requestPermissionInterface = (requestCode, resultCode, data) -> {
            if (requestCode == RequestCode.REQUEST_CAPTURE_SCREEN) {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        captureScreenResultData = data;
                        setupProjection();
                    } else {
                        ToastUtils.show(this, R.string.request_permission_error);
                    }
                } else {
                    ToastUtils.show(this, R.string.please_grant_permission);
                }
            }
            startFloatingWindow();
        };
        strokeColorHSVA.set(pv.getDrawingColor());
        panelColorHSVA.set(panelColor);
        panelTextColorHSVA.set(panelTextColor);
    }

    private void setPanel() {
        Dialog c = new Dialog(this);
        LinearLayout inflate = View.inflate(this, R.layout.fdb_panel_settings_view, null).findViewById(R.id.ll);
        Button panelColorBtn = inflate.findViewById(R.id.panel_color);
        panelColorBtn.setEnabled(!this.panelColorFollowPainting);
        panelColorBtn.setOnClickListener(v2 -> {
            Dialog TextViewsColorDialog = new Dialog(FloatingDrawingBoardMainActivity.this, R.style.dialog_with_background_dim_false);
            TextViewsColorDialog.setCanceledOnTouchOutside(true);
            setDialogAttr(TextViewsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
            HSVAColorPickerRL textViewsColorPicker = new HSVAColorPickerRL(this, panelColorHSVA);
            textViewsColorPicker.setOnColorPickedInterface((hsv, alpha, color) -> {
                panelColorHSVA.set(alpha, hsv[0], hsv[1], hsv[2]);
                setPanelColor(color);
            });

            TextViewsColorDialog.setContentView(textViewsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
            TextViewsColorDialog.show();
        });
        Button textsColorBtn = inflate.findViewById(R.id.text_color);
        textsColorBtn.setEnabled(!this.invertColorChecked);
        textsColorBtn.setOnClickListener(v2 -> {
            Dialog textsColorDialog = new Dialog(FloatingDrawingBoardMainActivity.this, R.style.dialog_with_background_dim_false);
            textsColorDialog.setCanceledOnTouchOutside(true);
            setDialogAttr(textsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
            HSVAColorPickerRL textsColorPicker = new HSVAColorPickerRL(this, panelTextColorHSVA);
            textsColorPicker.setOnColorPickedInterface((hsv, alpha, color) -> {
                panelTextColorHSVA.set(alpha, hsv);
                for (TextView childTextView : childTextViews) {
                    if (!invertColorChecked) {
                        panelTextColor = panelTextColorHSVA.getColor();
                    }
                    childTextView.setTextColor(panelTextColor);
                }
            });

            textsColorDialog.setContentView(textsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
            textsColorDialog.show();
        });
        textsColorBtn.setText(R.string.text_color);
        SwitchMaterial invertTextColor = inflate.findViewById(R.id.invert_text_color);
        invertTextColor.setChecked(invertColorChecked);
        invertTextColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textsColorBtn.setEnabled(!isChecked);
            invertColorChecked = isChecked;
            if (isChecked) setPanelTextColor(ColorUtils.invertColor(panelColor));
            else setPanelTextColor(panelTextColorHSVA.getColor());
        });
        SwitchMaterial followPaintingColor = inflate.findViewById(R.id.follow_painting_color);
        followPaintingColor.setChecked(this.panelColorFollowPainting);
        followPaintingColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.panelColorFollowPainting = isChecked;
            panelColorBtn.setEnabled(!this.panelColorFollowPainting);
            if (isChecked) {
                setPanelColor(pv.getDrawingColor());
            } else setPanelColor(panelColorHSVA.getColor());
            if (invertColorChecked) {
                setPanelTextColor(ColorUtils.invertColor(panelColor));
            } else setPanelTextColor(panelTextColorHSVA.getColor());
        });
        c.setContentView(inflate);
        setDialogAttr(c, false, WRAP_CONTENT
                , WRAP_CONTENT, true);
        c.show();
    }

    private void setPanelTextColor(@ColorInt int color) {
        for (TextView childTV : childTextViews) {
            childTV.setTextColor(color);
        }
    }

    private void setColor() {
        Dialog dialog;
        if (pv.eraserMode) {
            dialog = new Dialog(this);
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    MATCH_PARENT
                    , WRAP_CONTENT));
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            TextView titleTextView = new TextView(this);
            titleTextView.setText(R.string.opacity);
            SeekBar sb = new SeekBar(this);
            linearLayout.addView(titleTextView);
            linearLayout.addView(sb);
            sb.setProgress(pv.getEraserAlpha() * 100 / 255);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    pv.setEraserAlpha(HSVAColorPickerRL.limitValue(progress * 255 / 100, 0, 255));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            dialog.setContentView(linearLayout);
            setDialogAttr(dialog, false
                    , MATCH_PARENT, WRAP_CONTENT, true);
        } else {
            dialog = new Dialog(this, R.style.dialog_with_background_dim_false);
            dialog.setCanceledOnTouchOutside(true);
            final Window dialogWindow = dialog.getWindow();
            if (dialogWindow != null) {
                dialogWindow.setBackgroundDrawableResource(R.color.transparent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    dialogWindow.setAttributes(new WindowManager.LayoutParams(MATCH_PARENT, MATCH_PARENT
                            , WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 0, PixelFormat.RGBX_8888));
                }
            }
            setDialogAttr(dialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
            HSVAColorPickerRL hsvaColorPickerRL = new HSVAColorPickerRL(this, strokeColorHSVA);
            hsvaColorPickerRL.setOnColorPickedInterface((hsv, alpha, color) -> {
                strokeColorHSVA.set(alpha, hsv);
                pv.setDrawingColor(strokeColorHSVA.getColor());
            });

            setDialogAttr(dialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
            dialog.setContentView(hsvaColorPickerRL);
        }
        dialog.show();
    }

    private void setPanelColor(int color) {
        panelColor = color;
        for (TextView childTextView : childTextViews) {
            childTextView.setBackgroundColor(panelColor);
            if (invertColorChecked) {
                childTextView.setTextColor(panelTextColor = ColorUtils.invertColor(panelColor));
            }
        }
    }

    private void setupProjection() {
        mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null && captureScreenResultData != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(RESULT_OK, captureScreenResultData);
        } else {
            ToastUtils.show(this, R.string.acquire_service_failed);
        }
    }

    private void pickScreenColor() {
        if (mediaProjectionManager == null) {
            initCapture();
        } else {
            captureScreen();
        }
    }

    private Handler getBackgroundHandler() {
        if (backgroundHandler == null) {
            HandlerThread backgroundThread =
                    new HandlerThread("cat_window", android.os.Process
                            .THREAD_PRIORITY_BACKGROUND);
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
        return backgroundHandler;
    }

    private void captureScreen() {
        if (isCapturing) {
            ToastUtils.show(this, R.string.have_capture_task);
            Log.d("d", "capturing...");
            return;
        }
        isCapturing = true;
        ImageReader ir = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1);
        ir.setOnImageAvailableListener(reader -> {
            Log.d("d", "callback! " + reader);
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane plane = image.getPlanes()[0];
                    final ByteBuffer buffer = plane.getBuffer();
                    int pixelStride = plane.getPixelStride();
                    int rowStride = plane.getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    image.close();
                    ToastUtils.show(this, getString(R.string.ok) + bitmap);
                } else {
                    Log.d("d", "image is null.");
                }
            } catch (Exception e) {
                Common.showException(e, this);
            } finally {
                if (image != null) {
                    image.close();
                }
                ir.close();
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                }
                isCapturing = false;
            }
            ir.setOnImageAvailableListener(null, null);
        }, getBackgroundHandler());
        while (true) {
            try {
                virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture", width, height, dpi
                        , DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, ir.getSurface(), null, null);
                break;
            } catch (Exception ignored) {
                setupProjection();
            }
        }
    }

    private void initCapture() {
        stopFloatingWindow();
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this, RequestCaptureScreenActivity.class);
        intent.putExtra("millisecond", this.currentInstanceMillisecond);
        startActivityForResult(intent, RequestCode.REQUEST_CAPTURE_SCREEN);
    }

    private long getCacheFilesSize(@Nullable ArrayList<File> fileList) {
        if (fileList == null) {
            return 0;
        }
        long cacheSize = 0L;
        File[] listFiles = null;
        if (internalPathDir != null) {
            listFiles = new File(internalPathDir).listFiles();
        }
        if (listFiles != null) {
            for (File file : listFiles) {
                try {
                    String filename;
                    String filenameExtension = file.getName();
                    if (!filenameExtension.matches(".*\\..*")) {
                        filename = filenameExtension;
                    } else {
                        int index = filenameExtension.lastIndexOf('.');
                        filename = filenameExtension.substring(0, index);
                    }
                    if (FloatingDrawingBoardMainActivity.longActivityMap.containsKey(Long.parseLong(filename))) {
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                }
                fileList.add(file);
            }
        }
        File crashPath = new File(Common.getAppMainExternalStoragePath(this) + File.separatorChar + this.getString(R.string.crash));
        File[] crashFiles = crashPath.listFiles();
        if (crashFiles != null) {
            fileList.addAll(Arrays.asList(crashFiles));
        }
        for (File file : fileList) {
            if (file != null) {
                cacheSize += file.length();
            }
        }
        return cacheSize;
    }

    private void measureFB_LL() {
        int mode = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        fbLinearLayout.measure(mode, mode);
        fbMeasuredSpec.width = fbLinearLayout.getMeasuredWidth();
        fbMeasuredSpec.height = fbLinearLayout.getMeasuredHeight();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setSwitch() {
        fbSwitch = findViewById(R.id.f_b_switch);
        fbSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                new AbstractCheckOverlayPermission(this) {
                    @Override
                    public void granted() {
                        pv.setPathSaver(pathSaver);
                        startFloatingWindow();
                    }

                    @Override
                    public void denied() {
                        fbSwitch.setChecked(false);
                        pathSaver.reset();
                    }
                };
            } else {
                stopFloatingWindow();
            }
        });
    }

    @SuppressLint({"ClickableViewAccessibility"})
    void startFloatingWindow() {
        if (!longActivityMap.containsKey(currentInstanceMillisecond)) {
            longActivityMap.put(currentInstanceMillisecond, this);
        }
        try {
            wm.addView(pv, lp);
            mainDrawingBoardNotDisplay = false;
            fbLinearLayout.addView(iv);
            measureFB_LL();
            this.wm.addView(fbLinearLayout, lp2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void toggleDrawAndControlMode() {
        if (drawMode) {
            // noinspection deprecation
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            wm.updateViewLayout(pv, lp);
            childTextViews[1].setText(R.string.controlling);
            strings[1] = getString(R.string.controlling);
            drawMode = false;
        } else {
            //noinspection deprecation
            lp.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            wm.updateViewLayout(pv, lp);
            childTextViews[1].setText(R.string.drawing);
            strings[1] = getString(R.string.drawing);
            drawMode = true;
        }
    }

    private void exit() {
        pathSaver.close();
        stopFloatingWindow();
        FloatingDrawingBoardMainActivity.longActivityMap.remove(currentInstanceMillisecond);
        this.fbSwitch.setChecked(false);
    }

    private void hide() {
        try {
            wm.removeViewImmediate(fbLinearLayout);
        } catch (IllegalArgumentException e) {
            Common.showException(e, this);
        }
        NotificationManager nm = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        String date = SimpleDateFormat.getDateTimeInstance().format(new Date(this.currentInstanceMillisecond));
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
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            intent.setAction(BroadcastAction.ACTION_START_FLOATING_BOARD);
            intent.setPackage(getPackageName());
            intent.putExtra("millisecond", currentInstanceMillisecond);
            boolean isDrawMode = this.childTextViews[1].getText().equals(getString(R.string.drawing_mode));
            intent.putExtra("isDrawMode", isDrawMode);
            System.out.println("isDrawMode = " + isDrawMode);
            PendingIntent pi = getPendingIntent(intent);
            nb.setContentIntent(pi);
            Notification build = nb.build();
            build.flags = Notification.FLAG_AUTO_CANCEL;
            nm.notify(((int) (System.currentTimeMillis() - this.currentInstanceMillisecond)), build);
        } else {
            NotificationCompat.Builder ncb = new NotificationCompat.Builder(this, "channel1");
            ncb.setOngoing(false)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.drawing_board))
                    .setContentText(getString(R.string.appear_f_b, date))
                    .setSmallIcon(R.drawable.ic_launcher_foreground);
            Intent intent = new Intent();
            intent.setAction(BroadcastAction.ACTION_START_FLOATING_BOARD);
            intent.putExtra("millisecond", currentInstanceMillisecond);
            intent.setPackage(getPackageName());
            PendingIntent pi = getPendingIntent(intent);
            ncb.setContentIntent(pi);
            Notification build = ncb.build();
            build.flags = Notification.FLAG_AUTO_CANCEL;
            Objects.requireNonNull(nm).notify(((int) (System.currentTimeMillis() - this.currentInstanceMillisecond)), build);
        }
    }

    private PendingIntent getPendingIntent(Intent intent) {
        return PendingIntent.getBroadcast(this, ((int) (System.currentTimeMillis() - this.currentInstanceMillisecond)), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void changeStrokeWidth() {
        Dialog mainDialog = new Dialog(this);
        LinearLayout mainLL = View.inflate(this, R.layout.fdb_stoke_width_view, null).findViewById(R.id.ll);
        RadioGroup rg = mainLL.findViewById(R.id.rg);
        RadioButton[] radioButtons = new RadioButton[2];
        for (int i = 0; i < radioButtons.length; i++) {
            radioButtons[i] = (RadioButton) rg.getChildAt(i);
        }
        radioButtons[pv.eraserMode ? 1 : 0].setChecked(true);
        CheckBox lockStrokeCB = mainLL.findViewById(R.id.cb);
        lockStrokeCB.setChecked(pv.isLockingStroke());
        lockStrokeCB.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pv.setLockingStroke(isChecked);
            pv.lockStroke();
        });
        StrokeWatchView strokeWatchView = new StrokeWatchView(this);
        strokeWatchView.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        SeekBar sb = mainLL.findViewById(R.id.sb);
        TextView tv = mainLL.findViewById(R.id.tv);
        final int[] checked = {pv.eraserMode ? R.id.eraser_radio : R.id.brush_radio};
        tv.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                try {
                    float edit = Float.parseFloat(et.getText().toString());
                    double a = Math.log(edit * pv.getScale()) / Math.log(1.07D);
                    strokeWatchView.change((edit * pv.getCanvas().getScale()), pv.getDrawingColor());
                    sb.setProgress((int) a);
                    if (checked[0] == R.id.brush_radio) {
                        pv.setDrawingStrokeWidth(edit);
                    } else if (checked[0] == R.id.eraser_radio) {
                        pv.setEraserStrokeWidth(edit);
                    }
                    pv.lockStroke();
                    tv.setText(getString(R.string.fdb_stroke_width_info, edit, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
                } catch (Exception e) {
                    Common.showException(e, this);
                }
            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            }).setTitle(R.string.type_stroke_width__pixels).setView(et);
            AlertDialog ad = adb.create();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            } else {
                //noinspection deprecation
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
            }
            DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(et, ad);
            ad.show();
        });
        float pvStrokeWidthInUse = pv.getStrokeWidthInUse();
        strokeWatchView.change((pvStrokeWidthInUse * pv.getCanvas().getScale()), pv.getDrawingColor());
        tv.setText(getString(R.string.fdb_stroke_width_info, pvStrokeWidthInUse, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
        sb.setProgress((int) (Math.log(pvStrokeWidthInUse * pv.getScale()) / Math.log(1.07D)));
        sb.setMax(100);
        setDialogAttr(mainDialog, false, ((int) (((float) width) * .8F)), WRAP_CONTENT, true);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float pow = (float) Math.pow(1.07D, progress);
                    pow /= pv.getScale();
                    if (checked[0] == R.id.brush_radio) {
                        pv.setDrawingStrokeWidth(pow);
                    } else {
                        pv.setEraserStrokeWidth(pow);
                    }
                    pv.lockStroke();
                    tv.setText(getString(R.string.fdb_stroke_width_info, pow, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
                    strokeWatchView.change((pow * pv.getCanvas().getScale()), pv.getDrawingColor());
                }
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
            float strokeWidth = (checkedId == R.id.brush_radio) ? pv.getDrawingStrokeWidth() : pv.getEraserStrokeWidth();
            strokeWatchView.change(strokeWidth * pv.getCanvas().getScale(), pv.getDrawingColor());
            tv.setText(getString(R.string.fdb_stroke_width_info, strokeWidth, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
            sb.setProgress((int) (Math.log(strokeWidth * pv.getScale()) / Math.log(1.07D)));
        });
        mainLL.addView(strokeWatchView);
        mainDialog.setContentView(mainLL, new ViewGroup.LayoutParams(((int) (((float) width) * .8F)), WRAP_CONTENT));
        mainDialog.show();
    }

    private void stopFloatingWindow() {
        fbLinearLayout.removeAllViews();
        try {
            wm.removeViewImmediate(fbLinearLayout);
        } catch (Exception ignored) {
        }
        try {
            wm.removeViewImmediate(iv);
        } catch (Exception ignored) {
        }
        try {
            wm.removeViewImmediate(pv);
            mainDrawingBoardNotDisplay = true;
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, R.anim.slide_out_bottom);
    }

    private void moreOptions() {
        Dialog moreOptionsDialog = new Dialog(this);
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new ScrollView.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        setDialogAttr(moreOptionsDialog, false, WRAP_CONTENT, WRAP_CONTENT, true);
        LinearLayout ll = new LinearLayout(this);
        int[] textsRes = new int[]{
                R.string.import_image,
                R.string.export_image,
                R.string.import_path,
                R.string.export_path,
                R.string.reset_transform,
                R.string.manage_layer,
                R.string.hide_panel,
                R.string.drawing_statistics
        };
        File fbDir = new File(Common.getExternalStoragePath(this) + File.separator + getString(R.string.drawing_board));
        if (!fbDir.exists()) {
            System.out.println("d.mkdir() = " + fbDir.mkdir());
        }
        File pathDir = new File(fbDir.toString() + File.separator + "path");
        if (!pathDir.exists()) {
            System.out.println("pathDir.mkdir() = " + pathDir.mkdir());
        }
        File imageDir = new File(fbDir.toString() + File.separator + "image");
        if (!imageDir.exists()) {
            System.out.println("imageDir.mkdir() = " + imageDir.mkdir());
        }
        View.OnClickListener[] onClickListeners = new View.OnClickListener[]{
                v0 -> new PermissionRequester(() -> {
                    Dialog dialog = getFilePickerDialog(imageDir, (picker, path) -> {

                        AlertDialog.Builder importImageOptionsDialogBuilder = new AlertDialog.Builder(this);
                        LinearLayout linearLayout = new LinearLayout(this);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);
                        TextView infoTV = new TextView(this);
                        EditText[] editTexts = new EditText[4];
                        Bitmap imageBitmap;
                        if ((imageBitmap = BitmapFactory.decodeFile(path)) == null) {
                            ToastUtils.show(this, R.string.importing_failed);
                            return;
                        }
                        Point point = new Point();
                        BitmapUtil.getBitmapResolution(imageBitmap, point);
                        int imageBitmapWidth = point.x;
                        int imageBitmapHeight = point.y;
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
                        editTexts[0].setText(getString(R.string.str, "0"));
                        editTexts[1].setText(getString(R.string.str, "0"));
                        editTexts[2].setText(getString(R.string.str, String.valueOf(imageBitmapWidth)));
                        editTexts[3].setText(getString(R.string.str, String.valueOf(imageBitmapHeight)));
                        linearLayout.addView(infoTV);
                        AlertDialog importImageOptionsDialog = importImageOptionsDialogBuilder.setView(linearLayout)
                                .setTitle(R.string.set__top__left__scaled_width__scaled_height)
                                .setPositiveButton(R.string.confirm, (dialog1, which) -> {
                                    try {
                                        double c1 = (float) new Expression(completeParentheses(editTexts[0].getText().toString())).calculate();
                                        double c2 = (float) new Expression(completeParentheses(editTexts[1].getText().toString())).calculate();
                                        double c3 = new Expression(completeParentheses(editTexts[2].getText().toString())).calculate();
                                        double c4 = new Expression(completeParentheses(editTexts[3].getText().toString())).calculate();
                                        if (Double.isNaN(c1) || Double.isNaN(c2) || Double.isNaN(c3) | Double.isNaN(c4)) {
                                            throw new Exception("math expression invalid");
                                        }
                                        pv.importImage(imageBitmap, ((float) c1), ((float) c2), ((int) c3), ((int) c4));
                                        ToastUtils.show(this, R.string.importing_succeeded);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        ToastUtils.show(this, R.string.type_error);
                                    }
                                    moreOptionsDialog.dismiss();
                                })
                                .setNegativeButton(R.string.cancel, (dialog1, which) -> {
                                })
                                .create();
                        setDialogAttr(importImageOptionsDialog, false, WRAP_CONTENT, WRAP_CONTENT, true);
                        importImageOptionsDialog.show();

                    });
                    dialog.show();
                }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE),
                v1 -> new PermissionRequester(() -> {
                    LinearLayout linearLayout = View.inflate(this, R.layout.export_image_layout, null)
                            .findViewById(R.id.root);
                    EditText filenameEditText = linearLayout.findViewById(R.id.file_name);
                    setSelectedEditTextWithCurrentTimeMillisecond(filenameEditText);
                    EditText widthEditText = linearLayout.findViewById(R.id.width);
                    EditText heightEditText = linearLayout.findViewById(R.id.height);
                    Point point = new Point();
                    pv.bitmapResolution(point);
                    final int[] imageW = {point.x};
                    final int[] imageH = {point.y};
                    widthEditText.setText(String.valueOf(point.x));
                    heightEditText.setText(String.valueOf(point.y));
                    widthEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    heightEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    AlertDialog ad = adb.setTitle(R.string.export_image)
                            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                Expression expression = new Expression();
                                expression.setExpressionString(completeParentheses(widthEditText.getText().toString()));
                                imageW[0] = ((int) expression.calculate());
                                expression.setExpressionString(completeParentheses(heightEditText.getText().toString()));
                                imageH[0] = ((int) expression.calculate());
                                if (Double.isNaN(imageW[0]) || Double.isNaN(imageH[0]) || imageW[0] <= 0 || imageH[0] <= 0) {
                                    ToastUtils.show(FloatingDrawingBoardMainActivity.this, R.string.please_enter_correct_value_toast);
                                    moreOptionsDialog.dismiss();
                                    return;
                                }
                                File imageFile = new File(imageDir.toString() + File.separator + filenameEditText.getText().toString() + ".png");
                                pv.exportImg(imageFile, imageW[0], imageH[0]);
                                moreOptionsDialog.dismiss();
                            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                            }).setView(linearLayout).create();
                    setDialogAttr(ad, false, WRAP_CONTENT, WRAP_CONTENT, true);
                    DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(filenameEditText, ad);
                    ad.show();
                }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE),
                v2 -> importPath(moreOptionsDialog, pathDir),
                v3 -> new PermissionRequester(() -> {
                    EditText et = new EditText(this);
                    setSelectedEditTextWithCurrentTimeMillisecond(et);
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    AlertDialog alertDialog = adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                        File pathFile = new File(pathDir.toString() + File.separator + et.getText().toString() + ".path");
                        try {
                            if (!currentInternalPathFile.exists()) {
                                throw new FileNotFoundException(getString(R.string.native_file_not_exist));
                            }
                            FileUtil.copy(currentInternalPathFile, pathFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (pathFile.exists()) {
                            ToastUtils.show(this, getString(R.string.saving_succeeded_dialog) + "\n" + pathFile.toString());
                        } else {
                            ToastUtils.show(this, getString(R.string.concat, getString(R.string.saving_failed), et.toString()));
                        }
                        moreOptionsDialog.dismiss();
                    }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                    }).setTitle(R.string.type_filename).setView(et).create();
                    setDialogAttr(alertDialog, false, WRAP_CONTENT, WRAP_CONTENT, true);
                    DialogUtil.setAlertDialogWithEditTextAndAutoShowSoftKeyBoard(et, alertDialog);
                    alertDialog.show();
                }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE),
                v4 -> pv.resetTransform(),
                v5 -> setLayer(),
                v6 -> DialogUtil.createConfirmationAlertDialog(this, (dialog1, which) -> {
                    hide();
                    moreOptionsDialog.dismiss();
                }, (dialog1, which) -> {
                }, R.string.whether_to_hide, WRAP_CONTENT, WRAP_CONTENT, true).show(),
                v7 -> {
                    ToastUtils.show(this, R.string.choose_path_v_3_0_file);
                    Dialog dialog = getFilePickerDialog(pathDir, (picker, path) -> {

                        if (path == null) return;
                        SQLite3 db = SQLite3.open(path);
                        if (db.checkIfCorrupt()) {
                            ToastUtils.show(this, R.string.unsupported_path);
                            return;
                        }
                        String infoStr = getPathV3_0StatisticsInfoStr(db);
                        db.close();

                        // show info dialog
                        Dialog d = new Dialog(this);
                        setDialogAttr(d, false, WRAP_CONTENT, WRAP_CONTENT, true);

                        View v = View.inflate(this, R.layout.path_file_info_view, null);
                        Button confirmBtn = v.findViewById(R.id.confirm);
                        confirmBtn.setOnClickListener(v1 -> d.dismiss());

                        TextView infoTV = v.findViewById(R.id.tv);
                        infoTV.setText(infoStr);

                        d.setCanceledOnTouchOutside(false);
                        d.setContentView(v);
                        d.show();

                    });
                    dialog.show();
                }
        };
        Button[] buttons = new Button[textsRes.length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Button(this);
            buttons[i].setText(textsRes[i]);
            ll.addView(buttons[i]);
            buttons[i].setOnClickListener(onClickListeners[i]);
        }
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        ll.setLayoutParams(new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        sv.addView(ll);
        moreOptionsDialog.setContentView(sv);
        moreOptionsDialog.setCanceledOnTouchOutside(true);
        moreOptionsDialog.show();
    }

    private int getSelectedStatisticsIntData(SQLite3 db, int mark) {
        int r = 0;
        try {
            Statement statement = db.compileStatement("SELECT COUNT(*)\n" +
                    "FROM path\n" +
                    "WHERE mark is ?");
            statement.reset();
            statement.bind(1, mark);
            Cursor cursor = statement.getCursor();
            cursor.step();
            r = cursor.getInt(0);
            statement.release();
        } catch (Exception e) {
            Common.showException(e, this);
        }
        return r;
    }

    private String getPathV3_0StatisticsInfoStr(SQLite3 db) {
        /*
         * infos:
         * create time
         * total recorded points number,
         * total recorded paths number,
         * drawing paths number,
         * erasing paths number,
         * undo times,
         * redo times
         */

        int[] totalPointsNum = {0};
        int drawingPathsNum = getSelectedStatisticsIntData(db, 0x01);
        int erasingPathsNum = getSelectedStatisticsIntData(db, 0x11);
        int totalPathsNum = drawingPathsNum + erasingPathsNum;
        String[] createTime = new String[1];
        db.exec("SELECT create_timestamp\n" +
                "FROM info", contents -> {
            createTime[0] = new Date(Long.parseLong(contents[0])).toString();
            return 0;
        });
        db.exec("SELECT COUNT(*)\n" +
                "FROM path\n" +
                "WHERE mark IS NOT 0x01\n" +
                "  AND mark IS NOT 0x11\n" +
                "  AND mark IS NOT 0x20\n" +
                "  AND mark IS NOT 0x30", contents -> {
            totalPointsNum[0] = Integer.parseInt(contents[0]);
            return 0;
        });

        return getString(R.string.path_info_string
                , createTime[0]
                , totalPointsNum[0]
                , totalPathsNum
                , drawingPathsNum
                , erasingPathsNum
                , getSelectedStatisticsIntData(db, 0x20)
                , getSelectedStatisticsIntData(db, 0x30));
    }

    /**
     * Get the dialog containing file picker relative layout, call {@link Dialog#show()} to show.
     *
     * @param initialPath          initial path
     * @param onPickedResultAction callback called when on picked
     * @return dialog
     */
    @NotNull
    private Dialog getFilePickerDialog(File initialPath, FilePickerRL.OnPickedResultCallback onPickedResultAction) {
        Dialog dialog = new Dialog(this);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        FilePickerRL fp = new FilePickerRL(this, FilePickerRL.TYPE_PICK_FILE, initialPath, picker -> dialog.dismiss(), (picker, path) -> {
            dialog.dismiss();
            onPickedResultAction.result(picker, path);
        }, null);
        setFilePickerDialog(dialog, fp);
        return dialog;
    }

    private void setLayer() {
        Dialog dialog = new Dialog(this);
        View view = View.inflate(this, R.layout.layer_layout, null);
        dialog.setContentView(view);
        DialogUtil.setDialogAttr(dialog, false
                , ((int) (width * .8F)), ((int) (height * .8F)), true);
        dialog.show();
    }

    private void importPath(@Nullable Dialog moreOptionsDialog, File pathDir) {
        new PermissionRequester(() -> {
            Dialog dialog = getFilePickerDialog(pathDir, (picker, path) -> {

                if (currentInternalPathFile.getAbsolutePath().equals(path)) {
                    ToastUtils.show(this, R.string.can_not_import_itself);
                    return;
                }
                View inflate = View.inflate(this, R.layout.type_importing_sleep_time_view, null);
                EditText progressET = inflate.findViewById(R.id.progress);
                progressET.setText(R.string.zero);
                SeekBar progressSB = inflate.findViewById(R.id.type_importing_sleep_time);
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                AtomicInteger speedDelayMillis = new AtomicInteger();
                progressSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            progressET.setText(String.valueOf(progress));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                progressET.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        int parseInt = 0;
                        try {
                            parseInt = Integer.parseInt(s.toString());
                        } catch (NumberFormatException ignored) {
                        }
                        progressSB.setProgress(parseInt);
                        if (parseInt < 0) {
                            parseInt = 0;
                        }
                        speedDelayMillis.set(parseInt);
                    }
                });
                Runnable importAction = () -> {
                    Dialog importPathFileProgressDialog = new Dialog(this);
                    setDialogAttr(importPathFileProgressDialog, false, MATCH_PARENT, WRAP_CONTENT, true);
                    importPathFileProgressDialog.setCanceledOnTouchOutside(false);
                    RelativeLayout progressRelativeLayout = View.inflate(this, R.layout.progress_bar, null).findViewById(R.id.rl);
                    progressRelativeLayout.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                    importPathFileProgressDialog.show();
                    importPathFileProgressDialog.setContentView(progressRelativeLayout, new ViewGroup.LayoutParams(((int) (((float) width) * .95F)), WRAP_CONTENT));
                    TextView tv = progressRelativeLayout.findViewById(R.id.progress_tv);
                    tv.setText(R.string.importing);
                    ProgressBar progressBar = progressRelativeLayout.findViewById(R.id.progress_bar);
                    TextView pTextView = progressRelativeLayout.findViewById(R.id.progress_bar_title);

                    AsyncTryDo tryDo = new AsyncTryDo();

                    pv.asyncImportPathFile(new File(path), () -> {
                        runOnUiThread(importPathFileDoneAction);
                        importPathFileProgressDialog.dismiss();
                    }, aFloat -> {

                        tryDo.tryDo((self, notifier) -> {
                            runOnUiThread(() -> {
                                progressBar.setProgress((int) (aFloat * 100F));
                                pTextView.setText(getString(R.string.progress_tv, aFloat * 100F));
                                notifier.finish();
                            });
                        });

                    }, speedDelayMillis.get());
                    if (moreOptionsDialog != null) {
                        moreOptionsDialog.dismiss();
                    }
                };

                adb.setPositiveButton(R.string.confirm, (dialog1, which) -> importAction.run()).setNegativeButton(R.string.cancel, (dialog1, which) -> {
                    speedDelayMillis.set(0);
                    importAction.run();
                }).setTitle(R.string.type_importing_sleep_time).setView(inflate);
                AlertDialog ad = adb.create();
                setDialogAttr(ad, false, WRAP_CONTENT, MATCH_PARENT, true);
                ad.show();

            });
            dialog.show();
        }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
    }

    private void setFilePickerDialog(@NotNull Dialog dialog, FilePickerRL filePickerRL) {
        dialog.setOnKeyListener((dialog1, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    filePickerRL.previous();
                }
            }
            return false;
        });
        setDialogAttr(dialog, false, ((int) (((float) width) * .8)), ((int) (((float) height) * .8)), true);
        dialog.setContentView(filePickerRL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 23 && grantResults[0] == 0) {
            moreOptions();
        }
    }

    public void recover() {
        if (!this.mainDrawingBoardNotDisplay) {
            try {
                this.wm.addView(fbLinearLayout, lp2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        System.out.println("kill...");
        outState.putString("internalPathFile", currentInternalPathFile.getPath());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("internalPathFile")) {
            String lastInternalPath = savedInstanceState.getString("internalPathFile");
            ToastUtils.show(this, getString(R.string.str, lastInternalPath));
        }
    }

    public static abstract class AbstractCheckOverlayPermission {
        public AbstractCheckOverlayPermission(AppCompatActivity activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(activity)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, RequestCode.REQUEST_OVERLAY);
                    denied();
                } else {
                    granted();
                }
            }
        }

        public abstract void granted();

        public abstract void denied();
    }

    void onGlobalConfigurationChanged(@NotNull Configuration newConfig) {
        /*final int orientation = newConfig.orientation;
        Log.i(TAG, "onGlobalConfigurationChanged: orientation: " + orientation);
        ToastUtils.show(this, "orientation changed: " + orientation);

        pv.refreshBitmap(
                DisplayUtil.dip2px(this, newConfig.screenWidthDp),
                DisplayUtil.dip2px(this, newConfig.screenHeightDp)
        );*/
    }
}
