package pers.zhc.tools.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.jetbrains.annotations.Nullable;
import pers.zhc.tools.R;

/**
 * @author bczhc
 */
public class ColorShowRL extends RelativeLayout {
    private RoundColorView colorView;
    private TextView nameTV;
    private int color;
    private String name;

    @Nullable
    private OnColorViewClickedListener onColorViewClickedListener = null;

    public ColorShowRL(Context context) {
        this(context, null);
    }

    public ColorShowRL(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        final View inflate = View.inflate(context, R.layout.color_picker_color_show_ll, null);
        colorView = inflate.findViewById(R.id.color_view);
        nameTV = inflate.findViewById(R.id.name_tv);

        colorView.setOnClickListener(v -> {
            if (onColorViewClickedListener != null) {
                onColorViewClickedListener.onClick(this);
            }
        });

        this.addView(inflate);
    }

    public void setColor(int color, String name) {
        this.color = color;
        this.name = name;

        colorView.setColor(color);
        nameTV.setText(name);
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public void setOnColorViewClickedListener(OnColorViewClickedListener listener) {
        this.onColorViewClickedListener = listener;
    }

    public interface OnColorViewClickedListener {
        void onClick(ColorShowRL self);
    }
}
