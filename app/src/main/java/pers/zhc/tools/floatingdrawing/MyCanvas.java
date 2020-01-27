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
        this.startPointX += dx * scale;
        this.startPointY += dy * scale;
    }

    public void scale(float scale) {
        super.scale(scale, scale);
        this.scale *= scale;
    }

    public void scale(float scale, float px, float py) {
        this.translate(px, py);
        this.scale(scale);
        this.translate(-px, -py);
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

    public void invertTranslate(float canvasTransX, float canvasTransY) {
        translate(canvasTransX / this.scale, canvasTransY / this.scale);
    }

    public void invertScale(float canvasScale, float canvasPX, float canvasPY) {
        scale(canvasScale, (canvasPX - startPointX) / this.scale, (canvasPY - startPointY) / this.scale);
    }

    public void reset() {
        this.startPointX = 0;
        this.startPointY = 0;
        this.scale = 1F;
    }
}