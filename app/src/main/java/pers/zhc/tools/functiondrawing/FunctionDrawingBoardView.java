package pers.zhc.tools.functiondrawing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import pers.zhc.u.MathFloatFunctionInterface;

import java.util.ArrayList;
import java.util.List;

public class FunctionDrawingBoardView extends View {
    private Paint mPaint;
    private Path mPath;
    private int width, height;
    private FFMap funInf;

    public FunctionDrawingBoardView(Context context) {
        super(context);
        mPaint = new Paint();
        Point point = new Point();
        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(point);
        width = point.x;
        height = point.y;
        mPath = new Path();
        mPaint.setStrokeWidth(5);
        mPaint.setStyle(Paint.Style.STROKE);
        funInf = new FFMap();
    }

    private float lastX, lastY;
    private float haveStrokedWidth = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int xLength = 30;
        int yLength = 30;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                mPath = new Path();
                mPath.moveTo(lastX, lastY);
                haveStrokedWidth = 0F;
                funInf.reset();
                break;
            case MotionEvent.ACTION_MOVE:
                if (lastX >= haveStrokedWidth) {
                    mPath.quadTo((lastX + x) / 2, (lastY + y) / 2, x, y);
                    haveStrokedWidth = lastX;
                }
                lastX = x;
                lastY = y;
                float savedX = (x * xLength / width);
                float hD2 = height / 2F;
                if (y <= hD2) {
                    funInf.put(savedX, (hD2 - y) * yLength / height);
                } else funInf.put(savedX, (hD2 - y) * yLength / height);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
        canvas.drawLine(haveStrokedWidth - 1, 0F, haveStrokedWidth + 1, height, mPaint);
        canvas.drawLine(0, height / 2F, width, height / 2F, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width, height);
    }

    MathFloatFunctionInterface getFunction() {
        return funInf.getFunction();
    }

    private class FFMap {
        private List<FF> ffList = new ArrayList<>();

        void reset() {
            ffList.clear();
        }

        private class FF {
            private float k, v;

            FF(float k, float v) {
                this.k = k;
                this.v = v;
            }
        }

        void put(float k, float v) {
            ffList.add(new FF(k, v));
        }

        /*float get(float k) {
            for (FF ff : ffList) {
                if (ff.k == k) {
                    return ff.v;
                }
            }
            return 0F;
        }*/

        MathFloatFunctionInterface getFunction() {
            return v -> {
                for (int i = 1; i < ffList.size(); i++) {
                    try {
                        FF ff1 = ffList.get(i - 1);
                        FF ff2 = ffList.get(i + 1);
                        if (v > ff1.k && v <= ff2.k) {
                            return (ff1.v + ff2.v) / 2;
                        }
                    } catch (Exception ignored) {
                        return 0;
                    }
                }
                return 0;
            };
        }
    }
}
