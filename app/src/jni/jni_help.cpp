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
        JNIEnv e = *env;
        jstring str = e.NewStringUTF(s);
        char *T = nullptr;
        strcpyAndCat_auto(&T, "jniLog ", tag);
        jstring tagS = e.NewStringUTF(T);
        jclass mClass = e.FindClass("android/util/Log");
        jmethodID mid = e.GetStaticMethodID(mClass, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
        e.CallStaticIntMethod(mClass, mid, tagS, str);
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