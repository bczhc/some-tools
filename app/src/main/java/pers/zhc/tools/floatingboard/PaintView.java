package pers.zhc.tools.floatingboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.support.annotation.ColorInt;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.Common;
import pers.zhc.u.Random;
import pers.zhc.u.ValueInterface;
import pers.zhc.u.common.Documents;

import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("ViewConstructor")
public class PaintView extends View {
    private final File internalPathFile;
    private final int height;
    private final int width;
    private final Paint zP;
    boolean isEraserMode;
    private OutputStream os;
    private Paint mPaint;
    private Path mPath;
    private Paint eraserPaint;
    private Canvas mCanvas;
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



    /*public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }*/

    PaintView(Context context, int width, int height, File internalPathFile) {
        super(context);
        ctx = context;
        this.internalPathFile = internalPathFile;
        this.width = width;
        this.height = height;
        init();
        zP = new Paint();
        zP.setColor(Color.parseColor("#5000ff00"));
    }

    void setOS(File file, boolean append) {
        try {
            os = new FileOutputStream(file, append);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
            mCanvas = new Canvas(mBitmap);
            mBackgroundCanvas = new Canvas(backgroundBitmap);
            //抗锯齿
            mCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            //背景色
//                mCanvas.drawColor(Color.WHITE);
        });

