#pragma clang diagnostic push
#pragma ide diagnostic ignored "bugprone-reserved-identifier"

#include <cassert>
#include "../../third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_Sqlite3.h"
#include "../../third_party/my-cpp-lib/sqlite3.hpp"

using namespace bczhc;
using namespace std;

using Stat = Sqlite3::Statement;

class CB : public Sqlite3::SqliteCallback {
private:
    jclass stringClass;
    jmethodID callbackMId;
    JNIEnv *&env;
    jobject &callbackObject;
    jclass mCallbackClass{};
public:
    CB(JNIEnv *&env, jobject &callbackObject) : env(env), callbackObject(callbackObject) {
        jclass callbackClass = env->GetObjectClass(callbackObject);
        callbackMId = env->GetMethodID(callbackClass, "callback",
                                       "([Ljava/lang/String;)I");
        stringClass = (jclass) env->NewGlobalRef(env->FindClass("java/lang/String"));
    }

    ~CB() {
        env->DeleteLocalRef(mCallbackClass);
        env->DeleteGlobalRef(stringClass);
    }

public:
    int callback(void *arg, int colNum, char **content, char **colName) override {
        jobjectArray contentArray = env->NewObjectArray(colNum, stringClass, nullptr);
        for (int i = 0; i < colNum; ++i) {
            jstring s = env->NewStringUTF(content[i]);
            env->SetObjectArrayElement(contentArray, i, s);
            env->DeleteLocalRef(s);
        }
        return (int) env->CallIntMethod(callbackObject, callbackMId, contentArray);
    }
};

void throwException(JNIEnv *env, const char *msg) {
    jclass exceptionClass = env->FindClass("java/lang/Exception");
    env->ThrowNew(exceptionClass, msg);
    env->DeleteLocalRef(exceptionClass);
}

JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_open
        (JNIEnv *env, jclass, jstring path) {
    const char *file = env->GetStringUTFChars(path, (jboolean *) nullptr);
    try {
        auto *db = new Sqlite3(file);
        return (jlong) db;
    } catch (const SqliteException &e) {
        String msg = "Open or create database failed.";
        msg.append(" code: ")
                .append(String::toString(e.returnCode));
        throwException(env, msg.getCString());
    }
    env->ReleaseStringUTFChars(path, file);
    return (jlong) 0;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_close
        (JNIEnv *env, jclass, jlong id) {
    auto *db = (Sqlite3 *) id;
    try {
        db->close();
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
    delete db;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_exec
        (JNIEnv *env, jclass, jlong id, jstring cmd, jobject callbackInterface) {

    auto *db = (Sqlite3 *) id;
    const char *command = env->GetStringUTFChars(cmd, (jboolean *) nullptr);
    try {
        if (callbackInterface == nullptr) {
            db->exec(command);
        } else {
            CB callback(env, callbackInterface);
            db->exec(command, callback, nullptr);
        }
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
    env->ReleaseStringUTFChars(cmd, command);
}

JNIEXPORT jboolean JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_checkIfCorrupt
        (JNIEnv *env, jclass, jlong id) {
    return (jboolean) ((Sqlite3 *) id)->checkIfCorrupt();
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bind__JIJ
        (JNIEnv *env, jclass, jlong statId, jint row, jlong a) {
    try {
        ((Stat *) statId)->bind((int) row, (int64_t) a);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_compileStatement
        (JNIEnv *env, jclass, jlong id, jstring statJS) {
    try {
        const char *statement = env->GetStringUTFChars(statJS, nullptr);
        Sqlite3::Statement stat = ((Sqlite3 *) id)->compileStatement(statement);
        auto newStat = new Stat(stat);
        auto r = (int64_t) newStat;
        env->ReleaseStringUTFChars(statJS, statement);
        return (jlong) r;
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
    return (jlong) nullptr;
}

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

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bind__JII
        (JNIEnv *env, jclass, jlong statId, jint row, jint a) {
    try {
        ((Stat *) statId)->bind((int) row, (int32_t) a);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}


JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bind__JID
        (JNIEnv *env, jclass, jlong statId, jint row, jdouble a) {
    try {
        ((Stat *) statId)->bind((int) row, (double) a);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bindText
        (JNIEnv *env, jclass, jlong statId, jint row, jstring js) {
    const char *s = env->GetStringUTFChars(js, nullptr);
    try {
        ((Stat *) statId)->bindText((int) row, s, SQLITE_TRANSIENT);
        env->ReleaseStringUTFChars(js, s);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bindNull
        (JNIEnv *env, jclass, jlong statId, jint row) {
    try {
        ((Stat *) statId)->bindNull((int) row);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_reset
        (JNIEnv *env, jclass, jlong statId) {
    try {
        ((Stat *) statId)->reset();
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bindBlob
        (JNIEnv *env, jclass, jlong statId, jint row, jbyteArray jBytes, jint size) {
    jbyte *bytes = env->GetByteArrayElements(jBytes, nullptr);
    assert(sizeof(jbyte) == sizeof(char));
    char *b = (char *) bytes;
    try {
        ((Stat *) statId)->bindBlob((int) row, b, (int) size);
        env->ReleaseByteArrayElements(jBytes, bytes, 0);
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_step
        (JNIEnv *env, jclass, jlong statId) {
    try {
        ((Stat *) statId)->step();
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_finalize
        (JNIEnv *env, jclass, jlong statId) {
    try {
        ((Stat *) statId)->release();
        delete (Stat *) statId;
    } catch (const SqliteException &e) {
        throwException(env, e.what());
    }
}
