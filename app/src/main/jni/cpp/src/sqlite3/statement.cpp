//
// Created by bczhc on 2/11/21.
//

#include "../../third_party/my-cpp-lib/string.hpp"
#include "../jni_h/pers_zhc_tools_jni_JNI_Sqlite3_Statement.h"
#include "../jni_help.h"
#include "../../third_party/my-cpp-lib/sqlite3.hpp"
#include <cassert>

using namespace bczhc;
using Stmt = Sqlite3::Statement;
using Cursor = Sqlite3::Cursor;

String getBindErrorMsg(int status) {
    String msg = "Binding failed, error code: ";
    return msg += String::toString(status) += '.';
}

void throwBindErrorException(JNIEnv *&env, int status) {
    String s = getBindErrorMsg(status);
    throwException(env, s.getCString());
}

void checkBindStatus(JNIEnv *&env, int status) {
    if (status != SQLITE_OK) {
        throwBindErrorException(env, status);
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_bind__JII
        (JNIEnv *env, jclass, jlong statId, jint row, jint a) {
    try {
        ((Stmt *) statId)->bind((int) row, (int32_t) a);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}


JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_bind__JID
        (JNIEnv *env, jclass, jlong statId, jint row, jdouble a) {
    try {
        ((Stmt *) statId)->bind((int) row, (double) a);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_bindText
        (JNIEnv *env, jclass, jlong statId, jint row, jstring js) {
    const char *s = env->GetStringUTFChars(js, nullptr);
    try {
        ((Stmt *) statId)->bindText((int) row, s, SQLITE_TRANSIENT);
        env->ReleaseStringUTFChars(js, s);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_bindNull
        (JNIEnv *env, jclass, jlong statId, jint row) {
    try {
        ((Stmt *) statId)->bindNull((int) row);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_reset
        (JNIEnv *env, jclass, jlong statId) {
    try {
        ((Stmt *) statId)->reset();
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_bindBlob
        (JNIEnv *env, jclass, jlong statId, jint row, jbyteArray jBytes, jint size) {
    jbyte *bytes = env->GetByteArrayElements(jBytes, nullptr);
    assert(sizeof(jbyte) == sizeof(char));
    char *b = (char *) bytes;
    try {
        ((Stmt *) statId)->bindBlob((int) row, b, (int) size);
        env->ReleaseByteArrayElements(jBytes, bytes, 0);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_step
        (JNIEnv *env, jclass, jlong statId) {
    try {
        ((Stmt *) statId)->step();
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_finalize
        (JNIEnv *env, jclass, jlong statId) {
    try {
        ((Stmt *) statId)->release();
        delete (Stmt *) statId;
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_bind__JIJ
        (JNIEnv *env, jclass, jlong statId, jint row, jlong a) {
    try {
        ((Stmt *) statId)->bind((int) row, (int64_t) a);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_getCursor
        (JNIEnv *env, jclass, jlong stmtId) {
    return (jlong) new Cursor(*((Stmt *) stmtId));
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_stepRow
        (JNIEnv *env, jclass, jlong stmtId) {
    return (jint) ((Stmt *) stmtId)->stepRow();
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_00024Statement_getIndexByColumnName
        (JNIEnv *env, jclass, jlong stmtId, jstring nameJS) {
    auto name = env->GetStringUTFChars(nameJS, nullptr);
    int32_t r = ((Stmt *) stmtId)->getIndexByColumnName(name);
    env->ReleaseStringUTFChars(nameJS, name);
    return (jint) r;
}