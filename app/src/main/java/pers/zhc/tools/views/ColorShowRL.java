package pers.zhc.tools.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import pers.zhc.tools.utils.ColorUtils;

/**
 * @author bczhc
 */
public class ColorShowRL extends RelativeLayout {
    private ColorShowView colorShowView;
    private TextView hexTV;

    public ColorShowRL(Context context) {
        this(context, null);
    }

    public ColorShowRL(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        colorShowView = new ColorShowView(context);
        hexTV = new TextView(context);
        LinearLayout ll = new LinearLayout(context);

        hexTV.setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams llLP = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(llLP);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(colorShowView);
        ll.addView(hexTV);

        this.addView(ll);
    }

    public void setColor(int color) {
        colorShowView.setColor(color);
        hexTV.setText(ColorUtils.getHexString(color, true));
        int measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        hexTV.measure(measureSpec, measureSpec);
        colorShowView.setDiameter(hexTV.getMeasuredWidth());
    }
}
