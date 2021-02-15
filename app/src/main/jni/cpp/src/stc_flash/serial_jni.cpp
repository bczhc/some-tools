//
// Created by zhc on 2/1/21.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_StcFlash.h"
#include "serial_jni.h"
#include "../jni_help.h"

using namespace bczhc;

SArray<uchar> serial::jniImpl::read(JNIEnv *&env, int size, jobject &jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "read", "(I)[B");
    auto b = (jbyteArray) env->CallObjectMethod(jniInterface, mid, (jint) size);
    const jsize length = env->GetArrayLength(b);
    SArray<uchar> r(length);
    jbyte *arr = env->GetByteArrayElements(b, nullptr);
    for (int i = 0; i < length; ++i) {
        r[i] = arr[i];
    }
    env->ReleaseByteArrayElements(b, arr, 0);
    env->DeleteLocalRef(cls), env->DeleteLocalRef(b);
    return r;
}

ssize_t serial::jniImpl::write(JNIEnv *&env, uchar *buf, ssize_t size, jobject &jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "write", "([B)I");
    jbyteArray arr = env->NewByteArray((int32_t) size);
    for (int i = 0; i < size; ++i) {
        assert(sizeof(uchar) == sizeof(const int8_t));
        env->SetByteArrayRegion(arr, (int32_t) 0, (int32_t) size, (const int8_t *) buf);
    }
    const jint &readLen = env->CallIntMethod(jniInterface, mid, arr);
    Array<uchar> w((int) size);
    for (int i = 0; i < size; ++i) {
        w[i] = buf[i];
    }
    log(env, "jni", "read: %s", w.toString().getCString());
    env->DeleteLocalRef(cls), env->DeleteLocalRef(arr);
    return (ssize_t) readLen;
}

void serial::jniImpl::setSpeed(JNIEnv *&env, unsigned int speed, jobject jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "setSpeed", "(I)V");
    env->CallVoidMethod(jniInterface, mid, (jint) speed);
    env->DeleteLocalRef(cls);
}

void serial::jniImpl::close(JNIEnv *&env, jobject jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "close", "()V");
    env->CallVoidMethod(jniInterface, mid);
    env->DeleteLocalRef(cls);
}

unsigned int serial::jniImpl::getBaud(JNIEnv *&env, jobject jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "getBaud", "()I");
    const jint &r = env->CallIntMethod(jniInterface, mid);
    env->DeleteLocalRef(cls);
    return (int) r;
}

void serial::jniImpl::setTimeout(JNIEnv *&env, uint32_t timeout, jobject jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "setTimeout", "(I)V");
    env->CallVoidMethod(jniInterface, mid, (jint) timeout);
    env->DeleteLocalRef(cls);
}

void serial::jniImpl::setParity(JNIEnv *&env, char p, jobject jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "setParity", "(B)V");
    env->CallVoidMethod(jniInterface, mid, (jbyte) p);
    env->DeleteLocalRef(cls);
}

char serial::jniImpl::getParity(JNIEnv *&env, jobject jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "getParity", "()B");
    jbyte r = env->CallByteMethod(jniInterface, mid);
    env->DeleteLocalRef(cls);
    return (char) r;
}

uint32_t serial::jniImpl::getTimeout(JNIEnv *env, jobject jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "getTimeout", "()I");
    jint r = env->CallIntMethod(jniInterface, mid);
    env->DeleteLocalRef(cls);
    return (uint32_t) r;
}

void serial::jniImpl::flush(JNIEnv *&env, jobject &jniInterface) {
    jclass cls = env->GetObjectClass(jniInterface);
    jmethodID mid = env->GetMethodID(cls, "flush", "()V");
    env->CallVoidMethod(jniInterface, mid);
    env->DeleteLocalRef(cls);
}

SArray<uchar> serial::SerialJNI::read(ssize_t size) const {
    SArray<uchar> r = jniImpl::read(this->env, (int) size, this->jniInterface);
    log(env, "jni-read", "size: %zd, read %s", size, r->toString().getCString());
    return r;
}

ssize_t serial::SerialJNI::write(uchar *buf, ssize_t size) const {
    return jniImpl::write(this->env, buf, size, this->jniInterface);
}

uint32_t serial::SerialJNI::getTimeout() const {
    return jniImpl::getTimeout(this->env, this->jniInterface);
}

void serial::SerialJNI::setSpeed(unsigned int speed) {
    jniImpl::setSpeed(this->env, speed, this->jniInterface);
}

void serial::SerialJNI::close() const {
    jniImpl::close(this->env, this->jniInterface);
}

unsigned int serial::SerialJNI::getBaud() const {
    return jniImpl::getBaud(this->env, this->jniInterface);
}

void serial::SerialJNI::flush() const {
    jniImpl::flush(this->env, this->jniInterface);
}

void serial::SerialJNI::setTimeout(uint32_t t) {
    jniImpl::setTimeout(this->env, t, this->jniInterface);
}

void serial::SerialJNI::setParity(char p) {
    return jniImpl::setParity(this->env, p, this->jniInterface);
}

char serial::SerialJNI::getParity() const {
    return jniImpl::getParity(this->env, this->jniInterface);
}

serial::SerialJNI::SerialJNI(JNIEnv *&env, jobject &jniInterface, const String &portName) : env(env), jniInterface(jniInterface), portName(portName) {}

String serial::SerialJNI::getPortName() const {
    return this->portName;
}
