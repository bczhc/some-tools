package pers.zhc.tools.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;
import pers.zhc.util.Assertion;

/**
 * @author bczhc
 */
public class RoundColorView extends View {
    private int color = Color.TRANSPARENT;
    private Paint mPaint;

    public RoundColorView(Context context) {
        this(context, null);
    }

    public RoundColorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mPaint = new Paint();
        mPaint.setColor(this.color);
        mPaint.setStrokeWidth(20);

        final TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.RoundColorView);
        color = ta.getColor(R.styleable.RoundColorView_color, Color.TRANSPARENT);
        ta.recycle();
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        Assertion.doAssertion(getMeasuredWidth() == getMeasuredHeight());
        mPaint.setColor(this.color);
        int diameter = getMeasuredHeight();
        float start = diameter / 2F;
        canvas.translate(start, start);
        canvas.drawCircle(0, 0, start, mPaint);
    }

    private int mMeasure(int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
            case MeasureSpec.AT_MOST:
                return size;
            case MeasureSpec.UNSPECIFIED:
                return 0;
            default:
        }
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

//        if (heightMode == MeasureSpec.EXACTLY && widthMode == MeasureSpec.UNSPECIFIED) {
//            setMeasuredDimension(heightSize, heightSize);
//        }

        int measuredWidth = mMeasure(widthMeasureSpec);
        int measuredHeight = mMeasure(heightMeasureSpec);
        int min = Math.min(measuredWidth, measuredHeight);
        setMeasuredDimension(min, min);
    }
}
