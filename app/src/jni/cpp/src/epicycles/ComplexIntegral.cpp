//
// Created by root on 2020/4/2.
//

#include "ComplexIntegral.h"

ComplexValue ComplexIntegral::getDefiniteIntegralByTrapezium(double x0, double xn,
                                                             ComplexFunctionInterface &f) {
    double d = (xn - x0) / n;
    ComplexValue sum(0, 0), left(0, 0), right(0, 0);
    for (double t = 0; t <= xn; t += d) { // NOLINT(cert-flp30-c)
        f.x(left, t), f.x(right, t + d);
        sum.selfAdd(left.selfAdd(right).selfMultiply(d, 0).selfDivide(2, 0));
    }
    return sum;
}
