package pers.zhc.tools.utils;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;

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

    public void getTransformedPoint(PointF dest, float x, float y) {
        matrix.getValues(tmpValue);
        dest.x = tmpValue[MSCALE_X] * x + tmpValue[MSKEW_X] * y + tmpValue[MTRANS_X];
        dest.y = tmpValue[MSKEW_Y] * x + tmpValue[MSCALE_Y] * y + tmpValue[MTRANS_Y];
    }

    private final Matrix inverse = new Matrix();

    public void getInvertedTransformedPoint(PointF dest, float x, float y) {
        matrix.invert(inverse);
        inverse.getValues(inverseTmpValue);
        dest.x = inverseTmpValue[MSCALE_X] * x + inverseTmpValue[MSKEW_X] * y + inverseTmpValue[MTRANS_X];
        dest.y = inverseTmpValue[MSKEW_Y] * x + inverseTmpValue[MSCALE_Y] * y + inverseTmpValue[MTRANS_Y];
    }
}
