package pers.zhc.tools.test.epicycles_test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

public class ComplexGraphDrawingView extends View {

    private float width;
    private float height;
    private Paint mCoPaint;
    private Paint mPaint;
    private Path mPath;
    static ComplexFunction complexFunction;

    public ComplexGraphDrawingView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mCoPaint = new Paint();
        mCoPaint.setStrokeWidth(1);
        mCoPaint.setStyle(Paint.Style.STROKE);
        mCoPaint.setColor(Color.GRAY);
        mPaint = new Paint();
        mPaint.setStrokeWidth(2);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        complexFunction = new ComplexFunction();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        width = ((float) getWidth());
        height = ((float) getHeight());
        canvas.drawLine(0F, height / 2F, width, height / 2F, mCoPaint);
        canvas.drawLine(width / 2F, 0F, width / 2F, height, mCoPaint);
//            画实轴和虚轴 无箭头
        if (mPath != null) {
            canvas.drawPath(mPath, mPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                complexFunction.clear();
                mPath = new Path();
                mPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                mPath.close();
                break;
        }
        complexFunction.put(x - width / 2D, -y + height / 2D);
        invalidate();
        return true;
    }
}