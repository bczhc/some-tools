package pers.zhc.tools.floatingdrawing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.InputType;
import android.text.Selection;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Expression;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;
import pers.zhc.tools.filepicker.FilePickerRL;
import pers.zhc.tools.utils.BitmapUtil;
import pers.zhc.tools.utils.ColorUtils;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static pers.zhc.tools.utils.DialogUtil.setDialogAttr;

public class FloatingDrawingBoardMainActivity extends BaseActivity {
    static Map<Long, Context> longActivityMap;//memory leak?
    private final float[][] hsvaFloats = new float[3][0];
    RequestPermissionInterface requestPermissionInterface = null;
    boolean mainDrawingBoardNotDisplay = false;
    private WindowManager wm = null;
    private LinearLayout fbLL;
    private PaintView pv;
    private int width;
    private Bitmap icon;
    private int height;
    private int TVsColor = Color.WHITE, textsColor = Color.GRAY;
    private boolean invertColorChecked = false;
    private File currentInternalPathFile = null;
    private Runnable importPathFileDoneAction;
    private long currentInstanceMillisecond;
    private TextView[] childTVs;
    private Switch fbSwitch;
    private String[] strings;
    private WindowManager.LayoutParams lp;
    private WindowManager.LayoutParams lp2;
    private ImageView iv;
    private LinearLayout optionsLL;
    private FloatingViewOnTouchListener.ViewSpec fbMeasuredSpec;
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

