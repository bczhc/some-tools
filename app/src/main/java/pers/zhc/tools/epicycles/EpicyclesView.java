package pers.zhc.tools.epicycles;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pers.zhc.tools.BaseView;
import pers.zhc.tools.floatingdrawing.MyCanvas;
import pers.zhc.tools.utils.GestureResolver;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.CoordinateDouble;

@SuppressLint("ViewConstructor")
class EpicyclesView extends BaseView {
    static double T = 2 * Math.PI, omega = 2 * Math.PI / T;
    private final EpicyclesSequence epicyclesSequence;
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    private Bitmap mEpicyclesBitmap;
    private MyCanvas mEpicyclesCanvas;
    private Paint mBitmapPaint;
    private Paint mCoPaint;
    private Paint mCirclePaint;
    private Paint mVectorPaint;
    private Paint mPathPaint;
    private boolean b = true;
    private ExecutorService es;
    private GestureDetector gd;
    private CoordinateDouble center;
    private CoordinateDouble lastLineToPoint;
    private Path mPath;
    private double t;
    private boolean pathMove = true;
    private GestureResolver gestureResolver;

    EpicyclesView(Context context, EpicyclesSequence epicyclesSequence) {
        super(context);
        this.epicyclesSequence = epicyclesSequence;
        init(context);
    }

    public static double getT() {
        return EpicyclesView.T;
    }

    public static void setT(double T) {
        EpicyclesView.T = T;
        EpicyclesView.omega = 2 * Math.PI / T;
    }

    @SuppressWarnings("unused")
    public static double getOmega() {
        return EpicyclesView.getOmega();
    }

