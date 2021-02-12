package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.GestureResolver;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.Cursor;
import pers.zhc.tools.utils.sqlite.SQLite3;
import pers.zhc.tools.utils.sqlite.Statement;
import pers.zhc.u.CanDoHandler;
import pers.zhc.u.Random;
import pers.zhc.u.ValueInterface;
import pers.zhc.u.common.Documents;

import java.io.*;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author bczhc & (cv...)...
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
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<MyCanvas, Bitmap> bitmapMap;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, MyCanvas> canvasMap;
    private MyCanvas headCanvas;
    /**
     * HEAD, a reference
     */
    private Bitmap headBitmap;
    /**
     * 上次的坐标
     */
    private float mLastX, mLastY;
    private Paint mBitmapPaint;
    /**
     * 使用LinkedList 模拟栈，来保存 Path
     */
    private LinkedList<PathBean> undoList, redoList;
    private Bitmap backgroundBitmap;
    private Canvas mBackgroundCanvas;
    private GestureResolver gestureResolver;
    private boolean dontDrawWhileImporting = false;
    private boolean isLockingStroke = false;
    private float lockedStrokeWidth = 0F;
    private float lockedEraserStrokeWidth;
    private float scaleWhenLocked = 1F;
    private OnColorChangedCallback onColorChangedCallback = null;
    private Bitmap transBitmap;
    private MyCanvas transCanvas;
    private PathSaver pathSaver = null;

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

    private void createBitmap() {
        System.gc();
        headBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        backgroundBitmap = Bitmap.createBitmap(headBitmap);
        headCanvas = new MyCanvas(headBitmap);
        mBackgroundCanvas = new Canvas(backgroundBitmap);
        //抗锯齿
        headCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
    }

    /**
     * Initialize.
     */
    private void init() {
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

        undoList = new LinkedList<>();
        redoList = new LinkedList<>();
        this.gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {
            @Override
            public void onTwoPointsScroll(float distanceX, float distanceY, MotionEvent event) {
                headCanvas.invertTranslate(distanceX, distanceY);
                if (transCanvas != null) {
                    transCanvas.invertTranslate(distanceX, distanceY);
                }
            }

            @Override
            public void onTwoPointsZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float dScale, MotionEvent event) {
                headCanvas.invertScale(dScale, midPointX, midPointY);
                if (transCanvas != null) {
                    transCanvas.invertScale(dScale, midPointX, midPointY);
                }
                setCurrentStrokeWidthWithLockedStrokeWidth();
            }

            @Override
            public void onTwoPointsUp() {
                transBitmap = null;
                redrawCanvas();
            }

            @Override
            public void onTwoPointsDown() {
                mPath = null;
                if (transBitmap == null) {
                    transBitmap = Bitmap.createBitmap(headBitmap);
                    transCanvas = new MyCanvas(transBitmap);
                }
            }

            @Override
            public void onOnePointScroll(float distanceX, float distanceY, MotionEvent event) {

            }

            @Override
            public void onTwoPointsPress() {
                if (transBitmap != null) {
                    float startPointX = headCanvas.getStartPointX();
                    float startPointY = headCanvas.getStartPointY();
                    float scale = headCanvas.getScale();
                    MyCanvas c = new MyCanvas(transBitmap);
                    c.transTo(startPointX, startPointY, scale);
                    c.drawBitmap(headBitmap, 0, 0, mBitmapPaint);
                }
                postInvalidate();
            }
        });
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
        pathSaver.undo();
        if (!undoList.isEmpty()) {
            clearPaint();//清除之前绘制内容
            PathBean lastPb = undoList.removeLast();//将最后一个移除
            redoList.add(lastPb);//加入 恢复操作
            //遍历，将Path重新绘制到 headCanvas
            if (backgroundBitmap != null) {
                headCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
            }
            if (!dontDrawWhileImporting) {
                for (PathBean pb : undoList) {
                    headCanvas.drawPath(pb.path, pb.paint);
                }
                postInvalidate();
            }
        }
    }

    /**
     * 恢复操作
     */
    public void redo() {
        pathSaver.redo();
        if (!redoList.isEmpty()) {
            PathBean pathBean = redoList.removeLast();
            headCanvas.drawPath(pathBean.path, pathBean.paint);
            undoList.add(pathBean);
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
        redoList.clear();
        undoList.clear();
        mBackgroundCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
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
        Handler handler = new Handler();
        ToastUtils.show(ctx, R.string.saving);
        System.gc();
        Bitmap exportedBitmap = Bitmap.createBitmap(exportedWidth, exportHeight, Bitmap.Config.ARGB_8888);
        MyCanvas myCanvas = new MyCanvas(exportedBitmap);
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
                    ToastUtils.show(ctx, ctx.getString(R.string.saving_success) + "\n" + f.toString());
                } else {
                    ToastUtils.show(ctx, R.string.saving_failure);
                }
            });
        }).start();
    }

    /**
     * 是否可以撤销
     */
    public boolean canUndo() {
        return undoList.isEmpty();
    }

    /**
     * 是否可以恢复
     */
    public boolean canRedo() {
        return redoList.isEmpty();
    }

    /**
     * 清除绘制内容
     * 直接绘制白色背景
     */
    private void clearPaint() {
        headCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
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
        if (headBitmap == null) createBitmap();
        if (dontDrawWhileImporting) {
            return;
        }
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            if (transBitmap == null) {
                if (headBitmap != null) {
                    canvas.drawBitmap(headBitmap, 0, 0, mBitmapPaint);//将mBitmap绘制在canvas上,最终的显示
                    if (!dontDrawWhileImporting) {
                        if (mPath != null) {//显示实时正在绘制的path轨迹
                            float mCanvasScale = headCanvas.getScale();
                            canvas.translate(headCanvas.getStartPointX(), headCanvas.getStartPointY());
                            canvas.scale(mCanvasScale, mCanvasScale);
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
                transCanvas.drawBitmap(headBitmap, 0, 0, mBitmapPaint);
                canvas.drawBitmap(transBitmap, 0, 0, mBitmapPaint);
            }
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
        this.width = measuredW;
        this.height = measuredH;
    }

    /**
     * 导入路径
     *
     * @param f                   路径文件
     * @param doneAction          完成回调接口
     * @param floatValueInterface 进度回调接口
     */
    @SuppressWarnings("BusyWait")
    public void importPathFile(File f, Runnable doneAction, @Nullable ValueInterface<Float> floatValueInterface, int speedDelayMillis) {
        dontDrawWhileImporting = speedDelayMillis == 0;
        if (floatValueInterface != null) {
            floatValueInterface.f(0F);
        }
        CanDoHandler<Float> canDoHandler = new CanDoHandler<>(aFloat -> {
            if (floatValueInterface != null && aFloat != null) {
                floatValueInterface.f(aFloat);
            }
        });
        canDoHandler.start();
        Handler handler = new Handler();
        Thread thread = new Thread(() -> {
            SQLite3 open = SQLite3.open(f.getPath());
            if (!open.checkIfCorrupt()) {
                importPathVer3(open, canDoHandler, doneAction, speedDelayMillis);
                dontDrawWhileImporting = false;
                redrawCanvas();
                postInvalidate();
                return;
            }
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(f, "r");
                byte[] head = new byte[12];
                raf.read(head);
                raf.seek(0);
                StringBuilder sb = new StringBuilder();
                for (byte b : head) {
                    sb.append((char) b);
                }
                String headString = sb.toString();
                long length = f.length(), read;
                byte[] bytes;
                float x, y, strokeWidth;
                int color;
                switch (headString) {
                    case "path ver 2.0":
                        handler.post(() -> ToastUtils.show(ctx, R.string.import_old_2_0));
                        raf.skipBytes(12);
                        bytes = new byte[12];
                        read = 0L;
                        int lastP1, p1 = -1;
                        x = -1;
                        y = -1;
                        while (raf.read(bytes) != -1) {
                            Thread.sleep(speedDelayMillis);
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
                            canDoHandler.push(((float) read) * 100F / ((float) length));
                        }
                        break;
                    case "path ver 2.1":
                        //512 * 9
                        int bufferSize = 2304;
                        handler.post(() -> ToastUtils.show(ctx, R.string.import_2_1));
                        raf.skipBytes(12);
                        byte[] buffer = new byte[bufferSize];
                        int bufferRead;
                        read = 0L;
                        while ((bufferRead = raf.read(buffer)) != -1) {
                            int a = bufferRead / 9;
                            for (int i = 0; i < a; i++) {
                                Thread.sleep(speedDelayMillis);
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
                                canDoHandler.push(((float) read) * 100F / ((float) length));
                            }
                        }
                        break;
                    default:
                        handler.post(() -> ToastUtils.show(ctx, R.string.import_old));
                        bytes = new byte[26];
                        byte[] bytes_4 = new byte[4];
                        read = 0L;
                        while (raf.read(bytes) != -1) {
                            Thread.sleep(speedDelayMillis);
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
                                        motionAction = Random.ran_sc(0, 2);
                                    }
                                    if (strokeWidth <= 0) {
                                        strokeWidth = Random.ran_sc(1, 800);
                                    }
                                    if (eraserStrokeWidth <= 0) {
                                        eraserStrokeWidth = Random.ran_sc(1, 800);
                                    }
                                    setEraserMode(bytes[24] == 1);
                                    setEraserStrokeWidth(eraserStrokeWidth);
                                    setDrawingColor(color);
                                    setDrawingStrokeWidth(strokeWidth);
                                    onTouchAction(motionAction, x, y);
                                    canDoHandler.push(((float) read) * 100F / ((float) length));
                                    break;
                            }
                        }
                        break;
                }
                canDoHandler.stop();
                doneAction.run();
                ((FloatingDrawingBoardMainActivity) ctx).strokeColorHSVA.set(getDrawingColor());
            } catch (IOException | InterruptedException e) {
                handler.post(() -> ToastUtils.showError(ctx, R.string.read_error, e));
            } finally {
                dontDrawWhileImporting = false;
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                redrawCanvas();
                postInvalidate();
            }
        });
        thread.start();
    }

    private void importPathVer3(@NotNull SQLite3 db, CanDoHandler<Float> canDoHandler, Runnable doneAction, int speedDelayMillis) {
        int[] recordNum = {0};
        db.exec("SELECT * FROM path", contents -> {
            ++recordNum[0];
            return 0;
        });
        System.out.println("record num: " + recordNum[0]);
        dontDrawWhileImporting = speedDelayMillis == 0;
        Statement statement;
        try {
            statement = db.compileStatement("SELECT * FROM path");
        } catch (Exception e) {
            Common.showException(e, (Activity) ctx);
            return;
        }

        Cursor cursor = statement.getCursor();
        int c = 0;
        while (cursor.step()) {
            int mark = cursor.getInt(0);
            int p1 = cursor.getInt(1);
            int p2 = cursor.getInt(2);

            switch (mark) {
                case 0x01:
                    setDrawingColor(p1);
                    setDrawingStrokeWidth(Float.intBitsToFloat(p2));
                    setEraserMode(false);
                    break;
                case 0x02:
                case 0x12:
                    onTouchAction(MotionEvent.ACTION_DOWN, Float.intBitsToFloat(p1), Float.intBitsToFloat(p2));
                    break;
                case 0x03:
                case 0x13:
                    onTouchAction(MotionEvent.ACTION_MOVE, Float.intBitsToFloat(p1), Float.intBitsToFloat(p2));
                    break;
                case 0x04:
                case 0x14:
                    onTouchAction(MotionEvent.ACTION_UP, Float.intBitsToFloat(p1), Float.intBitsToFloat(p2));
                    break;
                case 0x11:
                    setEraserMode(true);
                    setEraserAlpha(p1);
                    setEraserStrokeWidth(Float.intBitsToFloat(p2));
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
            canDoHandler.push(((float) c) / ((float) recordNum[0]) * 100F);
            if (!dontDrawWhileImporting) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(speedDelayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        doneAction.run();

        try {
            statement.release();
        } catch (Exception e) {
            Common.showException(e, (Activity) ctx);
        }
    }

    private void onTouchAction(int motionAction, float x, float y) {
        float startPointX = headCanvas.getStartPointX();
        float startPointY = headCanvas.getStartPointY();
        float canvasScale = headCanvas.getScale();
        x = (x - startPointX) / canvasScale;
        y = (y - startPointY) / canvasScale;
        switch (motionAction) {
            case MotionEvent.ACTION_DOWN:
                pathSaver.onTouchDown(x, y);
                //路径
                mPath = new Path();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                pathSaver.onTouchMove(x, y);
                if (mPath != null) {
                    float dx = Math.abs(x - mLastX);
                    float dy = Math.abs(y - mLastY);
                    if (dx >= 0 || dy >= 0) {//绘制的最小距离 0px
                        //利用二阶贝塞尔曲线，使绘制路径更加圆滑
                        mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
                    }
                    mLastX = x;
                    mLastY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                pathSaver.onTouchUp(x, y);
                if (mPath != null) {
                    if (!dontDrawWhileImporting) {
                        headCanvas.drawPath(mPath, mPaintRef);//将路径绘制在mBitmap上
                    }
                    Path path = new Path(mPath);//复制出一份mPath
                    Paint paint = new Paint(mPaintRef);
                    PathBean pb = new PathBean(path, paint);
                    // for undoing
                    undoList.add(pb);
                    redoList.clear();
                    mPath.reset();
                    mPath = null;
                }
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

    public void importImage(@Documents.NotNull Bitmap imageBitmap, float left, float top, int scaledWidth, int scaledHeight) {
        System.gc();
        Bitmap bitmap = Bitmap.createScaledBitmap(imageBitmap, scaledWidth, scaledHeight, true);
        mBackgroundCanvas.drawBitmap(bitmap, left, top, mBitmapPaint);
        if (backgroundBitmap == null) {
            ToastUtils.show(ctx, ctx.getString(R.string.importing_failure));
        } else {
            headCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
        }
        postInvalidate();
    }

    public void resetTransform() {
        headCanvas.reset();
        redrawCanvas();
        postInvalidate();
        setCurrentStrokeWidthWithLockedStrokeWidth();
    }

    /**
     * 把路径绘制到缓冲Bitmap上
     */
    private void redrawCanvas() {
        headCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        headCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
        for (PathBean pathBean : this.undoList) {
            headCanvas.drawPath(pathBean.path, pathBean.paint);
        }
    }

    MyCanvas getCanvas() {
        return headCanvas;
    }

    public float getStrokeWidthInUse() {
        return mPaintRef.getStrokeWidth();
    }

    public boolean isLockingStroke() {
        return this.isLockingStroke;
    }

    public void bitmapResolution(@NotNull Point point) {
        point.x = headBitmap.getWidth();
        point.y = headBitmap.getHeight();
    }

    public void setLockStrokeMode(boolean mode) {
        this.isLockingStroke = mode;
    }

    public void lockStroke() {
        if (isLockingStroke) {
            this.lockedStrokeWidth = getDrawingStrokeWidth();
            this.lockedEraserStrokeWidth = getEraserStrokeWidth();
            this.scaleWhenLocked = headCanvas.getScale();
            setCurrentStrokeWidthWithLockedStrokeWidth();
        }
    }

    public void setCurrentStrokeWidthWithLockedStrokeWidth() {
        if (isLockingStroke) {
            float mCanvasScale = headCanvas.getScale();
            setDrawingStrokeWidth(lockedStrokeWidth * scaleWhenLocked / mCanvasScale);
            setEraserStrokeWidth(lockedEraserStrokeWidth * scaleWhenLocked / mCanvasScale);
        }
    }

    public void changeHead(String id) {
        headCanvas = canvasMap.get(id);
        headBitmap = bitmapMap.get(headCanvas);
    }

    public float getScale() {
        return headCanvas.getScale();
    }

    public float getZoomedStrokeWidthInUse() {
        return getScale() * getStrokeWidthInUse();
    }

    /**
     * 路径集合
     */
    public static class PathBean {
        final Path path;
        final Paint paint;

        PathBean(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }

    /**
     * Enable to save path, so that you can export path file later.
     *
     * @param tempFile the temporary file to record the strokes, etc. If this parameter is null, the method call is invalid.
     */
    public void enableSavePath(String tempFile) {
        if (tempFile == null) return;
        pathSaver = new PathSaver(SQLite3.open(tempFile));
    }

    /**
     * Path saver.
     * <p>One record structure in data saved:</p>
     * <h3>&lt;mark&gt;(1bytes) &lt;p1&gt;(4bytes) &lt;p2&gt;(4bytes)</h3>
     * When {@link MotionEvent#getAction()} is {@link MotionEvent#ACTION_DOWN}, it'll record 2 records.
     * <ul>
     *     <li>
     *         drawing:<br/>
     *         let action = {@link MotionEvent#getAction()}<br/>
     *         if action is:
     *         <ul>
     *             <li>
     *                 {@link MotionEvent#ACTION_DOWN}:
     *                 <ul>
     *                     record 1:
     *                     <li>mark: {@code 0x01}</li>
     *                     <li>p1: paint color as {@code int}</li>
     *                     <li>p2: stroke width, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *                 <ul>
     *                     record 2:
     *                     <li>mark: {@code 0x02}</li>
     *                     <li>p1: x touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                     <li>p2: y touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *             </li>
     *             <li>
     *                 {@link MotionEvent#ACTION_MOVE}:
     *                 <ul>
     *                     <li>mark: {@code 0x03}</li>
     *                     <li>p1: x touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                     <li>p2: y touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *             </li>
     *             <li>
     *                 {@link MotionEvent#ACTION_UP}
     *                 <ul>
     *                     <li>mark {@code 0x04}</li>
     *                     <li>p1: x touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                     <li>p2: y touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         erasing:<br/>
     *         let action = {@link MotionEvent#getAction()}<br/>
     *         if action is:
     *         <ul>
     *             <li>
     *                 {@link MotionEvent#ACTION_DOWN}:
     *                 <ul>
     *                     record 1:
     *                     <li>mark: {@code 0x11}</li>
     *                     <li>p1: eraser transparency (alpha value) as {@code int}</li>
     *                     <li>p2: stroke width, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *                 <ul>
     *                     record 2:
     *                     <li>mark: {@code 0x12}</li>
     *                     <li>p1: x touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                     <li>p2: y touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *             </li>
     *             <li>
     *                 {@link MotionEvent#ACTION_MOVE}:
     *                 <ul>
     *                     <li>mark: {@code 0x13}</li>
     *                     <li>p1: x touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                     <li>p2: y touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *             </li>
     *             <li>
     *                 {@link MotionEvent#ACTION_UP}
     *                 <ul>
     *                     <li>mark {@code 0x14}</li>
     *                     <li>p1: x touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                     <li>p2: y touch point coordinates, int value, converted by calling {@link Float#floatToRawIntBits(float)}</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>
     *         Undo:
     *         <ul>
     *             <li>mark: {@code 0x20}</li>
     *             <li>p1: {@code 0x00}</li>
     *             <li>p2: {@code 0x00}</li>
     *         </ul>
     *     </li>
     *     <li>
     *         Redo:
     *         <ul>
     *             <li>mark: {@code 0x30}</li>
     *             <li>p1: {@code 0x00}</li>
     *             <li>p2: {@code 0x00}</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private class PathSaver {
        private final SQLite3 tempPathDatabase;
        private Statement statement;

        private void configureDatabase() {
            // create table
            tempPathDatabase.exec("CREATE TABLE IF NOT EXISTS path\n" +
                    "(\n" +
                    "    mark INTEGER,\n" +
                    "    p1   INTEGER,\n" +
                    "    p2   INTEGER\n" +
                    ")");
            tempPathDatabase.exec("BEGIN TRANSACTION");
            // prepare statement
            try {
                statement = tempPathDatabase.compileStatement("INSERT INTO path\n" +
                        "VALUES (?, ?, ?)");
            } catch (Exception e) {
                Common.showException(e, (Activity) PaintView.this.ctx);
            }
        }

        private PathSaver(SQLite3 tempPathDatabase) {
            this.tempPathDatabase = tempPathDatabase;
            configureDatabase();
        }

        private void undo() {
            insert(0x20, 0x00, 0x00);
        }

        private void redo() {
            insert(0x30, 0x00, 0x00);
        }

        private void insert(int mark, int p1, int p2) {
            try {
                statement.reset();
                statement.bind(1, mark);
                statement.bind(2, p1);
                statement.bind(3, p2);
                statement.step();
            } catch (Exception e) {
                Common.showException(e, (Activity) PaintView.this.ctx);
            }
        }

        private void onTouchDown(float x, float y) {
            insert(eraserMode ? 0x11 : 0x01, getDrawingColor(), Float.floatToRawIntBits(getStrokeWidthInUse()));
            insert(eraserMode ? 0x12 : 0x02, Float.floatToRawIntBits(x), Float.floatToRawIntBits(y));
        }

        private void onTouchMove(float x, float y) {
            insert(eraserMode ? 0x13 : 0x03, Float.floatToRawIntBits(x), Float.floatToRawIntBits(y));
        }

        private void onTouchUp(float x, float y) {
            insert(eraserMode ? 0x14 : 0x04, Float.floatToRawIntBits(x), Float.floatToRawIntBits(y));
        }

        private void commitDatabase() {
            tempPathDatabase.exec("COMMIT");
            tempPathDatabase.exec("BEGIN TRANSACTION");
        }
    }

    public void commitPathDatabase() {
        if (pathSaver != null) {
            pathSaver.commitDatabase();
        }
    }

    public void closePathDatabase() {
        if (pathSaver == null) return;
        try {
            pathSaver.commitDatabase();
        } catch (Exception ignored) {
        }
        pathSaver.tempPathDatabase.close();
    }
}