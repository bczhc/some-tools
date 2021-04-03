//
// Created by bczhc on 4/3/21.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_Struct.h"
#include "../../third_party/my-cpp-lib/utils.hpp"

using namespace bczhc;

enum Mode {
    MODE_BIG_ENDIAN = 0,
    MODE_LITTLE_ENDIAN = 1
};

static int16_t t1 = 0x1234;
static Endianness endianness = *(int8_t *) &t1 == 0x12 ? Endianness::BIG : Endianness::LITTLE;
static Mode selfMode = endianness == Endianness::LITTLE ? Mode::MODE_LITTLE_ENDIAN : Mode::MODE_BIG_ENDIAN;

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_packShort
        (JNIEnv *env, jclass, jshort value, jbyteArray array, jint offset, jint mode) {
    auto b = (jbyte *) &value;
    if (mode != selfMode) {
        jbyte t = b[1];
        b[1] = b[0];
        b[0] = t;
    }
    env->SetByteArrayRegion(array, offset, 2, b);
}


JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_packInt
        (JNIEnv *env, jclass, jint value, jbyteArray array, jint offset, jint mode) {
    auto b = (jbyte *) &value;
    if (mode != selfMode) {
        jint t = value;
        auto p = (jbyte *) &t;
        b[0] = p[3];
        b[1] = p[2];
        b[2] = p[1];
        b[3] = p[0];
    }
    env->SetByteArrayRegion(array, offset, 4, b);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_packLong
        (JNIEnv *env, jclass, jlong value, jbyteArray array, jint offset, jint mode) {
    auto b = (jbyte *) &value;
    if (mode != selfMode) {
        auto t = value;
        auto p = (jbyte *) &t;
        b[0] = p[7];
        b[1] = p[6];
        b[2] = p[5];
        b[3] = p[4];
        b[4] = p[3];
        b[5] = p[2];
        b[6] = p[1];
        b[7] = p[0];
    }
    env->SetByteArrayRegion(array, offset, 8, b);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_packFloat
        (JNIEnv *env, jclass, jfloat value, jbyteArray array, jint offset, jint mode) {
    auto b = (jbyte *) &value;
    if (mode != selfMode) {
        auto t = value;
        auto p = (jbyte *) &t;
        b[0] = p[3];
        b[1] = p[2];
        b[2] = p[1];
        b[3] = p[0];
    }
    env->SetByteArrayRegion(array, offset, 4, b);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_packDouble
        (JNIEnv *env, jclass, jdouble value, jbyteArray array, jint offset, jint mode) {
    auto b = (jbyte *) &value;
    if (mode != selfMode) {
        auto t = value;
        auto p = (jbyte *) &t;
        b[0] = p[7];
        b[1] = p[6];
        b[2] = p[5];
        b[3] = p[4];
        b[4] = p[3];
        b[5] = p[2];
        b[6] = p[1];
        b[7] = p[0];
    }
    env->SetByteArrayRegion(array, offset, 8, b);
}

JNIEXPORT jshort JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_unpackShort
        (JNIEnv *env, jclass, jbyteArray array, jint offset, jint mode) {
    jbyte bytes[2];
    env->GetByteArrayRegion(array, offset, 2, bytes);
    if (mode != selfMode) {
        jbyte t[2];
        t[0] = bytes[1];
        t[1] = bytes[0];
        return *(jshort *) t;
    }
    return *(jshort *) bytes;
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_unpackInt
        (JNIEnv *env, jclass, jbyteArray array, jint offset, jint mode) {
    jbyte bytes[4];
    env->GetByteArrayRegion(array, offset, 4, bytes);
    if (mode != selfMode) {
        jbyte t[4];
        t[0] = bytes[3];
        t[1] = bytes[2];
        t[2] = bytes[1];
        t[3] = bytes[0];
        return *(jint *) t;
    }
    return *(jint *) bytes;
}

JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_unpackLong
        (JNIEnv *env, jclass, jbyteArray array, jint offset, jint mode) {
    jbyte bytes[8];
    env->GetByteArrayRegion(array, offset, 8, bytes);
    if (mode != selfMode) {
        jbyte t[8];
        t[0] = bytes[7];
        t[1] = bytes[6];
        t[2] = bytes[5];
        t[3] = bytes[4];
        t[4] = bytes[3];
        t[5] = bytes[2];
        t[6] = bytes[1];
        t[7] = bytes[0];
        return *(jlong *) t;
    }
    return *(jlong *) bytes;
}

JNIEXPORT jfloat JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_unpackFloat
        (JNIEnv *env, jclass, jbyteArray array, jint offset, jint mode) {
    jbyte bytes[4];
    env->GetByteArrayRegion(array, offset, 4, bytes);
    if (mode != selfMode) {
        jbyte t[4];
        t[0] = bytes[3];
        t[1] = bytes[2];
        t[2] = bytes[1];
        t[3] = bytes[0];
        return *(jfloat *) t;
    }
    return *(jfloat *) bytes;
}

JNIEXPORT jdouble JNICALL Java_pers_zhc_tools_jni_JNI_00024Struct_unpackDouble
        (JNIEnv *env, jclass, jbyteArray array, jint offset, jint mode) {
    jbyte bytes[8];
    env->GetByteArrayRegion(array, offset, 8, bytes);
    if (mode != selfMode) {
        jbyte t[8];
        t[0] = bytes[7];
        t[1] = bytes[6];
        t[2] = bytes[5];
        t[3] = bytes[4];
        t[4] = bytes[3];
        t[5] = bytes[2];
        t[6] = bytes[1];
        t[7] = bytes[0];
        return *(jdouble *) t;
    }
    return *(jdouble *) bytes;
}
