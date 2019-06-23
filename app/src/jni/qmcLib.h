//
// Created by zhc-2 on 2019/6/18.
//

#ifndef C99_QMCLIB_H
#define C99_QMCLIB_H

#endif //C99_QMCLIB_H

#include "./zhc.h"


//
// Created by zhc-2 on 2019/6/18.
//
void Log(JNIEnv *env, const char *s) {
    JNIEnv e = *env;
    jstring str = e->NewStringUTF(env, s);
    jstring tagS = e->NewStringUTF(env, "JNILog");
    jclass mClass = e->FindClass(env, "android/util/Log");
    jmethodID mid = e->GetStaticMethodID(env, mClass, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
    e->CallStaticIntMethod(env, mClass, mid, tagS, str);
}

void callMethod(JNIEnv *env, jclass c, jmethodID id, char *s, double d) {
    jstring str = (*env)->NewStringUTF(env, s);
    (*env)->CallStaticVoidMethod(env, c, id, str, (jdouble) d);
}

char seedMap[8][7] = {
        {0x4a, 0xd6, 0xca, 0x90, 0x67, 0xf7, 0x52},
        {0x5e, 0x95, 0x23, 0x9f, 0x13, 0x11, 0x7e},
        {0x47, 0x74, 0x3d, 0x90, 0xaa, 0x3f, 0x51},
        {0xc6, 0x09, 0xd5, 0x9f, 0xfa, 0x66, 0xf9},
        {0xf3, 0xd6, 0xa1, 0x90, 0xa0, 0xf7, 0xf0},
        {0x1d, 0x95, 0xde, 0x9f, 0x84, 0x11, 0xf4},
        {0x0e, 0x74, 0xbb, 0x90, 0xbc, 0x3f, 0x92},
        {0x00, 0x09, 0x5b, 0x9f, 0x62, 0x66, 0xa1}
};

int x = -1;
int y = 8;
int dx = 1;
int i = -1;

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

char nextMask_() {
    char ret;
    while (1) {
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
        if (!(i == 0x8000 || (i > 0x8000 && !((i + 1) % 0x8000))))
            break;
    }
    return ret;
}

int decode(const char *fileName, const char *destFileName, JNIEnv *env, jclass mClass, jmethodID id) {
    callMethod(env, mClass, id, "", (double) 0);
    FILE *fp, *fpO;
    if ((fp = fopen(fileName, "rb")) == NULL) return -1;
    if ((fpO = fopen(destFileName, "wb")) == NULL) return -1;
    char c[1024] = {0};
    dl fL = getFileSize(fp), a = fL / 1024;
    if (!fL)
        return 0;
    usi p = fL / 20480;
    int b = (int) (fL % 1024);
    for (int j = 0; j < a; ++j) {
        fread(c, 1024, 1, fp);
        for (int k = 0; k < 1024; ++k) {
            c[k] ^= nextMask_();
        }
        fwrite(c, 1024, 1, fpO);
        if (!(j % p)) callMethod(env, mClass, id, "", ((double) j) / (double) a * 100);
    }
    if (b) {
        fread(c, b, 1, fp);
        for (int j = 0; j < b; ++j) {
            c[j] ^= nextMask_();
        }
        fwrite(c, b, 1, fpO);
    }
    return 0;
}