//
// Created by root on 2020/3/31.
//

#include "./ComplexValue.h"
#include "ComplexIntegral.h"
#include <iostream>
#include <thread>
#include "./Epicycle.h"
#include "../zhc.h"

using namespace std;

class FourierSeriesCallback {
public:
    virtual void callback(double n, double re, double im) = 0;
};

class FourierSeries {
private:
    double omega{};
    double T{};
    ComplexFunctionInterface &f;
    int32_t epicyclesCount{};
public:
    FourierSeries(ComplexFunctionInterface &functionInterface, int32_t _epicyclesCount, int32_t period);

    void calc(ArrayList<Epicycle> &list, int integralD);

    void calc(FourierSeriesCallback &callback, int integralD);
};

void tF();