    private void init(Context context) {
        mBitmapPaint = new Paint();
        mCoPaint = new Paint();
        mCoPaint.setStrokeWidth(1);
        mCoPaint.setStyle(Paint.Style.STROKE);
        mCoPaint.setColor(Color.GRAY);
        mCirclePaint = new Paint();
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(1);
        mCirclePaint.setColor(Color.GRAY);
        mVectorPaint = new Paint();
        mVectorPaint.setStrokeWidth(2F);
        mVectorPaint.setStyle(Paint.Style.STROKE);
        mPathPaint = new Paint();
        mPathPaint.setStrokeWidth(3F);
        mPathPaint.setColor(Color.RED);
        mPathPaint.setStyle(Paint.Style.STROKE);
        es = Executors.newCachedThreadPool();
        gd = new GestureDetector(context, new GestureDetector.OnGestureListener() {
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
        });
        gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {
            @Override
            public void onTwoPointsScroll(float distanceX, float distanceY, MotionEvent event) {
                mEpicyclesCanvas.invertTranslate(distanceX, distanceY);
            }

            @Override
            public void onTwoPointsZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float dScale, MotionEvent event) {
                mEpicyclesCanvas.invertScale(dScale, midPointX, midPointY);
                setStrokeWidth();
            }

            @Override
            public void onTwoPointsUp() {

            }

            @Override
            public void onTwoPointsDown() {

            }

            @Override
            public void onTwoPointsPress() {
            }

            @Override
            public void onOnePointScroll(float distanceX, float distanceY, MotionEvent event) {
//                mEpicyclesCanvas.invertTranslate(distanceX, distanceY);
            }
        });
        gd.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mPath.reset();
                pathMove = true;
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }
        });
        mPath = new Path();
        t = 0;
        lastLineToPoint = new CoordinateDouble(0D, 0D);
        center = new CoordinateDouble(0D, 0D);
    }

    private void setStrokeWidth() {
        float canvasScale = mEpicyclesCanvas.getScale();
        float lockedCoPaintStrokeWidth = 1F;
        mCirclePaint.setStrokeWidth(lockedCoPaintStrokeWidth / canvasScale);
        mCoPaint.setStrokeWidth(mCirclePaint.getStrokeWidth());
        float lockedVectorPaintStrokeWidth = 2F;
        mVectorPaint.setStrokeWidth(lockedVectorPaintStrokeWidth / canvasScale);
        float lockedPathPaintStrokeWidth = 3F;
        mPathPaint.setStrokeWidth(lockedPathPaintStrokeWidth / canvasScale);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvasWidth == 0 && canvasHeight == 0) {
            canvasWidth = getWidth();
            canvasHeight = getHeight();
            System.gc();
            mEpicyclesBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
            mEpicyclesCanvas = new MyCanvas(mEpicyclesBitmap);
            mEpicyclesCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        }
        render(canvas);
    }
    /*private CoordinateDouble eComplexPower(ComplexValue complexValue, double e_pow_i_num) {
//        return new CoordinateDouble(Math.cos(e_pow_i_num) * complexValue.)
    }*/

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (b) {
                es.execute(this::run);
                this.b = false;
            }
        }
        gd.onTouchEvent(event);
        gestureResolver.onTouch(event);
        return true;
    }

    private CoordinateDouble rectCoordinateToCanvasCoordinate(double x, double y) {
        return new CoordinateDouble(x + canvasWidth / 2D, -y + canvasHeight / 2D);
    }

    private CoordinateDouble canvasCoordinateToRectCoordinate(CoordinateDouble coordinateDouble) {
        return new CoordinateDouble(coordinateDouble.x - canvasWidth / 2D, -coordinateDouble.y + canvasHeight / 2D);
    }

    private double getComplexArg(double re, double im) {
        if (re > 0) {
            return Math.atan(im / re);
        }
        if (re == 0 && im > 0) {
            return Math.PI / 2D;
        }
        if (re == 0 && im < 0) {
            return Math.PI / -2D;
        }
        if (re < 0 && im >= 0) {
            return Math.atan(im / re) + Math.PI;
        }
        if (re < 0 && im < 0) {
            return Math.atan(im / re) - Math.PI;
        }
        return 0;
    }

    void shutdownES() {
        es.shutdownNow();
    }

    private void run() {
        es.execute(() -> {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                postInvalidate();
                try {
                    Thread.sleep(0, 500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void render(Canvas canvas) {
        center.y = (center.x = 0);
        lastLineToPoint.x = (lastLineToPoint.y = 0);
//            清空上一次bitmap上绘画的
        mEpicyclesCanvas.drawColor(Color.WHITE);
//            画实轴和虚轴 无箭头
        mEpicyclesCanvas.drawLine(0F, canvasHeight / 2F, canvasWidth, canvasHeight / 2F, mCoPaint);
        mEpicyclesCanvas.drawLine(canvasWidth / 2F, 0F, canvasWidth / 2F, canvasHeight, mCoPaint);
        List<EpicyclesSequence.Epicycle> epicycles = this.epicyclesSequence.epicycles;
        for (int i = 0; i < epicycles.size(); i++) {
            EpicyclesSequence.Epicycle epicycle = epicycles.get(i);
            //一次画所有本轮
            float radius = ((float) Math.sqrt(Math.pow(epicycle.c.re, 2D) + Math.pow(epicycle.c.im, 2D)));
            CoordinateDouble centerPointCanvasCoordinate = rectCoordinateToCanvasCoordinate(center.x + lastLineToPoint.x, center.y + lastLineToPoint.y);
            mEpicyclesCanvas.drawCircle(((float) centerPointCanvasCoordinate.x), ((float) centerPointCanvasCoordinate.y)
                    , radius, mCirclePaint);
            double phaseAddition = getComplexArg(epicycle.c.re, epicycle.c.im);
            CoordinateDouble lineTo = rectCoordinateToCanvasCoordinate(
                    radius * Math.cos((t * omega) * epicycle.n + phaseAddition) + lastLineToPoint.x
                    , radius * Math.sin(((t * omega) * epicycle.n) + phaseAddition) + lastLineToPoint.y
            );
            mEpicyclesCanvas.drawLine(((float) centerPointCanvasCoordinate.x), ((float) centerPointCanvasCoordinate.y), ((float) lineTo.x), ((float) lineTo.y), mVectorPaint);
            lastLineToPoint = canvasCoordinateToRectCoordinate(lineTo);
            if (i == epicycles.size() - 1) {
                if (pathMove) {
                    mPath.moveTo(((float) (lineTo.x)), ((float) (lineTo.y)));
                    pathMove = false;
                }
                mPath.lineTo(((float) (lineTo.x)), ((float) (lineTo.y)));
                mEpicyclesCanvas.drawPath(mPath, mPathPaint);
            }
        }
        t += .1F;
        canvas.drawBitmap(mEpicyclesBitmap, 0F, 0F, mBitmapPaint);
    }
}