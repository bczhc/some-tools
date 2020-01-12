package pers.zhc.tools.utils;

import android.graphics.Color;

@SuppressWarnings({"unused"})
public class ColorUtils {
    private int[] colors;
    private float startPos, endPos;

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
        if (s.length() == 1) return "0" + s;
        return s;
    }

    public static String getHexString(int color) {
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);
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
        private RGB rgb1, rgb2;
        private float sP, eP;

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
}

