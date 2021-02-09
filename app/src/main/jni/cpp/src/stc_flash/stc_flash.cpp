//
// Created by zhc on 1/31/21.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_StcFlash.h"
#include "../../third_party/my-cpp-lib/app/stc_flash/stc_flash_lib.h"
#include "../jni_help.h"
#include "serial_jni.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "LocalValueEscapesScope"

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024StcFlash_burn
        (JNIEnv *env, jclass, jstring portNameJS, jstring hexFilePathS, jobject jniInterface, jobject echoCallback) {
    class CB : public EchoCallback {
    private:
        JNIEnv *&env;
        jobject &echoCallback;

        jclass cls;
        jmethodID printMId, flushMid;
    public:
        CB(JNIEnv *&env, jobject &echoCallback) : env(env), echoCallback(echoCallback) {
            cls = env->GetObjectClass(echoCallback);
            printMId = env->GetMethodID(cls, "print", "(Ljava/lang/String;)V");
            flushMid = env->GetMethodID(cls, "flush", "()V");
        }

        void print(const char *s) override {
            jstring jString = env->NewStringUTF(s);
            env->CallVoidMethod(echoCallback, printMId, jString);
        }

        void flush() override {
            env->CallVoidMethod(echoCallback, flushMid);
        }

        ~CB() {
            env->DeleteLocalRef(cls);
        }
    } cb(env, echoCallback);
    try {
        const char *hexFilePath = env->GetStringUTFChars(hexFilePathS, nullptr);
        const char* portNameCS = env->GetStringUTFChars(portNameJS, nullptr);
        String portName = portNameCS;
        serial::SerialJNI serialImpl(env, jniInterface, portName);

        run(hexFilePath, &cb, &serialImpl);
        env->ReleaseStringUTFChars(hexFilePathS, hexFilePath);
        env->ReleaseStringUTFChars(portNameJS, portNameCS);
    } catch (const String &e) {
        cb.print(e.getCString()), cb.flush();
        jnihelp::log(env, "jni exception", e.getCString());
        jclass exceptionClass = env->FindClass("java/lang/Exception");
        String msg = "JNI error: ";
        msg += e;
        env->ThrowNew(exceptionClass, msg.getCString());
        env->DeleteLocalRef(exceptionClass);
    }
}

#pragma clang diagnostic pop