#pragma clang diagnostic push
#pragma ide diagnostic ignored "hicpp-signed-bitwise"
//
// Created by zhc-2 on 2019/6/19.
//

#include "qmcLib.h"
#include "com_zhc_JNI.h"
#include "Base128Lib.h"
#include "kwm.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"

JNIEXPORT jint JNICALL Java_com_zhc_JNI_qmcDecode
        (JNIEnv *env, jobject obj, jstring f, jstring dF, jint mode) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *f1 = e->GetStringUTFChars(env, f, (jboolean *) 0);
    const char *f2 = e->GetStringUTFChars(env, dF, (jboolean *) 0);
    char *sQ = NULL;
    strcat_auto(&sQ, f1);
    strcat_auto(&sQ, " -> ");
    strcat_auto(&sQ, f2);
    Log(env, sQ);
    Log(env, "JNI————解码……");
    int rC;
    char u1[strlen(f1) + 1], u2[strlen(f2) + 1];
    ToUpperCase(u1, f1), ToUpperCase(u2, f2);
    char *nN = NULL;
    if (!strcmp(u1, u2)) NewFileName(&nN, f1), rC = decode(f1, nN, env, mid, obj), remove(f1), rename(nN, f1);
    else
        rC = decode(f1, f2, env, mid, obj);
    if (mode) remove(f1);
    return (jint) rC;
}

JNIEXPORT jint JNICALL Java_com_zhc_JNI_kwmDecode
        (JNIEnv *env, jobject obj, jstring f, jstring dF, jint mode) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *fN = e->GetStringUTFChars(env, f, (jboolean *) 0);
    const char *dFN = e->GetStringUTFChars(env, dF, (jboolean *) 0);
    int stt;
    char u1[strlen(fN) + 1], u2[strlen(dFN) + 1];
    ToUpperCase(u1, fN), ToUpperCase(u2, dFN);
    char *nN = NULL;
    if (!strcmp(u1, u2)) NewFileName(&nN, fN), stt = kwm(env, mid, obj, fN, nN), remove(fN), rename(nN, fN);
    else stt = kwm(env, mid, obj, fN, dFN);
    if (mode) remove(fN);
    return stt;
}

JNIEXPORT void JNICALL Java_com_zhc_JNI_Base128_1encode
        (JNIEnv *env, jobject obj, jstring f1, jstring f2, jint mode) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *FileName = (*env)->GetStringUTFChars(env, f1, (jboolean *) NULL);
    const char *DestFileName = (*env)->GetStringUTFChars(env, f2, (jboolean *) NULL);
    char upperCaseD1[strlen(FileName) + 1], upperCaseD2[strlen(DestFileName) + 1];
    ToUpperCase(upperCaseD1, FileName);
    ToUpperCase(upperCaseD2, DestFileName);
    char *nFN = NULL;
    if (!strcmp(upperCaseD1, upperCaseD2)) {
        NewFileName(&nFN, FileName);
        int status = eD(FileName, nFN, env, obj, mid);
        if (status != 0) return;
        remove(FileName);
        rename(nFN, DestFileName);
    } else {
        eD(FileName, DestFileName, env, obj, mid);
    }
    if (mode) remove(FileName);
}

JNIEXPORT void JNICALL Java_com_zhc_JNI_Base128_1decode
        (JNIEnv *env, jobject obj, jstring f1, jstring f2, jint mode) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *FileName = (*env)->GetStringUTFChars(env, f1, (jboolean *) NULL);
    const char *DestFileName = (*env)->GetStringUTFChars(env, f2, (jboolean *) NULL);
    char upperCaseD1[strlen(FileName) + 1], upperCaseD2[strlen(DestFileName) + 1];
    ToUpperCase(upperCaseD1, FileName);
    ToUpperCase(upperCaseD2, DestFileName);
    char *nFN = NULL;
    if (!strcmp(upperCaseD1, upperCaseD2)) {
        NewFileName(&nFN, FileName);
        int status = dD(FileName, nFN, env, obj, mid);
        if (status != 0) return;
        remove(FileName);
        rename(nFN, DestFileName);
    } else {
        dD(FileName, DestFileName, env, obj, mid);
    }
    if (mode) remove(FileName);
}

#pragma clang diagnostic pop
#pragma clang diagnostic pop