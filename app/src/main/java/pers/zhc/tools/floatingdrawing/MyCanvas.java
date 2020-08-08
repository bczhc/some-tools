package pers.zhc.tools.floatingdrawing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author bczhc
 */
public class MyCanvas extends Canvas {
    private float scale = 1F;
    private float startPointX, startPointY;
    private float savedScale, savedStartPointX, savedStartPointY;
    public MyCanvas(@NonNull Bitmap bitmap) {
        super(bitmap);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
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
        this.scale(1 / scale);
        this.translate(-startPointX, -startPointY);
    }

    @Override
    public int save() {
        savedStartPointX = startPointX;
        savedStartPointY = startPointY;
        savedScale = scale;
        return 0;
    }

    @Override
    public void restore() {
        transTo(savedStartPointX, savedStartPointY, savedScale);
    }

    public void transTo(float toStartPointX, float toStartPointY, float toScale) {
        scale(toScale / scale);
        translate(toStartPointX - startPointX, toStartPointY - startPointY);
    }
}