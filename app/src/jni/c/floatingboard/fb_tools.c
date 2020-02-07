//
// Created by root on 19-8-27.
//

#include <stddef.h>
#include "../verification/com_zhc_tools_floatingdrawing_JNI.h"
#include <stdio.h>
#include <stdlib.h>

JNIEXPORT void  JNICALL Java_pers_zhc_tools_floatingdrawing_JNI_floatToByteArray
        (JNIEnv *env, jobject obj, jbyteArray dst, jfloat a, jint offset) {
    JNIEnv e = *env;
    float f = (float) a;
    char *c = (char *) &f;
    jbyte R[4];
    R[0] = c[0];
    R[1] = c[1];
    R[2] = c[2];
    R[3] = c[3];
    e->SetByteArrayRegion(env, dst, offset, 4, R);
}

JNIEXPORT void  JNICALL Java_pers_zhc_tools_floatingdrawing_JNI_intToByteArray
        (JNIEnv *env, jobject obj, jbyteArray dst, jint j, jint offset) {
    JNIEnv e = *env;
    int i = (int) j;
    char *c = (char *) &i;
    jbyte R[4];
    R[0] = c[0];
    R[1] = c[1];
    R[2] = c[2];
    R[3] = c[3];
    e->SetByteArrayRegion(env, dst, offset, 4, R);
}

JNIEXPORT jfloat JNICALL Java_pers_zhc_tools_floatingdrawing_JNI_byteArrayToFloat
        (JNIEnv *env, jobject obj, jbyteArray bytes, jint offset) {
    JNIEnv e = *env;
    jbyte b[4];
    e->GetByteArrayRegion(env, bytes, offset, 4, b);
    float r = *((float *) b);
    return (jfloat) r;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_floatingdrawing_JNI_byteArrayToInt
        (JNIEnv *env, jobject obj, jbyteArray bytes, jint offset) {
    JNIEnv e = *env;
    jbyte b[4];
    e->GetByteArrayRegion(env, bytes, offset, 4, b);
    int r = *((int *) b);
    return (jint) r;
}