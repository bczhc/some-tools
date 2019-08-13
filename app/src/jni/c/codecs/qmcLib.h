//
// Created by zhc-2 on 2019/6/18.
//

#ifndef C99_QMCLIB_H
#define C99_QMCLIB_H

#endif //C99_QMCLIB_H


#include <jni.h>
#include "../../zhc.h"


//
// Created by zhc-2 on 2019/6/18.
//



/*
char nextMask() {
    char ret;
    ++i;
    if (x < 0) {
        dx = 1;
        y = ((8 - y) % 8);
        ret = 0xc3;
    } else if (x > 6) {
        dx = -1;
        y = 7 - y;
        ret = 0xd8;
    } else {
        ret = seedMap[y][x];
    }

    x += dx;
    if (i == 0x8000 || (i > 0x8000 && (i + 1) % 0x8000 == 0)) {
        return nextMask();
    }
    return ret;
}
*/

void Log(JNIEnv *env, const char *s);

void callMethod(JNIEnv *env, jmethodID id, char *s, double d, jobject obj);

char nextMask_();

int decode(const char *fileName, const char *destFileName, JNIEnv *env, jmethodID id, jobject obj);