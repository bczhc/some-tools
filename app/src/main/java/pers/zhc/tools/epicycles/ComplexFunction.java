package pers.zhc.tools.epicycles;

import androidx.annotation.FloatRange;
import pers.zhc.u.math.util.ComplexValue;

import java.util.ArrayList;
import java.util.List;

class ComplexFunction {
    private final List<ComplexValue> complexValueList;

    ComplexFunction() {
        complexValueList = new ArrayList<>();
    }

    void put(double re, double im) {
        int size = complexValueList.size();
        if (size > 0) {
            ComplexValue lastCV = complexValueList.get(size - 1);
            if (lastCV.re == re && lastCV.im == im) {
                return;
            }
        }
        complexValueList.add(new ComplexValue(re, im));
    }

    ComplexValue get(int index) {
        return this.complexValueList.get(index);
    }

    int length() {
        return this.complexValueList.size();
    }

    void clear() {
        this.complexValueList.clear();
    }

    @SuppressWarnings("SameParameterValue")
    ComplexFunctionInterface2 getFunction(double startTime, double endTime) {
        final int size = complexValueList.size();
        final double time = endTime - startTime;
        double moduleSum = 0L;
        double[] modules = new double[size];
        for (int i = 0; i < size; i++) {
            ComplexValue complexValue = complexValueList.get(i);
            ComplexValue next = complexValueList.get(i == size - 1 ? 0 : (i + 1));
            final double module = getModulus(complexValue.re, next.re, complexValue.im, next.im);
            moduleSum += module;
            modules[i] = module;
        }
        double[] linesTime = new double[size];
        final double base = time / moduleSum;
        for (int i = 0; i < modules.length; i++) {
            linesTime[i] = base * modules[i];
        }
        return (dest, t) -> {
            /*final double relativeTime = (t - startTime) % time;
            double lineTimeSum = 0;
            int c = 0;
            for (int i = 0; i < linesTime.length; i++) {
                ComplexValue a = complexValueList.get(i);
                final int nextIndex = i == size - 1 ? 0 : (i + 1);
                ComplexValue b = complexValueList.get(nextIndex);
                lineTimeSum += linesTime[i];
                if (relativeTime >= lineTimeSum) {
                    aPointLinearToBPoint(dest, a, b, (relativeTime - lineTimeSum) / linesTime[nextIndex]);
                }
            }*/
            dest.setValue(10, 10);
        };
    }

    private double getModulus(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private void aPointLinearToBPoint(ComplexValue dest, ComplexValue cv1, ComplexValue cv2, @FloatRange(from = 0D, to = 1D) double progress) {
        double reS = cv2.re - cv1.re;
        double imS = cv2.im - cv1.im;
        dest.setValue(cv1);
        dest.selfAdd(reS * progress, imS * progress);
    }
}