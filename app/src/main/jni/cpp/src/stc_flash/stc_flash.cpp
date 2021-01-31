//
// Created by zhc on 1/31/21.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_StcFlash.h"
#include "../../third_party/my-cpp-lib/app/stc_flash/stc_flash_lib.h"
#include "../jni_help.h"

static JNIEnv **envRef = nullptr;
static jobject *jniInterfaceRef = nullptr;

namespace jniImpl {
    Array<uchar> read(JNIEnv *&env, int size, jobject &jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "read", "(I)[B");
        auto b = (jbyteArray) env->CallObjectMethod(jniInterface, mid, (jint) size);
        const jsize length = env->GetArrayLength(b);
        Array<uchar> r(length);
        for (int i = 0; i < length; ++i) {
            r[i] = (uchar) *(env->GetByteArrayElements(b, nullptr));
        }
        env->DeleteLocalRef(cls), env->DeleteLocalRef(b);
        return r;
    }

    ssize_t write(JNIEnv *&env, uchar *buf, ssize_t size, jobject &jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "write", "([B)I");
        jbyteArray arr = env->NewByteArray((int32_t) size);
        for (int i = 0; i < size; ++i) {
            assert(sizeof(uchar) == sizeof(const int8_t));
            env->SetByteArrayRegion(arr, (int32_t) 0, (int32_t) size, (const int8_t *) buf);
        }
        const jint &readLen = env->CallIntMethod(jniInterface, mid, arr);
        env->DeleteLocalRef(cls);
        return (ssize_t) readLen;
    }

    void setSpeed(JNIEnv *env, unsigned int speed, jobject jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "setSpeed", "(I)V");
        env->CallVoidMethod(jniInterface, mid, (jint) speed);
        env->DeleteLocalRef(cls);
    }

    void close(JNIEnv *env, jobject jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "close", "()V");
        env->CallVoidMethod(jniInterface, mid);
        env->DeleteLocalRef(cls);
    }

    unsigned int getBaud(JNIEnv *env, jobject jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "getBaud", "()I");
        const jint &r = env->CallIntMethod(jniInterface, mid);
        env->DeleteLocalRef(cls);
        return (int) r;
    }

    void setTimeout(JNIEnv *env, uint32_t timeout, jobject jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "setTimeout", "(I)V");
        env->CallVoidMethod(jniInterface, mid, (jint) timeout);
        env->DeleteLocalRef(cls);
    }

    void setParity(JNIEnv *env, char p, jobject jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "setParity", "(B)V");
        env->CallVoidMethod(jniInterface, mid, (jbyte) p);
        env->DeleteLocalRef(cls);
    }

    char getParity(JNIEnv *env, jobject jniInterface) {
        jclass cls = env->GetObjectClass(jniInterface);
        jmethodID mid = env->GetMethodID(cls, "getParity", "()B");
        jbyte r = env->CallByteMethod(jniInterface, mid);
        env->DeleteLocalRef(cls);
        return (char) r;
    }
}

Array<uchar> Serial::read(ssize_t size) {
    return jniImpl::read(*envRef, (int) size, *jniInterfaceRef);
}

ssize_t Serial::write(uchar *buf, ssize_t size) {
    return jniImpl::write(*envRef, buf, size, *jniInterfaceRef);
}

Serial::Serial(int, uint32_t baud, uint32_t timeout) {

}

Serial Serial::open(const char *, uint32_t timeout) {
    return Serial(0, 9600, timeout);
}

void Serial::setSpeed(unsigned int speed) {
    jniImpl::setSpeed(*envRef, speed, *jniInterfaceRef);
}

void Serial::close() {
    jniImpl::close(*envRef, *jniInterfaceRef);
}

unsigned int Serial::getBaud() {
    return jniImpl::getBaud(*envRef, *jniInterfaceRef);
}

void Serial::flush() const {
    // empty implementation
}

void Serial::setTimeout(uint32_t t) {
    jniImpl::setTimeout(*envRef, t, *jniInterfaceRef);
}

void Serial::setParity(char p) {
    return jniImpl::setParity(*envRef, p, *jniInterfaceRef);
}

char Serial::getParity() {
    return jniImpl::getParity(*envRef, *jniInterfaceRef);
}

#pragma clang diagnostic push
#pragma ide diagnostic ignored "LocalValueEscapesScope"

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024StcFlash_burn
        (JNIEnv *env, jclass, jstring hexFilePathS, jobject jniInterface) {
    try {
        const char *hexFilePath = env->GetStringUTFChars(hexFilePathS, nullptr);
        envRef = &env;
        jniInterfaceRef = &jniInterface;
        run(hexFilePath);
        env->ReleaseStringUTFChars(hexFilePathS, hexFilePath);
    } catch (const String &e) {
        Log(env, "jni exception", e.getCString());
        jclass exceptionClass = env->FindClass("java/lang/Exception");
        String msg = "JNI error: ";
        msg += e;
        env->ThrowNew(exceptionClass, msg.getCString());
        env->DeleteLocalRef(exceptionClass);
    }
}

#pragma clang diagnostic pop