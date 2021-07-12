package pers.zhc.tools.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.floatingdrawing.FloatingViewOnTouchListener;
import pers.zhc.tools.floatingdrawing.ScreenColorPickerView;
import pers.zhc.tools.utils.ToastUtils;

public class S extends BaseActivity {
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        if (wm == null) {
            ToastUtils.show(this, "wm is null");
            return;
        }
        ScreenColorPickerView screenColorPickerView = new ScreenColorPickerView(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //noinspection deprecation
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        lp.format = PixelFormat.RGBA_8888;
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        int screenWidth = point.x;
        int screenHeight = point.y;
        int wMode = View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY);
        int hMode = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.EXACTLY);
        screenColorPickerView.measure(wMode, hMode);
        int measuredWidth = screenColorPickerView.getMeasuredWidth();
        int measuredHeight = screenColorPickerView.getMeasuredHeight();
        FloatingViewOnTouchListener listener = new FloatingViewOnTouchListener(lp, wm, screenColorPickerView, screenWidth, screenHeight, new FloatingViewOnTouchListener.ViewDimension(measuredWidth, measuredHeight));
        screenColorPickerView.setOnTouchListener(listener);
        lp.width = screenWidth;
        lp.height = screenHeight;
        wm.addView(screenColorPickerView, lp);
    }
}

class AStroke extends BaseView {
    private int width = -1, height = -1;
    private Paint mPaint;
    private Path mPath;


    public AStroke(Context context) {
        super(context);
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (width == -1 && height == -1) {
            width = getWidth();
            height = getHeight();
            init();
        }
        if (mPath != null) {
            canvas.drawPath(mPath, mPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath = new Path();
                mPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        invalidate();
        return true;
    }
}

class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private float mPrevX;
    private float mPrevY;
    private Path mPath;

    public MySurfaceView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
        mPaint = new Paint();
        mPaint.setStrokeWidth(10F);
        mPaint.setColor(Color.RED);
        setZOrderOnTop(true);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void run() {
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mPrevX = x;
                mPrevY = y;
                mPath = new Path();
                mPath.moveTo(x, y);//将 Path 起始坐标设为手指按下屏幕的坐标
                break;
            case MotionEvent.ACTION_MOVE:
                Canvas canvas = mSurfaceHolder.lockCanvas();
//                restorePreAction(canvas);//首先恢复之前绘制的内容
                mPath.quadTo(mPrevX, mPrevY, (x + mPrevX) / 2, (y + mPrevY) / 2);
                //绘制贝塞尔曲线，也就是光滑的曲线，如果此处使用 lineTo 方法滑出的曲线会有折角
                mPrevX = x;
                mPrevY = y;
                canvas.drawPath(mPath, mPaint);
                mSurfaceHolder.unlockCanvasAndPost(canvas);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }
}
