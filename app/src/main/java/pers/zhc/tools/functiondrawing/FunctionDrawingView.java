package pers.zhc.tools.functiondrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import pers.zhc.u.MathFloatFunctionInterface;

@SuppressLint("ViewConstructor")
class FunctionDrawingView extends View {
    private int width, height;
    private Paint mPaint;
    private MathFloatFunctionInterface f;
    private float xLength, yLength;
    private float phaseC = 0;
    private float yC = 0;

    FunctionDrawingView(Context context, int width, int height, float xLength, float yLength) {
        super(context);
        this.width = width;
        this.height = height;
        mPaint = new Paint();
        mPaint.setStrokeWidth(1);
        mPaint.setColor(Color.BLACK);
        this.xLength = xLength / 2F;
        this.yLength = yLength / 2F;
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
            float x = i * 2 * xLength / (float) width - xLength;
            float y = this.f.f(x + phaseC) + yC;
            float drawY = -(y * (float) width / yLength - (float) height / 2);
            canvas.drawPoint(i, drawY, mPaint);
        }
    }

    void zoom(float newXLength, float newYLength) {
        this.xLength = newXLength;
        this.yLength = newYLength;
        invalidate();
    }

    void drawFunction(MathFloatFunctionInterface f) {

        mPaint.setStrokeWidth(4);
        mPaint.setStrokeJoin(Paint.Join.ROUND);//使画笔更加圆润
        mPaint.setStrokeCap(Paint.Cap.ROUND);//同上
        this.f = f;
        invalidate();
    }

    private float lastX, lastY;
    private float nowX = 0, nowY = 0;
    private float pX, pY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getRawX();
                lastY = event.getRawY();
                pX = nowX;
                pY = nowY;
                break;
            case MotionEvent.ACTION_MOVE:
                float dX = event.getRawX() - lastX;
                float dY = event.getRawY() - lastY;
                nowX = pX + dX;
                nowY = pY + dY;
                this.phaseC = -nowX * xLength / width;
                this.yC = -nowY * yLength / height;
                invalidate();
            case MotionEvent.ACTION_UP:
                this.performClick();
                break;
        }
        return true;
    }
}