package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.R;
import pers.zhc.tools.fdb.*;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.*;
import pers.zhc.util.Assertion;
import pers.zhc.util.Random;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * @author bczhc
 */
@SuppressLint("ViewConstructor")
public class PaintView extends View {

    private int height = -1;
    private int width = -1;
    private final Context ctx;
    boolean eraserMode = false;
    private Paint mPaint;
    private Path mPath;
    private Paint eraserPaint;
    private Paint mPaintRef = null;
    private final ArrayList<Layer> layerArray = new ArrayList<>();
    private LayerPathSaver layerPathSaverRef;

    private Layer layerRef;
    private Canvas mCanvas;
    private CanvasTransformer canvasTransformer;
    private Bitmap bitmapRef;
    /**
     * 上次的坐标
     */
    private float mLastX, mLastY;
    private Paint mBitmapPaint;
    /**
     * 使用LinkedList 模拟栈，来保存 Path
     */
    private LinkedList<PathBean> undoListRef, redoListRef;
    private GestureResolver gestureResolver;
    private boolean dontDrawWhileImporting = false;
    private boolean lockStrokeEnabled = false;
    /**
     * locked absolute drawing stroke width
     */
    private float lockedDrawingStrokeWidth;
    /**
     * locked absolute eraser stroke width
     */
    private float lockedEraserStrokeWidth;
    private OnColorChangedCallback onColorChangedCallback = null;
    private Bitmap transBitmap;
    private Canvas transCanvas;
    private CanvasTransformer transCanvasTransformer;

    private PathSaver defaultTmpPathSaver;
    private PathSaver pathSaver = null;

    private OnScreenDimensionChangedListener onScreenDimensionChangedListener = null;

    private final Matrix defaultTransformation = new Matrix();

    private boolean moveTransformationEnabled = true;
    private boolean zoomTransformationEnabled = true;
    private boolean rotateTransformationEnabled = false;

    private float canvasScale = 1F;

    private float blurRadius = 0F;

    @Nullable
    private OnImportLayerAddedListener onImportLayerAddedListener = null;

    public PaintView(Context context) {
        this(context, null);
    }

    /**
     * For XML inflation
     *
     * @param context context
     * @param attrs   xml attributes
     */
    public PaintView(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
        this.ctx = context;
        init();
    }

    public void setOnColorChangedCallback(OnColorChangedCallback onColorChangedCallback) {
        this.onColorChangedCallback = onColorChangedCallback;
    }

    private void initBitmap(int width, int height, @NotNull Layer layer) {
        System.gc();
        layer.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // due to the bitmap was replaced, we need to re-link some references of the layer
        if (layer == layerRef) {
            updateLayerRefs();
        }
    }

    private void initBitmap(Layer layer) {
        initBitmap(width, height, layer);
    }

    private void setupBitmap(int width, int height, @NotNull Layer layer) {
        /*Matrix matrix = null;
        if (layer.bitmap != null) {
            matrix = canvasTransformer.getMatrix();

            final int prevWidth = layer.bitmap.getWidth();
            final int prevHeight = layer.bitmap.getHeight();

            final int tX = width / 2 - prevWidth / 2;
            final int tY = height / 2 - prevHeight / 2;
            matrix.postTranslate(tX, tY);
        }*/

        initBitmap(width, height, layer);

        /*if (matrix != null) {
            canvasTransformer.setMatrix(matrix);
        }*/
        layer.redrawBitmap(canvasTransformer.getMatrix());
    }

