package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import pers.zhc.tools.BaseView;

@SuppressLint("ViewConstructor")
public class StrokeWatchView extends BaseView {
    private final Paint mPaint;
    private float strokeWidth;

    StrokeWatchView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setColor(Color.BLACK);
        //使画笔更加圆润
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        //同上
        mPaint.setStrokeCap(Paint.Cap.ROUND);
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
