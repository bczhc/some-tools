package pers.zhc.tools.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import pers.zhc.tools.R;

/**
 * @author bczhc
 */
public class StrokeShowView extends View {
    private float diameter = 50F;
    private int color = Color.TRANSPARENT;
    private Paint mPaint;

    public StrokeShowView(Context context) {
        this(context, null);
    }

    public StrokeShowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mPaint = new Paint();
        mPaint.setColor(this.color);
        mPaint.setStrokeWidth(20);

        final TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.StrokeShowView);
        color = ta.getColor(R.styleable.StrokeShowView_color, Color.TRANSPARENT);
        diameter = ((int) ta.getDimension(R.styleable.StrokeShowView_diameter, 50));
        ta.recycle();
    }

    /**
     *
     * @param diameter in px
     */
    public void setDiameter(float diameter) {
        this.diameter = diameter;
        invalidate();
        requestLayout();
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        mPaint.setColor(this.color);
        canvas.save();
        float start = diameter / 2F;
        canvas.translate(start, start);
        canvas.drawCircle(0, 0, start, mPaint);
        canvas.restore();
    }

    private int mMeasure(int defaultSize, int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                return size;
            case MeasureSpec.UNSPECIFIED:
                return defaultSize;
            case MeasureSpec.AT_MOST:
                if (defaultSize > size) defaultSize = size;
                return defaultSize;
            default:
        }
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int d = ((int) Math.ceil(diameter));

        int measuredWidth = mMeasure(d, widthMeasureSpec);
        int measuredHeight = mMeasure(d, heightMeasureSpec);

        setMeasuredDimension(measuredWidth, measuredHeight);
    }
}
