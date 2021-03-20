//
// Created by root on 19-10-2.
//

#include "jni_help.h"
#include <cstdio>

using namespace bczhc;

void bczhc::log(JNIEnv *&env, const char *tag, const char *format, ...) {
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
        env->DeleteLocalRef(str), env->DeleteLocalRef(tagS);
    }
    free(msg);
    va_end(args);
}

void bczhc::throwException(JNIEnv *&env, const char *format, ...) {
    va_list args{};
    va_start(args, format);
    jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
    char *msg = nullptr;
    vasprintf(&msg, format, args);
    env->ThrowNew(exceptionClass, msg);
    free(msg);
    va_end(args);
}
