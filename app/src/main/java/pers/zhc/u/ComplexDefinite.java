package pers.zhc.u;

import pers.zhc.u.math.util.ComplexFunctionInterface;
import pers.zhc.u.math.util.ComplexValue;

public class ComplexDefinite {
    public int n = 100000;

    public static void main(String[] args) {
        ComplexDefinite complexDefinite = new ComplexDefinite();
        ComplexValue complexValue = new ComplexValue(0, 0);
        ComplexValue definiteIntegralByTrapezium = complexDefinite.getDefiniteIntegralByTrapezium(0, 2 * Math.PI, t -> complexValue.setValue(Math.cos(t), Math.sin(t)));
        System.out.println(definiteIntegralByTrapezium.toString());
    }

    //梯形法求定积分
    public ComplexValue getDefiniteIntegralByTrapezium(double x0, double xn, ComplexFunctionInterface complexFunctionInterface) {
        double d = (xn - x0) / n;
        ComplexValue sum = new ComplexValue(0, 0);
        ComplexValue cv1 = new ComplexValue(0, 0);
        ComplexValue cv2 = new ComplexValue(2, 0);
        ComplexValue dComplex = new ComplexValue(d, 0);
        for (double i = x0; i <= xn; i += d) {
            sum.selfAdd(cv1.setValue(complexFunctionInterface.x(i)).add(complexFunctionInterface.x(i + d)).multiply(dComplex).divide(cv2));
        }
        return sum;
    }

    public ComplexValue getDefiniteIntegralByRectangle2(double x0, double xn, ComplexFunctionInterface complexFunctionInterface) {
        double d = (xn - x0) / n;
        ComplexValue sum = new ComplexValue(0, 0);
        ComplexValue dComplex = new ComplexValue(d, 0);
        for (double i = x0; i <= xn; i += d) {
            sum.selfAdd(complexFunctionInterface.x(i).multiply(dComplex));
        }
        return sum;
    }
}
