package pers.zhc.tools.functiondrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import pers.zhc.tools.BaseView;
import pers.zhc.u.math.util.MathFunctionInterface;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("ViewConstructor")
public class FunctionDrawerView extends BaseView {
    private final Context ctx;
    private final double xLength;
    private final double yLength;
    private int canvasWidth, canvasHeight, canvasHalfWidth, canvasHalfHeight;
    private Paint coordPaint;
    private float xOffset, yOffset;
    private GestureDetector gestureDetector;
    private float xCenter, yCenter;
    private Paint curvePaint;
    private ExecutorService es;
    private MathFunctionInterface f;
    private float scaleX = 1;
    private float scaleY = 1;
    private boolean first2Down = false;
    private double firstDistance;


    FunctionDrawerView(Context context, double xLength, double yLength) {
        super(context);
        ctx = context;
        this.xLength = xLength;
        this.yLength = yLength;
        init();
    }

    private void init() {
        //坐标轴相关的paint
        coordPaint = new Paint();
        curvePaint = new Paint();
        curvePaint.setStrokeWidth(3);
        curvePaint.setColor(Color.WHITE);
        coordPaint.setStrokeWidth(1);
        coordPaint.setColor(Color.GRAY);
        gestureDetector = new GestureDetector(ctx, new GestureDetector.OnGestureListener() {
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
                xOffset -= distanceX;
                yOffset -= distanceY;
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
        gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                xOffset = (yOffset = 0);
                es.execute(FunctionDrawerView.this::postInvalidate);
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });
        es = Executors.newCachedThreadPool();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvasHalfWidth = (int) ((canvasWidth = getWidth()) / 2F);
        canvasHalfHeight = (int) ((canvasHeight = getHeight()) / 2F);
        setCanvasBackGround(canvas);
        canvas.translate(xCenter, yCenter);
        render(canvas);
    }

    private void render(Canvas canvas) {
        float endX = canvasHalfWidth - xOffset;
        float addition = ((float) (((double) canvasWidth) / 2000D));
        for (float x = (-canvasHalfWidth - xOffset) / scaleX; x <= endX; x += addition) {
            double fX = x * xLength / canvasWidth;
            try {
                double y = (-f.f(fX) * canvasHalfWidth / yLength) / scaleY;
                canvas.drawPoint(x, ((float) y), curvePaint);
            } catch (Exception ignored) {
            }
        }
    }

    private void setCanvasBackGround(Canvas canvas) {
        float canvasHalfWidthWithOffset = canvasHalfWidth + xOffset;
        float canvasHalfHeightWithOffset = canvasHalfHeight + yOffset;
        //背景颜色
        canvas.drawColor(Color.BLACK);
        //x轴
        canvas.drawLine(0F, canvasHalfHeightWithOffset, canvasWidth, canvasHalfHeightWithOffset, coordPaint);
        //y轴
        canvas.drawLine(canvasHalfWidthWithOffset, 0F, canvasHalfWidthWithOffset, canvasHeight, coordPaint);
        //x坐标轴箭头
        canvas.drawLine(canvasHalfWidthWithOffset, 0F, canvasHalfWidthWithOffset - 10F, 10F, coordPaint);
        canvas.drawLine(canvasHalfWidthWithOffset, 0F, canvasHalfWidthWithOffset + 10F, 10F, coordPaint);
        //y坐标轴箭头
        canvas.drawLine(canvasWidth, canvasHalfHeightWithOffset, canvasWidth - 10F, canvasHalfHeightWithOffset + 10F, coordPaint);
        canvas.drawLine(canvasWidth, canvasHalfHeightWithOffset, canvasWidth - 10F, canvasHalfHeightWithOffset - 10F, coordPaint);
        xCenter = canvasHalfWidthWithOffset;
        yCenter = canvasHalfHeightWithOffset;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount == 2) {
            float x1 = event.getX(0);
            float x2 = event.getX(1);
            float y1 = event.getY(0);
            float y2 = event.getY(1);
            double firstDistance = Math.sqrt(Math.pow((x1 - x2), 2)) + Math.sqrt(Math.pow((y1 - y2), 2));
            if (!first2Down) {
                this.firstDistance = firstDistance;
            }
            first2Down = true;
            //            float zoomPointX = (x1 + x2) / 2;
//            float zoomPointY = (y1 + y2) / 2;
            scaleX *= firstDistance / this.firstDistance;
            scaleY *= firstDistance / this.firstDistance;
        } else if (pointerCount == 1) {
            first2Down = false;
            gestureDetector.onTouchEvent(event);
            es.execute(this::postInvalidate);
        }
        return true;
    }

    void drawFunction(MathFunctionInterface functionInterface) {
        this.f = functionInterface;
        es.execute(this::postInvalidate);
    }
}