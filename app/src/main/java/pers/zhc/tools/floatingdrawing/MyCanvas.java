package pers.zhc.tools.floatingdrawing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import androidx.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * @author bczhc
 */
public class MyCanvas extends Canvas {
    private float scale = 1F;
    private float startPointX = 0F, startPointY = 0F;
    private float savedScale = 1F, savedStartPointX = 0F, savedStartPointY = 0F;

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

    public void translateReal(float canvasTransX, float canvasTransY) {
        translate(canvasTransX / this.scale, canvasTransY / this.scale);
    }

    public void scaleReal(float canvasScale, float canvasPX, float canvasPY) {
        scale(canvasScale, (canvasPX - startPointX) / this.scale, (canvasPY - startPointY) / this.scale);
    }

    public void reset() {
        transTo(0F, 0F, 1F);
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
        translateReal(toStartPointX - startPointX, toStartPointY - startPointY);
    }

    @Override
    public String toString() {
        return "MyCanvas{" +
                "scale=" + scale +
                ", startPointX=" + startPointX +
                ", startPointY=" + startPointY +
                '}';
    }

    @NotNull
    public State getStatus() {
        return new State(startPointX, startPointY, scale);
    }

    public void transTo(@NotNull State state) {
        transTo(state.startPointX, state.startPointY, state.scale);
    }

    public static class State {
        public float startPointX;
        public float startPointY;
        public float scale;

        public State(float startPointX, float startPointY, float scale) {
            this.startPointX = startPointX;
            this.startPointY = startPointY;
            this.scale = scale;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyCanvas myCanvas = (MyCanvas) o;

        if (Float.compare(myCanvas.scale, scale) != 0) return false;
        if (Float.compare(myCanvas.startPointX, startPointX) != 0) return false;
        if (Float.compare(myCanvas.startPointY, startPointY) != 0) return false;
        if (Float.compare(myCanvas.savedScale, savedScale) != 0) return false;
        if (Float.compare(myCanvas.savedStartPointX, savedStartPointX) != 0) return false;
        return Float.compare(myCanvas.savedStartPointY, savedStartPointY) == 0;
    }

    @Override
    public int hashCode() {
        int result = (scale != +0.0f ? Float.floatToIntBits(scale) : 0);
        result = 31 * result + (startPointX != +0.0f ? Float.floatToIntBits(startPointX) : 0);
        result = 31 * result + (startPointY != +0.0f ? Float.floatToIntBits(startPointY) : 0);
        result = 31 * result + (savedScale != +0.0f ? Float.floatToIntBits(savedScale) : 0);
        result = 31 * result + (savedStartPointX != +0.0f ? Float.floatToIntBits(savedStartPointX) : 0);
        result = 31 * result + (savedStartPointY != +0.0f ? Float.floatToIntBits(savedStartPointY) : 0);
        return result;
    }
}