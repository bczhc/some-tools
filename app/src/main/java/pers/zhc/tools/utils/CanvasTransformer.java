package pers.zhc.tools.utils;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import org.jetbrains.annotations.NotNull;

import static android.graphics.Matrix.*;

/**
 * @author bczhc
 */
public class CanvasTransformer {
    private final Canvas canvas;
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private final float[] tmpValue = new float[9];
    private final float[] inverseTmpValue = new float[9];
    private final float[] realScaleTmpValue = new float[9];
    private final Matrix identityMatrix = new Matrix();

    public CanvasTransformer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void absTranslate(float dx, float dy) {
        matrix.postTranslate(dx, dy);
        canvas.setMatrix(matrix);
    }

    public void absScale(float dScale, float px, float py) {
        matrix.postScale(dScale, dScale, px, py);
        canvas.setMatrix(matrix);
    }

    public void absRotate(float degrees, float px, float py) {
        matrix.postRotate(degrees, px, py);
        canvas.setMatrix(matrix);
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public Matrix getNewMatrix() {
        return new Matrix(matrix);
    }

    /**
     * deep copy
     * @param matrix matrix
     */
    public void setMatrix(Matrix matrix) {
        this.matrix.set(matrix);
        canvas.setMatrix(this.matrix);
    }

    public void save() {
        savedMatrix.set(this.matrix);
    }

    public void restore() {
        this.matrix.set(savedMatrix);
        canvas.setMatrix(this.matrix);
    }

    public void getTransformedPoint(@NotNull PointF dest, float x, float y) {
        matrix.getValues(tmpValue);
        dest.x = tmpValue[MSCALE_X] * x + tmpValue[MSKEW_X] * y + tmpValue[MTRANS_X];
        dest.y = tmpValue[MSKEW_Y] * x + tmpValue[MSCALE_Y] * y + tmpValue[MTRANS_Y];
    }

    private final Matrix inverse = new Matrix();

    public void getInvertedTransformedPoint(@NotNull PointF dest, float x, float y) {
        matrix.invert(inverse);
        inverse.getValues(inverseTmpValue);
        getTransformedPoint(dest, inverseTmpValue, x, y);
    }

    public float getRealScale() {
        matrix.getValues(realScaleTmpValue);
        float scaleX = realScaleTmpValue[MSCALE_X];
        float skewY = realScaleTmpValue[MSKEW_Y];
        return (float) Math.sqrt(scaleX * scaleX + skewY * skewY);
    }

    public static void getTransformedPoint(@NotNull PointF dest, float @NotNull [] matrixValues, float x, float y) {
        dest.x = matrixValues[MSCALE_X] * x + matrixValues[MSKEW_X] * y + matrixValues[MTRANS_X];
        dest.y = matrixValues[MSKEW_Y] * x + matrixValues[MSCALE_Y] * y + matrixValues[MTRANS_Y];
    }

    public static float getRealScale(@NotNull Matrix matrix) {
        float[] v = new float[9];
        matrix.getValues(v);
        float scaleX = v[MSCALE_X];
        float skewY = v[MSKEW_Y];
        return (float) Math.sqrt(scaleX * scaleX + skewY * skewY);
    }

    public void reset() {
        setMatrix(identityMatrix);
    }

    /**
     * Refresh the transformation. When the canvas' transformation changed, use this method to re-apply the held inner transformation to the canvas.
     */
    public void refresh() {
        canvas.setMatrix(this.matrix);
    }
}
