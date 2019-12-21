package pers.zhc.tools.test.viewtest;

import android.graphics.Color;
import android.graphics.Paint;

/**
 * Author: aaa
 * Date: 2016/10/13 09:54.
 * 涂鸦时所用的画笔
 */
public class MyPen {
    boolean mBooleanPen;//是否是画笔 true - 画笔  false - 橡皮差
    private int mPenColor;
    private int mPenSize;
    private int mPenAlpha;//0 - 100
    private Paint mPaint, mEraserPaint;

    public MyPen(int penColor, int penSize, int penAlpha, boolean bPen) {
        mPenAlpha = penAlpha;
        mPenColor = penColor;
        mPenSize = penSize;
        mBooleanPen = bPen;

        if (mBooleanPen){//画笔
            mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mPenSize);
            mPaint.setColor(mPenColor);
            mPaint.setAlpha(mPenAlpha);

            mPaint.setAntiAlias(true);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
        }else {//橡皮擦
            mEraserPaint = new Paint();
            mEraserPaint.setStyle(Paint.Style.STROKE);
            mEraserPaint.setAlpha(255);
            mEraserPaint.setColor(Color.WHITE);
            mEraserPaint.setStrokeWidth(mPenSize);
            mEraserPaint.setAntiAlias(true);
            mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
            mEraserPaint.setStrokeCap(Paint.Cap.ROUND);

        }
    }

    public int getPenAlpha() {
        return mPenAlpha;
    }

    public void setPenAlpha(int penAlpha) {
        mPenAlpha = penAlpha;
    }

    public int getPenColor() {
        return mPenColor;
    }

    public void setPenColor(int penColor) {
        mPenColor = penColor;
    }

    public int getPenSize() {
        return mPenSize;
    }

    public void setPenSize(int penSize) {
        mPenSize = penSize;
    }

    public boolean isBooleanPen() {
        return mBooleanPen;
    }

    public void setBooleanPen(boolean booleanBen) {
        mBooleanPen = booleanBen;
    }

    public Paint getPenPaint(){
        if (mBooleanPen) {
            return mPaint;
        }else{
            return mEraserPaint;
        }
    }
}