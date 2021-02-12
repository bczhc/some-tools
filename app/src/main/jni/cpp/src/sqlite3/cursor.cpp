//
// Created by bczhc on 2/11/21.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_Sqlite3_Cursor.h"
#include "../../third_party/my-cpp-lib/sqlite3.hpp"
#include "../jni_help.h"
#include <cassert>

using namespace bczhc;

using Cursor = Sqlite3::Cursor;

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Cursor_reset
        (JNIEnv *env, jclass, jlong cId) {
    try {
        ((Cursor *) cId)->reset();
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT jboolean JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Cursor_step
        (JNIEnv *env, jclass, jlong cId) {
    try {
        return (jboolean) ((Cursor *) cId)->step();
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
    return (jboolean) false;
}

JNIEXPORT jbyteArray JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Cursor_getBlob
        (JNIEnv *env, jclass, jlong cId, jint column) {
    auto b = ((Cursor *) cId)->getBlob((int) column);
    // TODO get blob size
    jbyteArray arr = env->NewByteArray(0);
    assert(sizeof(b[0]) == sizeof(jbyte));
    env->SetByteArrayRegion(arr, 0, 0, (const jbyte *) b);
    return arr;
}

JNIEXPORT jstring JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Cursor_getText
        (JNIEnv *env, jclass, jlong cId, jint c) {
    auto s = ((Cursor *) cId)->getText((int) c);
    auto ret = env->NewStringUTF(s);
    env->DeleteLocalRef(ret);
    return ret;
}

JNIEXPORT jdouble JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Cursor_getDouble
        (JNIEnv *, jclass, jlong cId, jint c) {
    return (jdouble) ((Cursor *) cId)->getDouble((int) c);
}


JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Cursor_getLong
        (JNIEnv *, jclass, jlong cId, jint c) {
    return (jlong) ((Cursor *) cId)->getLong((int) c);
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Cursor_getInt
        (JNIEnv *, jclass, jlong cId, jint c) {
    return (jint) ((Cursor *) cId)->getInt((int) c);
}