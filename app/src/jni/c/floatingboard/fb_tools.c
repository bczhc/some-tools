//
// Created by root on 19-8-27.
//

#include <stddef.h>
#include "../verification/com_zhc_tools_floatingboard_JNI.h"
#include <stdio.h>
#include <stdlib.h>

JNIEXPORT jbyteArray JNICALL Java_pers_zhc_tools_floatingboard_JNI_floatToByteArray
        (JNIEnv *env, jobject obj, jfloat a) {
    JNIEnv e = *env;
    float f = (float) a;
    char *c = (char *) &f;
    jbyteArray r = e->NewByteArray(env, 4);
    jbyte R[4];
    for (int i = 0; i < 4; ++i) {
        R[i] = c[i];
    }
    e->SetByteArrayRegion(env, r, 0, 4, R);
    return (jbyteArray) r;
}

JNIEXPORT jbyteArray JNICALL Java_pers_zhc_tools_floatingboard_JNI_intToByteArray
        (JNIEnv *env, jobject obj, jint j) {
    JNIEnv e = *env;
    int i = (int) j;
    char *c = (char *) &i;
    jbyteArray r = e->NewByteArray(env, 4);
    jbyte R[4];
    for (int k = 0; k < 4; ++k) {
        R[k] = c[k];
    }
    e->SetByteArrayRegion(env, r, 0, 4, R);
    return (jbyteArray) r;
}

JNIEXPORT jfloat JNICALL Java_pers_zhc_tools_floatingboard_JNI_byteArrayTofloat
        (JNIEnv *env, jobject obj, jbyteArray bytes) {
    JNIEnv e = *env;
    jbyte b[4];
    e->GetByteArrayRegion(env, bytes, 0, 4, b);
    float r = *((float *) b);
    return (jfloat) r;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_floatingboard_JNI_byteArrayToInt
        (JNIEnv *env, jobject obj, jbyteArray bytes) {
    JNIEnv e = *env;
    jbyte b[4];
    e->GetByteArrayRegion(env, bytes, 0, 4, b);
    int r = *((int *) b);
    return (jint) r;
}