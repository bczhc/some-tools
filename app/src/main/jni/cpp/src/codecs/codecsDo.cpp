#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma ide diagnostic ignored "hicpp-signed-bitwise"
//
// Created by zhc-2 on 2019/6/19.
//

#include "./codecsDo.h"

using namespace bczhc;

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"

void Callback(JNIEnv *env, jobject callback, const char *str, double d) {
    jclass cls = env->GetObjectClass(callback);
    jmethodID mid = env->GetMethodID(cls, "callback", "(Ljava/lang/String;D)V");
    jstring s = env->NewStringUTF(str);
    env->CallVoidMethod(callback, mid, s, (jdouble) d);
    env->DeleteLocalRef(cls);
    env->DeleteLocalRef(s);
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Codecs_qmcDecode
        (JNIEnv *env, jclass cls, jstring f, jstring dF, jint mode, jobject callback) {
    const char *f1 = env->GetStringUTFChars(f, (jboolean *) 0);
    const char *f2 = env->GetStringUTFChars(dF, (jboolean *) 0);
    const char *sQ;
    String sQString;
    sQString.append(f1)
            .append(" -> ")
            .append(f2);
    sQ = sQString.getCString();
    Log(env, "", sQ);
    Log(env, "", "JNI————解码……");
    int rC;
    const char *u1, *u2;
    u1 = String::toUpperCase(f1).getCString(), u2 = String::toUpperCase(f2).getCString();
    char *nN = NULL;
    if (!strcmp(u1, u2))
        NewFileName(&nN, f1), rC = decode(f1, nN, env, callback), remove(f1), rename(nN, f1);
    else
        rC = decode(f1, f2, env, callback);
    if (mode) remove(f1);
    env->ReleaseStringUTFChars(f, f1);
    env->ReleaseStringUTFChars(dF, f2);
    return (jint) rC;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Codecs_kwmDecode
        (JNIEnv *env, jclass cls, jstring f, jstring dF, jint mode, jobject callback) {
    const char *fN = env->GetStringUTFChars(f, (jboolean *) 0);
    const char *dFN = env->GetStringUTFChars(dF, (jboolean *) 0);
    int stt;
    const char *u1, *u2;
    u1 = String::toUpperCase(fN).getCString(), u2 = String::toUpperCase(dFN).getCString();
    char *nN = NULL;
    if (!strcmp(u1, u2))
        NewFileName(&nN, fN), stt = kwm(env, fN, nN, callback), remove(fN), rename(nN, fN);
    else stt = kwm(env, fN, dFN, callback);
    if (mode) remove(fN);
    env->ReleaseStringUTFChars(f, fN);
    env->ReleaseStringUTFChars(dF, dFN);
    return stt;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Codecs_Base128_1encode
        (JNIEnv *env, jclass cls, jstring f1, jstring f2, jint mode, jobject callback) {
    const char *FileName = (*env).GetStringUTFChars(f1, (jboolean *) NULL);
    const char *DestFileName = (*env).GetStringUTFChars(f2, (jboolean *) NULL);
    const char *upperCaseD1, *upperCaseD2;
    upperCaseD1 = String::toUpperCase(FileName).getCString();
    upperCaseD2 = String::toUpperCase(DestFileName).getCString();
    char *nFN = NULL;
    if (!strcmp(upperCaseD1, upperCaseD2)) {
        NewFileName(&nFN, FileName);
        int status = eD(FileName, nFN, env, callback);
        if (status != 0) {
            env->ReleaseStringUTFChars(f1, FileName);
            env->ReleaseStringUTFChars(f2, DestFileName);
            return status;
        }
        remove(FileName);
        rename(nFN, DestFileName);
    } else {
        eD(FileName, DestFileName, env, callback);
    }
    if (mode) remove(FileName);
    env->ReleaseStringUTFChars(f1, FileName);
    env->ReleaseStringUTFChars(f2, DestFileName);
    return 0;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Codecs_Base128_1decode
        (JNIEnv *env, jclass cls, jstring f1, jstring f2, jint mode, jobject callback) {
    const char *FileName = (*env).GetStringUTFChars(f1, (jboolean *) NULL);
    const char *DestFileName = (*env).GetStringUTFChars(f2, (jboolean *) NULL);
    const char *upperCaseD1, *upperCaseD2;
    upperCaseD1 = String::toUpperCase(FileName).getCString();
    upperCaseD2 = String::toUpperCase(DestFileName).getCString();
    char *nFN = NULL;
    if (!strcmp(upperCaseD1, upperCaseD2)) {
        NewFileName(&nFN, FileName);
        int status = dD(FileName, nFN, env, callback);
        if (status != 0) {
            env->ReleaseStringUTFChars(f1, FileName);
            env->ReleaseStringUTFChars(f2, DestFileName);
            return status;
        }
        remove(FileName);
        rename(nFN, DestFileName);
    } else {
        dD(FileName, DestFileName, env, callback);
    }
    if (mode) remove(FileName);
    env->ReleaseStringUTFChars(f1, FileName);
    env->ReleaseStringUTFChars(f2, DestFileName);
    return 0;
}

#pragma clang diagnostic pop
#pragma clang diagnostic pop