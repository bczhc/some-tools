//
// Created by root on 19-10-2.
//

#include "jni_help.h"
#include "../third_party/my-cpp-lib/String.h"
using namespace bczhc;

void Log(JNIEnv *env, const char *tag, const char* msg) {
    if (env == nullptr) {
        printf("%s: %s\n", tag, msg);
    } else {
        jstring str = env->NewStringUTF(msg);
        jstring tagS = env->NewStringUTF(tag);
        jclass mClass = env->FindClass("android/util/Log");
        jmethodID mid = env->GetStaticMethodID(mClass, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
        env->CallStaticIntMethod(mClass, mid, tagS, str);
        env->DeleteLocalRef(str);
        env->DeleteLocalRef(tagS);
        env->DeleteLocalRef(mClass);
    }
}

//void Log(JNIEnv *env, const String& tag, const String& msg) {
//    Log(env, tag.getCString(), msg.getCString());
//}