        undoList = new LinkedList<>();
        redoList = new LinkedList<>();
    }

    /**
     * 绘制
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);//将mBitmap绘制在canvas上,最终的显示
            if (null != mPath) {//显示实时正在绘制的path轨迹
                if (isEraserMode) canvas.drawPath(mPath, eraserPaint);
                else {
                    canvas.drawPath(mPath, mPaint);
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
        try {
            byte[] bytes = new byte[26];
            bytes[25] = 1;
            os.write(bytes);
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
        try {
            byte[] bytes = new byte[26];
            bytes[25] = 2;
            os.write(bytes);
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
    }

    /**
     * 保存到指定的文件夹中
     */
    void saveImg(File f) {
        //保存图片
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(f);
            if (mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                fileOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(fileOutputStream);
        }
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
        onTouchAction(event.getAction(), event.getX(), event.getY(), event.getPointerCount());
        if (event.getPointerCount() == 2) {
            /*mPath = null;
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mCanvas.scale(scaleC, scaleC, scalePointX + finalTranslateX, scalePointY + finalTranslateY);
            finalScale *= scaleC;
            mCanvas.translate(finalTranslateX, finalTranslateY);
            for (PathBean pathBean : undoList) {
                mCanvas.drawPath(pathBean.path, pathBean.paint);
            }
            mCanvas.drawRect(0, 0, width, height, zP);
//            mCanvas.scale(1 / finalScale, 1 / finalScale, scalePointX, scalePointY);
            mCanvas.translate(-finalTranslateX, -finalTranslateY);*/
        }
        invalidate();
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

    void closePathRecoderOS() {
        closeStream(os);
    }

    void importPathFile(File f, Runnable d, @Documents.Nullable ValueInterface<Float> floatValueInterface) {
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute(() -> {
            try {
                long length = f.length(), haveRead = 0L;
                InputStream is = new FileInputStream(f);
                byte[] bytes = new byte[26];
                byte[] bytes_4 = new byte[4];
                while (is.read(bytes) != -1) {
                    haveRead += 26L;
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
                            float x = jni.byteArrayToFloat(bytes_4);
                            System.arraycopy(bytes, 4, bytes_4, 0, 4);
                            float y = jni.byteArrayToFloat(bytes_4);
                            System.arraycopy(bytes, 8, bytes_4, 0, 4);
                            int color = jni.byteArrayToInt(bytes_4);
                            System.arraycopy(bytes, 12, bytes_4, 0, 4);
                            float strokeWidth = jni.byteArrayToFloat(bytes_4);
                            System.arraycopy(bytes, 16, bytes_4, 0, 4);
                            int motionAction = jni.byteArrayToInt(bytes_4);
                            System.arraycopy(bytes, 20, bytes_4, 0, 4);
                            float eraserStrokeWidth = jni.byteArrayToFloat(bytes_4);
                            if (motionAction != 0 && motionAction != 1 && motionAction != 2)
                                motionAction = Random.ran_sc(0, 2);
                            if (strokeWidth <= 0) strokeWidth = Random.ran_sc(1, 800);
                            if (eraserStrokeWidth <= 0) eraserStrokeWidth = Random.ran_sc(1, 800);
                            setEraserMode(bytes[24] == 1);
                            setEraserStrokeWidth(eraserStrokeWidth);
                            setPaintColor(color);
                            setStrokeWidth(strokeWidth);
                            onTouchAction(motionAction, x, y, 1);
                            floatValueInterface.f(((float) haveRead) / ((float) length) * 100F);
                            break;
                    }
                }
                is.close();
                d.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        es.shutdown();
    }

    private void onTouchAction(int motionAction, float x, float y, int pointCount) {
        byte[][] bytes = new byte[6][4];
        bytes[0] = jni.floatToByteArray(x);
        bytes[1] = jni.floatToByteArray(y);
        bytes[2] = jni.intToByteArray(getColor());
        bytes[3] = jni.floatToByteArray(getStrokeWidth());
        bytes[4] = jni.intToByteArray(motionAction);
        bytes[5] = jni.floatToByteArray(getEraserStrokeWidth());
        byte[] data = new byte[26];
        for (int i = 0; i < bytes.length; i++) {
            System.arraycopy(bytes[i], 0, data, 4 * i, bytes[i].length);
        }
        try {
            data[24] = (byte) (isEraserMode ? 1 : 0);
            os.write(data);
            os.flush();
        } catch (IOException e) {
            Common.showException(e, (Activity) ctx);
        }
        switch (motionAction) {
            case MotionEvent.ACTION_DOWN:
                //路径
                mPath = new Path();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_UP:
                if (mPath != null) {
                    Paint paintRef = isEraserMode ? eraserPaint : mPaint;
                    mCanvas.drawPath(mPath, paintRef);//将路径绘制在mBitmap上
                    Path path = new Path(mPath);//复制出一份mPath
                    Paint paint = new Paint(paintRef);
                    PathBean pb = new PathBean(path, paint);
                    undoList.add(pb);//将路径对象存入集合
                    mPath.reset();
                    mPath = null;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(x - mLastX);
                float dy = Math.abs(y - mLastY);
                if (dx >= 0 || dy >= 0) {//绘制的最小距离 0px
                    //利用二阶贝塞尔曲线，使绘制路径更加圆滑
                    try {
                        mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
                    } catch (NullPointerException ignored) {
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
        }
        postInvalidate();
    }

    void clearTouchRecordOSContent() {
        try {
            os.close();
            setOS(internalPathFile, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    float getEraserStrokeWidth() {
        return eraserPaint.getStrokeWidth();
    }

    void setEraserStrokeWidth(float w) {
        eraserPaint.setStrokeWidth(w);
    }

    void importImage(@Documents.NotNull Bitmap imageBitmap, float left, float top, int scaledWidth, int scaledHeight) {
        try {
            System.gc();
            Bitmap bitmap = Bitmap.createScaledBitmap(imageBitmap, scaledWidth, scaledHeight, true);
            mBackgroundCanvas.drawBitmap(bitmap, left, top, mBitmapPaint);
            if (backgroundBitmap == null) {
                Toast.makeText(ctx, ctx.getString(R.string.importing_failed), Toast.LENGTH_SHORT).show();
            } else {
                mCanvas.drawBitmap(backgroundBitmap, 0F, 0F, mBitmapPaint);
            }
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(ctx, ctx.getString(R.string.importing_failed) + "\n" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    void resetTransform() {
        invalidate();
    }

    private class FloatPoint {
        private float x, y;

        FloatPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        FloatPoint() {
        }
    }

    private class MyPoint {
        private FloatPoint p1, p2;

        private MyPoint() {
            p1 = new FloatPoint();
            p2 = new FloatPoint();
        }

        private FloatPoint getMidPoint() {
            return new FloatPoint((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        }
    }

    /**
     * 路径对象
     */
    class PathBean {
        Path path;
        Paint paint;

        PathBean(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }
}