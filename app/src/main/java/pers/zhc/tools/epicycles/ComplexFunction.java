package pers.zhc.tools.epicycles;

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
        /*double[] moduli = new double[size - 1];
        double moduliSum = 0D, z = 0D;
        for (int i = 0; i < this.complexValueList.size() - 1; i++) {
            ComplexValue complexValue = this.complexValueList.get(i);
            ComplexValue nextComplexValue = this.complexValueList.get(i + 1);
            moduliSum += (moduli[i] = getModulus(complexValue.re, complexValue.im, nextComplexValue.re, nextComplexValue.im));
        }
        double[] scale = new double[size - 1];
        for (int i = 0; i < moduli.length; i++) {
            z += moduli[i];
            scale[i] = z / moduliSum;
        }
        return v -> {
            for (int i = 0; i < scale.length; i++) {
                if (scale[i] * s)
            }
        };*/
        ComplexValue cv = new ComplexValue(0, 0);
        return t -> {
            double i = (t - t_start) / s * (size - 1);
            if (i < size) {
                ComplexValue complexValue = this.complexValueList.get(((int) i));
                try {
                    return complexValue.selfAdd(this.complexValueList.get(((int) i) + 1).selfMultiply(cv.setValue(i - ((int) i), 0)));
                } catch (Exception ignored) {
                }
                return complexValue.selfAdd(this.complexValueList.get(0).selfMultiply(cv.setValue(i - ((int) i), 0)));
            }
            return new ComplexValue(0, 0);
        };
        /*return v -> {
            if (v < s / 2) return new ComplexValue(20, 0);
            return new ComplexValue(-10, -5);
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

    /*private double getModulus(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - y1, 2) + Math.pow(x2 - y2, 2));
    }*/
}