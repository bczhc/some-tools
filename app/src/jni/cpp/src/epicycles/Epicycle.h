//
// Created by root on 2020/4/2.
//

#ifndef CPP_EPICYCLE_H
#define CPP_EPICYCLE_H

#include "./ComplexValue.h"

class Epicycle {
public:
    double n{};
    ComplexValue c{};

    Epicycle();

    Epicycle(double n, const ComplexValue &c);
};

#endif //CPP_EPICYCLE_H
