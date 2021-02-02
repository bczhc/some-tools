//
// Created by root on 19-10-2.
//

#include "jni_help.h"
#include "../third_party/my-cpp-lib/string.hpp"
#include <cstdio>

using namespace bczhc;

void jnihelp::log(JNIEnv *&env, const char *tag, const char *format, ...) {
    va_list args{};
    va_start(args, format);
    char *msg = nullptr;
    vasprintf(&msg, format, args);
    if (env == nullptr) {
        printf("%s: %s\n", tag, msg);
    } else {
        jstring str = env->NewStringUTF(msg);
        jstring tagS = env->NewStringUTF(tag);
        jclass mClass = env->FindClass("android/util/Log");
        jmethodID mid = env->GetStaticMethodID(mClass, "d",
                                               "(Ljava/lang/String;Ljava/lang/String;)I");
        env->CallStaticIntMethod(mClass, mid, tagS, str);
        env->DeleteLocalRef(mClass);
    }
    free(msg);
    va_end(args);
}

void jnihelp::throwException(JNIEnv *&env, const char *format, ...) {
    va_list args{};
    va_start(args, format);
    jclass exceptionClass = env->FindClass("java/lang/Exception");
    char *msg = nullptr;
    vasprintf(&msg, format, args);
    env->ThrowNew(exceptionClass, msg);
    free(msg);
    va_end(args);
}
