//
// Created by root on 19-7-3.
//
#include "codecsDo.h"
#include "../../third_party/my-cpp-lib/file.hpp"

using namespace bczhc;

#define dl int64_t

char seedMap[8][7] = {
        {(char) 0x4a, (char) 0xd6, (char) 0xca, (char) 0x90, (char) 0x67, (char) 0xf7, (char) 0x52},
        {(char) 0x5e, (char) 0x95, (char) 0x23, (char) 0x9f, (char) 0x13, (char) 0x11, (char) 0x7e},
        {(char) 0x47, (char) 0x74, (char) 0x3d, (char) 0x90, (char) 0xaa, (char) 0x3f, (char) 0x51},
        {(char) 0xc6, (char) 0x09, (char) 0xd5, (char) 0x9f, (char) 0xfa, (char) 0x66, (char) 0xf9},
        {(char) 0xf3, (char) 0xd6, (char) 0xa1, (char) 0x90, (char) 0xa0, (char) 0xf7, (char) 0xf0},
        {(char) 0x1d, (char) 0x95, (char) 0xde, (char) 0x9f, (char) 0x84, (char) 0x11, (char) 0xf4},
        {(char) 0x0e, (char) 0x74, (char) 0xbb, (char) 0x90, (char) 0xbc, (char) 0x3f, (char) 0x92},
        {(char) 0x00, (char) 0x09, (char) 0x5b, (char) 0x9f, (char) 0x62, (char) 0x66, (char) 0xa1}
};

int x = -1;
int y = 8;
int dx = 1;
int i = -1;

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

int decode(const char *filename, const char *destFileName, JNIEnv *env, jobject callback) {
    x = -1;
    y = 8;
    dx = 1;
    i = -1;
    Callback(env, callback, "", (double) 0);
    FILE *fp, *fpO;
    if ((fp = fopen(filename, "rb")) == NULL) return -1;
    if ((fpO = fopen(destFileName, "wb")) == NULL) return -1;
    char c[1024] = {0};
    dl fL = bczhc::File::getFileSize(fp), a = fL / 1024;
    if (!fL)
        return 0;
    uint32_t p = fL / 20480;
    int b = (int) (fL % 1024);
    for (int j = 0; j < a; ++j) {
        fread(c, 1024, 1, fp);
        for (int k = 0; k < 1024; ++k) {
            c[k] ^= nextMask_();
        }
        fwrite(c, 1024, 1, fpO);
        if (!(j % p)) Callback(env, callback, "", ((double) j) / (double) a * 100);
    }
    if (b) {
        fread(c, b, 1, fp);
        for (int j = 0; j < b; ++j) {
            c[j] ^= nextMask_();
        }
        fwrite(c, b, 1, fpO);
    }
    fclose(fp);
    fclose(fpO);
    Callback(env, callback, "", (double) 100);
    return 0;
}
