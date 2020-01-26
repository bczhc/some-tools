//
// Created by root on 19-10-2.
//

#include "jni_help.h"

void Log(JNIEnv *env, const char *tag, const char *s) {
    if (env == NULL) {
        char *R = NULL;
        strcpyAndCat_auto(&R, tag, ": ");
        strcat_auto(&R, s);
        printf("%s\n", R);
    } else {
        JNIEnv e = *env;
        jstring str = e->NewStringUTF(env, s);
        char *T = NULL;
        strcpyAndCat_auto(&T, "jniLog ", tag);
        jstring tagS = e->NewStringUTF(env, T);
        jclass mClass = e->FindClass(env, "android/util/Log");
        jmethodID mid = e->GetStaticMethodID(env, mClass, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
        e->CallStaticIntMethod(env, mClass, mid, tagS, str);
        e->DeleteLocalRef(env, str);
        e->DeleteLocalRef(env, tagS);
        e->DeleteLocalRef(env, mClass);
    }
}

void LogArr(JNIEnv *env, const char *tag, const char *s, int size) {
    char *r = NULL;
    char *R = NULL;
    for (int i = 0; i < size; ++i) {
        m_itoa(&r, (int) (usi) s[i]);
        strcat_auto(&R, r);
        strcat_auto(&R, " ");
    }
    Log(env, tag, R);
}