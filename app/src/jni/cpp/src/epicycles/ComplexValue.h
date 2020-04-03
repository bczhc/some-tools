//
// Created by root on 2020/4/2.
//

#ifndef CPP_COMPLEXVALUE_H
#define CPP_COMPLEXVALUE_H

#include <cmath>
#include <cstdint>
#include <iostream>

using namespace std;

class ComplexValue {
public:
    double re{}, im{};

    ComplexValue(double re, double im);

    ComplexValue() = default;

    double getComplexModule();

    ComplexValue add(ComplexValue &cv);

    ComplexValue add(double _re, double _im);

    ComplexValue &selfAdd(ComplexValue &cv);

    ComplexValue &selfAdd(double _re, double _im);

    ComplexValue subtract(ComplexValue cv);

    ComplexValue subtract(double _re, double _im);

    ComplexValue &selfSubtract(ComplexValue cv);

    ComplexValue &selfSubtract(double _re, double _im);

    ComplexValue multiply(ComplexValue cv);

    ComplexValue multiply(double _re, double _im);

    ComplexValue &selfMultiply(ComplexValue cv);

    ComplexValue &selfMultiply(double _re, double _im);

    ComplexValue divide(ComplexValue cv);

    ComplexValue divide(double _re, double _im);

    ComplexValue &selfDivide(ComplexValue cv);

    ComplexValue &selfDivide(double _re, double _im);

    ComplexValue &setValue(ComplexValue cv);

    ComplexValue &setValue(double _re, double _im);

    string toString();

    static string toString(ComplexValue cv);

    static string toString(double _re, double _im);
};

typedef void (*OneVariableComplexFunction_t)(ComplexValue &, double);

class ComplexFunctionInterface {
public:
    virtual void x(ComplexValue &dest, double t) = 0;
};

#endif //CPP_COMPLEXVALUE_H
