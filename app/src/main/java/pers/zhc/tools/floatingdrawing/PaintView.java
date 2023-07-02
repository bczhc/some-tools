package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import pers.zhc.jni.sqlite.Cursor;
import pers.zhc.jni.sqlite.SQLite3;
import pers.zhc.jni.sqlite.Statement;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.R;
import pers.zhc.tools.fdb.*;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.*;
import pers.zhc.util.Assertion;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import static pers.zhc.tools.utils.NullSafeHelper.getNonNull;

/**
 * @author bczhc
 */
@SuppressLint("ViewConstructor")
public class PaintView extends BaseView {

    private int height = -1;
    private int width = -1;
    private final Context ctx;
    boolean eraserMode = false;
    public boolean isImportingTerminated = false;
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
    /**
     * 模拟栈，来保存 Path
     */
    private ArrayList<PathBean> undoListRef, redoListRef;
    private GestureResolver gestureResolver;
    private boolean showDrawing = true;
    private boolean importingPath = false;
    private volatile boolean pathImportPaused = false;
    private boolean lockStrokeEnabled = false;
    /**
     * locked absolute drawing stroke width
     */
    private float lockedDrawingStrokeWidth;
    /**
     * locked absolute eraser stroke width
     */
    private float lockedEraserStrokeWidth;
    private OnPathImportColorChangedCallback onPathImportColorChangedCallback = null;
    private Bitmap transBitmap;
    private Matrix transCanvasTransformation;

    private PathSaver defaultTmpPathSaver;
    private PathSaver pathSaver = null;

    private OnScreenDimensionChangedListener onScreenDimensionChangedListener = null;

    private final Matrix defaultTransformation = new Matrix();

    private boolean moveTransformationEnabled = true;
    private boolean zoomTransformationEnabled = true;
    private boolean rotateTransformationEnabled = false;

    private float canvasScale = 1F;

    private float strokeHardness = 100F;
    private float eraserHardness = 100F;

    /**
     * in microseconds
     */
    private int drawingInterval = 0;

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

