package pers.zhc.tools.floatingdrawing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseView;

public class ScreenColorPickerView extends BaseView {
    private int width = -1, height = -1;

    public ScreenColorPickerView(Context context) {
        super(context);
    }

    public ScreenColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.RED);
    }

    private void init() {
        Paint mPaint = new Paint();
        mPaint.setStrokeWidth(10F);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        boolean b = false;
        if (width == -1 && height == -1) {
            b = true;
        }
        width = ((int) (MeasureSpec.getSize(widthMeasureSpec) * .4F));
        height = ((int) (MeasureSpec.getSize(heightMeasureSpec) * .4F));
        int w = 0, h = 0;
        if (wMode != MeasureSpec.AT_MOST && hMode != MeasureSpec.AT_MOST) {
            int min = Math.min(width, height);
            width = (height = min);
            w = width;
            h = height;
        }
        setMeasuredDimension(w, h);
        if (b) {
            init();
        }
    }
}
