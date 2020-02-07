package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

@SuppressLint("ViewConstructor")
public class StrokeWatchView extends View {
    private Paint mPaint;
    private float strokeWidth;

    StrokeWatchView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeJoin(Paint.Join.ROUND);//使画笔更加圆润
        mPaint.setStrokeCap(Paint.Cap.ROUND);//同上
    }

    void change(float strokeWidth, int color) {
        forceLayout();
        requestLayout();
        mPaint.setColor(color);
        mPaint.setStrokeWidth(strokeWidth);
        this.strokeWidth = strokeWidth;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(strokeWidth / 2F, strokeWidth / 2F);
        canvas.drawPoint(0, 0, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int d = ((int) Math.floor(strokeWidth));
        setMeasuredDimension(d, d);
    }
}