    /**
     * Initialize.
     */
    private void init() {
        mCanvas = new Canvas();
        canvasTransformer = new CanvasTransformer(mCanvas);
        //抗锯齿
        mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

        setEraserMode(false);
        eraserPaint = new Paint();
        eraserPaint.setColor(Color.BLACK);
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        eraserPaint.setAntiAlias(true);
        eraserPaint.setStyle(Paint.Style.STROKE);
        //使画笔更加圆润
        eraserPaint.setStrokeJoin(Paint.Join.ROUND);
        //同上
        eraserPaint.setStrokeCap(Paint.Cap.ROUND);
        eraserPaint.setAlpha(255);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaintRef = mPaint;
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        //使画笔更加圆润
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        //同上
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        this.gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {

            @Contract(pure = true)
            private boolean transformationEnabled() {
                return moveTransformationEnabled || zoomTransformationEnabled || rotateTransformationEnabled;
            }

            @Override
            public void onTwoPointsScroll(float distanceX, float distanceY, MotionEvent event) {
                if (moveTransformationEnabled) {
                    canvasTransformer.absTranslate(distanceX, distanceY);
                    if (transCanvas != null) {
                        transCanvasTransformer.absTranslate(distanceX, distanceY);
                    }
                }
            }

            @Override
            public void onTwoPointsZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float dScale, MotionEvent event) {
                if (zoomTransformationEnabled) {
                    canvasTransformer.absScale(dScale, midPointX, midPointY);
                    canvasScale *= dScale;
                    if (transCanvas != null) {
                        transCanvasTransformer.absScale(dScale, midPointX, midPointY);
                    }
                    setCurrentStrokeWidthWhenLocked();
                }
            }

            @Override
            public void onTwoPointsRotate(MotionEvent event, float firstMidX, float firstMidY, float degrees, float midX, float midY) {
                if (rotateTransformationEnabled) {
                    canvasTransformer.absRotate(degrees, midX, midY);
                    if (transCanvas != null) {
                        transCanvasTransformer.absRotate(degrees, midX, midY);
                    }
                }
            }

            @Override
            public void onTwoPointsUp(MotionEvent event) {
                if (transformationEnabled()) {
                    transBitmap = null;
                    // redraw all layers' bitmaps
                    redrawAllLayerBitmap();
                    postInvalidate();
                }
            }

            @Override
            public void onTwoPointsDown(MotionEvent event) {
                if (moveTransformationEnabled || zoomTransformationEnabled || rotateTransformationEnabled) {
                    mPath = null;
                    if (transBitmap == null) {
                        transBitmap = Bitmap.createBitmap(bitmapRef.getWidth(), bitmapRef.getHeight(), Bitmap.Config.ARGB_8888);
                        transCanvas = new Canvas(transBitmap);
                        transCanvasTransformer = new CanvasTransformer(transCanvas);
                    }
                    if (layerPathSaverRef != null) {
                        layerPathSaverRef.clearTempTable();
                    }
                }
            }

            @Override
            public void onOnePointScroll(float distanceX, float distanceY, MotionEvent event) {

            }

            @Override
            public void onTwoPointsPress(MotionEvent event) {
                if (transBitmap != null) {
                    Canvas c = new Canvas(transBitmap);
                    c.setMatrix(canvasTransformer.getMatrix());
                    for (int i = layerArray.size() - 1; i >= 0; i--) {
                        Layer layer = layerArray.get(i);
                        transCanvas.drawBitmap(layer.bitmap, 0, 0, mBitmapPaint);
                    }
                }
                postInvalidate();
            }
        });

        final File internalPathDir = new File(ctx.getFilesDir(), "path");
        if (!internalPathDir.exists()) {
            if (!internalPathDir.mkdir()) {
                throw new MkdirException();
            }
        }

        File tmpPathFile = new File(internalPathDir, String.valueOf(System.currentTimeMillis()));

        // use an internal temporary PathSaver
        defaultTmpPathSaver = new PathSaver(tmpPathFile.getPath());
        pathSaver = defaultTmpPathSaver;
    }

    public float getDrawingStrokeWidth() {
        return this.mPaint.getStrokeWidth();
    }

    public void setDrawingStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    public int getDrawingColor() {
        return this.mPaint.getColor();
    }

    /**
     * 设置画笔颜色
     */
    public void setDrawingColor(@ColorInt int color) {
        mPaint.setColor(color);
        if (this.onColorChangedCallback != null) {
            onColorChangedCallback.change(color);
        }
    }

    /**
     * 撤销操作
     */
    public void undo() {
        if (!undoListRef.isEmpty()) {
            layerPathSaverRef.undo();

            clearPaint();//清除之前绘制内容
            PathBean lastPb = undoListRef.removeLast();//将最后一个移除
            redoListRef.add(lastPb);//加入 恢复操作
            //遍历，将Path重新绘制到 headCanvas
            if (!dontDrawWhileImporting) {
                for (PathBean pb : undoListRef) {
                    mCanvas.drawPath(pb.path, pb.paint);
                }
                postInvalidate();
            }
        }
    }

    /**
     * 恢复操作
     */
    public void redo() {
        if (!redoListRef.isEmpty()) {
            layerPathSaverRef.redo();

            PathBean pathBean = redoListRef.removeLast();
            mCanvas.drawPath(pathBean.path, pathBean.paint);
            undoListRef.add(pathBean);
            if (!dontDrawWhileImporting) {
                postInvalidate();
            }
        }
    }

    public int getEraserAlpha() {
        return this.eraserPaint.getAlpha();
    }

    public void setEraserAlpha(@IntRange(from = 0, to = 255) int alpha) {
        this.eraserPaint.setAlpha(alpha);
    }

    /**
     * 清空，包括撤销和恢复操作列表
     */
    public void clearAll() {
        clearPaint();
        mLastY = 0f;
        //清空 撤销 ，恢复 操作列表
        redoListRef.clear();
        undoListRef.clear();
    }

    /**
     * 设置橡皮擦模式
     */
    public void setEraserMode(boolean isEraserMode) {
        this.eraserMode = isEraserMode;
        this.mPaintRef = isEraserMode ? eraserPaint : mPaint;
    }

    public boolean isEraserMode() {
        return eraserMode;
    }

    /**
     * 导出图片
     */
    public void exportImg(File f, int exportedWidth, int exportHeight) {
        /*TODO
        Handler handler = new Handler();
        ToastUtils.show(ctx, R.string.saving);
        System.gc();
        Bitmap exportedBitmap = Bitmap.createBitmap(exportedWidth, exportHeight, Bitmap.Config.ARGB_8888);
        Canvas myCanvas = new Canvas(exportedBitmap);
        myCanvas.translate(headCanvas.getStartPointX() * exportedWidth / width
                , headCanvas.getStartPointY() * exportedWidth / width);
        myCanvas.scale(headCanvas.getScale() * exportedWidth / width);
        for (PathBean pathBean : undoList) {
            myCanvas.drawPath(pathBean.path, pathBean.paint);
        }
        //保存图片
        final FileOutputStream[] fileOutputStream = {null};
        new Thread(() -> {
            try {
                fileOutputStream[0] = new FileOutputStream(f);
                if (exportedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream[0])) {
                    fileOutputStream[0].flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Common.showException(e, (Activity) ctx);
            } finally {
                closeStream(fileOutputStream[0]);
                System.gc();
            }
            handler.post(() -> {
                if (f.exists()) {
                    ToastUtils.show(ctx, ctx.getString(R.string.saving_succeeded_dialog) + "\n" + f);
                } else {
                    ToastUtils.show(ctx, R.string.saving_failed);
                }
            });
        }).start();*/
    }

    /**
     * 是否可以撤销
     */
    public boolean canUndo() {
        return undoListRef.isEmpty();
    }

    /**
     * 是否可以恢复
     */
    public boolean canRedo() {
        return redoListRef.isEmpty();
    }

    /**
     * 清除绘制内容
     * 直接绘制白色背景
     */
    private void clearPaint() {
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        postInvalidate();
    }

    /**
     * 触摸事件 触摸绘制
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureResolver.onTouch(event);
        onTouchAction(event.getAction(), event.getX(), event.getY());
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (dontDrawWhileImporting) {
            return;
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (transBitmap == null) {
            if (bitmapRef != null) {
                // 将bitmap绘制在canvas上,最终的显示
                for (int i = layerArray.size() - 1; i >= 0; i--) {
                    Layer layer = layerArray.get(i);
                    canvas.drawBitmap(layer.bitmap, 0, 0, mBitmapPaint);
                }

                if (!dontDrawWhileImporting) {
                    if (mPath != null) {//显示实时正在绘制的path轨迹
                        canvas.setMatrix(canvasTransformer.getMatrix());
                        if (eraserMode) {
                            canvas.drawPath(mPath, eraserPaint);
                        } else {
                            canvas.drawPath(mPath, mPaint);
                        }
                    }
                }
            }
        } else {
            transCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            transCanvas.drawBitmap(bitmapRef, 0, 0, mBitmapPaint);
            canvas.drawBitmap(transBitmap, 0, 0, mBitmapPaint);
        }
    }

    /**
     * 关闭流
     *
     * @param closeable c
     */
    private void closeStream(OutputStream closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Measure width or height
     *
     * @param measureSpec measure spec
     * @return measured size
     */
    private int measure(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int measuredSize = 0;
        switch (specMode) {
            case MeasureSpec.EXACTLY:
                measuredSize = specSize;
                break;
            case MeasureSpec.UNSPECIFIED:
            case MeasureSpec.AT_MOST:
                measuredSize = 200;
                break;
            default:
        }
        return measuredSize;
    }

    /**
     * 测量
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredW = measure(widthMeasureSpec);
        int measuredH = measure(heightMeasureSpec);
        setMeasuredDimension(measuredW, measuredH);

        if (width == -1 && height == -1) {
            // init
            this.width = measuredW;
            this.height = measuredH;
        }

        if (measuredW != width || measuredH != height) {
            // adapt for the change of screen orientation
            this.width = measuredW;
            this.height = measuredH;
            refreshAllLayerBitmap(width, height);
            if (onScreenDimensionChangedListener != null) {
                onScreenDimensionChangedListener.onChange(width, height);
            }
        }
    }

    private void refreshBitmap(int width, int height, Layer layer) {
        setupBitmap(width, height, layer);
        invalidate();
    }

    private void refreshAllLayerBitmap(int width, int height) {
        for (Layer layer : layerArray) {
            setupBitmap(width, height, layer);
        }
        invalidate();
    }

    public enum PathVersion {
        VERSION_1_0,
        VERSION_2_0,
        VERSION_2_1,
        VERSION_3_0,
        Unknown
    }

    @NotNull
    public static PathVersion getPathVersion(@NotNull File f) {
        PathVersion version = null;

        // check paths that use SQLite database
        final SQLite3 db = SQLite3.open(f.getPath());
        if (!db.checkIfCorrupt()) {
            final Statement statement = db.compileStatement("SELECT version FROM info");
            final Cursor cursor = statement.getCursor();
            if (cursor.step()) {
                final String versionString = cursor.getText(0);
                if ("3.0".equals(versionString)) {
                    version = PathVersion.VERSION_3_0;
                } else {
                    version = PathVersion.Unknown;
                }
            } else {
                version = PathVersion.Unknown;
            }
            statement.release();
        }
        db.close();

        if (version != null) {
            return version;
        }

        // check path versions 1.0, 2.0, 2.1
        try {
            FileInputStream is = new FileInputStream(f);
            byte[] buf = new byte[12];
            Common.doAssertion(is.read(buf) == 12);
            if (Arrays.equals(buf, "path ver 2.0".getBytes())) {
                version = PathVersion.VERSION_2_0;
            } else if (Arrays.equals(buf, "path ver 2.1".getBytes())) {
                version = PathVersion.VERSION_2_1;
            } else {
                version = PathVersion.VERSION_1_0;
            }
            is.close();
            return version;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * 导入路径
     *
     * @param f                路径文件
     * @param doneAction       完成回调接口
     * @param progressCallback 进度回调接口 Range: [0-1]
     * @param speedDelayMillis interval of reading per point
     * @param pathVersion      path version
     */
    public void asyncImportPathFile(File f, Runnable doneAction, @Nullable Consumer<Float> progressCallback, int speedDelayMillis, PathVersion pathVersion) {
        new Thread(() -> {
            importPathFile(f, doneAction, progressCallback, speedDelayMillis, pathVersion);
        }).start();
    }

    public void importPathFile(File f, Runnable doneAction, @Nullable Consumer<Float> progressCallback, int speedDelayMillis, PathVersion pathVersion) {
        dontDrawWhileImporting = speedDelayMillis == 0;
        if (progressCallback != null) {
            progressCallback.accept(0F);
        }

        switch (pathVersion) {
            case VERSION_1_0:
                ToastUtils.show(ctx, R.string.import_path_1_0);
                try {
                    importPathVer1_0(f, progressCallback, speedDelayMillis);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.showException(ctx, e);
                }
                break;
            case VERSION_2_0:
                ToastUtils.show(ctx, R.string.import_old_2_0);
                try {
                    importPathVer2_0(f, progressCallback, speedDelayMillis);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.showException(ctx, e);
                }
                break;
            case VERSION_2_1:
                ToastUtils.show(ctx, R.string.import_2_1);
                try {
                    importPathVer2_1(f, progressCallback, speedDelayMillis);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.showException(ctx, e);
                }
                break;
            case VERSION_3_0:
                ToastUtils.show(ctx, R.string.fdb_import_path_version_3_0_toast);
                importPathVer3_0(f.getPath(), progressCallback, speedDelayMillis);
                break;
            default:
                throw new RuntimeException("Unknown path version");
        }

        dontDrawWhileImporting = false;
        redrawBitmap();
        postInvalidate();
        if (doneAction != null) {
            doneAction.run();
        }
    }

    public void importPathVer1_0(@NotNull File f, @Nullable Consumer<Float> progressCallback, int speedDelayMillis) throws IOException {
        long length = f.length(), read;
        byte[] bytes;
        float x, y, strokeWidth;
        int color;

        FileInputStream is = new FileInputStream(f);

        bytes = new byte[26];
        byte[] bytes_4 = new byte[4];
        read = 0L;
        while (is.read(bytes) != -1) {
            try {
                // noinspection BusyWait
                Thread.sleep(speedDelayMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            read += 26L;
            switch (bytes[25]) {
                case 1:
                    undo();
                    System.out.println("undo!");
                    break;
                case 2:
                    redo();
                    System.out.println("redo!");
                    break;
                default:
                    System.arraycopy(bytes, 0, bytes_4, 0, 4);
                    x = JNI.FloatingBoard.byteArrayToFloat(bytes_4, 0);
                    System.arraycopy(bytes, 4, bytes_4, 0, 4);
                    y = JNI.FloatingBoard.byteArrayToFloat(bytes_4, 0);
                    System.arraycopy(bytes, 8, bytes_4, 0, 4);
                    color = JNI.FloatingBoard.byteArrayToInt(bytes_4, 0);
                    System.arraycopy(bytes, 12, bytes_4, 0, 4);
                    strokeWidth = JNI.FloatingBoard.byteArrayToFloat(bytes_4, 0);
                    System.arraycopy(bytes, 16, bytes_4, 0, 4);
                    int motionAction = JNI.FloatingBoard.byteArrayToInt(bytes_4, 0);
                    System.arraycopy(bytes, 20, bytes_4, 0, 4);
                    float eraserStrokeWidth = JNI.FloatingBoard.byteArrayToFloat(bytes_4, 0);
                    if (motionAction != 0 && motionAction != 1 && motionAction != 2) {
                        motionAction = randomGen(0, 2);
                    }
                    if (strokeWidth <= 0) {
                        strokeWidth = randomGen(1, 800);
                    }
                    if (eraserStrokeWidth <= 0) {
                        eraserStrokeWidth = randomGen(1, 800);
                    }
                    setEraserMode(bytes[24] == 1);
                    setEraserStrokeWidth(eraserStrokeWidth);
                    setDrawingColor(color);
                    setDrawingStrokeWidth(strokeWidth);
                    onTouchAction(motionAction, x, y);
                    if (progressCallback != null) {
                        progressCallback.accept((float) read / (float) length);
                    }
                    break;
            }
        }

        is.close();
    }

    public void importPathVer2_0(@NotNull File f, @Nullable Consumer<Float> progressCallback, int speedDelayMillis) throws IOException {
        long length = f.length(), read;
        byte[] bytes;
        float x, y, strokeWidth;
        int color;

        FileInputStream is = new FileInputStream(f);

        Assertion.doAssertion(is.skip(12) == 12);
        bytes = new byte[12];
        read = 0L;
        int lastP1, p1 = -1;
        x = -1;
        y = -1;
        while (is.read(bytes) != -1) {
            try {
                // noinspection BusyWait
                Thread.sleep(speedDelayMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lastP1 = p1;
            p1 = JNI.FloatingBoard.byteArrayToInt(bytes, 0);
            switch (p1) {
                case 4:
                    undo();
                    break;
                case 5:
                    redo();
                    break;
                case 1:
                case 2:
                    strokeWidth = JNI.FloatingBoard.byteArrayToFloat(bytes, 4);
                    color = JNI.FloatingBoard.byteArrayToInt(bytes, 8);
                    setEraserMode(p1 == 2);
                    if (eraserMode) {
                        setEraserStrokeWidth(strokeWidth);
                    } else {
                        setDrawingStrokeWidth(strokeWidth);
                        setDrawingColor(color);
                    }
                    break;
                case 3:
                    if (x != -1 && y != -1) {
                        onTouchAction(MotionEvent.ACTION_UP, x, y);
                    }
                    break;
                case 0:
                    x = JNI.FloatingBoard.byteArrayToFloat(bytes, 4);
                    y = JNI.FloatingBoard.byteArrayToFloat(bytes, 8);
                    if (lastP1 == 1 || lastP1 == 2) {
                        onTouchAction(MotionEvent.ACTION_DOWN, x, y);
                    }
                    onTouchAction(MotionEvent.ACTION_MOVE, x, y);
                    break;
                default:
                    break;
            }
            read += 12;
            if (progressCallback != null) {
                progressCallback.accept((float) read / ((float) length));
            }
        }

        is.close();
    }

    public void importPathVer2_1(@NotNull File f, @Nullable Consumer<Float> progressCallback, int speedDelayMillis) throws IOException {
        long length = f.length(), read;
        byte[] bytes;
        float x, y, strokeWidth;
        int color;

        FileInputStream is = new FileInputStream(f);

        // 512 * 9
        int bufferSize = 2304;
        Assertion.doAssertion(is.skip(12) == 12);
        byte[] buffer = new byte[bufferSize];
        int bufferRead;
        read = 0L;
        while ((bufferRead = is.read(buffer)) != -1) {
            int a = bufferRead / 9;
            for (int i = 0; i < a; i++) {
                try {
                    // noinspection BusyWait
                    Thread.sleep(speedDelayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                switch (buffer[i * 9]) {
                    case (byte) 0xA1:
                    case (byte) 0xA2:
                        strokeWidth = JNI.FloatingBoard.byteArrayToFloat(buffer, 1 + i * 9);
                        color = JNI.FloatingBoard.byteArrayToInt(buffer, 5 + i * 9);
                        setEraserMode(buffer[i * 9] == (byte) 0xA2);
                        mPaintRef.setColor(color);
                        mPaintRef.setStrokeWidth(strokeWidth);
                        break;
                    case (byte) 0xB1:
                        x = JNI.FloatingBoard.byteArrayToFloat(buffer, 1 + i * 9);
                        y = JNI.FloatingBoard.byteArrayToFloat(buffer, 5 + i * 9);
                        onTouchAction(MotionEvent.ACTION_DOWN, x, y);
                        break;
                    case (byte) 0xB3:
                        x = JNI.FloatingBoard.byteArrayToFloat(buffer, 1 + i * 9);
                        y = JNI.FloatingBoard.byteArrayToFloat(buffer, 5 + i * 9);
                        onTouchAction(MotionEvent.ACTION_MOVE, x, y);
                        break;
                    case (byte) 0xB2:
                        x = JNI.FloatingBoard.byteArrayToFloat(buffer, 1 + i * 9);
                        y = JNI.FloatingBoard.byteArrayToFloat(buffer, 5 + i * 9);
                        onTouchAction(MotionEvent.ACTION_UP, x, y);
                        break;
                    case (byte) 0xC1:
                        undo();
                        break;
                    case (byte) 0xC2:
                        redo();
                        break;
                    default:
                        break;
                }
                read += 9L;
                if (progressCallback != null) {
                    progressCallback.accept((float) read / (float) length);
                }
            }
        }

        is.close();
    }

    private int randomGen(int min, int max) {
        double ran_sc_db = Math.round(Math.random() * (max - min)) + min;
        return (int) ran_sc_db;
    }

    private final PointF importTmpPoint = new PointF();

    private void transformedOnTouchAction(int motionAction, float x, float y, float[] matrixValues) {
        CanvasTransformer.getTransformedPoint(importTmpPoint, matrixValues, x, y);
        onTouchAction(motionAction, importTmpPoint.x, importTmpPoint.y);
    }

    private @NotNull ArrayList<String> getDatabaseTables(@NotNull SQLite3 db) {
        ArrayList<String> list = new ArrayList<>();
        final Statement statement = db.compileStatement("SELECT tbl_name FROM sqlite_master");
        final Cursor cursor = statement.getCursor();
        while (cursor.step()) {
            list.add(cursor.getText(0));
        }
        statement.release();
        return list;
    }

    private void importPathVer3_0(@NotNull String path, Consumer<Float> progressCallback, int speedDelayMillis) {
        final SQLite3 db = SQLite3.open(path);
        if (db.checkIfCorrupt()) {
            db.close();
            throw new SQLiteDatabaseCorruptException();
        }

        final Matrix savedTransformation = new Matrix(getTransformationMatrix());

        Matrix defaultTransformation = null;
        ArrayList<LayerInfo> layersInfo = null;
        try {
            final JSONObject extraInfos = PathSaver.getExtraInfos(db);
            final JSONObject defaultTransformationJSON = Objects.requireNonNull(extraInfos).getJSONObject("defaultTransformation");
            defaultTransformation = ExtraInfosUtils.Companion.getDefaultTransformation(defaultTransformationJSON);

            layersInfo = ExtraInfosUtils.Companion.getLayersInfo(extraInfos);
        } catch (JSONException | NullPointerException ignored) {
            ToastUtils.show(ctx, R.string.fdb_import_get_extra_infos_failed);
        }
        if (defaultTransformation == null) {
            defaultTransformation = new Matrix();
        }
        float defaultTransformationScale = CanvasTransformer.getRealScale(defaultTransformation);

        float[] transformationValue = new float[9];
        defaultTransformation.getValues(transformationValue);

        if (layersInfo != null) {
            for (LayerInfo layerInfo : layersInfo) {
                final long originalLayerId = layerInfo.getLayerId();
                final long newLayerId = layerInfo.getLayerId() + layerInfo.getName().hashCode() + System.currentTimeMillis() + Random.generate(0, 10);
                add1Layer(newLayerId);
                switchLayer(newLayerId);
                if (onImportLayerAddedListener != null) {
                    onImportLayerAddedListener.onAdded(new LayerInfo(newLayerId, layerInfo.getName(), layerInfo.getVisible()));
                }

                importLayerPath(
                        db,
                        originalLayerId,
                        defaultTransformationScale,
                        transformationValue,
                        progressCallback,
                        speedDelayMillis
                );
            }
        } else {
            // TODO: 8/5/21 for old path ver3.0
            TODO.todo();
        }

        String extraStr = null;
        try {
            Statement infoStatement = db.compileStatement("SELECT extra_infos\n" +
                    "FROM info");
            Cursor infoCursor = infoStatement.getCursor();
            if (infoCursor.step()) {
                extraStr = infoCursor.getText(0);
            }
            infoStatement.release();
        } catch (RuntimeException ignored) {
        }

        if (extraStr != null) {
            try {
                JSONObject jsonObject = new JSONObject(extraStr);
                boolean isLockingStroke = jsonObject.getBoolean("isLockingStroke");
                setLockingStroke(isLockingStroke);
                lockedDrawingStrokeWidth = (float) jsonObject.getDouble("lockedDrawingStrokeWidth");
                lockedEraserStrokeWidth = (float) jsonObject.getDouble("lockedEraserStrokeWidth");
                setCurrentStrokeWidthWhenLocked();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        transformTo(savedTransformation);

        postInvalidate();
    }

    private void importLayerPath(
            SQLite3 db,
            long layerId,
            float defaultTransformationScale,
            float[] transformationValue,
            Consumer<Float> progressCallback,
            int speedDelayMillis
    ) {
        final String pathTable = "path_layer_" + layerId;
        final int rowCount = SQLiteUtilsKt.getRowCount(db, "SELECT COUNT() FROM " + pathTable);

        final Statement statement = db.compileStatement("SELECT mark, p1, p2 FROM " + pathTable);
        final Cursor cursor = statement.getCursor();

        int c = 0;
        while (cursor.step()) {
            int mark = cursor.getInt(0);

            switch (mark) {
                case 0x01:
                    setDrawingColor(cursor.getInt(1));
                    setDrawingStrokeWidth(cursor.getFloat(2) * defaultTransformationScale / canvasScale);
                    setEraserMode(false);
                    break;
                case 0x02:
                case 0x12:
                    transformedOnTouchAction(
                            MotionEvent.ACTION_DOWN,
                            cursor.getFloat(1),
                            cursor.getFloat(2),
                            transformationValue
                    );
                    break;
                case 0x03:
                case 0x13:
                    transformedOnTouchAction(
                            MotionEvent.ACTION_MOVE,
                            cursor.getFloat(1),
                            cursor.getFloat(2),
                            transformationValue
                    );
                    break;
                case 0x04:
                case 0x14:
                    transformedOnTouchAction(
                            MotionEvent.ACTION_UP,
                            cursor.getFloat(1),
                            cursor.getFloat(2),
                            transformationValue
                    );
                    break;
                case 0x11:
                    setEraserMode(true);
                    setEraserAlpha(cursor.getInt(1));
                    setEraserStrokeWidth(cursor.getFloat(2) * defaultTransformationScale / canvasScale);
                    break;
                case 0x20:
                    undo();
                    break;
                case 0x30:
                    redo();
                    break;
                default:
            }

            ++c;
            progressCallback.accept((float) c / (float) rowCount);
            if (!dontDrawWhileImporting) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(speedDelayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        statement.release();
    }

    private final PointF transformedPoint = new PointF();
    private final PointF inverseTransformedPoint = new PointF();

    private void onTouchAction(int motionAction, float x, float y) {
        canvasTransformer.getInvertedTransformedPoint(inverseTransformedPoint, x, y);
        // convert the screen coordinates to a new unknown point
        // which after the transformation coincides with the screen point
        // so that the new point can be shown on your finger's position after it's drawn on the transformed canvas
        // principle: inverse matrix
        x = inverseTransformedPoint.x;
        y = inverseTransformedPoint.y;
        switch (motionAction) {
            case MotionEvent.ACTION_DOWN:
                if (eraserMode) {
                    layerPathSaverRef.onErasingTouchDown(x, y, getEraserAlpha(), getEraserStrokeWidth(), blurRadius);
                } else {
                    layerPathSaverRef.onDrawingTouchDown(x, y, getDrawingColor(), getDrawingStrokeWidth(), blurRadius);
                }
                //路径
                mPath = new Path();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                layerPathSaverRef.onTouchMove(x, y, eraserMode);
                if (mPath != null) {
                    mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
                    mLastX = x;
                    mLastY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mPath != null) {
                    layerPathSaverRef.onTouchUp(x, y, eraserMode);
                    if (!dontDrawWhileImporting) {
                        mCanvas.drawPath(mPath, mPaintRef);//将路径绘制在mBitmap上
                    }
                    Path path = new Path(mPath);//复制出一份mPath
                    Paint paint = new Paint(mPaintRef);
                    PathBean pb = new PathBean(path, paint);
                    // for undoing
                    undoListRef.add(pb);
                    redoListRef.clear();
                    mPath.reset();
                    mPath = null;
                }
                layerPathSaverRef.transferToPathTableAndClear();
                break;
            default:
        }
        postInvalidate();
    }

    public float getEraserStrokeWidth() {
        return eraserPaint.getStrokeWidth();
    }

    public void setEraserStrokeWidth(float w) {
        eraserPaint.setStrokeWidth(w);
    }

    public void importImage(@NonNull Bitmap imageBitmap, float left, float top, int scaledWidth, int scaledHeight) {
        // TODO: 8/4/21 import image
        /*System.gc();
        Bitmap bitmap = Bitmap.createScaledBitmap(imageBitmap, scaledWidth, scaledHeight, true);
        mBackgroundCanvas.drawBitmap(bitmap, left, top, mBitmapPaint);
        if (backgroundBitmap == null) {
            ToastUtils.show(ctx, ctx.getString(R.string.importing_failed));
        } else {
            mCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
        }
        postInvalidate();*/
    }

    public void resetTransformation() {
        transformTo(defaultTransformation);
    }

    /**
     * 把路径绘制到缓冲Bitmap上
     */
    private void redrawBitmap() {
        layerRef.redrawBitmap(canvasTransformer.getMatrix());
    }

    public float getStrokeWidthInUse() {
        return mPaintRef.getStrokeWidth();
    }

    public boolean isLockingStroke() {
        return this.lockStrokeEnabled;
    }

    public void setLockingStroke(boolean mode) {
        this.lockStrokeEnabled = mode;
    }

    // ----------------------- new API of stroke locking -------------------------------

    public void setLockStrokeEnabled(boolean enabled) {
        lockStrokeEnabled = enabled;
        if (enabled) {
            updateLockedStrokeWidth();
        }
    }

    public boolean isLockStrokeEnabled() {
        return lockStrokeEnabled;
    }

    public void setCurrentStrokeWidthWhenLocked() {
        if (lockStrokeEnabled) {
            setDrawingStrokeWidth(lockedDrawingStrokeWidth / canvasScale);
            setEraserStrokeWidth(lockedEraserStrokeWidth / canvasScale);
        }
    }

    public void updateLockedStrokeWidth() {
        lockedDrawingStrokeWidth = getDrawingStrokeWidth() * getScale();
        lockedEraserStrokeWidth = getEraserStrokeWidth() * getScale();
    }

    // ----------------------------------- end -----------------------------------------

    public float getScale() {
        return canvasScale;
    }

    public float getZoomedStrokeWidthInUse() {
        return getScale() * getStrokeWidthInUse();
    }

    /**
     * 路径集合
     */
    public static class PathBean {
        public final Path path;
        public final Paint paint;

        PathBean(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }

    /**
     * Set the path saver so that the drawing data will be stored
     *
     * @param pathSaver path saver
     */
    public void setPathSaver(PathSaver pathSaver) {
        final File tmpFile = new File(defaultTmpPathSaver.getDatabasePath());
        if (tmpFile.exists()) {
            if (!tmpFile.delete()) {
                throw new DeleteException();
            }
        }

        this.pathSaver = pathSaver;
    }

    public void flushPathSaver() {
        if (pathSaver != null) {
            pathSaver.flush();
        }
    }

    public void setOnScreenDimensionChangedListener(OnScreenDimensionChangedListener onScreenDimensionChangedListener) {
        this.onScreenDimensionChangedListener = onScreenDimensionChangedListener;
    }

    public interface OnScreenDimensionChangedListener {
        void onChange(int width, int height);
    }

    public Matrix getTransformationMatrix() {
        return canvasTransformer.getMatrix();
    }

    /**
     * Set the current transformation state the default transformation
     * When "reset transformation" button clicked, this saved state will be restored
     */
    public void setAsDefaultTransformation() {
        defaultTransformation.set(getTransformationMatrix());
    }

    /**
     * Specify a transformation state to set; see {@link PaintView#setAsDefaultTransformation()}
     */
    public void setDefaultTransformation(Matrix matrix) {
        defaultTransformation.set(matrix);
    }

    public boolean isMoveTransformationEnabled() {
        return moveTransformationEnabled;
    }

    public void setMoveTransformationEnabled(boolean moveTransformationEnabled) {
        this.moveTransformationEnabled = moveTransformationEnabled;
    }

    public boolean isZoomTransformationEnabled() {
        return zoomTransformationEnabled;
    }

    public void setZoomTransformationEnabled(boolean zoomTransformationEnabled) {
        this.zoomTransformationEnabled = zoomTransformationEnabled;
    }

    public boolean isRotateTransformationEnabled() {
        return rotateTransformationEnabled;
    }

    public void setRotateTransformationEnabled(boolean rotateTransformationEnabled) {
        this.rotateTransformationEnabled = rotateTransformationEnabled;
    }

    public void transformTo(Matrix matrix) {
        canvasTransformer.setMatrix(matrix);
        redrawAllLayerBitmap();
        postInvalidate();
        canvasScale = CanvasTransformer.getRealScale(matrix);
        setCurrentStrokeWidthWhenLocked();
    }

    public void transformToOrigin() {
        transformTo(new Matrix());
    }

    public void resetDefaultTransformation() {
        defaultTransformation.set(new Matrix());
    }

    int a = 1;

    public void testAPI() {
        switchLayer(a % 2);
        redrawBitmap();
        postInvalidate();
        ++a;
    }

    public long add1Layer(long id) {
        final Layer layer = new Layer(width, height, id);
        layerArray.add(layer);
        pathSaver.addNewLayerPathSaver(id);
        return layer.getId();
    }

    public ArrayList<Layer> getLayers() {
        return layerArray;
    }

    private void switchLayer(int index) {
        layerRef = layerArray.get(index);
        updateLayerRefs();
    }

    private void updateLayerRefs() {
        undoListRef = layerRef.undoList;
        redoListRef = layerRef.redoList;
        bitmapRef = layerRef.bitmap;
        layerPathSaverRef = pathSaver.getLayerPathSaver(layerRef.getId());
        if (layerPathSaverRef == null) throw new RuntimeException("Not found specific LayerPathSaver in PathSaver");

        mCanvas.setBitmap(bitmapRef);
        canvasTransformer.refresh();
    }

    private int getLayerIndexById(long id) {
        for (int i = 0; i < layerArray.size(); i++) {
            if (layerArray.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private @org.jetbrains.annotations.Nullable Layer getLayerById(long id) {
        final int i = getLayerIndexById(id);
        if (i != -1) {
            return layerArray.get(i);
        }
        return null;
    }

    public void updateLayerState(@NotNull List<Long> orderList, long checkedId) {
        ArrayList<Layer> newLayerArray = new ArrayList<>();
        for (Long id : orderList) {
            newLayerArray.add(getLayerById(id));
        }
        layerArray.clear();
        layerArray.addAll(newLayerArray);
        switchLayer(checkedId);
    }

    public void switchLayer(long id) {
        switchLayer(getLayerIndexById(id));
    }

    private void redrawAllLayerBitmap() {
        for (int i = layerArray.size() - 1; i >= 0; i--) {
            layerArray.get(i).redrawBitmap(canvasTransformer.getMatrix());
        }
    }

    public float getLockedDrawingStrokeWidth() {
        return lockedDrawingStrokeWidth;
    }

    public float getLockedEraserStrokeWidth() {
        return lockedEraserStrokeWidth;
    }

    public Matrix getDefaultTransformation() {
        return defaultTransformation;
    }

    public long getCurrentLayerId() {
        return layerRef.getId();
    }

    public ArrayList<Long> getLayerIds() {
        ArrayList<Long> list = new ArrayList<>();
        for (Layer layer : layerArray) {
            list.add(layer.getId());
        }
        return list;
    }

    public interface OnImportLayerAddedListener {
        void onAdded(LayerInfo layerInfo);
    }

    public void setOnImportLayerAddedListener(@Nullable OnImportLayerAddedListener onImportLayerAddedListener) {
        this.onImportLayerAddedListener = onImportLayerAddedListener;
    }

    public void setBlurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        mPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
    }

    public float getBlurRadius() {
        return blurRadius;
    }
}
