package pers.zhc.tools.epicycles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.FloatRange;
import pers.zhc.u.math.util.ComplexFunctionInterface;
import pers.zhc.u.math.util.ComplexValue;

import java.util.ArrayList;
import java.util.List;

class ComplexFunction {
    private List<ComplexValue> complexValueList;
    private float w = 1361, h = 636;

    ComplexFunction() {
        complexValueList = new ArrayList<>();
    }

    void put(double re, double im) {
        int size = complexValueList.size();
        if (size > 0) {
            ComplexValue lastCV = complexValueList.get(size - 1);
            if (lastCV.re == re && lastCV.im == im) return;
        }
        complexValueList.add(new ComplexValue(re, im));
    }

    void clear() {
        this.complexValueList.clear();
    }

    @SuppressWarnings("SameParameterValue")
    ComplexFunctionInterface getFunction(double t_start, double t_end) {
        int size = this.complexValueList.size();
        double s = t_end - t_start;
        double[] moduli = new double[size - 1];
        double moduliSum = 0D;
        final double[] z = {0D};
        for (int i = 0; i < this.complexValueList.size() - 1; i++) {
            ComplexValue complexValue = this.complexValueList.get(i);
            ComplexValue nextComplexValue = this.complexValueList.get(i + 1);
            moduliSum += (moduli[i] = getModulus(complexValue.re, complexValue.im, nextComplexValue.re, nextComplexValue.im));
        }
        double finalModuliSum = moduliSum;
        Bitmap bitmap = Bitmap.createBitmap(((int) w), ((int) h), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);
        return v -> {
            z[0] = 0D;
            double currModulusLen = finalModuliSum * (v - t_start) / (s - t_start);
            for (int i = 0; i < moduli.length; i++) {
                z[0] += moduli[i];
                if (z[0] >= currModulusLen) {
                    ComplexValue r = aPointLinearToBPoint(complexValueList.get(i), complexValueList.get(i + 1)
                            , (currModulusLen - z[0] + moduli[i]) / moduli[i]);
                    canvas.drawPoint(((float) (r.re + w / 2F)), ((float) (-r.im + h / 2F)), paint);
                    bitmap.getHeight();
                    return r.selfDivide(new ComplexValue(50, 0));
                }
            }
            return new ComplexValue(0, 0);
        };
//        ComplexValue cv = new ComplexValue(0, 0);
        /*return t -> {
            double i = (t - t_start) / s * (size - 1);
            if (i < size) {
                ComplexValue complexValue = this.complexValueList.get(((int) i));
                try {
                    return complexValue.add(this.complexValueList.get(((int) i) + 1).selfMultiply(cv.setValue(i - ((int) i), 0)));
                } catch (Exception ignored) {
                }
                return complexValue.add(this.complexValueList.get(0).selfMultiply(cv.setValue(i - ((int) i), 0)));
            }
            return new ComplexValue(0, 0);
        };*/
        /*return v -> {
            if (v < s / 3 * 2) return new ComplexValue(2 * Math.cos(v), 3 * Math.cos(v) + 1);
            return new ComplexValue(20, 15);
        };*/
//        return v -> {
//            if (v < s / 2) return new ComplexValue(10, 0);
//            return new ComplexValue(-10, 5);
//            return new ComplexValue(10, 0);
//            return new ComplexValue(v / 1000, v / 1000);
//            return new ComplexValue(10, 5);
//            return new ComplexValue(10 * Math.cos(2 * v * EpicyclesView.omega), 10 * Math.sin(3 * v * EpicyclesView.omega - 10));
//        };
    }

    private double getModulus(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - y1, 2) + Math.pow(x2 - y2, 2));
    }

    private ComplexValue aPointLinearToBPoint(ComplexValue cv1, ComplexValue cv2, @FloatRange(from = 0D, to = 1D) double progress) {
        double reS = cv2.re - cv1.re;
        double imS = cv2.im - cv1.im;
        return cv1.add(new ComplexValue(reS * progress, imS * progress));//todo alloc频繁
    }
}