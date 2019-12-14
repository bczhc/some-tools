package pers.zhc.tools.epicycles;

import android.support.annotation.FloatRange;
import pers.zhc.u.math.util.ComplexFunctionInterface;
import pers.zhc.u.math.util.ComplexValue;

import java.util.ArrayList;
import java.util.List;

class ComplexFunction {
    private List<ComplexValue> complexValueList;

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
        for (int i = 0; i < this.complexValueList.size() - 1; i++) {
            ComplexValue complexValue = this.complexValueList.get(i);
            ComplexValue nextComplexValue;
            nextComplexValue = this.complexValueList.get(i + 1);
            moduliSum += (moduli[i] = getModulus(complexValue.re, complexValue.im, nextComplexValue.re, nextComplexValue.im));
        }
        double finalModuliSum = moduliSum;
        System.gc();
        return v -> {
            double z = 0;
            double currModulusLen = finalModuliSum * (v - t_start) / (s - t_start);
            for (int i = 0; i < moduli.length; i++) {
                z += moduli[i];
                if (z >= currModulusLen) {
                    ComplexValue r;
                    r = aPointLinearToBPoint(complexValueList.get(i), complexValueList.get(i + 1)
                            , (currModulusLen - z + moduli[i]) / moduli[i]);
                    return r.selfDivide(50, 0);
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
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /*private double getModulus(ComplexValue cv1, ComplexValue cv2) {
        return Math.sqrt(Math.pow(cv1.re - cv2.re, 2) + Math.pow(cv1.im - cv2.im, 2));
    }*/

    private ComplexValue aPointLinearToBPoint(ComplexValue cv1, ComplexValue cv2, @FloatRange(from = 0D, to = 1D) double progress) {
        double reS = cv2.re - cv1.re;
        double imS = cv2.im - cv1.im;
        return cv1.add(reS * progress, imS * progress);
    }
}