package pers.zhc.tools.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * @author bczhc
 */
public class ColorShowView extends View {
    private int diameter = 50;
    private int color = Color.TRANSPARENT;
    private Paint mPaint;

    public ColorShowView(Context context) {
        this(context, null);
    }

    public ColorShowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(this.color);
        mPaint.setStrokeWidth(20);
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
        invalidate();
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        mPaint.setColor(this.color);
        canvas.save();
        float start = ((float) diameter) / 2F;
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
        int measuredWidth = mMeasure(diameter, widthMeasureSpec);
        int measuredHeight = mMeasure(diameter, heightMeasureSpec);

        diameter = Math.min(measuredWidth, measuredHeight);
        setMeasuredDimension(diameter, diameter);
    }
}
