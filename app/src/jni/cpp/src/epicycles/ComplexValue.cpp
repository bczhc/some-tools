//
// Created by root on 2020/4/2.
//

#include "ComplexValue.h"

double ComplexValue::getComplexModule() {
    return sqrt(pow(this->re, 2) + pow(this->im, 2));
}

ComplexValue ComplexValue::add(ComplexValue &cv) {
    ComplexValue r(this->re + cv.re, this->im + cv.im);
    return r;
}

ComplexValue::ComplexValue(double re, double im) : re(re), im(im) {}

ComplexValue ComplexValue::add(double _re, double _im) {
    ComplexValue r(this->re + _re, this->im + _im);
    return r;
}

ComplexValue &ComplexValue::selfAdd(ComplexValue &cv) {
    this->re += cv.re;
    this->im += cv.im;
    return *this;
}

ComplexValue &ComplexValue::selfAdd(double _re, double _im) {
    this->re += _re;
    this->im += _im;
    return *this;
}

ComplexValue ComplexValue::subtract(double _re, double _im) {
    ComplexValue r(this->re - _re, this->im - _im);
    return r;
}

ComplexValue &ComplexValue::selfSubtract(ComplexValue cv) {
    this->re -= cv.re;
    this->im -= cv.im;
    return *this;
}

ComplexValue &ComplexValue::selfSubtract(double _re, double _im) {
    this->re -= _re;
    this->im -= _im;
    return *this;
}

ComplexValue ComplexValue::multiply(ComplexValue cv) {
    ComplexValue r(this->re * cv.re - this->im * cv.im, this->re * cv.im + cv.re * this->im);
    return r;
}

ComplexValue ComplexValue::multiply(double _re, double _im) {
    ComplexValue r(this->re * _re - this->im * _im, this->re * _im + _re * this->im);
    return r;
}

ComplexValue &ComplexValue::selfMultiply(ComplexValue cv) {
    double re1 = this->re * cv.re - this->im * cv.im;
    double im1 = this->re * cv.im + cv.re * this->im;
    this->re = re1;
    this->im = im1;
    return *this;
}

ComplexValue &ComplexValue::selfMultiply(double _re, double _im) {
    double re1 = this->re * _re - this->im * _im;
    double im1 = this->re * _im + _re * this->im;
    this->re = re1;
    this->im = im1;
    return *this;
}

ComplexValue ComplexValue::divide(ComplexValue cv) {
    double a = pow(cv.re, 2) + pow(cv.im, 2);
    ComplexValue r((this->re * cv.re + this->im * cv.im) / a, (cv.re * this->im - this->re * cv.im) / a);
    return r;
}

ComplexValue ComplexValue::divide(double _re, double _im) {
    double a = pow(this->re, 2) + pow(this->im, 2);
    ComplexValue r((this->re * _re + this->im * _im) / a, (_re * this->im - this->re * _im) / a);
    return r;
}

ComplexValue &ComplexValue::selfDivide(ComplexValue cv) {
    double a = pow(cv.re, 2) + pow(cv.im, 2);
    double re1 = (this->re * cv.re + this->im * cv.im) / a;
    double im1 = (cv.re * this->im - this->re * cv.im) / a;
    this->re = re1;
    this->im = im1;
    return *this;
}

ComplexValue &ComplexValue::selfDivide(double _re, double _im) {
    double a = pow(_re, 2) + pow(_im, 2);
    double re1 = (this->re * _re + this->im * _im) / a;
    double im1 = (_re * this->im - this->re * _im) / a;
    this->re = re1;
    this->im = im1;
    return *this;
}

ComplexValue &ComplexValue::setValue(ComplexValue cv) {
    this->re = cv.re;
    this->im = cv.im;
    return *this;
}

ComplexValue &ComplexValue::setValue(double _re, double _im) {
    this->re = _re;
    this->im = _im;
    return *this;
}

ComplexValue ComplexValue::subtract(ComplexValue cv) {
    ComplexValue r(this->re - cv.re, im - cv.im);
    return r;
}

string ComplexValue::toString() {
    return ComplexValue::toString(this->re, this->im);
}

string ComplexValue::toString(ComplexValue cv) {
    return ComplexValue::toString(cv.re, cv.im);
}

string ComplexValue::toString(double _re, double _im) {
    if (_re == 0 && _im == 0) {
        string s = "0";
        return s;
    } else if (_re ==0) {
        string s = to_string(_re);
        s.append("i");
        return s;
    } else if (_im == 0) {
        string s = to_string(_re);
        return s;
    } else if (_im > 0) {
        string s = to_string(_re);
        s.append("+")
                .append(to_string(_im))
                .append("i");
        return s;
    } else if (_im < 0) {
        string s = to_string(_re);
        s.append(to_string(_im))
                .append("i");
        return s;
    }
    string s = "0";
    return s;
}
