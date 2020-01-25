package pers.zhc.tools.test.viewtest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import pers.zhc.tools.utils.GestureResolver;

@SuppressLint("ViewConstructor")
public class AView extends View {
    private Bitmap bitmap;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private int width = 0, height = 0;
    private Paint mBitmapPaint;
    private GestureResolver gestureResolver;
    private float scale = 1;
    private float screenTransX, screenTransY;
    private float canvasTransX, canvasTransY;

    AView(Context context, Bitmap bitmap) {
        super(context);
        this.bitmap = bitmap;
    }

    private void init() {
        Paint mPaint = new Paint();
        mPaint.setColor(Color.RED);
        Paint mPaint2 = new Paint();
        mPaint2.setColor(Color.GREEN);
        this.mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.mCanvas = new Canvas(mBitmap);
        mBitmapPaint = new Paint();
        gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {
            //            PointF midPoint = new PointF();
//            private float realX, realY;
            @Override
            public void onTwoPointScroll(float distanceX, float distanceY, MotionEvent motionEvent) {
                screenTransX += distanceX;
                screenTransY += distanceY;
                canvasTransX = distanceX / scale;
                canvasTransY = distanceY / scale;
            }

            @Override
            public void onTwoPointZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float dScale, MotionEvent event) {
                AView.this.scale *= dScale;
                mCanvas.scale(dScale, dScale, midPointX - canvasTransX, midPointY - canvasTransY);
            }
        });
    }

    /*private void scaleWithPivot(PointF destPointF, float x, float y, float px, float py, float scale, boolean reverse) {
        if (reverse) {
            destPointF.x = scale * (x - px) + px;
            destPointF.y = scale * (y - py) + py;
        } else {
            destPointF.x = (x - px) / scale + px;
            destPointF.y = (y - py) / scale + py;
        }
    }*/


    @Override
    protected void onDraw(Canvas canvas) {
        if (width == 0 && height == 0) {
            width = getWidth();
            height = getHeight();
            init();
            System.out.println("init");
        }
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gestureResolver.onTouch(event);
        mCanvas.translate(canvasTransX, canvasTransY);
        mCanvas.drawBitmap(this.bitmap, 0, 0, mBitmapPaint);
        invalidate();
        return true;
    }
}