package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import pers.zhc.tools.R;
import pers.zhc.tools.jni.JNI;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.GestureResolver;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.tools.utils.sqlite.MySQLite3;
import pers.zhc.u.CanDoHandler;
import pers.zhc.u.Random;
import pers.zhc.u.ValueInterface;
import pers.zhc.u.common.Documents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author bczhc & (cv...)...
 */
@SuppressLint("ViewConstructor")
public class PaintView extends View {
    private final File internalPathFile;
    private final int height;
    private final int width;
    private final Context ctx;
    boolean isEraserMode;
    private Paint mPaint;
    private Path mPath;
    private Paint eraserPaint;
    private Paint mPaintRef = null;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<MyCanvas, Bitmap> bitmapMap;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, MyCanvas> canvasMap;
    private MyCanvas headCanvas;
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
    private MySQLite3 savePathDatabase;

    public PaintView(Context context, int width, int height, File internalPathFile) {
        super(context);
        ctx = context;
        this.internalPathFile = internalPathFile;
        this.width = width;
        this.height = height;
        init();
    }

    public void setOnColorChangedCallback(OnColorChangedCallback onColorChangedCallback) {
        this.onColorChangedCallback = onColorChangedCallback;
    }

    /***
     * 初始化
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
        //保存签名的画布
        //拿到控件的宽和高
        post(() -> {
            //获取PaintView的宽和高
            //由于橡皮擦使用的是 Color.TRANSPARENT ,不能使用RGB-565
            System.gc();
            headBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            backgroundBitmap = Bitmap.createBitmap(headBitmap);
            headCanvas = new MyCanvas(headBitmap);
            mBackgroundCanvas = new Canvas(backgroundBitmap);
            //抗锯齿
            headCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            //背景色
        });

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

    void configPathDatabase() {
        savePathDatabase = MySQLite3.open(internalPathFile.getPath());
        savePathDatabase.exec("CREATE TABLE IF NOT EXISTS info (\n" +
                "    " +
                "version text,\n" +
                "    creation_timestamp long,\n" +
                "    modification_timestamp long\n" +
                ");", null);
        savePathDatabase.exec("CREATE TABLE IF NOT EXISTS path (\n" +
                "    mark char,\n" +
                "    num1 number,\n" +
                "    num2 number\n" +
                ");", null);
        savePathDatabase.exec("BEGIN TRANSACTION", null);
        boolean putCreationTime = false;
        if (!savePathDatabase.hasTable("info")) putCreationTime = true;
        final long currentTimeMillis = System.currentTimeMillis();
        if (putCreationTime) {
            savePathDatabase.exec("INSERT INTO info VALUES(" + currentTimeMillis + "," + currentTimeMillis + ")", null);
        } else savePathDatabase.exec("UPDATE info set modification_timestamp=" + currentTimeMillis, null);
    }

    float getStrokeWidth() {
        return this.mPaint.getStrokeWidth();
    }

    void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    int getPaintColor() {
        return this.mPaint.getColor();
    }

    /**
     * 设置画笔颜色
     */
    void setPaintColor(@ColorInt int color) {
        mPaint.setColor(color);
        if (this.onColorChangedCallback != null) {
            onColorChangedCallback.change(color);
        }
    }

    /**
     * 撤销操作
     */
    void undo() {
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
        pathInsert(0xC1);
    }

    private void pathInsert(int mark, float num1, int num2) {
        savePathDatabase.exec("INSERT INTO path VALUES(" + mark + "," + num1 + "," + num2 + ")", null);
    }

    private void pathInsert(int mark) {
        savePathDatabase.exec("INSERT INTO path VALUES(" + mark + ",null,null)", null);
    }

    private void pathInsert(int mark, float num1, float num2) {
        savePathDatabase.exec("INSERT INTO path VALUES(" + mark + "," + num1 + "," + num2 + ")", null);
    }

    /**
     * 恢复操作
     */
    void redo() {
        if (!redoList.isEmpty()) {
            PathBean pathBean = redoList.removeLast();
            headCanvas.drawPath(pathBean.path, pathBean.paint);
            undoList.add(pathBean);
            if (!dontDrawWhileImporting) {
                postInvalidate();
            }
        }
        pathInsert(0xC2);
    }

