//
// Created by zhc-2 on 2019/6/19.
//

#include <stdio.h>
#include "qmcLib.h"
#include "com_zhc_JNI.h"
#include "Base128Lib.h"

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
JNIEXPORT jint JNICALL Java_com_zhc_JNI_qmcDecode
        (JNIEnv *env, jobject obj, jstring f, jstring dF) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *f1 = e->GetStringUTFChars(env, f, (jboolean *) 0);
    const char *f2 = e->GetStringUTFChars(env, dF, (jboolean *) 0);
    char *sQ = NULL;
    strcat_auto(&sQ, f1);
    strcat_auto(&sQ, " => ");
    strcat_auto(&sQ, f2);
    Log(env, sQ);
    Log(env, "JNI————解码……");
    int rC = decode(f1, f2, env, mClass, mid, obj);
    return (jint) rC;
}


#define MAX_FIND_KEY_TIME 468


JNIEXPORT jint JNICALL Java_com_zhc_JNI_kwmDecode
        (JNIEnv *env, jobject obj, jstring f, jstring dF) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *fN = e->GetStringUTFChars(env, f, (jboolean *) 0);
    const char *dFN = e->GetStringUTFChars(env, dF, (jboolean *) 0);

    int haveFoundKey = 0;
    FILE *fp = NULL, *fpO = NULL;
    if ((fp = fopen(fN, "rb")) == NULL) {
        printf("fopen error.");
        return -1;
    }
    if ((fpO = fopen(dFN, "wb")) == NULL) {
        printf("fopen error.");
        return -1;
    }
    char key[32] = {0}, old_key[32] = {0};
    dl fS = getFileSize(fp), a = fS / 1024;
    usi b = (usi) fS % 1024;
    fseek(fp, 1024L, SEEK_SET);
    usi p = fS / 20480;
    for (int i = 0; i < MAX_FIND_KEY_TIME; ++i) {
        fread(key, 32, 1, fp);
        int cmpR = cmpCharArray(key, 32, old_key, 32);
        if (cmpR) {
            haveFoundKey = 1;
            break;
        }
        for (int j = 0; j < 32; ++j) {
            old_key[j] = key[j];
        }
    }
    if (!haveFoundKey) {
        char *newKey = NULL;
        newKey = (char *) malloc(32);
        for (int i = 0; i < 16; ++i) {
            newKey[i] = key[i + 16];
        }
        for (int j = 16; j < 32; ++j) {
            newKey[j] = key[j - 16];
        }
        for (int k = 0; k < 32; ++k) {
            key[k] = newKey[k];
        }
        free(newKey);
    }
    fseek(fp, 1024L, SEEK_SET);
    printf("key: ");
    PrintArr(key, ARR_len(key));
    printf("\n");
    int allZero = 1;
    for (int m = 0; m < 32; ++m) {
        if (key[m]) {
            allZero = 0;
            break;
        }
    }
    if (allZero) return 2;
    char buf[1024] = {0};
    for (int l = 1; l < a; ++l) {
        fread(buf, 1024, 1, fp);
        for (int i = 0; i < 1024; ++i) {
            buf[i] ^= key[i & 31];
        }
        fwrite(buf, 1024, 1, fpO);
        if (!(l % p)) callMethod(env, mid, "", ((double) l) / (double) a * 100, obj);
    }
    if (b) {
        fread(buf, b, 1, fp);
        for (int i = 0; i < b; ++i) {
            buf[i] ^= key[i & 31];
        }
        fwrite(buf, b, 1, fpO);
    }
    return 0;
}

JNIEXPORT void JNICALL Java_com_zhc_JNI_Base128_1encode
        (JNIEnv *env, jobject obj, jstring f1, jstring f2) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *FileName = (*env)->GetStringUTFChars(env, f1, (jboolean *) NULL);
    const char *DestFileName = (*env)->GetStringUTFChars(env, f2, (jboolean *) NULL);
    eD(FileName, DestFileName, env, obj, mid);
}

JNIEXPORT void JNICALL Java_com_zhc_JNI_Base128_1decode
        (JNIEnv *env, jobject obj, jstring f1, jstring f2) {
    JNIEnv e = *env;
    jclass mClass = e->GetObjectClass(env, obj);
    jmethodID mid = e->GetMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *FileName = (*env)->GetStringUTFChars(env, f1, (jboolean *) NULL);
    const char *DestFileName = (*env)->GetStringUTFChars(env, f2, (jboolean *) NULL);
    dD(FileName, DestFileName, env, obj, mid);
}
#pragma clang diagnostic pop