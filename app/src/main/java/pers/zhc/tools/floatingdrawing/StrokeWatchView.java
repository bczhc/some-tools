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

    StrokeWatchView(Context context, int width, int height) {
        super(context);
        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeJoin(Paint.Join.ROUND);//使画笔更加圆润
        mPaint.setStrokeCap(Paint.Cap.ROUND);//同上
    }

    void change(float strokeWidth, int color) {
        mPaint.setColor(color);
        mPaint.setStrokeWidth(strokeWidth);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(mPaint.getStrokeWidth() / 2, mPaint.getStrokeWidth() / 2);
        canvas.drawPoint(0, 0, mPaint);
    }
}