    int getEraserAlpha() {
        return this.eraserPaint.getAlpha();
    }

    void setEraserAlpha(@IntRange(from = 0, to = 255) int alpha) {
        this.eraserPaint.setAlpha(alpha);
    }

    /**
     * 清空，包括撤销和恢复操作列表
     */
    void clearAll() {
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
    void setEraserMode(boolean isEraserMode) {
        this.isEraserMode = isEraserMode;
        this.mPaintRef = isEraserMode ? eraserPaint : mPaint;
    }

    /**
     * 导出图片
     */
    void exportImg(File f, int exportedWidth, int exportHeight) {
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
                Common.showException(e, ctx);
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
    @SuppressWarnings("unused")
    public boolean isCanUndo() {
        return undoList.isEmpty();
    }

    /**
     * 是否可以恢复
     */
    @SuppressWarnings("unused")
    public boolean isCanRedo() {
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
                            if (isEraserMode) {
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
     * 测量
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (wSpecMode == MeasureSpec.EXACTLY && hSpecMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        } else if (wSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(200, hSpecSize);
        } else if (hSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(wSpecSize, 200);
        }
    }

    /**
     * 导入路径
     *
     * @param f                   路径文件
     * @param doneAction          完成回调接口
     * @param floatValueInterface 进度回调接口
     *                            路径存储结构：
     *                            <p>一条笔迹中一个记录点或一个操作记录为数据库一条记录，参阅 {@link #configPathDatabase()}</p>
     *                            <p>标记，绘画路径开始为0xA1，橡皮擦路径开始为0xA2</p>
     *                            <p>按下事件（紧接着绘画路径开始后）为0xB1，抬起事件（路径结束）为0xB2，移动事件（路径中）为0xB3；
     *                            撤销为0xC1，恢复为0xC2。(byte)</p>
     *                            <p>如果标记为0xA1，排列结构：标记(byte)+笔迹宽度(float)+颜色(int)</p>
     *                            <p>如果标记为0xA2，排列结构：标记(byte)+橡皮擦宽度(float)+像皮擦颜色（alpha信息）(int)</p>
     *                            <p>如果标记为0xB1或0xB2或0xB3，排列结构：标记(byte)+x坐标(float)+y坐标(float)</p>
     *                            <p>如果标记为0xC1或0xC2，则后8字节忽略。</p>
     */
    @SuppressWarnings("BusyWait")
    void importPathFile(File f, Runnable doneAction, @Nullable ValueInterface<Float> floatValueInterface, int speedDelayMillis) {
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
            try {
                SQLiteDatabase db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                importPathVer3(db, canDoHandler, doneAction, speedDelayMillis);
                return;
            } catch (Exception ignored) {
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
                                    if (isEraserMode) {
                                        setEraserStrokeWidth(strokeWidth);
                                    } else {
                                        setStrokeWidth(strokeWidth);
                                        setPaintColor(color);
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
                                    setPaintColor(color);
                                    setStrokeWidth(strokeWidth);
                                    onTouchAction(motionAction, x, y);
                                    canDoHandler.push(((float) read) * 100F / ((float) length));
                                    break;
                            }
                        }
                        break;
                }
                canDoHandler.stop();
                doneAction.run();
                ((FloatingDrawingBoardMainActivity) ctx).strokeColorHSVA.set(getPaintColor());
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

    private void importPathVer3(SQLiteDatabase db, CanDoHandler<Float> canDoHandler, Runnable doneAction, int speedDelayMillis) {
        Cursor cursor = db.rawQuery("SELECT * FROM path", null);
        final int markColumnIndex = cursor.getColumnIndex("mark");
        final int num1ColumnIndex = cursor.getColumnIndex("num1");
        final int num2ColumnIndex = cursor.getColumnIndex("num2");
        if (cursor.moveToFirst()) {
            do {
                switch (cursor.getShort(markColumnIndex)) {
                    case 0xA1:
                        setEraserMode(false);
                        mPaintRef.setStrokeWidth(cursor.getFloat(num1ColumnIndex));
                        mPaintRef.setColor(cursor.getInt(num2ColumnIndex));
                        break;
                    case 0xA2:
                        setEraserMode(true);
                        mPaintRef.setStrokeWidth(cursor.getFloat(num1ColumnIndex));
                        mPaintRef.setColor(cursor.getInt(num2ColumnIndex));
                        break;
                    case 0xB1:
                        onTouchAction(MotionEvent.ACTION_DOWN, cursor.getInt(num1ColumnIndex), cursor.getInt(num2ColumnIndex));
                        break;
                    case 0xB2:
                        onTouchAction(MotionEvent.ACTION_UP, cursor.getInt(num1ColumnIndex), cursor.getInt(num2ColumnIndex));
                        break;
                    case 0xB3:
                        onTouchAction(MotionEvent.ACTION_MOVE, cursor.getInt(num1ColumnIndex), cursor.getInt(num2ColumnIndex));
                        break;
                    case 0xC1:
                        undo();
                        break;
                    case 0xC2:
                        redo();
                        break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        dontDrawWhileImporting = false;
        redrawCanvas();
        postInvalidate();
        doneAction.run();
    }

    private void onTouchAction(int motionAction, float x, float y) {
        float startPointX = headCanvas.getStartPointX();
        float startPointY = headCanvas.getStartPointY();
        float canvasScale = headCanvas.getScale();
        x = (x - startPointX) / canvasScale;
        y = (y - startPointY) / canvasScale;
        switch (motionAction) {
            case MotionEvent.ACTION_MOVE:
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
                pathInsert(0xB3, x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (mPath != null) {
                    if (!dontDrawWhileImporting) {
                        headCanvas.drawPath(mPath, mPaintRef);//将路径绘制在mBitmap上
                    }
                    Path path = new Path(mPath);//复制出一份mPath
                    Paint paint = new Paint(mPaintRef);
                    PathBean pb = new PathBean(path, paint);
                    undoList.add(pb);//将路径对象存入集合
                    mPath.reset();
                    mPath = null;
                }
                pathInsert(0xB2, x, y);
                break;
            case MotionEvent.ACTION_DOWN:
                //路径
                mPath = new Path();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                pathInsert(isEraserMode ? 0xA2 : 0xA1, mPaintRef.getStrokeWidth(), mPaintRef.getColor());
                pathInsert(0xB1, x, y);
                break;
        }
        postInvalidate();
    }

    float getEraserStrokeWidth() {
        return eraserPaint.getStrokeWidth();
    }

    void setEraserStrokeWidth(float w) {
        eraserPaint.setStrokeWidth(w);
    }

    void importImage(@Documents.NotNull Bitmap imageBitmap, float left, float top, int scaledWidth, int scaledHeight) {
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

    void resetTransform() {
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

    void bitmapResolution(Point point) {
        point.x = headBitmap.getWidth();
        point.y = headBitmap.getHeight();
    }

    void setLockStrokeMode(boolean mode) {
        this.isLockingStroke = mode;
    }

    void lockStroke() {
        if (isLockingStroke) {
            this.lockedStrokeWidth = getStrokeWidth();
            this.lockedEraserStrokeWidth = getEraserStrokeWidth();
            this.scaleWhenLocked = headCanvas.getScale();
            setCurrentStrokeWidthWithLockedStrokeWidth();
        }
    }

    void setCurrentStrokeWidthWithLockedStrokeWidth() {
        if (isLockingStroke) {
            float mCanvasScale = headCanvas.getScale();
            setStrokeWidth(lockedStrokeWidth * scaleWhenLocked / mCanvasScale);
            setEraserStrokeWidth(lockedEraserStrokeWidth * scaleWhenLocked / mCanvasScale);
        }
    }

    @SuppressWarnings("unused")
    void changeHead(String id) {
        headCanvas = canvasMap.get(id);
        headBitmap = bitmapMap.get(headCanvas);
    }

    float getScale() {
        return headCanvas.getScale();
    }

    float getZoomedStrokeWidthInUse() {
        return getScale() * getStrokeWidthInUse();
    }

    public void releasePathDatabase() {
        savePathDatabase.close();
    }

    public void commitDB() {
        savePathDatabase.exec("COMMIT", null);
    }

    /**
     * 路径集合
     */
    static class PathBean {
        final Path path;
        final Paint paint;

        PathBean(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }
}
