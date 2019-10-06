package pers.zhc.tools.epicycles;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import pers.zhc.u.math.fourier.EpicyclesSequence;
import pers.zhc.u.math.util.CoordinateDouble;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("ViewConstructor")
class EpicyclesView extends View {
    private final EpicyclesSequence epicyclesSequence;
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    private Bitmap mEpicyclesBitmap;
    private Bitmap mDrawingBitmap;
    private Canvas mEpicyclesCanvas;
    private Canvas mDrawingCanvas;
    private Paint mBitmapPaint;
    private Paint mCoPaint;
    private Paint mCirclePaint;
    private Paint mVectorPaint;
    private Paint mPathPaint;
    private boolean b = true;
    private double epicyclesScale = 10D;
    private ExecutorService es;
    private double reOffset, imOffset;
    private QuadDrawing quadDrawing;
    private GestureDetector gd;
    private CoordinateDouble center;
    private CoordinateDouble lastLineToPoint;
    private Path path;
    private double t;
    static double T = 2 * Math.PI, omega = 2 * Math.PI / T;

    EpicyclesView(Context context, EpicyclesSequence epicyclesSequence) {
        super(context);
        this.epicyclesSequence = epicyclesSequence;
        init(context);
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
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                reOffset -= distanceX;
                imOffset -= distanceY;
//                quadDrawing.reset();
                quadDrawing.path.offset(-distanceX, -distanceY);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }
        });
        gd.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                EpicyclesView.this.reOffset = (EpicyclesView.this.imOffset = 0);
                quadDrawing.reset();
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }
        });
        path = new Path();
        quadDrawing = new QuadDrawing(path);
        t = 0;
        lastLineToPoint = new CoordinateDouble(0D, 0D);
        center = new CoordinateDouble(0D, 0D);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvasWidth == 0 && canvasHeight == 0) {
            canvasWidth = getWidth();
            canvasHeight = getHeight();
            System.gc();
            mDrawingBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
            mDrawingCanvas = new Canvas(mDrawingBitmap);
            mEpicyclesBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
            mEpicyclesCanvas = new Canvas(mEpicyclesBitmap);
            mEpicyclesCanvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        }
        if (mDrawingBitmap != null) {
            render(canvas);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (b) {
                es.execute(this::run);
                this.b = false;
            }
        }
        return gd.onTouchEvent(event);
    }

    private CoordinateDouble rectCoordinateToCanvasCoordinate(double x, double y) {
        return new CoordinateDouble((x + reOffset) + canvasWidth / 2D, -y + imOffset + canvasHeight / 2D);
    }

    /*private CoordinateDouble rectCoordinateToCanvasCoordinate(CoordinateDouble coordinateDouble) {
        return new CoordinateDouble(coordinateDouble.x + canvasWidth / 2D, -coordinateDouble.y + canvasHeight / 2D);
    }

    private CoordinateDouble canvasCoordinateToRectCoordinate(double x, double y) {
        return new CoordinateDouble(x - canvasWidth / 2D, -y + canvasHeight / 2D);
    }*/

    private CoordinateDouble canvasCoordinateToRectCoordinate(CoordinateDouble coordinateDouble) {
        return new CoordinateDouble(coordinateDouble.x - canvasWidth / 2D - reOffset, -coordinateDouble.y + canvasHeight / 2D + imOffset);
    }
    /*private CoordinateDouble eComplexPower(ComplexValue complexValue, double e_pow_i_num) {
//        return new CoordinateDouble(Math.cos(e_pow_i_num) * complexValue.)
    }*/

    private double getComplexArg(double re, double im) {
        if (re > 0) return Math.atan(im / re);
        if (re == 0 && im > 0) return Math.PI / 2D;
        if (re == 0 && im < 0) return Math.PI / -2D;
        if (re < 0 && im >= 0) return Math.atan(im / re) + Math.PI;
        if (re < 0 && im < 0) return Math.atan(im / re) - Math.PI;
        return 0;
    }

    void scale(double a) {
        quadDrawing.reset();
        epicyclesScale = a;
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
        mEpicyclesCanvas.drawColor(Color.WHITE);
//            清空上一次bitmap上绘画的
        mEpicyclesCanvas.drawLine(0F, (float) (canvasHeight / 2F + imOffset), canvasWidth, (float) (canvasHeight / 2F + imOffset), mCoPaint);
        mEpicyclesCanvas.drawLine((float) (canvasWidth / 2F + reOffset), 0F, (float) (canvasWidth / 2F + reOffset), canvasHeight, mCoPaint);
//            画实轴和虚轴 无箭头
        List<EpicyclesSequence.AEpicycle> epicycles = this.epicyclesSequence.epicycles;
        for (int i = 0; i < epicycles.size(); i++) {
            EpicyclesSequence.AEpicycle epicycle = epicycles.get(i);
            //一次画所有本轮
            float radius = ((float) Math.sqrt(Math.pow(epicycle.c.re * epicyclesScale, 2D) + Math.pow(epicycle.c.im * epicyclesScale, 2D)));
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
                quadDrawing.quadTo(((float) lineTo.x), ((float) lineTo.y));
                mEpicyclesCanvas.drawPath(path, mPathPaint);
            }
        }
        mDrawingCanvas.drawBitmap(mEpicyclesBitmap, 0F, 0F, mBitmapPaint);
        t += .1F;
        canvas.drawBitmap(mDrawingBitmap, 0F, 0F, mBitmapPaint);
    }

    class QuadDrawing {
        Path path;
        private byte i = -1;
        private float cX;
        private float cY;

        QuadDrawing(Path path) {
            this.path = path;
        }

        void quadTo(float x, float y) {
            switch (i) {
                case -1:
                    path.moveTo(x, y);
                    break;
                case 1:
                    cX = x;
                    cY = y;
                    break;
                case 2:
                    path.quadTo(cX, cY, x, y);
                    i = 0;
                    break;
            }
            ++i;
        }

        void reset() {
            path.reset();
            i = -1;
        }
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
}