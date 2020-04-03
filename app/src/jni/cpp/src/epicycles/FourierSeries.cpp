//
// Created by root on 2020/3/31.
//

#include "./FourierSeries.h"

FourierSeries::FourierSeries(ComplexFunctionInterface &functionInterface, int32_t _epicyclesCount, int32_t period)
        : f(functionInterface), T(period), epicyclesCount(_epicyclesCount) {
    omega = M_PI * 2 / period;
}

/*void FourierSeries::calc(ArrayList<Epicycle> &list) {

}*/

void FourierSeries::calc(FourierSeriesCallback &callback, int integralD) {
    int32_t a = -epicyclesCount / 2;
    int32_t t = a + epicyclesCount;
    class FuncInIntegral : public ComplexFunctionInterface {
    private:
        ComplexFunctionInterface &mF;
        double &mOmega;
    public:
        double n{};

        FuncInIntegral(ComplexFunctionInterface &mF, double &mOmega) : mF(mF), mOmega(mOmega) {}

    private:
        void x(ComplexValue &dest, double t) override {
            mF.x(dest, t);
            dest.selfMultiply(cos(-n * t * mOmega), sin(-n * t * mOmega));
        }
    } funcInIntegral(f, omega);
    ComplexIntegral complexIntegral{.n = integralD};
    for (int32_t n = a; n < t; ++n) {
        funcInIntegral.n = n;
        ComplexValue integralResult = complexIntegral.getDefiniteIntegralByTrapezium(0, T, funcInIntegral);
        integralResult.selfDivide(T, 0);
        callback.callback(n, integralResult.re, integralResult.im);
    }
}