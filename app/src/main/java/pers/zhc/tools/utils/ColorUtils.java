package pers.zhc.tools.utils;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;

@SuppressWarnings({"unused"})
public class ColorUtils {
    private final int[] colors;
    private final float startPos;
    private final float endPos;

    ColorUtils(int[] colors, float startPosition, float endPosition) {
        this.colors = colors;
        this.startPos = startPosition;
        this.endPos = endPosition;
    }

    private static RGB parseRGB(int color) {
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);
        return new RGB(red, green, blue);
    }

    public static float getH(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[0];
    }

    private static String get2Hex(int i) {
        String s = Integer.toHexString(i);
        if (s.length() == 1) {
            return "0" + s;
        }
        return s;
    }

    public static String getHexString(int color, boolean alpha) {
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);
        if (alpha) {
            return "#" + get2Hex(Color.alpha(color)).toUpperCase()
                    + get2Hex(red).toUpperCase() +
                    get2Hex(green).toUpperCase() +
                    get2Hex(blue).toUpperCase();
        }
        return "#" + get2Hex(red).toUpperCase() +
                get2Hex(green).toUpperCase() +
                get2Hex(blue).toUpperCase();
    }

    public static void colorHexToHSV(float[] hsv, String hex) {
        String a = hex.substring(1, 3);
        String b = hex.substring(3, 5);
        String c = hex.substring(5, 7);
        int color = parseColorInt(Integer.parseInt(a, 16), Integer.parseInt(b, 16), Integer.parseInt(c, 16));
        Color.colorToHSV(color, hsv);
    }

    private static int parseColorInt(RGB rgb) {
        return 0xff000000 | (rgb.r << 16) | (rgb.g << 8) | rgb.b;
    }

    private static int parseColorInt(int r, int g, int b) {
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static int invertColor(int color) {
        /*float[] HSV = new float[3];
        Color.colorToHSV(color, HSV);
        HSV[0] = HSV[0] + 180 - (HSV[0] > 180 ? 360 : 0);
        return Color.HSVToColor(HSV);*/
        RGB rgb = parseRGB(color);
        return parseColorInt(new RGB(255 - rgb.r, 255 - rgb.g, 255 - rgb.b));
    }

    /**
     * @param hue        hue
     * @param saturation saturation
     * @param value      value
     * @param alpha      alpha
     * @return color int
     */
    @ColorInt
    public static int HSVAtoColor(@FloatRange(from = 0, to = 255) int alpha,
                                @FloatRange(from = 0, to = 360) float hue,
                                @FloatRange(from = 0, to = 1) float saturation,
                                @FloatRange(from = 0, to = 1) float value) {
        hue /= 360;
        int r, g, b;
        int h = (int) (hue * 6);
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        switch (h) {
            case 0:
                r = ((int) (value * 255));
                g = ((int) (t * 255));
                b = ((int) (p * 255));
                break;
            case 1:
                r = ((int) (q * 255));
                g = ((int) (value * 255));
                b = ((int) (p * 255));
                break;
            case 2:
                r = ((int) (p * 255));
                g = ((int) (value * 255));
                b = ((int) (t * 255));
                break;
            case 3:
                r = ((int) (p * 255));
                g = ((int) (q * 255));
                b = ((int) (value * 255));
                break;
            case 4:
                r = ((int) (t * 255));
                g = ((int) (p * 255));
                b = ((int) (value * 255));
                break;
            default:
                r = ((int) (value * 255));
                g = ((int) (p * 255));
                b = ((int) (q * 255));
                break;
        }
        return b | g << 8 | r << 16 | alpha << 24;
    }

    int getColor(float pos) throws Exception {
        float perAreaWidth = (endPos - startPos) / (colors.length - 1);
        int area = (int) ((pos - startPos) / perAreaWidth) + (((pos - startPos) % perAreaWidth) > 0 ? 1 : 0);
//        System.out.println("area = " + area);
        int color;
        try {
            color = new TwoColor(colors[area - 1], colors[area], 0F, perAreaWidth).getColor((pos - startPos) % perAreaWidth);
        } catch (Exception e) {
            throw new Exception(e.toString());
        }
        return color;
    }

    private static class TwoColor {
        private final RGB rgb1;
        private final RGB rgb2;
        private final float sP;
        private final float eP;

        private TwoColor(int color1, int color2, float sP, float eP) {
            this.sP = sP;
            this.eP = eP;
            rgb1 = parseRGB(color1);
            rgb2 = parseRGB(color2);
        }

        private int getColor(float pos) {
            float R = ((rgb2.r - rgb1.r) / (eP - sP)) * pos + rgb1.r - sP * (rgb2.r - rgb1.r) / (eP - sP);
            float G = ((rgb2.g - rgb1.g) / (eP - sP)) * pos + rgb1.g - sP * (rgb2.g - rgb1.g) / (eP - sP);
            float B = ((rgb2.b - rgb1.b) / (eP - sP)) * pos + rgb1.b - sP * (rgb2.b - rgb1.b) / (eP - sP);
            return parseColorInt(new RGB(((int) R), ((int) G), ((int) B)));
        }
    }

    public static class HSVAColor {
        public float[] hsv;
        public float alpha;

        public HSVAColor() {
            hsv = new float[3];
        }

        public float getH() {
            return hsv[0];
        }

        public float getS() {
            return hsv[1];
        }

        public float getV() {
            return hsv[2];
        }

        public float getAlpha() {
            return alpha;
        }
    }
}

