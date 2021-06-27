package pers.zhc.tools.test;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        WindowManager wm = (WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        final MV b = new MV(this);
        b.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.width = MATCH_PARENT;
        lp.height = MATCH_PARENT;
        lp.format = PixelFormat.RGBA_8888;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        wm.addView(b, lp);

    }

    private static class MV extends View {
        private final Paint mPaint;

        public MV(Context context) {
            super(context);
            mPaint = new Paint();
            mPaint.setColor(Color.RED);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float width = getMeasuredWidth();
            final float height = getMeasuredHeight();
            canvas.drawRect(width / 4F, height / 4F, width / 4F * 3F, height / 4F * 3F, mPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}