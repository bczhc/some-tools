package pers.zhc.tools.test;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import pers.zhc.tools.BaseView;
import pers.zhc.tools.diary.DiaryBaseActivity;
import pers.zhc.tools.views.HSVAColorPickerRL;

/**
 * @author bczhc
 */
public class Demo extends DiaryBaseActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final HSVAColorPickerRL hsvaColorPickerRL = new HSVAColorPickerRL(this, Color.RED);
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(hsvaColorPickerRL);
        dialog.show();

        setContentView(new HSVAColorPickerRL(this, Color.RED));
    }

    private static class MV extends RelativeLayout {
        private View[] views = new View[2];
        private LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 0);

        public MV(Context context) {
            super(context);
            LinearLayout ll = new LinearLayout(getContext());
            ll.setOrientation(LinearLayout.VERTICAL);

            views[0] = new CV(getContext());
            views[1] = new CV2(getContext());

            for (View v : views) {
                ll.addView(v);
            }

            this.addView(ll);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
            final int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

            params.width = measuredWidth;
            params.height = measuredHeight / 2;
            for (View view : views) {
                view.setLayoutParams(params);
            }

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        private static class CV extends BaseView {
            public CV(Context context) {
                super(context);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                canvas.drawColor(Color.RED);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                final int w = MeasureSpec.getSize(widthMeasureSpec);
                final int h = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(w, h);
            }
        }

        private static class CV2 extends BaseView {
            public CV2(Context context) {
                super(context);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                canvas.drawColor(Color.BLUE);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                final int w = MeasureSpec.getSize(widthMeasureSpec);
                final int h = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(w, h);
            }
        }
    }
}