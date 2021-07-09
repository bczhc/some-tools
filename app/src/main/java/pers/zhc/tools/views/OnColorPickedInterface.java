package pers.zhc.tools.views;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;

/**
 * @author bczhc
 */
public interface OnColorPickedInterface {
    void onColorPicked(
            @FloatRange(from = 0, to = 1) float[] hsv,
            @IntRange(from = 0, to = 255) int alpha,
            int color
    );
}
