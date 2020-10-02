//
// Created by root on 2020/9/26.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_JniTest.h"
#include <sys/time.h> // NOLINT(modernize-deprecated-headers)

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024JniTest_call
        (JNIEnv *, jclass) {

}

JNIEXPORT int JNICALL Java_pers_zhc_tools_jni_JNI_00024JniTest_toCall
        (JNIEnv *env, jclass cls) {
    int64_t start;
    timeval t{};
    gettimeofday(&t, nullptr);
    start = t.tv_sec * 1000 + t.tv_usec / 1000;

    jmethodID mid = env->GetStaticMethodID(cls, "forJNI", "()V");
    int c = 0;
    for (;;) {
        gettimeofday(&t, nullptr);
        if (t.tv_sec * 1000 + t.tv_usec / 1000 - start >= 1000) break;
        env->CallStaticVoidMethod(cls, mid);
        ++c;
    }
    return (jint) c;
}

JNIEXPORT int JNICALL Java_pers_zhc_tools_jni_JNI_00024JniTest_toCall2
        (JNIEnv *env, jclass cls, jobject instance) {
    jclass mClass = env->GetObjectClass(instance);
    int64_t start;
    timeval t{};
    gettimeofday(&t, nullptr);
    start = t.tv_sec * 1000 + t.tv_usec / 1000;
    jmethodID mid = env->GetMethodID(mClass, "myCall", "()V");
    int c = 0;
    for (;;) {
        gettimeofday(&t, nullptr);
        if (t.tv_sec * 1000 + t.tv_usec / 1000 - start >= 1000) break;
        env->CallVoidMethod(instance, mid);
        ++c;
    }
    return c;
}