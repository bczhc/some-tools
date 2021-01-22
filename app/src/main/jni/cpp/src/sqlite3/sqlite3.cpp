#include "../../third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_Sqlite3.h"
#include "../../third_party/my-cpp-lib/sqlite3.h"
#include "../jni_help.h"
#include "../../third_party/my-cpp-lib/string.h"

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
        (JNIEnv *env, jclass cls, jstring path) {
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
        (JNIEnv *env, jclass cls, jlong id) {
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
        (JNIEnv *env, jclass cls, jlong id, jstring cmd, jobject callbackInterface) {

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