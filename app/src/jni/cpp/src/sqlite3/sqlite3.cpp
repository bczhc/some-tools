#include "../../third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_Sqlite3.h"
#include "../../third_party/my-cpp-lib/zhc.h"

using namespace bczhc;

typedef int(*Callback)(void *arg, int colNum, char **content, char **colName);

class CallbackBean {
public:
    JNIEnv *&env;
    jobject &callbackInterface;

    CallbackBean(JNIEnv *&env, jobject &callbackInterface) : env(env), callbackInterface(callbackInterface) {}
};

inline static int callback(void *arg, int colNum, char **content, char **colName) {
    auto *bean = (CallbackBean *) arg;
    jclass callbackClass = bean->env->GetObjectClass(bean->callbackInterface);
    jmethodID callbackMID = bean->env->GetMethodID(callbackClass, "callback",
                                               "([Ljava/lang/String;)I");
    jclass stringClass = bean->env->FindClass("java/lang/String");
    jobjectArray contentArray = bean->env->NewObjectArray(colNum, stringClass, nullptr);
    for (int i = 0; i < colNum; ++i) {
        jstring s = bean->env->NewStringUTF(content[i]);
        bean->env->SetObjectArrayElement(contentArray, i, s);
    }
    return (int) bean->env->CallIntMethod(bean->callbackInterface, callbackMID, contentArray);
}

class Sqlite3 {
private:
    sqlite3 *db;

public:
    int id;
    char *errMsg;

    inline int open(const char *path) {
        return sqlite3_open(path, &db);
    }

    inline int close() {
        return sqlite3_close(db);
    }

    inline int exec(const char *cmd, Callback callback) {
        return sqlite3_exec(db, cmd, callback, nullptr, &errMsg);
    }

    inline int exec(const char *cmd, Callback callback, void *arg) {
        return sqlite3_exec(db, cmd, callback, arg, &errMsg);
    }
};

ArrayList<Sqlite3 *> sqlArray; //NOLINT

void throwException(JNIEnv *env, const char *msg) {
    jclass exceptionClass = env->FindClass("java/lang/Exception");
    env->ThrowNew(exceptionClass, msg);
    env->DeleteLocalRef(exceptionClass);
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_createHandler
        (JNIEnv *env, jclass cls) {
    auto *db = new Sqlite3;
    sqlArray.add(db);
    db->id = sqlArray.length() - 1;
    return db->id;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_releaseHandler
        (JNIEnv *env, jclass cls, jint id) {
    delete sqlArray.remove(id);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_open
        (JNIEnv *env, jclass cls, jint id, jstring path) {
    const char *file = env->GetStringUTFChars(path, (jboolean *) nullptr);
    if (sqlArray.get(id)->open(file)) throwException(env, "Open or create database failed.");
    env->ReleaseStringUTFChars(path, file);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_close
        (JNIEnv *env, jclass cls, jint id) {
    if (sqlArray.get(id)->close()) throwException(env, "Close database failed.");
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_exec
        (JNIEnv *env, jclass cls, jint id, jstring cmd, jobject callbackInterface) {
    int dbId = (int) id;
    const char *command = env->GetStringUTFChars(cmd, (jboolean *) nullptr);
    if (callbackInterface == nullptr) {
        if (sqlArray.get(id)->exec(command, (Callback) nullptr)) {
            env->ReleaseStringUTFChars(cmd, command);
            throwException(env, sqlArray.get(id)->errMsg);
        }
    } else {
        CallbackBean bean(env, callbackInterface);
        if (sqlArray.get(id)->exec(command, callback, &bean)) {
            env->ReleaseStringUTFChars(cmd, command);
            throwException(env, sqlArray.get(id)->errMsg);
        }
    }
    env->ReleaseStringUTFChars(cmd, command);
}