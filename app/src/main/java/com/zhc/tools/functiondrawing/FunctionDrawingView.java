package com.zhc.tools.functiondrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

@SuppressLint("ViewConstructor")
class FunctionDrawingView extends View {
    private int width, height;
    private Paint mPaint;
    private boolean firstDraw = true;
    private FunctionInterface f;

    FunctionDrawingView(Context context, int width, int height) {
        super(context);
        this.width = width;
        this.height = height;
        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawLine(((float) width) / 2F, 0, ((float) width) / 2, ((float) height), mPaint);
        canvas.drawLine(0F, ((float) height) / 2, ((float) width), ((float) height) / 2, mPaint);
        canvas.drawLine(((float) width), ((float) (height / 2)), ((float) width - 10F), ((float) (height / 2) + 10F), mPaint);
        canvas.drawLine(((float) width), ((float) (height / 2)), ((float) width - 15F), ((float) (height / 2) - 15F), mPaint);
        canvas.drawLine(((float) (width / 2)), 0F, ((float) (width / 2) - 15F), 15F, mPaint);
        canvas.drawLine(((float) (width / 2)), 0F, ((float) (width / 2) + 15F), 15F, mPaint);
        for (int i = 0; i < width; i++) {
            float x = i * 200F / (float) width -100F;
            float y = this.f.f(x);
            float drawY = -(y * (float) width / 200 - (float) height / 2);
            canvas.drawPoint(i, drawY, mPaint);
        }
    }

    void drawFunction(FunctionInterface f, int strokeWidth) {
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStrokeJoin(Paint.Join.ROUND);//使画笔更加圆润
        mPaint.setStrokeCap(Paint.Cap.ROUND);//同上
        this.f = f;
        invalidate();
    }

    interface FunctionInterface {
        float f(float x);
    }
}