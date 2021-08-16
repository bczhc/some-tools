package pers.zhc.tools.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.R;
import pers.zhc.tools.utils.CanvasTransformer;
import pers.zhc.tools.utils.GestureResolver;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new MyView(this));
    }

    private static class MyView extends BaseView {

        private final Paint mPaint;
        private final Bitmap mBitmap;
        private final Canvas mCanvas;
        private final Paint bitmapPaint = new Paint();
        private final CanvasTransformer canvasTransformer;

        private final GestureResolver gestureResolver = new GestureResolver(new GestureResolver.GestureInterface() {
            @Override
            public void onTwoPointsScroll(float distanceX, float distanceY, MotionEvent event) {
                canvasTransformer.absTranslate(distanceX, distanceY);
            }

            @Override
            public void onTwoPointsZoom(float firstMidPointX, float firstMidPointY, float midPointX, float midPointY, float firstDistance, float distance, float scale, float dScale, MotionEvent event) {
                canvasTransformer.absScale(dScale, midPointX, midPointY);
            }

            @Override
            public void onTwoPointsUp(MotionEvent event) {

            }

            @Override
            public void onTwoPointsDown(MotionEvent event) {

            }

            @Override
            public void onTwoPointsPress(MotionEvent event) {

            }

            @Override
            public void onTwoPointsRotate(MotionEvent event, float firstMidX, float firstMidY, float degrees, float midX, float midY) {

            }

            @Override
            public void onOnePointScroll(float distanceX, float distanceY, MotionEvent event) {

            }
        });

        public MyView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            mBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            canvasTransformer = new CanvasTransformer(mCanvas);

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);

            redraw();
        }

        private void redraw() {
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mPaint.setMaskFilter(new BlurMaskFilter(50, BlurMaskFilter.Blur.NORMAL));
            mPaint.setStrokeWidth(100);
            mCanvas.drawPoint(100, 100, mPaint);
        }

        @Override
        protected void onDraw(@NotNull Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, bitmapPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            gestureResolver.onTouch(event);
            redraw();
            invalidate();
            return true;
        }
    }
}