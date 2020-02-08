package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.view.MotionEvent;
import android.view.View;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.tools.utils.GestureResolver;
import pers.zhc.tools.utils.ToastUtils;
import pers.zhc.u.Random;
import pers.zhc.u.ValueInterface;
import pers.zhc.u.common.Documents;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressLint("ViewConstructor")
public class PaintView extends View {
    private final File internalPathFile;
    private final int height;
    private final int width;
    boolean isEraserMode;
    private OutputStream os;
    private Paint mPaint;
    private Path mPath;
    private Paint eraserPaint;
    private Paint mPaintRef = null;
    private MyCanvas mCanvas;
    private Bitmap mBitmap;
    private float mLastX, mLastY;//上次的坐标
    private Paint mBitmapPaint;
    //使用LinkedList 模拟栈，来保存 Path
    private LinkedList<PathBean> undoList;
    private LinkedList<PathBean> redoList;
    private JNI jni = new JNI();
    private Context ctx;
    private Bitmap backgroundBitmap;
    private Canvas mBackgroundCanvas;
    private GestureResolver gestureResolver;
    private List<byte[]> savedData = null;
    private byte[] data = null;
    private boolean importingPath = false;
    private boolean isLockingStroke = false;
    private float lockedStrokeWidth = 0F;
    private float lockedEraserStrokeWidth;
    private float scaleWhenLocked = 1F;

    PaintView(Context context, int width, int height, File internalPathFile) {
        super(context);
        ctx = context;
        this.internalPathFile = internalPathFile;
        this.width = width;
        this.height = height;
        init();
        Paint zP = new Paint();
        zP.setColor(Color.parseColor("#5000ff00"));
    }

