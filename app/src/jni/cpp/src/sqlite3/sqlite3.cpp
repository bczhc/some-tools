#include "../../third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_Sqlite3.h"
#include "../../third_party/my-cpp-lib/Sqlite3.h"
#include "../jni_help.h"

using namespace bczhc;

class CallbackBean {
public:
    JNIEnv *&env;
    jobject &callbackInterface;

    CallbackBean(JNIEnv *&env, jobject &callbackInterface) : env(env), callbackInterface(callbackInterface) {}
};

class CB : public Sqlite3::SqliteCallback {
public:
    int callback(void *arg, int colNum, char **content, char **colName) override {
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
} callback;

void throwException(JNIEnv *env, const char *msg) {
    jclass exceptionClass = env->FindClass("java/lang/Exception");
    env->ThrowNew(exceptionClass, msg);
    env->DeleteLocalRef(exceptionClass);
}

JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_open
        (JNIEnv *env, jclass cls, jstring path) {
    auto *db = new Sqlite3;
    const char *file = env->GetStringUTFChars(path, (jboolean *) nullptr);
    if (db->open(file)) {
        throwException(env, "Open or create database failed.");
    }
    env->ReleaseStringUTFChars(path, file);
    return (jlong) db;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_close
        (JNIEnv *env, jclass cls, jlong id) {
    auto *db = (Sqlite3 *) id;
    if (db->close()) {
        throwException(env, "Close database failed");
    }
    delete db;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Sqlite3_exec
        (JNIEnv *env, jclass cls, jlong id, jstring cmd, jobject callbackInterface) {
    auto *db = (Sqlite3 *) id;
    const char *command = env->GetStringUTFChars(cmd, (jboolean *) nullptr);
    if (callbackInterface == nullptr) {
        if (db->exec(command)) {
            throwException(env, db->errMsg);
        }
    } else {
        CallbackBean bean(env, callbackInterface);
        if (db->exec(command, callback, &bean)) {
            throwException(env, db->errMsg);
        }
    }
    env->ReleaseStringUTFChars(cmd, command);
}