    public static void setSelectedET_currentMillisecond(EditText et) {
        @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        et.setText(String.format(et.getContext().getString(R.string.tv), format));
        Selection.selectAll(et.getText());
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
            if (savedInstanceState.containsKey("internalPathFile")) {
                String lastInternalPath = savedInstanceState.getString("internalPathFile");
                ToastUtils.show(this, getString(R.string.tv, lastInternalPath));
            }
        }
        new PermissionRequester(() -> {
        }).requestPermission(this, Manifest.permission.INTERNET, RequestCode.REQUEST_PERMISSION_INTERNET);
        init();
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
        currentInstanceMillisecond = System.currentTimeMillis();
        internalPathDir = getFilesDir().getPath() + File.separatorChar + "path";
        File currentInternalPathDir = new File(internalPathDir);
        if (!currentInternalPathDir.exists()) {
            System.out.println("currentInternalPathDir.mkdirs() = " + currentInternalPathDir.mkdirs());
        }
        currentInternalPathFile = new File(currentInternalPathDir.getPath() + File.separatorChar + currentInstanceMillisecond + ".path");
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
        Arrays.fill(hsvaFloats, null);
        Point point = new Point();
        /*//noinspection deprecation
        width = this.getWindowManager().getDefaultDisplay().getWidth();
        //noinspection deprecation
        height = this.getWindowManager().getDefaultDisplay().getHeight();*/
        getWindowManager().getDefaultDisplay().getSize(point);
        width = point.x;
        height = point.y;
        dpi = (int) getResources().getDisplayMetrics().density;
        View keepNotBeingKilledView = new View(this);
        keepNotBeingKilledView.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        pv = new PaintView(this, width, height, currentInternalPathFile);
        pv.setOnColorChangedCallback(color -> {
            if (this.panelColorFollowPainting) {
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                float[] hsva = new float[4];
                System.arraycopy(hsv, 0, hsva, 0, hsv.length);
                hsva[3] = Color.alpha(color);
                setPanelColor(color, hsva);
            }
        });
        pv.setStrokeWidth(10F);
        pv.setEraserStrokeWidth(10F);
        pv.setPaintColor(Color.RED);
        wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        setSwitch();
        new Thread(() -> uploadPaths(this)).start();
        strings = getResources().getStringArray(R.array.btn_string);
        childTVs = new TextView[strings.length];
        // 更新悬浮窗位置
        pv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
        lp.width = this.width;
        lp.height = this.height;
        //noinspection deprecation
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        lp2.width = /*(int) (width * proportionX)*/WindowManager.LayoutParams.WRAP_CONTENT;
        lp2.height = WindowManager.LayoutParams.WRAP_CONTENT;
        //noinspection deprecation
        lp2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        fbLL = new LinearLayout(this);
        fbLL.setOrientation(LinearLayout.VERTICAL);
        fbLL.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        System.gc();
        InputStream inputStream = getResources().openRawResource(R.raw.db);
        icon = BitmapFactory.decodeStream(inputStream);
        try {
            inputStream.close();
        } catch (IOException ignored) {
        }
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
        this.optionsLL = new LinearLayout(this);
        optionsLL.setOrientation(LinearLayout.VERTICAL);
        optionsLL.setGravity(Gravity.CENTER);
        optionsLL.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        iv = new ImageView(this);
        float proportionX = ((float) 75) / ((float) 720);
        float proportionY = ((float) 75) / ((float) 1360);
        this.iv.setLayoutParams(new ViewGroup.LayoutParams((int) (width * proportionX), (int) (height * proportionY)));
//        iv.setImageBitmap(icon);
        iv.setImageIcon(Icon.createWithResource(this, R.drawable.ic_db));
        iv.setOnClickListener(v -> {
            fbLL.removeAllViews();
            fbLL.addView(optionsLL);
            measureFB_LL();
        });
        this.fbMeasuredSpec = new FloatingViewOnTouchListener.ViewSpec();
        FloatingViewOnTouchListener floatingViewOnTouchListener = new FloatingViewOnTouchListener(lp2, wm, fbLL, width, height, fbMeasuredSpec);
        iv.setOnTouchListener(floatingViewOnTouchListener);
        for (int i = 0; i < strings.length; i++) {
            childTVs[i] = new TextView(this);
            LinearLayout smallLL = new LinearLayout(this);
            smallLL.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1F));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (height / strings.length * .7));
            layoutParams.setMargins(0, 0, 0, 5);
            childTVs[i].setLayoutParams(layoutParams);
            childTVs[i].setText(strings[i]);
            childTVs[i].setBackgroundColor(TVsColor);
            childTVs[i].setTextColor(textsColor);
            childTVs[i].setOnTouchListener(floatingViewOnTouchListener);
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
                        fbLL.removeAllViews();
                        fbLL.addView(iv);
                        measureFB_LL();
                        wm.updateViewLayout(fbLL, lp2);
                        break;
                    case 1:
                        toggleDrawAndControlMode();
                        break;
                    case 2:
                        Dialog dialog = new Dialog(this);
                        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.color.transparent);
                        dialog.getWindow().setAttributes(new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                                , WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 0, PixelFormat.RGBX_8888));
                        setDialogAttr(dialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
                        HSVAColorPickerRL hsvColorPickerRL = new HSVAColorPickerRL(this, pv.getColor(), ((int) (width * .8)), ((int) (height * .4)), hsvaFloats[0], dialog) {
                            @Override
                            void onPickedAction(int color, float[] hsva) {
                                pv.setPaintColor(color);
                                hsvaFloats[0] = hsva;
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
                        DialogUtil.createConfirmationAD(this, (dialog1, which) -> {
                            pv.clearAll();
                            pv.clearTouchRecordOSContent();
                        }, (dialog1, which) -> {
                        }, R.string.whether_to_clear, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).show();
                        break;
                    case 8:
                        pickScreenColor();
                        break;
                    case 9:
                        Dialog c = new Dialog(this);
                        LinearLayout inflate = View.inflate(this, R.layout.panel_view, null).findViewById(R.id.ll);
                        Button panelColorBtn = inflate.findViewById(R.id.panel_color);
                        panelColorBtn.setOnClickListener(v2 -> {
                            Dialog TVsColorDialog = new Dialog(FloatingDrawingBoardMainActivity.this);
                            setDialogAttr(TVsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
                            HSVAColorPickerRL TVsColorPicker = new HSVAColorPickerRL(this, TVsColor, ((int) (width * .8)), ((int) (height * .4)), hsvaFloats[1], TVsColorDialog) {
                                @Override
                                void onPickedAction(int color, float[] hsva) {
                                    setPanelColor(color, hsva);
                                }
                            };
                            TVsColorDialog.setContentView(TVsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
                            TVsColorDialog.show();
                        });
                        Button textsColorBtn = inflate.findViewById(R.id.text_color);
                        textsColorBtn.setEnabled(!this.invertColorChecked);
                        textsColorBtn.setOnClickListener(v2 -> {
                            Dialog textsColorDialog = new Dialog(FloatingDrawingBoardMainActivity.this);
                            setDialogAttr(textsColorDialog, true, ((int) (((float) width) * .8)), ((int) (((float) height) * .4)), true);
                            HSVAColorPickerRL textsColorPicker = new HSVAColorPickerRL(this, textsColor, ((int) (width * .8)), ((int) (height * .4)), hsvaFloats[2], textsColorDialog) {
                                @Override
                                void onPickedAction(int color, float[] hsva) {
                                    for (TextView childTV : childTVs) {
                                        if (!invertColorChecked) textsColor = color;
                                        childTV.setTextColor(textsColor);
                                    }
                                    hsvaFloats[2] = hsva;
                                }
                            };
                            textsColorDialog.setContentView(textsColorPicker, new ViewGroup.LayoutParams(((int) (width * .8)), ((int) (height * .4))));
                            textsColorDialog.show();
                        });
                        textsColorBtn.setText(R.string.text_color);
                        Switch whetherTextColorIsInverted = inflate.findViewById(R.id.invert_text_color);
                        whetherTextColorIsInverted.setChecked(invertColorChecked);
                        whetherTextColorIsInverted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            textsColorBtn.setEnabled(!isChecked);
                            invertColorChecked = isChecked;
                            for (TextView childTV : childTVs) {
                                childTV.setTextColor(textsColor = ColorUtils.invertColor(TVsColor));
                            }
                        });
                        Switch followPaintingColor = inflate.findViewById(R.id.follow_painting_color);
                        followPaintingColor.setOnCheckedChangeListener((buttonView, isChecked) -> this.panelColorFollowPainting = isChecked);
                        c.setContentView(inflate);
                        DialogUtil.setDialogAttr(c, false, ViewGroup.LayoutParams.WRAP_CONTENT
                        , ViewGroup.LayoutParams.WRAP_CONTENT, true);
                        c.show();
                        break;
                    case 10:
                        moreOptions();
                        break;
                    case 11:
                        DialogUtil.createConfirmationAD(this, (dialog1, which) -> exit(), (dialog1, which) -> {
                        }, R.string.whether_to_exit, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).show();
//                            pv.scaleCanvas((float) (width * 2), ((float) (height * 2)));
                        break;
                }
                System.out.println("i = " + finalI);
            });
            smallLL.setGravity(Gravity.CENTER);
            smallLL.addView(childTVs[i]);
            optionsLL.addView(smallLL);
        }
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
                    } else ToastUtils.show(this, R.string.request_permission_error);
                } else ToastUtils.show(this, R.string.please_grant_permission);
            }
            startFloatingWindow();
        };
        TextView checkForUpdateTV = findViewById(R.id.check_for_update_tv);
        checkForUpdateTV.setOnClickListener(v -> super.checkForUpdate(update -> {
            if (!update) {
                runOnUiThread(() -> ToastUtils.show(this, R.string.current_version_is_the_latest));
            }
        }));
    }

    private void setPanelColor(int color, float[] hsva) {
        for (TextView childTV : childTVs) {
            TVsColor = color;
            childTV.setBackgroundColor(TVsColor);
            if (invertColorChecked)
                childTV.setTextColor(textsColor = ColorUtils.invertColor(TVsColor));
        }
        hsvaFloats[1] = hsva;
    }

    private void setupProjection() {
        mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null && captureScreenResultData != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(RESULT_OK, captureScreenResultData);
        } else ToastUtils.show(this, R.string.acquire_service_failed);
    }

    private void pickScreenColor() {
        if (mediaProjectionManager == null) {
            initCapture();
        } else
            captureScreen();
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
                    Log.d("d", "image! " + image);
                    /*int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane plane = image.getPlanes()[0];
                    final ByteBuffer buffer = plane.getBuffer();
                    int pixelStride = plane.getPixelStride();
                    int rowStride = plane.getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    image.close();*/
                    System.out.println("image = " + image);
                    ToastUtils.show(this, getString(R.string.ok) + image);
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
        if (fileList == null) return 0;
        long cacheSize = 0L;
        File[] listFiles = null;
        if (internalPathDir != null) {
            listFiles = new File(internalPathDir).listFiles();
        }
        if (listFiles != null) {
            for (File file : listFiles) {
                try {
                    String fileName;
                    String fileNameExtension = file.getName();
                    if (!fileNameExtension.matches(".*\\..*")) fileName = fileNameExtension;
                    else {
                        int index = fileNameExtension.lastIndexOf('.');
                        fileName = fileNameExtension.substring(0, index);
                    }
                    if (FloatingDrawingBoardMainActivity.longActivityMap.containsKey(Long.parseLong(fileName))) {
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                }
                fileList.add(file);
            }
        }
        File crashPath = new File(Common.getExternalStoragePath(this) + File.separatorChar
                + this.getString(R.string.some_tools_app) + File.separatorChar + this.getString(R.string.crash));
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
        fbLL.measure(mode, mode);
        fbMeasuredSpec.width = fbLL.getMeasuredWidth();
        fbMeasuredSpec.height = fbLL.getMeasuredHeight();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setSwitch() {
        fbSwitch = findViewById(R.id.f_b_switch);
        fbSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                new CheckOverlayPermission(this) {
                    @Override
                    public void granted() {
                        startFloatingWindow();
                    }

                    @Override
                    public void denied() {
                        fbSwitch.setChecked(false);
                    }
                };
            } else stopFloatingWindow();
        });
    }

    @SuppressLint({"ClickableViewAccessibility"})
    void startFloatingWindow() {
        pv.setOS(currentInternalPathFile, true);
        if (!longActivityMap.containsKey(currentInstanceMillisecond)) {
            longActivityMap.put(currentInstanceMillisecond, this);
        }
        try {
            wm.addView(pv, lp);
            mainDrawingBoardNotDisplay = false;
            fbLL.addView(iv);
            measureFB_LL();
            this.wm.addView(fbLL, lp2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void toggleDrawAndControlMode() {
        if (drawMode) {
            //noinspection deprecation
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            wm.updateViewLayout(pv, lp);
            childTVs[1].setText(R.string.controlling);
            strings[1] = getString(R.string.controlling);
            drawMode = false;
        } else {
            //noinspection deprecation
            lp.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            wm.updateViewLayout(pv, lp);
            childTVs[1].setText(R.string.drawing);
            strings[1] = getString(R.string.drawing);
            drawMode = true;
        }
    }

    private void exit() {
        stopFloatingWindow();
        new Thread(() -> uploadPaths(this)).start();
        FloatingDrawingBoardMainActivity.longActivityMap.remove(currentInstanceMillisecond);
        this.fbSwitch.setChecked(false);
        pv.closePathRecorderOS();
    }

    private void hide() {
        try {
            wm.removeViewImmediate(fbLL);
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
//            Intent intent = new Intent(this, NotificationClickReceiver.class);
            Intent intent = new Intent();
            intent.setAction("pers.zhc.tools.START_FB");
            intent.setPackage(getPackageName());
            intent.putExtra("millisecond", currentInstanceMillisecond);
            boolean isDrawMode = this.childTVs[1].getText().equals(getString(R.string.drawing_mode));
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
                    .setSmallIcon(R.mipmap.ic_launcher);
            Intent intent = new Intent();
            intent.setAction("pers.zhc.tools.START_FB");
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
        LinearLayout mainLL = View.inflate(this, R.layout.change_stroke_width_view, null).findViewById(R.id.ll);
        RadioGroup rg = mainLL.findViewById(R.id.rg);
        RadioButton[] radioButtons = new RadioButton[2];
        for (int i = 0; i < radioButtons.length; i++) {
            radioButtons[i] = (RadioButton) rg.getChildAt(i);
        }
        radioButtons[pv.isEraserMode ? 1 : 0].setChecked(true);
        CheckBox lockStrokeCB = mainLL.findViewById(R.id.cb);
        lockStrokeCB.setChecked(pv.isLockingStroke());
        lockStrokeCB.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pv.setLockStrokeMode(isChecked);
            pv.lockStroke();
        });
        StrokeWatchView strokeWatchView = new StrokeWatchView(this);
        strokeWatchView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        SeekBar sb = mainLL.findViewById(R.id.sb);
        TextView tv = mainLL.findViewById(R.id.tv);
        final int[] checked = {pv.isEraserMode ? R.id.radio2 : R.id.radio1};
        tv.setOnClickListener(v -> {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            EditText et = new EditText(this);
            et.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                try {
                    float edit = Float.parseFloat(et.getText().toString());
                    double a = Math.log(edit) / Math.log(1.07D);
                    strokeWatchView.change((edit * pv.getCanvas().getScale()), pv.getColor());
                    sb.setProgress((int) a);
                    if (checked[0] == R.id.radio1)
                        pv.setStrokeWidth(edit);
                    else if (checked[0] == R.id.radio2) pv.setEraserStrokeWidth(edit);
                    pv.lockStroke();
                    tv.setText(getString(R.string.stroke_width_info, edit, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
                } catch (Exception e) {
                    Common.showException(e, this);
                }
            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
            }).setTitle(R.string.type_stroke_width__pixels).setView(et);
            AlertDialog ad = adb.create();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            } else //noinspection deprecation
                Objects.requireNonNull(ad.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
            DialogUtil.setADWithET_autoShowSoftKeyboard(et, ad);
            ad.show();
        });
        float pvStrokeWidthInUse = pv.getStrokeWidthInUse();
        strokeWatchView.change((pvStrokeWidthInUse * pv.getCanvas().getScale()), pv.getColor());
        tv.setText(getString(R.string.stroke_width_info, pvStrokeWidthInUse, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
        sb.setProgress((int) (Math.log(pvStrokeWidthInUse) / Math.log(1.07D)));
        sb.setMax(100);
        setDialogAttr(mainDialog, false, ((int) (((float) width) * .8F)), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float pow = (float) Math.pow(1.07D, progress);
                    if (checked[0] == R.id.radio1) {
                        pv.setStrokeWidth(pow);
                    } else {
                        pv.setEraserStrokeWidth(pow);
                    }
                    pv.lockStroke();
                    tv.setText(getString(R.string.stroke_width_info, pow, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
                    strokeWatchView.change((pow * pv.getCanvas().getScale()), pv.getColor());
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
            float strokeWidth = (checkedId == R.id.radio1) ? pv.getStrokeWidth() : pv.getEraserStrokeWidth();
            strokeWatchView.change(strokeWidth * pv.getCanvas().getScale(), pv.getColor());
            tv.setText(getString(R.string.stroke_width_info, strokeWidth, pv.getZoomedStrokeWidthInUse(), pv.getScale() * 100F));
            sb.setProgress((int) (Math.log(strokeWidth) / Math.log(1.07D)));
        });
        mainLL.addView(strokeWatchView);
        mainDialog.setContentView(mainLL, new ViewGroup.LayoutParams(((int) (((float) width) * .8F)), ViewGroup.LayoutParams.WRAP_CONTENT));
        mainDialog.show();
    }

    private void stopFloatingWindow() {
        fbLL.removeAllViews();
        try {
            wm.removeViewImmediate(fbLL);
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
        sv.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setDialogAttr(moreOptionsDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        LinearLayout ll = new LinearLayout(this);
        int[] textsRes = new int[]{
                R.string.import_image,
                R.string.export_image,
                R.string.import_path,
                R.string.export_path,
                R.string.reset_transform,
                R.string.manage_layer,
                R.string.hide_panel
        };
        File fbDir = new File(Common.getExternalStoragePath(this) + File.separator + getString(R.string.drawing_board));
        if (!fbDir.exists()) System.out.println("d.mkdir() = " + fbDir.mkdir());
        File pathDir = new File(fbDir.toString() + File.separator + "path");
        if (!pathDir.exists()) System.out.println("pathDir.mkdir() = " + pathDir.mkdir());
        File imageDir = new File(fbDir.toString() + File.separator + "image");
        if (!imageDir.exists()) System.out.println("imageDir.mkdir() = " + imageDir.mkdir());
        View.OnClickListener[] onClickListeners = new View.OnClickListener[]{
                v0 -> new PermissionRequester(() -> {
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
                        editTexts[0].setText(getString(R.string.tv, "0"));
                        editTexts[1].setText(getString(R.string.tv, "0"));
                        editTexts[2].setText(getString(R.string.tv, String.valueOf(imageBitmapWidth)));
                        editTexts[3].setText(getString(R.string.tv, String.valueOf(imageBitmapHeight)));
                        linearLayout.addView(infoTV);
                        AlertDialog importImageOptionsDialog = importImageOptionsDialogBuilder.setView(linearLayout)
                                .setTitle(R.string.set__top__left__scaled_width__scaled_height)
                                .setPositiveButton(R.string.confirm, (dialog1, which) -> {
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
                    }, null);
                    setFilePickerDialog(dialog, filePickerRL);
                }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE),
                v1 -> new PermissionRequester(() -> {
                    LinearLayout linearLayout = View.inflate(this, R.layout.export_image_layout, null)
                            .findViewById(R.id.root);
                    EditText fileNameET = linearLayout.findViewById(R.id.file_name);
                    setSelectedET_currentMillisecond(fileNameET);
                    EditText widthET = linearLayout.findViewById(R.id.width);
                    EditText heightET = linearLayout.findViewById(R.id.height);
                    Point point = new Point();
                    pv.bitmapResolution(point);
                    final int[] imageW = {point.x};
                    final int[] imageH = {point.y};
                    widthET.setText(String.valueOf(point.x));
                    heightET.setText(String.valueOf(point.y));
                    widthET.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    heightET.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    AlertDialog ad = adb.setTitle(R.string.export_image)
                            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                                Expression expression = new Expression();
                                expression.setExpressionString(widthET.getText().toString());
                                imageW[0] = ((int) expression.calculate());
                                expression.setExpressionString(heightET.getText().toString());
                                imageH[0] = ((int) expression.calculate());
                                if (Double.isNaN(imageW[0]) || Double.isNaN(imageH[0]) || imageW[0] <= 0 || imageH[0] <= 0) {
                                    ToastUtils.show(FloatingDrawingBoardMainActivity.this, R.string.please_type_correct_value);
                                    moreOptionsDialog.dismiss();
                                    return;
                                }
                                File imageFile = new File(imageDir.toString() + File.separator + fileNameET.getText().toString() + ".png");
                                pv.exportImg(imageFile, imageW[0], imageH[0]);
                                moreOptionsDialog.dismiss();
                            }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                            }).setView(linearLayout).create();
                    DialogUtil.setDialogAttr(ad, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    DialogUtil.setADWithET_autoShowSoftKeyboard(fileNameET, ad);
                    ad.show();
                }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE),
                v2 -> importPath(moreOptionsDialog, pathDir),
                v3 -> new PermissionRequester(() -> {
                    EditText et = new EditText(this);
                    setSelectedET_currentMillisecond(et);
                    AlertDialog.Builder adb = new AlertDialog.Builder(this);
                    AlertDialog alertDialog = adb.setPositiveButton(R.string.confirm, (dialog, which) -> {
                        File pathFile = new File(pathDir.toString() + File.separator + et.getText().toString() + ".path");
                        try {
                            if (!currentInternalPathFile.exists()) {
                                throw new FileNotFoundException(getString(R.string.native_file_not_exist));
                            }
                            FileU.FileCopy(currentInternalPathFile, pathFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (pathFile.exists()) {
                            ToastUtils.show(this, getString(R.string.saving_success) + "\n" + pathFile.toString());
                            new Thread(() -> uploadPaths(this)).start();
                        } else
                            ToastUtils.show(this, getString(R.string.concat, getString(R.string.saving_failed), et.toString()));
                        moreOptionsDialog.dismiss();
                    }).setNegativeButton(R.string.cancel, (dialog, which) -> {
                    }).setTitle(R.string.type_file_name).setView(et).create();
                    setDialogAttr(alertDialog, false, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                    DialogUtil.setADWithET_autoShowSoftKeyboard(et, alertDialog);
                    alertDialog.show();
                }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE),
                v4 -> pv.resetTransform(),
                v5 -> setLayer(),
                v6 -> DialogUtil.createConfirmationAD(this, (dialog1, which) -> {
                    hide();
                    moreOptionsDialog.dismiss();
                }, (dialog1, which) -> {
                }, R.string.whether_to_hide, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).show()
        };
        Button[] buttons = new Button[textsRes.length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Button(this);
            buttons[i].setText(textsRes[i]);
            ll.addView(buttons[i]);
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

    private void setLayer() {
        Dialog dialog = new Dialog(this);
        View view = View.inflate(this, R.layout.layer_layout, null);
        @SuppressWarnings("unused") LinearLayout linearLayout = view.findViewById(R.id.ll);
        dialog.setContentView(view);
        DialogUtil.setDialogAttr(dialog, false
                , ((int) (width * .8F)), ((int) (height * .8F)), true);
        dialog.show();
    }

    private void importPath(@Nullable Dialog moreOptionsDialog, File pathDir) {
        new PermissionRequester(() -> {
            Dialog dialog = new Dialog(this);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            FilePickerRL filePickerRL = new FilePickerRL(this, FilePickerRL.TYPE_PICK_FILE, pathDir, dialog::dismiss, s -> {
                dialog.dismiss();
                if (currentInternalPathFile.getAbsolutePath().equals(s)) {
                    ToastUtils.show(this, R.string.can_not_import_itself);
                    return;
                }
                Dialog importPathFileProgressDialog = new Dialog(this);
                setDialogAttr(importPathFileProgressDialog, false, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
                importPathFileProgressDialog.setCanceledOnTouchOutside(false);
                RelativeLayout progressRL = View.inflate(this, R.layout.progress_bar, null).findViewById(R.id.rl);
                progressRL.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                importPathFileProgressDialog.show();
                importPathFileProgressDialog.setContentView(progressRL, new ViewGroup.LayoutParams(((int) (((float) width) * .95F)), ViewGroup.LayoutParams.WRAP_CONTENT));
                TextView tv = progressRL.findViewById(R.id.progress_tv);
                tv.setText(R.string.importing);
                ProgressBar progressBar = progressRL.findViewById(R.id.progress_bar);
                TextView pTV = progressRL.findViewById(R.id.progress_bar_title);
                pv.importPathFile(new File(s), () -> {
                    this.hsvaFloats[0] = null;
                    runOnUiThread(importPathFileDoneAction);
                    importPathFileProgressDialog.dismiss();
                }, aFloat -> runOnUiThread(() -> {
                    progressBar.setProgress(aFloat.intValue());
                    pTV.setText(getString(R.string.progress_tv, aFloat));
                }));
                if (moreOptionsDialog != null) {
                    moreOptionsDialog.dismiss();
                }
            }, null);
            setFilePickerDialog(dialog, filePickerRL);
        }).requestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                , RequestCode.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);
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
        if (requestCode == 23 && grantResults[0] == 0) moreOptions();
    }

    public void recover() {
        if (!this.mainDrawingBoardNotDisplay) {
            try {
                this.wm.addView(fbLL, lp2);
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
            ToastUtils.show(this, getString(R.string.tv, lastInternalPath));
        }
    }


    public static abstract class CheckOverlayPermission {
        public CheckOverlayPermission(Activity activity) {
            if (!Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, RequestCode.REQUEST_OVERLAY);
                denied();
            } else granted();
        }

        public abstract void granted();

        public abstract void denied();
    }
}