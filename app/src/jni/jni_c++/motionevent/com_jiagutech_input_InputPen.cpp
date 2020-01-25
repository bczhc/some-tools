//
// Created by root on 2020/1/25.
//
#include <cstdlib>
#include <malloc.h>
#include <jni.h>
#include "JNIHelper.h"
#include "ALog.h"
#include "android_runtime/AndroidRuntime.h"

#include "input_pen.h"
#include "debug.h"

//Java类名
#define CLASS_NAME "com/jiagutech/input/InputPen"

using namespace android;


static jobject mCallbacksObj = NULL;
static jmethodID method_get_event;


static void checkAndClearExceptionFromCallback(JNIEnv *env, const char *methodName) {
    if (env->ExceptionCheck()) {
        LOGE("An exception was thrown by callback '%s'.", methodName);
        LOGE_EX(env);
        env->ExceptionClear();
    }
}

//获得input_event数据的回调函数
static void GetEventCallback(__u16 type, __u16 code, __s32 value) {
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    //invoke java callback method
    env->CallVoidMethod(mCallbacksObj, method_get_event, type, code, value);

    checkAndClearExceptionFromCallback(env, __FUNCTION__);
}

//创建线程的回调函数
static pthread_t CreateThreadCallback(const char *name, void (*start)(void *), void *arg) {
    return (pthread_t) AndroidRuntime::createJavaThread(name, start, arg);
}

//释放线程资源的回调函数
static int DetachThreadCallback(void) {
    JavaVM *vm;
    jint result;

    vm = AndroidRuntime::getJavaVM();
    if (vm == NULL) {
        LOGE("detach_thread_callback :getJavaVM failed\n");
        return -1;
    }

    result = vm->DetachCurrentThread();
    if (result != JNI_OK)
        LOGE("ERROR: thread detach failed\n");
    return result;
}


//回调函数结构体变量
static input_callback mCallbacks = {
        GetEventCallback,
        CreateThreadCallback,
        DetachThreadCallback,
};

//初始化Java的回调函数
static void jni_class_init_native
        (JNIEnv *env, jclass clazz) {
    LOGD("jni_class_init_native");

    method_get_event = env->GetMethodID(clazz, "getEvent", "(III)V");


}

//初始化
static jboolean jni_input_pen_init
        (JNIEnv *env, jobject obj) {
    LOGD("jni_input_pen_init");

    if (!mCallbacksObj)
        mCallbacksObj = env->NewGlobalRef(obj);

    return input_pen_init(&mCallbacks);
}


//退出
static void jni_input_pen_exit
        (JNIEnv *env, jobject obj) {
    LOGD("jni_input_pen_exit");
    input_pen_exit();
}

static const JNINativeMethod gMethods[] = {
        {"class_init_native",     "()V", (void *) jni_class_init_native},
        {"native_input_pen_init", "()Z", (void *) jni_input_pen_init},
        {"native_input_pen_exit", "()V", (void *) jni_input_pen_exit},
};


static int registerMethods(JNIEnv *env) {


    const char *const kClassName = CLASS_NAME;
    jclass clazz;
    /* look up the class */
    clazz = env->FindClass(kClassName);
    if (clazz == NULL) {
        LOGE("Can't find class %s/n", kClassName);
        return -1;
    }
    /* register all the methods */
    if (env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) != JNI_OK) {
        LOGE("Failed registering methods for %s/n", kClassName);
        return -1;
    }
    /* fill out the rest of the ID cache */
    return 0;
}


jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jint result = -1;
    LOGI("InputPen JNI_OnLoad");
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed/n");
        goto fail;
    }

    if (env == NULL) {
        goto fail;
    }
    if (registerMethods(env) != 0) {
        LOGE("ERROR: PlatformLibrary native registration failed/n");
        goto fail;
    }
    /* success -- return valid version number */
    result = JNI_VERSION_1_4;
    fail:
    return result;
}