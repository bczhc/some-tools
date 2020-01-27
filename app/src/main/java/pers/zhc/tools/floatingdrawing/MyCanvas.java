package pers.zhc.tools.floatingdrawing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;

public class MyCanvas extends Canvas {
    private float scale = 1F;
    private float startPointX, startPointY;

    public MyCanvas(@NonNull Bitmap bitmap) {
        super(bitmap);
    }

    @Override
    public void translate(float dx, float dy) {
        super.translate(dx, dy);
    }

    public void scale(float scale) {
        super.scale(scale, scale);
        this.scale *= scale;
    }

    public void scale(float scale, float px, float py) {
        super.scale(scale, scale, px, py);
        this.scale *= scale;
    }

    public float getScale() {
        return scale;
    }

    public float getStartPointX() {
        return startPointX;
    }

    public float getStartPointY() {
        return startPointY;
    }
}