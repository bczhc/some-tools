package pers.zhc.tools.epicycles;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import pers.zhc.tools.floatingdrawing.MyCanvas;
import pers.zhc.tools.utils.GestureResolver;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.CoordinateDouble;

import java.util.List;

@SuppressLint("ViewConstructor")
class EpicyclesView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    static double T = 2 * Math.PI, omega = 2 * Math.PI / T;
    private final EpicyclesSequence epicyclesSequence;
    private int canvasWidth = -1;
    private int canvasHeight = -1;
    private Bitmap mEpicyclesBitmap;
    private MyCanvas mEpicyclesCanvas;
    private Paint mBitmapPaint;
    private Paint mCoPaint;
    private Paint mCirclePaint;
    private Paint mVectorPaint;
    private Paint mPathPaint;
    private boolean b = true;
    private GestureDetector gd;
    private CoordinateDouble center;
    private CoordinateDouble lastLineToPoint;
    private Path mPath;
    private double t;
    private boolean pathMove = true;
    private GestureResolver gestureResolver;
    private SurfaceHolder mSurfaceHolder;
    private boolean drawing = true;

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
        return EpicyclesView.omega;
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

    private void init(Context context) {
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
        mBitmapPaint = new Paint();
        mCoPaint = new Paint();
        mCoPaint.setStyle(Paint.Style.STROKE);
        mCoPaint.setColor(Color.GRAY);
        mCirclePaint = new Paint();
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setColor(Color.GRAY);
        mVectorPaint = new Paint();
        mVectorPaint.setStyle(Paint.Style.STROKE);
        mPathPaint = new Paint();
        mPathPaint.setColor(Color.RED);
        mPathPaint.setStyle(Paint.Style.STROKE);
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
        //noinspection DuplicatedCode
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

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvasWidth == -1 && canvasHeight == -1) {
            canvasWidth = getWidth();
            canvasHeight = getHeight();
            System.gc();
            mEpicyclesBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
            mEpicyclesCanvas = new MyCanvas(mEpicyclesBitmap);
            mEpicyclesCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (b) {
                new Thread(this).start();
                this.b = false;
            }
        }
        gestureResolver.onTouch(event);
        gd.onTouchEvent(event);
        return true;
    }

    private CoordinateDouble rectCoordinateToCanvasCoordinate(double x, double y) {
        return new CoordinateDouble(x + canvasWidth / 2D, -y + canvasHeight / 2D);
    }

    private CoordinateDouble canvasCoordinateToRectCoordinate(CoordinateDouble coordinateDouble) {
        return new CoordinateDouble(coordinateDouble.x - canvasWidth / 2D, -coordinateDouble.y + canvasHeight / 2D);
    }

    private double getComplexArg(double re, double im) {
        if (re > 0) return Math.atan(im / re);
        if (re == 0 && im > 0) return Math.PI / 2D;
        if (re == 0 && im < 0) return Math.PI / -2D;
        if (re < 0 && im >= 0) return Math.atan(im / re) + Math.PI;
        if (re < 0 && im < 0) return Math.atan(im / re) - Math.PI;
        return 0;
    }

    @Override
    public void run() {
        while (drawing) {
            Canvas canvas = mSurfaceHolder.lockCanvas();
            render();
            canvas.drawBitmap(mEpicyclesBitmap, 0, 0, mBitmapPaint);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void render() {
        mEpicyclesCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        center.y = (center.x = 0);
        lastLineToPoint.x = (lastLineToPoint.y = 0);
        mEpicyclesCanvas.drawLine(0F, canvasHeight / 2F, canvasWidth, canvasHeight / 2F, mCoPaint);
        mEpicyclesCanvas.drawLine(canvasWidth / 2F, 0F, canvasWidth / 2F, canvasHeight, mCoPaint);
//            画实轴和虚轴 无箭头
        List<EpicyclesSequence.AEpicycle> epicycles = this.epicyclesSequence.epicycles;
        for (int i = 0; i < epicycles.size(); i++) {
            EpicyclesSequence.AEpicycle epicycle = epicycles.get(i);
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
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawing = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawing = false;
    }
}