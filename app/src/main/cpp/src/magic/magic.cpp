//
// Created by bczhc on 7/4/21.
//

#include "../../third_party/libmagic/src/magic.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_Magic.h"
#include <third_party/jni-lib/src/jni_help.h>


JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Magic_init
        (JNIEnv *, jclass, jint flag) {
    return (jlong) magic_open((int) flag);
}


JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Magic_close
        (JNIEnv *, jclass, jlong addr) {
    magic_close((magic_t) addr);
}


JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Magic_load
        (JNIEnv *env, jclass, jlong addr, jstring pathJS) {
    const char *path = env->GetStringUTFChars(pathJS, nullptr);
    int status = magic_load((magic_t) addr, path);
    env->ReleaseStringUTFChars(pathJS, path);

    if (status != 0) {
        const char *errMsg = magic_error((magic_t) addr);
        if (errMsg != nullptr) {
            throwException(env, "Failed to load database: %s", errMsg);
        } else {
            throwException(env, "Failed to load database");
        }
    }
}


JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Magic_setFlag
        (JNIEnv *env, jclass, jlong addr, jint flag) {
    if (magic_setflags(((magic_t) addr), (int) flag) != 0) {
        const char *errMsg = magic_error((magic_t) addr);
        if (errMsg != nullptr) {
            throwException(env, "Failed to set flags: %s", errMsg);
        } else {
            throwException(env, "Failed to set flags");
        }
    }
}


JNIEXPORT jstring JNICALL Java_pers_zhc_tools_jni_JNI_00024Magic_file
        (JNIEnv *env, jclass, jlong addr, jstring pathJS) {
    const char *path = env->GetStringUTFChars(pathJS, nullptr);
    const char *r = magic_file(((magic_t) addr), path);
    env->ReleaseStringUTFChars(pathJS, path);

    if (r == nullptr) {
        const char *errMsg = magic_error((magic_t) addr);
        if (errMsg != nullptr) {
            throwException(env, "Failed to describe the file: %s", errMsg);
        } else {
            throwException(env, "Failed to describe the file");
        }
        return nullptr;
    }
    return env->NewStringUTF(r);
}