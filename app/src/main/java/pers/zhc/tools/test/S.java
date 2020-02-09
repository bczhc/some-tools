package pers.zhc.tools.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.floatingdrawing.MyCanvas;
import pers.zhc.tools.floatingdrawing.PaintView;

import java.io.File;

public class S extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Point point = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(point);
        int width = point.x, height = point.y;
        File file = new File("/storage/emulated/0/z.path");
        PaintView paintView = new PaintView(this, width, height, file);
        paintView.setOS(file, false);
        setContentView(paintView);
    }
}

class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private SurfaceHolder mSurfaceHolder;
    private Canvas surfaceCanvas;
    private boolean isDrawing;
    private Paint mPaint;
    private Path mPath = null;
    private Bitmap mBitmap;
    private int width, height;
    private Paint mBitmapPaint;
    private MyCanvas mCanvas;

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
        mBitmapPaint = new Paint();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.isDrawing = true;
        width = getWidth();
        height = getHeight();
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        mCanvas = new MyCanvas(mBitmap);
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.isDrawing = false;
    }

    @Override
    public void run() {
        while (isDrawing) {
            drawing();
        }
    }

    private void drawing() {
        try {
            surfaceCanvas = mSurfaceHolder.lockCanvas();
        } catch (Exception ignored) {

        }
        if (surfaceCanvas != null) {
            surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            surfaceCanvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            mSurfaceHolder.unlockCanvasAndPost(surfaceCanvas);
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
                mCanvas.drawPath(mPath, mPaint);
                mPath = null;
                break;
        }
        return true;
    }
}
