package com.zhc.tools.floatingboard;

class GradientUtil {
    private int[] colors;
    private float startPos, endPos;

    GradientUtil(int[] colors, float startPosition, float endPosition) {
        this.colors = colors;
        this.startPos = startPosition;
        this.endPos = endPosition;
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

    private class TwoColor {
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

    static RGB parseRGB(int color) {
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);
        return new RGB(red, green, blue);
    }

    private static int parseColorInt(RGB rgb) {
        return 0xff000000 | (rgb.r << 16) | (rgb.g << 8) | rgb.b;
    }
}

class RGB {
    int r, g, b;

    RGB(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}