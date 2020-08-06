package pers.zhc.tools.pressuretest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import pers.zhc.tools.BaseActivity;
import pers.zhc.tools.R;

/**
 * @author bczhc
 */
public class PressureTest extends BaseActivity {

    private TextView tv;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tv = new TextView(this);
        tv.setBackgroundColor(Color.YELLOW);
        TextView view = new TextView(this);
        tv.setTextSize(20F);
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setId(R.id.tv);
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.BELOW, tv.getId());
        view.setLayoutParams(layoutParams);
        relativeLayout.addView(tv);
        relativeLayout.addView(view);
        setContentView(relativeLayout);
    }

    public class TestView extends View {

        public TestView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.GREEN);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE) {
                tv.setText(String.valueOf(event.getPressure()));
            } else tv.setText("0");
            return true;
        }
    }
}
