//
// Created by root on 19-8-27.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_FloatingBoard.h"

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_floatToByteArray
        (JNIEnv *env, jclass cls, jbyteArray dst, jfloat a, jint offset) {
    auto f = (float) a;
    char *c = (char *) &f;
    jbyte R[4];
    R[0] = c[0];
    R[1] = c[1];
    R[2] = c[2];
    R[3] = c[3];
    env->SetByteArrayRegion(dst, offset, 4, R);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_intToByteArray
        (JNIEnv *env, jclass cls, jbyteArray dst, jint j, jint offset) {
    int i = (int) j;
    char *c = (char *) &i;
    jbyte R[4];
    R[0] = c[0];
    R[1] = c[1];
    R[2] = c[2];
    R[3] = c[3];
    env->SetByteArrayRegion(dst, offset, 4, R);
}

JNIEXPORT jfloat JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_byteArrayToFloat
        (JNIEnv *env, jclass cls, jbyteArray bytes, jint offset) {
    jbyte b[4];
    env->GetByteArrayRegion(bytes, offset, 4, b);
    float r = *((float *) b);
    return (jfloat) r;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_byteArrayToInt
        (JNIEnv *env, jclass cls, jbyteArray bytes, jint offset) {
    jbyte b[4];
    env->GetByteArrayRegion(bytes, offset, 4, b);
    int r = *((int *) b);
    return (jint) r;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024FloatingBoard_packStrokeInfo3_11
        (JNIEnv *env, jclass, jbyteArray dest, jint color, jfloat width, jfloat blurRadius) {
    const char *colorBytes = (char *) &color;
    const char *widthBytes = (char *) &width;
    const char *blurRadiusBytes = (char *) &blurRadius;

    env->SetByteArrayRegion(dest, 0, 4, (const jbyte *) colorBytes);
    env->SetByteArrayRegion(dest, 4, 4, (const jbyte *) widthBytes);
    env->SetByteArrayRegion(dest, 8, 4, (const jbyte *) blurRadiusBytes);
}