    void setOS(File file, boolean append) {
        try {
            os = new FileOutputStream(file, append);
            try {
                byte[] headInfo = "path ver 2.1".getBytes();
                if (headInfo.length != 12) {
                    Common.showException(new Exception("native error"), (Activity) ctx);
                }
                os.write(headInfo);
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            Common.showException(e, (Activity) ctx);
        }
    }

    /***
     * 初始化
     */
    private void init() {
        setOS(internalPathFile, true);
        setEraserMode(false);
        eraserPaint = new Paint();
        eraserPaint.setColor(Color.TRANSPARENT);
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraserPaint.setAntiAlias(true);
        eraserPaint.setStyle(Paint.Style.STROKE);
        eraserPaint.setStrokeJoin(Paint.Join.ROUND);//使画笔更加圆润
        eraserPaint.setStrokeCap(Paint.Cap.ROUND);//同上
        //关闭硬件加速
        //否则橡皮擦模式下，设置的 PorterDuff.Mode.CLEAR ，实时绘制的轨迹是黑色
//        setBackgroundColor(Color.WHITE);//设置白色背景
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaintRef = mPaint;
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);//使画笔更加圆润
        mPaint.setStrokeCap(Paint.Cap.ROUND);//同上
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        //保存签名的画布
        //拿到控件的宽和高
        post(() -> {
            //获取PaintView的宽和高
            //由于橡皮擦使用的是 Color.TRANSPARENT ,不能使用RGB-565
            System.gc();
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            backgroundBitmap = Bitmap.createBitmap(mBitmap);
            mCanvas = new MyCanvas(mBitmap);
            mBackgroundCanvas = new Canvas(backgroundBitmap);
            //抗锯齿
            mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            //背景色
//                mCanvas.drawColor(Color.WHITE);
        });

        undoList = new LinkedList<>();
        redoList = new LinkedList<>();
        this.gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {
            @Override
            public void onTwoPointsScroll(float distanceX, float distanceY, MotionEvent event) {
                mCanvas.invertTranslate(distanceX, distanceY);
            }

            @Override
            public void onTwoPointsZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float dScale, MotionEvent event) {
                mCanvas.invertScale(dScale, midPointX, midPointY);
                setCurrentStrokeWidthWithLockedStrokeWidth();
            }

            @Override
            public void onTwoPointsUp() {
            }

            @Override
            public void onTwoPointsDown() {
                mPath = null;
                savedData = null;
            }

            @Override
            public void onTwoPointPress() {
                redrawCanvas();
            }
        });
    }

    /**
     * 绘制
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);//将mBitmap绘制在canvas上,最终的显示
            if (!importingPath) {
                if (mPath != null) {//显示实时正在绘制的path轨迹
                    float mCanvasScale = mCanvas.getScale();
                    canvas.translate(mCanvas.getStartPointX(), mCanvas.getStartPointY());
                    canvas.scale(mCanvasScale, mCanvasScale);
                    if (isEraserMode) canvas.drawPath(mPath, eraserPaint);
                    else canvas.drawPath(mPath, mPaint);
                }
            }
        }
    }

    float getStrokeWidth() {
        return this.mPaint.getStrokeWidth();
    }

    void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    int getColor() {
        return this.mPaint.getColor();
    }

    /**
     * 撤销操作
     */
    void undo() {
        data = new byte[9];
        data[0] = (byte) 0xC1;
        try {
            os.write(data);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!undoList.isEmpty()) {
            clearPaint();//清除之前绘制内容
            PathBean lastPb = undoList.removeLast();//将最后一个移除
            redoList.add(lastPb);//加入 恢复操作
            //遍历，将Path重新绘制到 mCanvas
            if (backgroundBitmap != null) {
                mCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
            }
            for (PathBean pb : undoList) {
                mCanvas.drawPath(pb.path, pb.paint);
            }
            postInvalidate();
        }
    }

    /**
     * 恢复操作
     */
    void redo() {
        data = new byte[9];
        data[0] = (byte) 0xC2;
        try {
            os.write(data);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!redoList.isEmpty()) {
            PathBean pathBean = redoList.removeLast();
            mCanvas.drawPath(pathBean.path, pathBean.paint);
            undoList.add(pathBean);
            postInvalidate();
        }
    }


    /**
     * 设置画笔颜色
     */
    void setPaintColor(@ColorInt int color) {
        mPaint.setColor(color);
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
        myCanvas.translate(mCanvas.getStartPointX(), mCanvas.getStartPointY());
        myCanvas.scale(mCanvas.getScale() * exportedWidth / width);
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
                if (f.exists())
                    ToastUtils.show(ctx, ctx.getString(R.string.saving_success) + "\n" + f.toString());
                else ToastUtils.show(ctx, R.string.saving_failed);
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

    void closePathRecorderOS() {
        closeStream(os);
    }

    /**
     * 导入路径
     *
     * @param f                   路径文件
     * @param d                   完成接口
     * @param floatValueInterface 进度接口
     *                            路径存储结构：
     *                            一条笔迹或一个操作记录记录长度为9字节
     *                            byte b[9];(length=9)
     *                            1+4+4
     *                            b[0]: 标记，绘画路径开始为0xA1，橡皮擦路径开始为0xA2；
     *                            \ 按下事件（紧接着绘画路径开始后）为0xB1，抬起事件（路径结束）为0xB2，移动事件（路径中）为0xB3；
     *                            \ 撤销为0xC1，恢复为0xC2。(byte)
     *                            如果标记为0xA1，排列结构：标记(int)+笔迹宽度(float)+颜色(int)
     *                            如果标记为0xA2，排列结构：标记(int)+橡皮擦宽度(float)+TRANSPARENT(int)
     *                            如果标记为0xB1或0xB2或0xB3，排列结构：标记(int)+x坐标(float)+y坐标(float)
     *                            如果标记为0xC1或0xC2，则后8字节忽略。
     */
    void importPathFile(File f, Runnable d, @Documents.Nullable ValueInterface<Float> floatValueInterface) {
        floatValueInterface.f(0F);
        Handler handler = new Handler();
        importingPath = true;
        Thread thread = new Thread(() -> {
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
                            lastP1 = p1;
                            p1 = jni.byteArrayToInt(bytes, 0);
                            switch (p1) {
                                case 4:
                                    undo();
                                    break;
                                case 5:
                                    redo();
                                    break;
                                case 1:
                                case 2:
                                    strokeWidth = jni.byteArrayToFloat(bytes, 4);
                                    color = jni.byteArrayToInt(bytes, 8);
                                    setEraserMode(p1 == 2);
                                    if (isEraserMode) {
                                        setEraserStrokeWidth(strokeWidth);
                                    } else {
                                        setStrokeWidth(strokeWidth);
                                        setPaintColor(color);
                                    }
                                    break;
                                case 3:
                                    if (x != -1 && y != -1) onTouchAction(MotionEvent.ACTION_UP, x, y);
                                    break;
                                case 0:
                                    x = jni.byteArrayToFloat(bytes, 4);
                                    y = jni.byteArrayToFloat(bytes, 8);
                                    if (lastP1 == 1 || lastP1 == 2) {
                                        onTouchAction(MotionEvent.ACTION_DOWN, x, y);
                                    }
                                    onTouchAction(MotionEvent.ACTION_MOVE, x, y);
                                    break;
                            }
                            read += 12;
                            floatValueInterface.f(((float) read) * 100F / ((float) length));
                        }
                        break;
                    case "path ver 2.1":
                        handler.post(() -> ToastUtils.show(ctx, R.string.import_2_1));
                        raf.skipBytes(12);
                        bytes = new byte[9];
                        read = 0L;
                        while (raf.read(bytes) != -1) {
                            switch (bytes[0]) {
                                case (byte) 0xA1:
                                case (byte) 0xA2:
                                    strokeWidth = jni.byteArrayToFloat(bytes, 1);
                                    color = jni.byteArrayToInt(bytes, 5);
                                    setEraserMode(bytes[0] == (byte) 0xA2);
                                    mPaintRef.setColor(color);
                                    mPaintRef.setStrokeWidth(strokeWidth);
                                    break;
                                case (byte) 0xB1:
                                    x = jni.byteArrayToFloat(bytes, 1);
                                    y = jni.byteArrayToFloat(bytes, 5);
                                    onTouchAction(MotionEvent.ACTION_DOWN, x, y);
                                    break;
                                case (byte) 0xB3:
                                    x = jni.byteArrayToFloat(bytes, 1);
                                    y = jni.byteArrayToFloat(bytes, 5);
                                    onTouchAction(MotionEvent.ACTION_MOVE, x, y);
                                    break;
                                case (byte) 0xB2:
                                    x = jni.byteArrayToFloat(bytes, 1);
                                    y = jni.byteArrayToFloat(bytes, 5);
                                    onTouchAction(MotionEvent.ACTION_UP, x, y);
                                    break;
                                case (byte) 0xC1:
                                    undo();
                                    break;
                                case (byte) 0xC2:
                                    redo();
                                    break;
                            }
                            read += 9;
                            floatValueInterface.f(((float) read) * 100F / ((float) length));
                        }
                        break;
                    default:
                        handler.post(() -> ToastUtils.show(ctx, R.string.import_old));
                        bytes = new byte[26];
                        byte[] bytes_4 = new byte[4];
                        read = 0L;
                        while (raf.read(bytes) != -1) {
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
                                    x = jni.byteArrayToFloat(bytes_4, 0);
                                    System.arraycopy(bytes, 4, bytes_4, 0, 4);
                                    y = jni.byteArrayToFloat(bytes_4, 0);
                                    System.arraycopy(bytes, 8, bytes_4, 0, 4);
                                    color = jni.byteArrayToInt(bytes_4, 0);
                                    System.arraycopy(bytes, 12, bytes_4, 0, 4);
                                    strokeWidth = jni.byteArrayToFloat(bytes_4, 0);
                                    System.arraycopy(bytes, 16, bytes_4, 0, 4);
                                    int motionAction = jni.byteArrayToInt(bytes_4, 0);
                                    System.arraycopy(bytes, 20, bytes_4, 0, 4);
                                    float eraserStrokeWidth = jni.byteArrayToFloat(bytes_4, 0);
                                    if (motionAction != 0 && motionAction != 1 && motionAction != 2)
                                        motionAction = Random.ran_sc(0, 2);
                                    if (strokeWidth <= 0) strokeWidth = Random.ran_sc(1, 800);
                                    if (eraserStrokeWidth <= 0) eraserStrokeWidth = Random.ran_sc(1, 800);
                                    setEraserMode(bytes[24] == 1);
                                    setEraserStrokeWidth(eraserStrokeWidth);
                                    setPaintColor(color);
                                    setStrokeWidth(strokeWidth);
                                    onTouchAction(motionAction, x, y);
                                    floatValueInterface.f(((float) read) / ((float) length) * 100F);
                                    break;
                            }
                        }
                        importingPath = false;
                        break;
                }
                d.run();
            } catch (IOException e) {
                handler.post(() -> ToastUtils.show(ctx, R.string.read_error));
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            importingPath = false;
        });
        thread.start();
    }

    private void onTouchAction(int motionAction, float x, float y) {
        float startPointX = mCanvas.getStartPointX();
        float startPointY = mCanvas.getStartPointY();
        float canvasScale = mCanvas.getScale();
        x = (x - startPointX) / canvasScale;
        y = (y - startPointY) / canvasScale;
        switch (motionAction) {
            case MotionEvent.ACTION_DOWN:
                //路径
                mPath = new Path();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                savedData = new ArrayList<>();
                data = new byte[9];
                data[0] = (byte) (isEraserMode ? 0xA2 : 0xA1);
                jni.floatToByteArray(data, getStrokeWidthInUse(), 1);
                jni.intToByteArray(data, mPaintRef.getColor(), 5);
                savedData.add(data);
                data = new byte[9];
                data[0] = (byte) 0xB1;
                jni.floatToByteArray(data, x, 1);
                jni.floatToByteArray(data, y, 5);
                savedData.add(data);
                break;
            case MotionEvent.ACTION_UP:
                if (mPath != null) {
                    mCanvas.drawPath(mPath, mPaintRef);//将路径绘制在mBitmap上
                    Path path = new Path(mPath);//复制出一份mPath
                    Paint paint = new Paint(mPaintRef);
                    PathBean pb = new PathBean(path, paint);
                    undoList.add(pb);//将路径对象存入集合
                    mPath.reset();
                    mPath = null;
                }
                if (savedData != null) {
                    data = new byte[9];
                    data[0] = (byte) 0xB2;
                    jni.floatToByteArray(data, x, 1);
                    jni.floatToByteArray(data, y, 5);
                    savedData.add(data);
                    try {
                        for (byte[] bytes : savedData) {
                            os.write(bytes);
                            os.flush();
                        }
                    } catch (IOException e) {
                        ((Activity) ctx).runOnUiThread(() -> ToastUtils.show(ctx, R.string.write_error));
                    }
                }
                break;
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
                if (savedData != null) {
                    data = new byte[9];
                    data[0] = (byte) 0xB3;
                    jni.floatToByteArray(data, x, 1);
                    jni.floatToByteArray(data, y, 5);
                    savedData.add(data);
                }
                break;
        }
        postInvalidate();
    }

    void clearTouchRecordOSContent() {
        closePathRecorderOS();
        setOS(internalPathFile, false);
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
            ToastUtils.show(ctx, ctx.getString(R.string.importing_failed));
        } else {
            mCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
        }
        invalidate();
    }

    void resetTransform() {
        mCanvas.reset();
        redrawCanvas();
    }

    private void redrawCanvas() {
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
        for (PathBean pathBean : this.undoList) {
            mCanvas.drawPath(pathBean.path, pathBean.paint);
        }
        invalidate();
    }

    MyCanvas getCanvas() {
        return mCanvas;
    }

    public float getStrokeWidthInUse() {
        return mPaintRef.getStrokeWidth();
    }

    public boolean isLockingStroke() {
        return this.isLockingStroke;
    }

    void bitmapResolution(Point point) {
        point.x = mBitmap.getWidth();
        point.y = mBitmap.getHeight();
    }

    void setLockStrokeMode(boolean mode) {
        this.isLockingStroke = mode;
    }

    void lockStroke() {
        if (isLockingStroke) {
            this.lockedStrokeWidth = getStrokeWidth();
            this.lockedEraserStrokeWidth = getEraserStrokeWidth();
            this.scaleWhenLocked = mCanvas.getScale();
            setCurrentStrokeWidthWithLockedStrokeWidth();
        }
    }

    void setCurrentStrokeWidthWithLockedStrokeWidth() {
        if (isLockingStroke) {
            float mCanvasScale = mCanvas.getScale();
            setStrokeWidth(lockedStrokeWidth * scaleWhenLocked / mCanvasScale);
            setEraserStrokeWidth(lockedEraserStrokeWidth * scaleWhenLocked / mCanvasScale);
        }
    }

    /**
     * 路径对象
     */
    static class PathBean {
        Path path;
        Paint paint;

        PathBean(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }
}