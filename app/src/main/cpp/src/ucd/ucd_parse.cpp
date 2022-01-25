//
// Created by bczhc on 1/23/22.
//

#include <third_party/my-cpp-lib/sqlite3.hpp>
#include "../jni_h/pers_zhc_tools_jni_JNI_CharUcd.h"
#include <third_party/my-cpp-lib/io.hpp>
#include <cassert>
#include <third_party/my-cpp-lib/utils.hpp>
#include <third_party/jni-lib/src/jni_help.h>

using i32 = int32_t;

using namespace bczhc;

class ProgressCallback {
private:
    jmethodID callbackMId{};
    jobject callbackObj{};
    JNIEnv *env{};
    jclass callbackClass{};

public:
    explicit ProgressCallback(JNIEnv *env, jobject callbackJ) {
        this->env = env;
        auto objClass = env->GetObjectClass(callbackJ);
        callbackClass = objClass;
        callbackMId = env->GetMethodID(objClass, "progress", "(I)V");
        callbackObj = callbackJ;
    }

    ~ProgressCallback() {
        env->DeleteLocalRef(callbackClass);
    }

    void progress(i32 i) {
        env->CallVoidMethod(callbackObj, callbackMId, (jint) i);
    }
};

void writeDatabase(JNIEnv *env, jstring srcJS, jstring destJS, jobject progressCallbackJ) {
    auto src = env->GetStringUTFChars(srcJS, nullptr);
    auto dest = env->GetStringUTFChars(destJS, nullptr);

    auto progressCallback = ProgressCallback(env, progressCallbackJ);

    auto db = Sqlite3(dest);
    db.exec("CREATE TABLE IF NOT EXISTS ucd\n(\n    codepoint  INTEGER PRIMARY KEY,\n    properties TEXT NOT NULL\n)");
    db.exec("BEGIN TRANSACTION");

    auto statement = db.compileStatement("INSERT INTO ucd (codepoint, properties) VALUES (?, ?)");

    auto is = InputStream(src);
    auto reader = LineReader(is);
    i32 count = 0;
    while (true) {
        auto line = reader.readLine();
        if (line.isNull()) {
            break;
        }

        auto index = line.firstIndexOf(' ');
        assert(index != -1);
        auto codepointStr = line.substring(0, index);
        auto propJsonStr = line.substring(index + 1, line.length());
        auto codepoint = Integer::parseInt(codepointStr);

        statement.reset();
        statement.bind(1, codepoint);
        statement.bindText(2, propJsonStr.getCString(), SQLITE_TRANSIENT);
        statement.step();

        ++count;
        if (count % 1000 == 0) {
            progressCallback.progress(count);
        }
    }

    statement.release();
    db.exec("COMMIT");
    db.close();

    is.close();

    env->ReleaseStringUTFChars(srcJS, src);
    env->ReleaseStringUTFChars(destJS, dest);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024CharUcd_writeDatabase
        (JNIEnv *env,  jclass, jstring srcJS, jstring destJS, jobject progressCallbackJ) {
    try {
        writeDatabase(env, srcJS, destJS, progressCallbackJ);
    } catch (const std::exception &e) {
        throwException(env, "Exception caught: %s", e.what());
    }
}
