package pers.zhc.tools.floatingboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.support.annotation.ColorInt;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
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
    boolean isEraserMode;
    private JNI jni = new JNI();
    private Context ctx;
    private Bitmap backgroundBitmap;
    private Canvas mBackgroundCanvas;
    private GestureDetectorCompat gestureDetector;
    private MyGesture onGestureListener;



    /*public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }*/

    void setOS(File file, boolean append) {
        try {
            os = new FileOutputStream(file, append);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    PaintView(Context context, int width, int height, File internalPathFile) {
        super(context);
        ctx = context;
        this.internalPathFile = internalPathFile;
        this.width = width;
        this.height = height;
        init();
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
        onGestureListener = new MyGesture() {
            @Override
            public void onFirst2Down(float p1X, float p1Y, float p2X, float p2Y) {
                lastP.p1.x = p1X;
                lastP.p1.y = p1Y;
                lastP.p2.x = p2X;
                lastP.p2.y = p2Y;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                Log.v("gd", "onDown" + e);
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                Log.v("gd", "onShowPress" + e);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.v("gd", "onSingleTapUp" + e);
                return true;
            }

            private PointWithBoolean lastP = new PointWithBoolean();

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.v("gd", "onScroll" + e1 + " " + e2 + " " + distanceX + " " + distanceY);
                if (e2.getPointerCount() == 2) {
                    float x1 = e2.getX(0);
                    float x2 = e2.getX(1);
                    float y1 = e2.getY(0);
                    float y2 = e2.getY(1);
                    mCanvas.translate((x1 + x2) / 2 - lastP.getCentralPoint().x, (y1 + y2) / 2 - lastP.getCentralPoint().y);
                    lastP.p1.x = x1;
                    lastP.p2.x = x2;
                    lastP.p1.y = y1;
                    lastP.p2.y = y2;
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                Log.v("gd", "onLongPress" + e);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.v("gd", "onFling" + e1 + " " + e2 + " " + velocityX + " " + velocityY);
                return true;
            }

            @Override
            void onZoom(float scale) {
            }
        };
        gestureDetector = new GestureDetectorCompat(ctx, onGestureListener);
    }

    /**
     * 绘制
     */
    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);//将mBitmap绘制在canvas上,最终的显示
            if (null != mPath) {//显示实时正在绘制的path轨迹
                if (isEraserMode) canvas.drawPath(mPath, eraserPaint);
                else canvas.drawPath(mPath, mPaint);
            }
        }
    }

    void setStrokeWidth(float width) {
        mPaint.setStrokeWidth(width);
    }

    float getStrokeWidth() {
        return this.mPaint.getStrokeWidth();
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

    private class FloatPoint {
        private float x, y;

        FloatPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        FloatPoint() {
        }
    }

    private class PointWithBoolean {
        private FloatPoint p1, p2;

        private PointWithBoolean() {
            p1 = new FloatPoint();
            p2 = new FloatPoint();
        }

        private boolean b = false;

        private FloatPoint getCentralPoint() {
            return new FloatPoint((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
        }
    }

    private float firstDistance;
    private float lastScale = 1;
    private PointWithBoolean firstP = new PointWithBoolean();
    private PointWithBoolean lastP = new PointWithBoolean();


    /**
     * 触摸事件 触摸绘制
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            onTouchAction(event.getAction(), event.getX(), event.getY());
            onGestureListener.first2Down = false;
        } else if (event.getPointerCount() == 2) {
            float x1 = event.getX(0);
            float y1 = event.getY(0);
            float x2 = event.getX(1);
            float y2 = event.getY(1);
            if (!onGestureListener.first2Down) {
                onGestureListener.onFirst2Down(x1, y1, x2, y2);
                firstDistance = (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
            }
            onGestureListener.first2Down = true;
            float distance = (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
            float scale = distance / firstDistance;
            mCanvas.scale(scale / lastScale, scale / lastScale, (x1 + x2) / 2, (y1 + y2) / 2);
            lastScale = scale;
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (PathBean pathBean : undoList) mCanvas.drawPath(pathBean.path, pathBean.paint);
        }
        /*int action = event.getAction();
        int pointerCount = event.getPointerCount();
        if (pointerCount >= 2) {
            float x1 = event.getX(0);
            float x2 = event.getX(1);
            float y1 = event.getY(0);
            float y2 = event.getY(1);
            if (!firstP.b) {
                firstP.p1.x = x1;
                firstP.p2.x = x2;
                firstP.p1.y = y1;
                firstP.p2.y = y2;
                firstP.b = true;
            }
            float distance = (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
            if (firstDistance == 0) {
                firstDistance = distance;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                if (distance > firstDistance) {
                    System.out.println("zoom+");
                } else System.out.println("zoom-");
                float centralPointX = (x1 + x2) / 2;
                float centralPointY = (y1 + y2) / 2;
                FloatPoint firstPCentralPoint = firstP.getCentralPoint();
                float scale = 1F / lastScale * (distance / firstDistance);
                mCanvas.scale(scale, scale, centralPointX, centralPointY);
                lastScale = distance / firstDistance;
                System.out.println("scale = " + scale);
                mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                FloatPoint lastPCentralPoint = lastP.getCentralPoint();
                mCanvas.translate(centralPointX - lastPCentralPoint.x, centralPointY - lastPCentralPoint.y);
                for (PathBean pathBean : undoList) {
                    mCanvas.drawPath(pathBean.path, pathBean.paint);
                }
                lastP.p1.x = x1;
                lastP.p2.x = x2;
                lastP.p1.y = y1;
                lastP.p2.y = y2;
            }
        }*/
        boolean b = gestureDetector.onTouchEvent(event);
        invalidate();
        return b;
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
                            onTouchAction(motionAction, x, y);
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

    private void onTouchAction(int motionAction, float x, float y) {
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

    void setEraserStrokeWidth(float w) {
        eraserPaint.setStrokeWidth(w);
    }

    float getEraserStrokeWidth() {
        return eraserPaint.getStrokeWidth();
    }

    void importImage(@Documents.NotNull Bitmap imageBitmap, float left, float top, int scaledWidth, int scaledHeight) {
        try {
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

    void scaleCanvas(float scaledWidth, float scaledHeight) {

    }

    private class MyGesture implements GestureDetector.OnGestureListener {
        boolean first2Down;

        void onFirst2Down(float p1X, float p1Y, float p2X, float p2Y) {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        void onZoom(float scale) {
        }
    }
}