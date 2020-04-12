//
// Created by zhc-2 on 2019/6/18.
//

#ifndef C99_QMCLIB_H
#define C99_QMCLIB_H

#include <jni.h>
#include "../zhc.h"


//
// Created by zhc-2 on 2019/6/18.
//


char nextMask_();

int decode(const char *filename, const char *destFileName, JNIEnv *env, jobject callback);

#endif //C99_QMCLIB_H