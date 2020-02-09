package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SurfacePaintView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private boolean isDrawing = false;
    private SurfaceHolder mSurfaceHolder;
    private Canvas surfaceCanvas;
    private int width, height;
    private Bitmap mBitmap;
    private MyCanvas mCanvas;
    private Paint mBitmapPaint;
    private Paint mPaint;

    public SurfacePaintView(Context context) {
        super(context);
        surfaceInit();
    }

    private void surfaceInit() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isDrawing = true;
        width = getWidth();
        height = getHeight();
        new Thread(this).start();
        init();
    }

    private void init() {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new MyCanvas(mBitmap);
        mBitmapPaint = new Paint();
        mPaint = new Paint();
        mPaint.setStrokeWidth(10F);
        mPaint.setColor(Color.RED);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDrawing = false;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (surfaceCanvas != null) {
            surfaceCanvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            mSurfaceHolder.unlockCanvasAndPost(surfaceCanvas);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
