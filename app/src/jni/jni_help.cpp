//
// Created by root on 19-10-2.
//

#include "jni_help.h"

void Log(JNIEnv *env, const char *tag, const char *s) {
    if (env == nullptr) {
        char *R = nullptr;
        strcpyAndCat_auto(&R, tag, ": ");
        strcat_auto(&R, s);
        printf("%s\n", R);
    } else {
        jstring str = env->NewStringUTF(s);
        char *T = nullptr;
        strcpyAndCat_auto(&T, "jniLog ", tag);
        jstring tagS = env->NewStringUTF(T);
        jclass mClass = env->FindClass("android/util/Log");
        jmethodID mid = env->GetStaticMethodID(mClass, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
        env->CallStaticIntMethod(mClass, mid, tagS, str);
        env->DeleteLocalRef(str);
        env->DeleteLocalRef(tagS);
        env->DeleteLocalRef(mClass);
    }
}

void LogArr(JNIEnv *env, const char *tag, const char *s, int size) {
    char *r = nullptr;
    char *R = nullptr;
    for (int i = 0; i < size; ++i) {
        m_itoa(&r, (int) (usi) s[i]);
        strcat_auto(&R, r);
        strcat_auto(&R, " ");
    }
    Log(env, tag, R);
}