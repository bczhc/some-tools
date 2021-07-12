package pers.zhc.tools.test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.fdb.FdbWindow;

/**
 * @author bczhc
 */
public class Demo extends BaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LinearLayout ll = new LinearLayout(this);
        ll.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final VV vv = new VV(this);
        vv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.addView(vv);

        setContentView(ll);
    }


    private static class VV extends BaseView {
        private float d = 100;
        private Paint paint = new Paint();

        public VV(Context context) {
            super(context);

            paint.setColor(Color.RED);

            setOnClickListener(v -> {
                d += 10;
                invalidate();
                requestLayout();

                final FdbWindow fdbWindow = new FdbWindow(((Activity) getContext()));
                fdbWindow.startFDB();
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float p = d / 2F;
            canvas.drawCircle(p, p, p, paint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int m = (int) ((int) d);
            setMeasuredDimension(m, m);
        }
    }
}