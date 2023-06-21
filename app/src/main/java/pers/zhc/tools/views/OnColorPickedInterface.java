package pers.zhc.tools.views;

import androidx.annotation.IntRange;

/**
 * @author bczhc
 */
public interface OnColorPickedInterface {
    /**
     * @param hsv   HSV array:
     *              <ul>
     *                <li><code>hsv[0]</code> is Hue \([0..360[\)</li>
     *                <li><code>hsv[1]</code> is Saturation \([0...1]\)</li>
     *                <li><code>hsv[2]</code> is Value \([0...1]\)</li>
     *              </ul>
     * @param alpha alpha: [0, 255]
     * @param color ColorInt
     */
    void onColorPicked(
            float[] hsv,
            @IntRange(from = 0, to = 255) int alpha,
            int color,
            boolean fromUser
    );
}
