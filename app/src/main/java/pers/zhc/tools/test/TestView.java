package pers.zhc.tools.test;

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
public class TestView extends View {
    private int w, h;
    private Paint mPaint;

    public TestView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setTextSize(20);
        mPaint.setColor(Color.RED);
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        canvas.drawText(String.valueOf(w), 30, 30, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        w = MeasureSpec.getSize(widthMeasureSpec);
        h = MeasureSpec.getSize(heightMeasureSpec);
    }
}
