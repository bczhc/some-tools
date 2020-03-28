#ifndef CODECS_DO_H
#define CODECS_DO_H

#include "./Base128Lib.h"
#include "./qmcLib.h"
#include "../jni_help.h"
#include "./kwm.h"
#include "../jni_h/pers_zhc_tools_jni_JNI_Codecs.h"

void Callback(JNIEnv *env, jobject callback, const char *str, double d);

#endif