#include <jni.h>
#include "third_party/my-cpp-lib/math/fourier_series.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_FourierSeries.h"
#include <third_party/jni-lib/src/jni_help.h>
#include "third_party/my-cpp-lib/array_list.hpp"

using namespace bczhc;

class Point {
public:
    float x, y;

    Point() = default;

    Point(float x, float y) : x(x), y(y) {}

    bool operator==(const Point &rhs) const {
        return x == rhs.x &&
               y == rhs.y;
    }

    bool operator!=(const Point &rhs) const {
        return !(rhs == *this);
    }
};

class F : public ComplexFunctionInterface {
private:
    ArrayList<Point> &list;
    double period, scale{};
    double pathsTotalLength{};
    double *sumLength = nullptr;
    int listLength;

    static double getPathLength(Point &p1, Point &p2) {
        return sqrt(pow(p1.x - p2.x, 2) + pow(p1.y - p2.y, 2));
    }

    static int search(const double *arr, int length, double target) {
        if (target < arr[0]) return 0;
        for (int i = 0; i < length - 1; ++i) {
            if (target > arr[i] && target < arr[i + 1]) return i + 1;
        }
        if (target > arr[length - 1]) return length - 1;
        return -1;
    }

    static Point linearMoveBetweenTwoPoints(Point p1, Point p2, double progress) {
        double s = progress / getPathLength(p1, p2);
        Point p((float) (p1.x + (p2.x - p1.x) * s),
                (float) (p1.y + (p2.y - p1.y) * s));
        return p;
    }

public:

    explicit F(ArrayList<Point> &list, double period) : list(list), period(period) {
        listLength = list.length();

        sumLength = new double[listLength];
        for (int i = 0; i < listLength; ++i) {
            Point next = i == listLength - 1 ? list.get(0) : list.get(i + 1);
            Point currPoint = list.get(i);
            pathsTotalLength += getPathLength(currPoint, next);
            sumLength[i] = pathsTotalLength;
        }
    }

    ~F() {
        delete[] sumLength;
    }

    void x(ComplexValue &dest, double t) override {
        t = t - (t >= period ? floor(t / period) * period : 0);
        double mapToLength = t * pathsTotalLength / period;
        int index = search(sumLength, listLength, mapToLength);
        Point r = linearMoveBetweenTwoPoints(
                list.get(index),
                index == listLength - 1 ? list.get(0) : list.get(index + 1),
                index == 0 ? mapToLength : (mapToLength - sumLength[index - 1]));
        dest.re = r.x, dest.im = r.y;
    }
};


static jobject globalCallback;
static JavaVM *globalJvm;

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FourierSeries_calc
        (JNIEnv *env, jclass cls, jobject points, jdouble period, jint epicyclesCount,
         jobject callback,
         jint threadNum, jint integralN) {
    jclass listCLass = env->GetObjectClass(points);
    jmethodID sizeMId = env->GetMethodID(listCLass, "size", "()I");
    int length = (int) env->CallIntMethod(points, sizeMId);
    jmethodID getMId = env->GetMethodID(listCLass, "get", "(I)Ljava/lang/Object;");
    ArrayList<Point> list;
    for (int i = 0; i < length; ++i) {
        jobject complexValueObj = env->CallObjectMethod(points, getMId, (jint) i);
        jclass complexValueClass = env->GetObjectClass(complexValueObj);
        jfieldID reFId = env->GetFieldID(complexValueClass, "re", "D");
        jfieldID imFId = env->GetFieldID(complexValueClass, "im", "D");
        Point p(env->GetDoubleField(complexValueObj, reFId),
                env->GetDoubleField(complexValueObj, imFId));
        list.add(p);
        env->DeleteLocalRef(complexValueObj);
        env->DeleteLocalRef(complexValueClass);
    }

    env->DeleteLocalRef(listCLass);

    F f(list, period);

    env->GetJavaVM(&globalJvm);
    globalCallback = env->NewGlobalRef(callback);

    class Callback : public FourierSeriesCallback {
    private:
    public:
        void callback(double n, double re, double im) override {
            JNIEnv *env = nullptr;
            globalJvm->AttachCurrentThread(&env, nullptr);

            jclass callbackClass = env->GetObjectClass(globalCallback);
            jmethodID callbackMId = env->GetMethodID(callbackClass, "callback", "(DDD)V");
            env->CallVoidMethod(globalCallback, callbackMId, (jdouble) n, (jdouble) re,
                                (jdouble) im);
            globalJvm->DetachCurrentThread();
        }
    } cb;

    FourierSeries fs(f, epicyclesCount, period);
    fs.calc(cb, integralN, threadNum);
    log(env, "jni---", "Fourier series calculation finished.");
}
