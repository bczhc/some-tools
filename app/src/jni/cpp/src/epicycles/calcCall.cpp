//
// Created by root on 2020/4/3.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_FourierSeriesCalc.h"
#include "../../third_party/my-cpp-lib/FourierSeries.h"
#include "../jni_help.h"

class CallBean {
public:
    JNIEnv *&mEnv;
    jobject &mCall;
    jmethodID &mF_Mid;
    jmethodID &mCallback_Mid;
    jmethodID &mGetFunctionResultCallMid;

    CallBean(JNIEnv *&mEnv, jobject &mCall, jmethodID &mFMid, jmethodID &mCallbackMid,
             jmethodID &getFunctionResultCallMid) : mEnv(mEnv), mCall(mCall), mF_Mid(mFMid),
                                                    mCallback_Mid(mCallbackMid),
                                                    mGetFunctionResultCallMid(getFunctionResultCallMid) {}
};

inline void getResult(CallBean callBean, double dest[], double t) {
//    callBean.mEnv->GetDoubleArrayElements()
//    callBean.mEnv->CallVoidMethod(callBean.mCall, callBean.mGetFunctionResultCallMid, dest,(jdouble) t);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FourierSeriesCalc_calc
        (JNIEnv *env, jclass instance, jdouble period, jint epicyclesCount, jobject call, jint threadNum) {
    double funcData[threadNum * 2];
    jclass fClass = env->GetObjectClass(call);
    jmethodID fMid = env->GetMethodID(fClass, "getFunctionResult", "(D)V");
    jmethodID callbackMid = env->GetMethodID(fClass, "callback", "(DDD)V");
    jmethodID getFunctionResultCallMid = env->GetMethodID(fClass, "getFunctionResult", "([DD)V");
    CallBean callBean(env, call, fMid, callbackMid, getFunctionResultCallMid);
    class Func : public ComplexFunctionInterface {
    private:
        CallBean mCallBean;
    public:
        void x(ComplexValue &dest, double t) override {
            mCallBean.mEnv->CallVoidMethod(mCallBean.mCall, mCallBean.mF_Mid, (jdouble) t);
//            dest.setValue(function_re, function_im);
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
    fs.calc(cb, 10000, 1);
    env->DeleteLocalRef(fClass);
}