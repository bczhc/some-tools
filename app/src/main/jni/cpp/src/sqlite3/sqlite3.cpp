#pragma clang diagnostic push
#pragma ide diagnostic ignored "bugprone-reserved-identifier"
#include "../../third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_Sqlite3.h"
#include "../../third_party/my-cpp-lib/sqlite3.h"
#include "../jni_help.h"
#include "../../third_party/my-cpp-lib/string.hpp"
#include <cassert>

using namespace bczhc;
using namespace std;
using namespace string;

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
    auto *db = new Sqlite3;
    const char *file = env->GetStringUTFChars(path, (jboolean *) nullptr);
    int code = db->open(file);
    if (code) {
        String msg = "Open or create database failed.";
        msg.append(" code: ")
                .append(String::toString(code));
        throwException(env, msg.getCString());
    }
    env->ReleaseStringUTFChars(path, file);
    return (jlong) db;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_close
        (JNIEnv *env, jclass, jlong id) {
    auto *db = (Sqlite3 *) id;
    int code = db->close();
    if (code) {
        String msg = "Close database failed";
        msg.append(" code: ")
                .append(String::toString(code));
        throwException(env, msg.getCString());
    }
    delete db;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_exec
        (JNIEnv *env, jclass, jlong id, jstring cmd, jobject callbackInterface) {

    auto *db = (Sqlite3 *) id;
    const char *command = env->GetStringUTFChars(cmd, (jboolean *) nullptr);
    if (callbackInterface == nullptr) {
        if (db->exec(command)) {
            throwException(env, db->errMsg);
        }
    } else {
        CB callback(env, callbackInterface);
        if (db->exec(command, callback, nullptr)) {
            throwException(env, db->errMsg);
        }
    }

    env->ReleaseStringUTFChars(cmd, command);
}

JNIEXPORT jboolean JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_checkIfCorrupt
        (JNIEnv *env, jclass, jlong id) {
    return (jboolean) ((Sqlite3 *) id)->checkIfCorrupt();
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bind__JIJ
        (JNIEnv *, jclass, jlong addr, jint row, jlong a) {
}

using Stat = Sqlite3::Statement;

JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_compileStatement
        (JNIEnv *env, jclass, jlong id, jstring statJS) {
    const char *statement = env->GetStringUTFChars(statJS, nullptr);
    Sqlite3::Statement stat = ((Sqlite3 *) id)->compileStatement(statement);
    int64_t r = 0;
    if (stat.status == SQLITE_OK) {
        auto newStat = new Stat(stat);
        r = (int64_t) newStat;
    } else {
        String msg = "Statement compilation failed, error code: ";
        msg += String::toString(stat.status) += '.';
        throwException(env, msg.getCString());
    }
    env->ReleaseStringUTFChars(statJS, statement);
    return (jlong) r;
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
    int status = ((Stat *) statId)->bind((int) row, (int32_t) a);
    checkBindStatus(env, status);
}


JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bind__JID
        (JNIEnv *env, jclass, jlong statId, jint row, jdouble a) {
    int status = ((Stat *) statId)->bind((int) row, (int64_t) a);
    checkBindStatus(env, status);
}

class ByteCopier {
public:
    char *bytes = nullptr;
    explicit ByteCopier(const char *src, size_t size) {
        bytes = new char[size];
        for (int i = 0; i < size; ++i) {
            bytes[i] = src[i];
        }
    }

    ~ByteCopier() {
        delete bytes;
    }
};

static LinkedList<ByteCopier *> copy;

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bindText
        (JNIEnv *env, jclass, jlong statId, jint row, jstring js) {
    const char *s = env->GetStringUTFChars(js, nullptr);
    auto newStr = new ByteCopier(s, env->GetStringUTFLength(js) + 1);
    int status = ((Stat *) statId)->bindText((int) row, newStr->bytes);
    checkBindStatus(env, status);
    env->ReleaseStringUTFChars(js, s);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bindNull
        (JNIEnv *env, jclass, jlong statId, jint row) {
    int status = ((Stat *) statId)->bindNull((int) row);
    checkBindStatus(env, status);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_reset
        (JNIEnv *env, jclass, jlong statId) {
    int status = ((Stat *) statId)->reset();
    if (status != SQLITE_OK) {
        String msg = "Reset failed, error code: ";
        msg += String::toString(status) += '.';
        throwException(env, msg.getCString());
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_bindBlob
        (JNIEnv *env, jclass, jlong statId, jint row, jbyteArray jBytes, jint size) {
    jbyte *bytes = env->GetByteArrayElements(jBytes, nullptr);
    assert(sizeof(jbyte) == sizeof(char));
    char *b = (char *) bytes;
    auto newBlob = new ByteCopier(b, (size_t) size);
    copy.insert(newBlob);
    int status = ((Stat *) statId)->bindBlob((int) row, newBlob->bytes, (int) size);
    env->ReleaseByteArrayElements(jBytes, bytes, 0);
    checkBindStatus(env, status);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_step
        (JNIEnv * env, jclass, jlong statId) {
    int status = ((Stat *) statId)->step();
    if (status != SQLITE_DONE) {
        String msg = "Execution of step failed, error code :";
        msg += String::toString(status) += '.';
        throwException(env, msg.getCString());
    }

    auto it = copy.getIterator();
    while (it.hasNext()) {
        delete it.next();
    }
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_finalize
        (JNIEnv *env, jclass, jlong statId) {
    int status = ((Stat *) statId)->release();
    delete (Stat *) statId;
    if (status != SQLITE_OK) {
        String msg = "Releasing sqlite statement failed, error code: ";
        msg += String::toString(status) += '.';
        throwException(env, msg.getCString());
    }
}

#pragma clang diagnostic pop