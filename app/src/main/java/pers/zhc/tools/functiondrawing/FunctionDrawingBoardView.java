package pers.zhc.tools.functiondrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

import pers.zhc.tools.BaseView;
import pers.zhc.u.math.util.MathFloatFunctionInterface;
import pers.zhc.u.util.FFMap;

@SuppressLint("ViewConstructor")
public class FunctionDrawingBoardView extends BaseView {
    private final int xLength;
    private final int yLength;
    private final Paint mPaint;
    private final int width;
    private final int height;
    private final FFMap funInf;
    private Path mPath;
    private float lastX, lastY;
    private float haveStrokedWidth = 0;

    FunctionDrawingBoardView(Context context, int[] r) {
        super(context);
        mPaint = new Paint();
        Point point = new Point();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getSize(point);
        width = point.x;
        height = point.y;
        mPath = new Path();
        mPaint.setStrokeWidth(5);
        mPaint.setStyle(Paint.Style.STROKE);
        funInf = new FFMap();
        xLength = r[0];
        yLength = r[1];
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
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
                funInf.put(savedX, (hD2 - y) * yLength / height);
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
}
