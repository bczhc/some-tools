package pers.zhc.tools.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * @author bczhc
 */
public class ColorShowRL extends RelativeLayout {
    private StrokeShowView strokeShowView;
    private TextView hexTV;

    public ColorShowRL(Context context) {
        this(context, null);
    }

    public ColorShowRL(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        strokeShowView = new StrokeShowView(context);
        hexTV = new TextView(context);
        LinearLayout ll = new LinearLayout(context);

        hexTV.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams llLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(llLP);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(strokeShowView);
        ll.addView(hexTV);

        this.addView(ll);
    }

    public void setColor(int color, String name) {
        strokeShowView.setColor(color);
        hexTV.setText(name);
        int measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        hexTV.measure(measureSpec, measureSpec);
        strokeShowView.setDiameter(hexTV.getMeasuredWidth());
    }
}
