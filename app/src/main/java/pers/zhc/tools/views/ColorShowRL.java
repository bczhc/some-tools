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
    private HSVAColorPickerRL.SavedColor savedColor;

    @Nullable
    private OnColorViewClickedListener onColorViewClickedListener = null;
    @Nullable
    private OnLongClickListener onLongClickListener = null;

    public ColorShowRL(Context context) {
        this(context, null);
    }

    public ColorShowRL(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        final View inflate = View.inflate(context, R.layout.color_picker_color_show_layout, null);
        colorView = inflate.findViewById(R.id.color_view);
        nameTV = inflate.findViewById(R.id.name_tv);

        colorView.setOnClickListener(v -> {
            if (onColorViewClickedListener != null) {
                onColorViewClickedListener.onClick(this);
            }
        });

        colorView.setOnLongClickListener(v -> {
            if (onLongClickListener != null) {
                return onLongClickListener.onLongClick(v);
            }
            return false;
        });
        nameTV.setOnLongClickListener(v -> {
            if (onLongClickListener != null) {
                return onLongClickListener.onLongClick(v);
            }
            return false;
        });

        this.addView(inflate);
    }

    public void setColor(HSVAColorPickerRL.SavedColor savedColor) {
        this.savedColor = savedColor;
        this.color = savedColor.getColorInt();
        this.colorView.setColor(this.color);
    }

    public HSVAColorPickerRL.SavedColor getColor() {
        return savedColor;
    }

    public void setName(String name) {
        nameTV.setText(name);
    }

    public String getName() {
        return name;
    }

    public void setOnColorViewClickedListener(@Nullable OnColorViewClickedListener listener) {
        this.onColorViewClickedListener = listener;
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public interface OnColorViewClickedListener {
        void onClick(ColorShowRL self);
    }
}
