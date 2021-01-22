//
// Created by root on 19-10-2.
//

#ifndef JNI_JNI_HELP_H
#define JNI_JNI_HELP_H

#include <jni.h>
#include "../third_party/my-cpp-lib/string.h"
using namespace bczhc::string;

//void Log(JNIEnv *env, const String& tag, const String& msg);

void Log(JNIEnv *env, const char *tag, const char *msg);

#endif //JNI_JNI_HELP_H