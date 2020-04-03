//
// Created by root on 2020/4/3.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_FourierSeriesCalc.h"
#include "./ComplexValue.h"
#include "./FourierSeries.h"
#include "../jni_help.h"

static double function_re, function_im;

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FourierSeriesCalc_nSetFunctionResult
        (JNIEnv *env, jclass instance, jdouble re, jdouble im) {
    function_re = re;
    function_im = im;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FourierSeriesCalc_calc
        (JNIEnv *env, jclass instance, jdouble period, jint epicyclesCount, jobject call) {
    class CallBean {
    public:
        JNIEnv *&mEnv;
        jobject &mCall;
        jmethodID &mF_Mid;
        jmethodID &mCallback_Mid;

        CallBean(JNIEnv *&mEnv, jobject &mCall, jmethodID &mFMid, jmethodID &mCallbackMid) : mEnv(mEnv), mCall(mCall),
                                                                                             mF_Mid(mFMid),
                                                                                             mCallback_Mid(
                                                                                                     mCallbackMid) {}
    };
    jclass fClass = env->GetObjectClass(call);
    jmethodID fMid = env->GetMethodID(fClass, "getFunctionResult", "(D)V");
    jmethodID callbackMid = env->GetMethodID(fClass, "callback", "(DDD)V");
    CallBean callBean(env, call, fMid, callbackMid);
    class Func : public ComplexFunctionInterface {
    private:
        CallBean mCallBean;
    public:
        void x(ComplexValue &dest, double t) override {
            mCallBean.mEnv->CallVoidMethod(mCallBean.mCall, mCallBean.mF_Mid, (jdouble) t);
            dest.setValue(function_re, function_im);
        }

        explicit Func(CallBean &mCallBean) : mCallBean(mCallBean) {}
    } func(callBean);
    class Callback : public FourierSeriesCallback {
    private:
        CallBean mCallBean;
    public:
        explicit Callback(const CallBean &mCallBean) : mCallBean(mCallBean) {}

    public:
        void callback(double n, double re, double im) override {
            string s = ComplexValue::toString(re, im);
            unsigned long length = s.length();
            char a[length + 1]; //size
            a[length] = 0;
            s.copy(a, length, 0);
            mCallBean.mEnv->CallVoidMethod(mCallBean.mCall, mCallBean.mCallback_Mid, (jdouble) n, (jdouble) re,
                                           (jdouble) im);
            Log(mCallBean.mEnv, "FourierSeries", a);
        }
    } cb(callBean);
    FourierSeries fs(func, epicyclesCount, period);
    fs.calc(cb, 10000);
    env->DeleteLocalRef(fClass);
}