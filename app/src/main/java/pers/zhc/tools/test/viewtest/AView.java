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
    private Paint mPaint;
    private Paint mPaint2;
    private Bitmap bitmap;
    private Canvas mCanvas;
    private Bitmap mBitmap;
    private int width = 0, height = 0;
    private Paint mBitmapPaint;
    private GestureResolver gestureResolver;
    private float scale = 1;
    private float transX, transY;

    AView(Context context, Bitmap bitmap) {
        super(context);
        this.bitmap = bitmap;
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint2 = new Paint();
        mPaint2.setColor(Color.YELLOW);
        this.mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.mCanvas = new Canvas(mBitmap);
        mBitmapPaint = new Paint();
        gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {
            private float realX, realY;
            @Override
            public void onTwoPointScroll(float distanceX, float distanceY, MotionEvent motionEvent) {
//                mCanvas.translate(distanceX / scale, distanceY / scale);
                realX = distanceX / scale;
                realY = distanceY / scale;
                transX += distanceX;
                transY += distanceY;
                mCanvas.translate(realX, realY);
            }

            @Override
            public void onTwoPointZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float pScale, MotionEvent event) {
                AView.this.scale *= pScale;
                mCanvas.scale(pScale, pScale, firstMidPointX - transX, firstMidPointY - transY);
            }
        });
    }

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
        mCanvas.drawBitmap(this.bitmap, 0, 0, mBitmapPaint);
        invalidate();
        this.gestureResolver.onTouch(event);
        return true;
    }
}