    public void setOnColorChangedCallback(OnPathImportColorChangedCallback onPathImportColorChangedCallback) {
        this.onPathImportColorChangedCallback = onPathImportColorChangedCallback;
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
        /*TODO keep the "rotate origin" fixed
        Matrix matrix = null;
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
        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaintRef = mPaint;
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        //使画笔更加圆润
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        //同上
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        this.gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {

            @Contract(pure = true)
            private boolean transformationEnabled() {
                return moveTransformationEnabled || zoomTransformationEnabled || rotateTransformationEnabled;
            }

            @Override
            public void onTwoPointsScroll(float distanceX, float distanceY, MotionEvent event) {
                if (moveTransformationEnabled) {
                    canvasTransformer.absTranslate(distanceX, distanceY);
                    transCanvasTransformation.postTranslate(distanceX, distanceY);
                }
            }

            @Override
            public void onTwoPointsZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float dScale, MotionEvent event) {
                if (zoomTransformationEnabled) {
                    canvasTransformer.absScale(dScale, midPointX, midPointY);
                    canvasScale *= dScale;
                    updateStrokeWidthIfLocked();
                    transCanvasTransformation.postScale(dScale, dScale, midPointX, midPointY);
                }
            }

            @Override
            public void onTwoPointsRotate(MotionEvent event, float firstMidX, float firstMidY, float degrees, float midX, float midY) {
                if (rotateTransformationEnabled) {
                    canvasTransformer.absRotate(degrees, midX, midY);
                    transCanvasTransformation.postRotate(degrees, midX, midY);
                }
            }

            private final Handler debounceHandler = new Handler(Looper.myLooper());
            private boolean debounceFinished = true;

            @Override
            public void onTwoPointsUp(MotionEvent event) {
                if (transformationEnabled()) {
                    int debounceInterval = 265;
                    debounceHandler.removeCallbacksAndMessages(null);
                    debounceFinished = false;
                    debounceHandler.postDelayed(() -> {
                        endTransformation();
                        debounceFinished = true;
                    }, debounceInterval);
                }
            }

            @Override
            public void onTwoPointsDown(MotionEvent event) {
                if (transformationEnabled()) {
                    // clear erroneous paths between two touch points while zooming and scrolling
                    mPath = null;
                    if (layerPathSaverRef != null) {
                        layerPathSaverRef.clearTempTable();
                    }

                    if (debounceFinished) {
                        startTransformation();
                    } else {
                        debounceHandler.removeCallbacksAndMessages(null);
                    }
                }
            }

            @Override
            public void onOnePointScroll(float distanceX, float distanceY, MotionEvent event) {

            }

            @Override
            public void onTwoPointsPress(MotionEvent event) {
            }

            private void startTransformation() {
                if (transBitmap == null) {
                    transBitmap = Bitmap.createBitmap(bitmapRef.getWidth(), bitmapRef.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas transCanvas = new Canvas(transBitmap);

                    for (int i = layerArray.size() - 1; i >= 0; i--) {
                        final Layer layer = layerArray.get(i);
                        if (layer.isVisible()) {
                            transCanvas.drawBitmap(layer.bitmap, 0F, 0F, null);
                        }
                    }

                    transCanvasTransformation = new Matrix();
                }
            }

            private void endTransformation() {
                transBitmap = null;
                // redraw all layers' bitmaps
                redrawAllLayerBitmap();
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
        updateHardness();
    }

    public int getDrawingColor() {
        return this.mPaint.getColor();
    }

    /**
     * 设置画笔颜色
     */
    public void setDrawingColor(@ColorInt int color) {
        setDrawingColor(color, false);
    }

    public void setDrawingColor(@ColorInt int color, boolean fromPathImport) {
        mPaint.setColor(color);
        if (this.onPathImportColorChangedCallback != null && fromPathImport) {
            onPathImportColorChangedCallback.change(color);
        }
    }

    /**
     * 撤销操作
     */
    public void undo() {
        Stopwatch stopwatch = new Stopwatch();

        if (canUndo()) {
            layerPathSaverRef.undo();

            clearPaint();//清除之前绘制内容
            PathBean lastPb = undoListRef.remove(undoListRef.size() - 1);//将最后一个移除
            redoListRef.add(lastPb);//加入 恢复操作
            //遍历，将Path重新绘制到 headCanvas
            if (showDrawing) {
                for (PathBean pb : undoListRef) {
                    mCanvas.drawPath(pb.path, pb.paint);
                }
                postInvalidate();
            }
        }

        Log.i(TAG, "undo() time elapsed: " + stopwatch.stop() + " ms");
    }

    /**
     * 恢复操作
     */
    public void redo() {
        if (canRedo()) {
            layerPathSaverRef.redo();

            PathBean pathBean = redoListRef.remove(redoListRef.size() - 1);
            mCanvas.drawPath(pathBean.path, pathBean.paint);
            undoListRef.add(pathBean);
            if (showDrawing) {
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
        layerPathSaverRef.clearTempTable();
        layerPathSaverRef.clearLayerTable();
    }

    public void clearAllLayers() {
        for (Layer layer : layerArray) {
            Canvas canvas = new Canvas(layer.bitmap);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            layer.redoList.clear();
            layer.undoList.clear();
            LayerPathSaver layerPathSaver = Objects.requireNonNull(pathSaver.getLayerPathSaver(layer.getId()));
            layerPathSaver.clearTempTable();
            layerPathSaver.clearLayerTable();
        }
        postInvalidate();
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

    private Bitmap drawPathsToBitmap(@NotNull List<PathBean> pathBeans, @Nullable Matrix matrix, int width, int height, FloatCallback progressCallback) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        canvas.setMatrix(matrix);

        final int size = pathBeans.size();

        float count = 0F;
        for (PathBean pathBean : pathBeans) {
            canvas.drawPath(pathBean.path, pathBean.paint);

            progressCallback.call(count / (float) size);

            count += 1F;
        }

        return bitmap;
    }

    /**
     * 导出图片
     */
    public void exportImg(File f, int width, int height, ImageExportProgressCallback progressCallback) throws IOException {
        final Matrix matrix = canvasTransformer.getMatrix();
        matrix.postScale(((float) width) / ((float) getBitmapWidth()), (float) height / (float) getBitmapHeight());

        final Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas overlayCanvas = new Canvas(resultBitmap);

        for (int i = layerArray.size() - 1; i >= 0; i--) {
            final Layer layer = layerArray.get(i);
            if (!layer.isVisible()) continue;
            final ArrayList<PathBean> undoList = layer.undoList;


            final Bitmap bitmap = drawPathsToBitmap(undoList, matrix, width, height, p -> {
                // TODO layer's name
                progressCallback.call(ImageExportProgressType.REDRAWING, String.valueOf(layer.getId()), p);
            });
            overlayCanvas.drawBitmap(bitmap, 0F, 0F, null);
            bitmap.recycle();
            System.gc();
        }

        progressCallback.call(ImageExportProgressType.COMPRESSING, "", 0F);

//        JNI.Bitmap.compressToPng(resultBitmap, f.getPath());
        FileOutputStream os = new FileOutputStream(f);
        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();
    }

    /**
     * 是否可以撤销
     */
    public boolean canUndo() {
        return !undoListRef.isEmpty();
    }

    /**
     * 是否可以恢复
     */
    public boolean canRedo() {
        return !redoListRef.isEmpty();
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
        if (!importingPath) {
            gestureResolver.onTouch(event);
            onTouchAction(event.getAction(), event.getX(), event.getY());
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!showDrawing) {
            return;
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (transBitmap == null) {
            if (bitmapRef != null) {
                // 将bitmap绘制在canvas上,最终的显示
                for (int i = layerArray.size() - 1; i >= 0; i--) {
                    Layer layer = layerArray.get(i);
                    if (layer.isVisible()) {
                        canvas.drawBitmap(layer.bitmap, 0, 0, null);
                    }
                }

                if (showDrawing) {
                    Path catchedPath = mPath;
                    if (catchedPath != null) {//显示实时正在绘制的path轨迹
                        canvas.setMatrix(canvasTransformer.getMatrix());
                        if (eraserMode) {
                            canvas.drawPath(catchedPath, eraserPaint);
                        } else {
                            canvas.drawPath(catchedPath, mPaint);
                        }
                    }
                }
            }
        } else {
            canvas.setMatrix(transCanvasTransformation);
            canvas.drawBitmap(transBitmap, 0, 0, null);
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
            if (layer.isVisible()) {
                setupBitmap(width, height, layer);
            }
        }
        invalidate();
    }

    public void importPathFile(File f, @Nullable PathImportCallback progressCallback,
                               PathVersion pathVersion) {
        importingPath = true;

        if (progressCallback != null) {
            progressCallback.progress(0F, null, 0, 0);
        }

        switch (pathVersion) {
            case VERSION_1_0:
                ToastUtils.show(ctx, R.string.import_path_1_0);
                try {
                    importPathVer1_0(f, progressCallback);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.showException(ctx, e);
                }
                break;
            case VERSION_2_0:
                ToastUtils.show(ctx, R.string.import_old_2_0);
                try {
                    importPathVer2_0(f, progressCallback);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.showException(ctx, e);
                }
                break;
            case VERSION_2_1:
                ToastUtils.show(ctx, R.string.import_2_1);
                try {
                    importPathVer2_1(f, progressCallback);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastUtils.showException(ctx, e);
                }
                break;
            case VERSION_3_0:
                ToastUtils.show(ctx, R.string.fdb_import_path_version_3_0_toast);
                importPathVer3_0(f.getPath(), progressCallback);
                break;
            case VERSION_3_1:
                ToastUtils.show(ctx, R.string.fdb_import_path_version_3_1_toast);
                importPathVer3_1(f.getPath(), progressCallback);
                break;
            case VERSION_4_0:
                ToastUtils.show(ctx, R.string.fdb_import_path_version_4_0_toast);
                importPathVer4_0(f.getPath(), progressCallback);
                break;
            default:
                throw new RuntimeException("Unknown path version");
        }

        importingPath = false;
        // even if the path is imported without `showDrawing`, this also needs to be
        // true for displaying the imported result when redrawing
        showDrawing = true;
        redrawBitmap();
        postInvalidate();
    }

    public void importPathVer1_0(@NotNull File f, @Nullable PathImportCallback progressCallback) throws IOException {
        pathImportPaused = false;
        isImportingTerminated = false;
        long length = f.length(), read;
        byte[] bytes;
        float x, y, strokeWidth;
        int color;

        FileInputStream is = new FileInputStream(f);

        bytes = new byte[26];
        byte[] bytes_4 = new byte[4];
        read = 0L;
        while (is.read(bytes) != -1) {
            // noinspection StatementWithEmptyBody
            while (pathImportPaused && !isImportingTerminated) ;
            if (!isImportingTerminated) {
                spinSleep(drawingInterval);
            }

            read += 26L;
            switch (bytes[25]) {
                case 1:
                    if (isImportingTerminated) break;
                    undo();
                    System.out.println("undo!");
                    break;
                case 2:
                    if (isImportingTerminated) break;
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
                    if (isImportingTerminated) {
                        onTouchAction(MotionEvent.ACTION_UP, x, y);
                        break;
                    }
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
                    setDrawingColor(color, true);
                    setDrawingStrokeWidth(strokeWidth);
                    onTouchAction(motionAction, x, y);
                    if (progressCallback != null) {
                        progressCallback.progress((float) read / (float) length, null, 0, 0);
                    }
                    break;
            }
        }

        is.close();
    }

    public void importPathVer2_0(@NotNull File f, @Nullable PathImportCallback progressCallback) throws IOException {
        pathImportPaused = false;
        isImportingTerminated = false;
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
            // noinspection StatementWithEmptyBody
            while (pathImportPaused && !isImportingTerminated) ;
            if (isImportingTerminated) {
                if (x != -1 && y != -1) {
                    onTouchAction(MotionEvent.ACTION_UP, x, y);
                }
                break;
            } else {
                spinSleep(drawingInterval);
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
                        setDrawingColor(color, true);
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
                progressCallback.progress((float) read / ((float) length), null, 0, 0);
            }
        }

        is.close();
    }

    public void importPathVer2_1(@NotNull File f, @Nullable PathImportCallback progressCallback) throws IOException {
        pathImportPaused = false;
        isImportingTerminated = false;
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
                // noinspection StatementWithEmptyBody
                while (pathImportPaused && !isImportingTerminated) ;
                if (isImportingTerminated) {
                    x = JNI.FloatingBoard.byteArrayToFloat(buffer, 1 + i * 9);
                    y = JNI.FloatingBoard.byteArrayToFloat(buffer, 5 + i * 9);
                    onTouchAction(MotionEvent.ACTION_UP, x, y);
                    break;
                } else {
                    spinSleep(drawingInterval);
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
                    progressCallback.progress((float) read / (float) length, null, 0, 0);
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

    private void setupExtraConfig(ExtraInfo extraInfo) {
        // TODO: 8/16/21
    }

    @SuppressWarnings("DuplicatedCode")
    private void importPathVer3_0(@NotNull String path, PathImportCallback progressCallback) {
        pathImportPaused = false;
        isImportingTerminated = false;
        final SQLite3 db = SQLite3.open(path);
        if (db.checkIfCorrupt()) {
            db.close();
            throw new SQLiteDatabaseCorruptException();
        }

        final Matrix savedTransformation = new Matrix(getTransformationMatrix());

        final ExtraInfo extraInfo = getNonNull(ExtraInfo.Companion.getExtraInfo(db), getDefaultExtraInfos());

        Matrix defaultTransformation = new Matrix();

        final float[] values = extraInfo.getDefaultTransformation();
        if (values != null) defaultTransformation.setValues(values);

        float defaultTransformationScale = CanvasTransformer.getRealScale(defaultTransformation);

        float[] transformationValue = new float[9];
        defaultTransformation.getValues(transformationValue);

        float canvasScale = CanvasTransformer.getRealScale(getTransformationMatrix());

        int recordNum = PathSaver.getPathCount(db);

//        dontDrawWhileImporting = speedDelayMillis == 0;

        final List<String> tables = SQLite3UtilsKt.getTables(db);
        for (String table : tables) {
            if (Layer.checkTableName(table)) {
                String layerId = Layer.getTableLayerId(table);
                add1Layer(new LayerInfo(layerId, layerId, true));

            }
        }
        Statement statement = db.compileStatement("SELECT mark, p1, p2\n" +
                "FROM path");

        Cursor cursor = statement.getCursor();
        int c = 0;
        while (cursor.step()) {
            // noinspection StatementWithEmptyBody
            while (pathImportPaused && !isImportingTerminated) ;
            if (isImportingTerminated) {
                transformedOnTouchAction(
                        MotionEvent.ACTION_UP,
                        cursor.getFloat(1),
                        cursor.getFloat(2),
                        transformationValue
                );
                break;
            } else {
                spinSleep(drawingInterval);
            }
            int mark = cursor.getInt(0);

            switch (mark) {
                case 0x01:
                    setDrawingColor(cursor.getInt(1), true);
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
            progressCallback.progress((float) c / (float) recordNum, null, 0, 0);
        }

        setLockingStrokesFromExtraInfos(extraInfo);

        transformTo(savedTransformation);

        postInvalidate();
    }

    private void setLockingStrokesFromExtraInfos(@NotNull ExtraInfo extraInfo) {
        setLockStrokeEnabled(Boolean.TRUE.equals(extraInfo.isLockingStroke()));
        lockedDrawingStrokeWidth = getNonNull(extraInfo.getLockedDrawingStrokeWidth(), 10F);
        lockedEraserStrokeWidth = getNonNull(extraInfo.getLockedEraserStrokeWidth(), 10F);
        updateStrokeWidthIfLocked();
    }

    private void importPathVer3_1(@NotNull String path, PathImportCallback progressCallback) {
        pathImportPaused = false;
        isImportingTerminated = false;
        final SQLite3 db = SQLite3.open(path);
        int layerNumber = 0;
        if (db.checkIfCorrupt()) {
            db.close();
            throw new SQLiteDatabaseCorruptException();
        }

        final ExtraInfo extraInfo = ExtraInfo.Companion.getExtraInfo(db);
        if (extraInfo == null) {
            throw new InvalidExtraInfoException(ctx.getString(R.string.fdb_path_info_corrupt_toast));
        }

        final Matrix savedTransformation = new Matrix(getTransformationMatrix());

        Matrix defaultTransformation = new Matrix();

        final float[] values = extraInfo.getDefaultTransformation();
        if (values != null) {
            defaultTransformation.setValues(values);
        }

        List<LayerInfo> layersInfo = extraInfo.getLayersInfo();

        float defaultTransformationScale = CanvasTransformer.getRealScale(defaultTransformation);

        float[] transformationValue = new float[9];
        defaultTransformation.getValues(transformationValue);

        // pathVer3.0 records the layers info
        if (layersInfo == null) {
            ToastUtils.show(ctx, R.string.fdb_layer_info_missing_importing_terminated_toast);
            return;
        }

        for (int i = layersInfo.size() - 1; i >= 0; i--) {
            LayerInfo layerInfo = layersInfo.get(i);

            LayerInfo newLayerInfo = new LayerInfo(Layer.randomId(), layerInfo.getName(), layerInfo.getVisible());
            add1Layer(newLayerInfo);
            switchLayer(newLayerInfo.getId());

            if (onImportLayerAddedListener != null) {
                onImportLayerAddedListener.onAdded(newLayerInfo);
            }
            ++layerNumber;
            importLayerPath3_1(
                    db,
                    layerInfo,
                    defaultTransformationScale,
                    transformationValue,
                    progressCallback,
                    layerNumber,
                    layersInfo.size()
            );
        }

        setLockingStrokesFromExtraInfos(extraInfo);

        transformTo(savedTransformation);

        postInvalidate();
    }

    private void importPathVer4_0(@NotNull String path, PathImportCallback progressCallback) {
        pathImportPaused = false;
        isImportingTerminated = false;
        final SQLite3 db = SQLite3.open(path);
        int layerNumber = 0;
        if (db.checkIfCorrupt()) {
            db.close();
            throw new SQLiteDatabaseCorruptException();
        }

        final ExtraInfo extraInfo = ExtraInfo.Companion.getExtraInfo(db);
        // multi-layer info is needed, and they're stored in extraInfos
        if (extraInfo == null) {
            throw new InvalidExtraInfoException(ctx.getString(R.string.fdb_path_info_corrupt_toast));
        }

        final Matrix savedTransformation = new Matrix(getTransformationMatrix());

        Matrix defaultTransformation = new Matrix();

        final float[] values = extraInfo.getDefaultTransformation();
        if (values != null) {
            defaultTransformation.setValues(values);
        }

        // stored layer information is required
        final List<LayerInfo> layersInfo = extraInfo.getLayersInfo();
        if (layersInfo == null) {
            ToastUtils.show(ctx, R.string.fdb_layer_info_missing_importing_terminated_toast);
            return;
        }
        float defaultTransformationScale = CanvasTransformer.getRealScale(defaultTransformation);

        float[] transformationValue = new float[9];
        defaultTransformation.getValues(transformationValue);

        for (int i = layersInfo.size() - 1; i >= 0; i--) {
            LayerInfo layerInfo = layersInfo.get(i);

            LayerInfo newLayerInfo = new LayerInfo(Layer.randomId(), layerInfo.getName(), layerInfo.getVisible());
            add1Layer(newLayerInfo);
            switchLayer(newLayerInfo.getId());

            if (onImportLayerAddedListener != null) {
                onImportLayerAddedListener.onAdded(newLayerInfo);
            }
            ++layerNumber;
            importLayerPath4_0(
                    db,
                    layerInfo,
                    defaultTransformationScale,
                    transformationValue,
                    progressCallback,
                    layerNumber,
                    layersInfo.size()
            );
        }

        setLockingStrokesFromExtraInfos(extraInfo);

        transformTo(savedTransformation);

        postInvalidate();
    }

    @SuppressWarnings("DuplicatedCode")
    private void importLayerPath3_1(
            SQLite3 db,
            LayerInfo layerInfo,
            float defaultTransformationScale,
            float[] transformationValue,
            PathImportCallback progressCallback,
            int layerNumber,
            int layerCount
    ) {
        final String layerId = layerInfo.getId();

        final String pathTable = "path_layer_" + layerId;
        final int rowCount = SQLite3UtilsKt.getRowCount(db, "SELECT COUNT() FROM " + pathTable, null);

        final Statement statement = db.compileStatement("SELECT mark, p1, p2 FROM " + pathTable);
        final Cursor cursor = statement.getCursor();

        int c = 0;
        while (cursor.step()) {
            // noinspection StatementWithEmptyBody
            while (pathImportPaused && !isImportingTerminated) ;
            if (isImportingTerminated) {
                transformedOnTouchAction(
                        MotionEvent.ACTION_UP,
                        cursor.getFloat(1),
                        cursor.getFloat(2),
                        transformationValue
                );
                break;
            } else {
                spinSleep(drawingInterval);
            }
            int mark = cursor.getInt(0);

            switch (mark) {
                case 0x01:
                    setDrawingColor(cursor.getInt(1), true);
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
            progressCallback.progress((float) c / (float) rowCount, layerInfo.getName(), layerNumber, layerCount);
        }

        statement.release();
    }

    @SuppressWarnings("DuplicatedCode")
    private void importLayerPath4_0(
            SQLite3 db,
            LayerInfo layerInfo,
            float defaultTransformationScale,
            float[] transformationValue,
            PathImportCallback progressCallback,
            int layerNumber,
            int layerCount
    ) {
        final String layerId = layerInfo.getId();

        final String pathTable = "path_layer_" + layerId;
        final int rowCount = SQLite3UtilsKt.getRowCount(db, "SELECT COUNT() FROM " + pathTable, null);

        final Statement statement = db.compileStatement("SELECT mark, info, x, y FROM " + pathTable);
        final Cursor cursor = statement.getCursor();

        int c = 0;
        while (cursor.step()) {
            // noinspection StatementWithEmptyBody
            while (pathImportPaused && !isImportingTerminated) ;
            if (isImportingTerminated) {
                transformedOnTouchAction(
                        MotionEvent.ACTION_UP,
                        cursor.getFloat(2),
                        cursor.getFloat(3),
                        transformationValue
                );
                break;
            } else {
                spinSleep(drawingInterval);
            }
            int mark = cursor.getInt(0);

            switch (mark) {
                case 0x02:
                case 0x12:
                    transformedOnTouchAction(
                            MotionEvent.ACTION_MOVE,
                            cursor.getFloat(2),
                            cursor.getFloat(3),
                            transformationValue
                    );
                    break;
                case 0x01:
                    // TODO: 8/16/21 optimize (byte array allocation, unpack value in machine byte order)
                    final byte[] info = cursor.getBlob(1);
                    int color = pers.zhc.jni.JNI.Struct.unpackInt(info, 0, pers.zhc.jni.JNI.Struct.MODE_LITTLE_ENDIAN);
                    float width = pers.zhc.jni.JNI.Struct.unpackFloat(info, 4, pers.zhc.jni.JNI.Struct.MODE_LITTLE_ENDIAN);
                    float blurRadius = pers.zhc.jni.JNI.Struct.unpackFloat(info, 8, pers.zhc.jni.JNI.Struct.MODE_LITTLE_ENDIAN);
                    setDrawingColor(color, true);

                    final float drawingStrokeWidth = width * defaultTransformationScale / canvasScale;
                    setDrawingStrokeWidth(drawingStrokeWidth);

                    setStrokeHardness(toStrokeHardness(drawingStrokeWidth, blurRadius * defaultTransformationScale / canvasScale));

                    setEraserMode(false);

                    transformedOnTouchAction(
                            MotionEvent.ACTION_DOWN,
                            cursor.getFloat(2),
                            cursor.getFloat(3),
                            transformationValue
                    );
                    break;
                case 0x11:
                    final byte[] info2 = cursor.getBlob(1);
                    int color2 = pers.zhc.jni.JNI.Struct.unpackInt(info2, 0, pers.zhc.jni.JNI.Struct.MODE_LITTLE_ENDIAN);
                    float width2 = pers.zhc.jni.JNI.Struct.unpackFloat(info2, 4, pers.zhc.jni.JNI.Struct.MODE_LITTLE_ENDIAN);
                    float blurRadius2 = pers.zhc.jni.JNI.Struct.unpackFloat(info2, 8, pers.zhc.jni.JNI.Struct.MODE_LITTLE_ENDIAN);

                    setEraserMode(true);
                    setEraserAlpha(color2);
                    float eraserStrokeWidth = width2 * defaultTransformationScale / canvasScale;
                    setEraserStrokeWidth(eraserStrokeWidth);
                    setEraserHardness(toStrokeHardness(eraserStrokeWidth, blurRadius2 * defaultTransformationScale / canvasScale));

                    transformedOnTouchAction(
                            MotionEvent.ACTION_DOWN,
                            cursor.getFloat(2),
                            cursor.getFloat(3),
                            transformationValue
                    );
                    break;
                case 0x03:
                case 0x13:
                    transformedOnTouchAction(
                            MotionEvent.ACTION_UP,
                            cursor.getFloat(2),
                            cursor.getFloat(3),
                            transformationValue
                    );
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
            progressCallback.progress((float) c / (float) rowCount, layerInfo.getName(), layerNumber, layerCount);
        }

        statement.release();
    }

    private final PointF transformedPoint = new PointF();
    private final PointF inverseTransformedPoint = new PointF();

    private void onTouchAction(int motionAction, float x, float y) {
        final float strokeBlurRadius = toBlurRadius(getDrawingStrokeWidth(), strokeHardness);
        final float eraserBlurRadius = toBlurRadius(getEraserStrokeWidth(), eraserHardness);

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
                    layerPathSaverRef.onErasingTouchDown(x, y, getEraserAlpha(), getEraserStrokeWidth(), eraserBlurRadius);
                } else {
                    layerPathSaverRef.onDrawingTouchDown(x, y, getDrawingColor(), getDrawingStrokeWidth(), strokeBlurRadius);
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
                    if (showDrawing) {
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
        updateHardness();
    }

    public void importImage(@NonNull Bitmap imageBitmap, float left, float top, int scaledWidth, int scaledHeight) {
        // TODO: 8/4/21 import image
        /*System.gc();
        Bitmap bitmap = Bitmap.createScaledBitmap(imageBitmap, scaledWidth, scaledHeight, true);
        mBackgroundCanvas.drawBitmap(bitmap, left, top, null);
        if (backgroundBitmap == null) {
            ToastUtils.show(ctx, ctx.getString(R.string.importing_failed));
        } else {
            mCanvas.drawBitmap(backgroundBitmap, 0F, 0F, null);
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

    public void updateStrokeWidthIfLocked() {
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

    public boolean isPathImportPaused() {
        return pathImportPaused;
    }

    public void setPathImportPaused(boolean pathImportPaused) {
        this.pathImportPaused = pathImportPaused;
    }

    public int getDrawingInterval() {
        return drawingInterval;
    }

    public void setDrawingInterval(int drawingInterval) {
        this.drawingInterval = drawingInterval;
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
        updateStrokeWidthIfLocked();
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

    public String add1Layer(LayerInfo layerInfo) {
        final Layer layer = new Layer(width, height, layerInfo);
        layerArray.add(0, layer);
        pathSaver.addNewLayerPathSaver(layerInfo.getId());
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
        if (layerPathSaverRef == null)
            throw new RuntimeException("Not found specific LayerPathSaver in PathSaver");

        mCanvas.setBitmap(bitmapRef);
        canvasTransformer.refresh();
    }

    private int getLayerIndexById(String id) {
        for (int i = 0; i < layerArray.size(); i++) {
            if (layerArray.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private Layer getLayerById(String id) {
        final int i = getLayerIndexById(id);
        if (i != -1) {
            return layerArray.get(i);
        }
        throw new NoSuchElementException();
    }

    public void updateLayerState(@NotNull LayerManagerView.LayerState layerState) {
        final ArrayList<Layer> newLayerArray = new ArrayList<>();
        for (LayerInfo layerInfo : layerState.getOrderList()) {
            final Layer layer = getLayerById(layerInfo.getId());
            layer.setLayerInfo(layerInfo);
            newLayerArray.add(layer);
        }
        layerArray.clear();
        layerArray.addAll(newLayerArray);
        switchLayer(layerState.getCheckedId());

        redrawAllLayerBitmap();

        invalidate();
    }

    public void switchLayer(String id) {
        switchLayer(getLayerIndexById(id));
    }

    private void redrawAllLayerBitmap() {
        for (int i = layerArray.size() - 1; i >= 0; i--) {
            final Layer layer = layerArray.get(i);
            if (layer.isVisible()) {
                layer.redrawBitmap(canvasTransformer.getMatrix());
            }
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

    public String getCurrentLayerId() {
        return layerRef.getId();
    }

    public ArrayList<String> getLayerIds() {
        ArrayList<String> list = new ArrayList<>();
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

    public int getBitmapWidth() {
        return bitmapRef.getWidth();
    }

    public int getBitmapHeight() {
        return bitmapRef.getHeight();
    }

    /**
     * @param hardness in [0.0, 100.0]
     */
    public void setStrokeHardness(float hardness) {
        if (hardness == 100.0) {
            mPaint.setMaskFilter(null);
        } else {
            final float blurRadius = toBlurRadius(getDrawingStrokeWidth(), hardness);
            mPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        }
        this.strokeHardness = hardness;
    }

    /**
     * because `blurRadius` is related to the stroke width
     */
    private void updateHardness() {
        setStrokeHardness(strokeHardness);
        setEraserHardness(eraserHardness);
    }

    public float getStrokeHardness() {
        return strokeHardness;
    }

    public float getEraserHardness() {
        return eraserHardness;
    }

    public void setEraserHardness(float hardness) {
        if (hardness == 100.0) {
            eraserPaint.setMaskFilter(null);
        } else {
            final float blurRadius = toBlurRadius(getEraserStrokeWidth(), hardness);
            eraserPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        }
        this.eraserHardness = hardness;
    }

    /**
     * @param strokeWidth stroke width
     * @param hardness    hardness, in [0.0, 100.0]
     * @return blur radius
     */
    @Contract(pure = true)
    public static float toBlurRadius(float strokeWidth, float hardness) {
        return (float) ((1.0 - ((double) hardness) / 100.0) * (((double) strokeWidth) / 2.0));
    }

    @Contract(pure = true)
    public static float toStrokeHardness(float strokeWidth, float blurRadius) {
        return (float) ((1.0 - ((double) blurRadius) * 2.0 / ((double) strokeWidth)) * 100.0);
    }

    public ExtraInfo getDefaultExtraInfos() {
        float[] defaultTransformation = new float[9];
        new Matrix().getValues(defaultTransformation);
        return new ExtraInfo(
                false,
                10F,
                10F,
                new ArrayList<>(),
                defaultTransformation,
                new ArrayList<>()
        );
    }

    private static class PathImportException extends RuntimeException {
        public PathImportException() {
            super();
        }

        public PathImportException(String message) {
            super(message);
        }
    }

    private static class InvalidExtraInfoException extends PathImportException {
        public InvalidExtraInfoException() {
            super();
        }

        public InvalidExtraInfoException(String message) {
            super(message);
        }
    }

    public interface ImageExportProgressCallback {
        void call(@NotNull ImageExportProgressType type, @NotNull String layerName, float progress);
    }

    public enum ImageExportProgressType {
        REDRAWING,
        COMPRESSING
    }

    private void spinSleep(int microsecond) {
        if (microsecond == 0) return;
        long start = System.nanoTime();
        // noinspection StatementWithEmptyBody
        while (System.nanoTime() - start < (long) microsecond * 1000 && !isImportingTerminated) ;
    }

    public boolean isShowDrawing() {
        return showDrawing;
    }

    public void setShowDrawing(boolean showDrawing) {
        this.showDrawing = showDrawing;
    }

    public interface PathImportCallback {
        void progress(float progress, @Nullable String layerName, int layerNumber, int layerCount);
    }
}
