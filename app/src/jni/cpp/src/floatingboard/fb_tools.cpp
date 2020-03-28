//
// Created by root on 19-8-27.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_FloatingBoard.h"

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_floatToByteArray
        (JNIEnv *env, jclass cls, jbyteArray dst, jfloat a, jint offset) {
    JNIEnv e = *env;
    auto f = (float) a;
    char *c = (char *) &f;
    jbyte R[4];
    R[0] = c[0];
    R[1] = c[1];
    R[2] = c[2];
    R[3] = c[3];
    e.SetByteArrayRegion(dst, offset, 4, R);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_intToByteArray
        (JNIEnv *env, jclass cls, jbyteArray dst, jint j, jint offset) {
    JNIEnv e = *env;
    int i = (int) j;
    char *c = (char *) &i;
    jbyte R[4];
    R[0] = c[0];
    R[1] = c[1];
    R[2] = c[2];
    R[3] = c[3];
    e.SetByteArrayRegion(dst, offset, 4, R);
}

JNIEXPORT jfloat JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_byteArrayToFloat
        (JNIEnv *env, jclass cls, jbyteArray bytes, jint offset) {
    JNIEnv e = *env;
    jbyte b[4];
    e.GetByteArrayRegion(bytes, offset, 4, b);
    float r = *((float *) b);
    return (jfloat) r;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_byteArrayToInt
        (JNIEnv *env, jclass cls, jbyteArray bytes, jint offset) {
    JNIEnv e = *env;
    jbyte b[4];
    e.GetByteArrayRegion(bytes, offset, 4, b);
    int r = *((int *) b);
    return (jint) r;
}