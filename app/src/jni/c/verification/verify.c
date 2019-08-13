//
// Created by root on 19-8-13.
//

#include "../../zhc.h"
#include "./com_zhc_tools_floatingboard_JNI.h"
#include "../codecs/qmcLib.h"

char b128_e_table_l[] = {1, 3, 7, 15, 31, 63, 127};

char encodeTable[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

char *e1_1(char *Dest, const char cArr[3]) {
    char *r = Dest;
    r[0] = encodeTable[(cArr[0] & 255) >> 2];
    r[1] = encodeTable[(((cArr[0] & 255) & 3) << 4) | ((cArr[1] & 255) >> 4)];
    r[2] = encodeTable[(((cArr[1] & 255) & 15) << 2) | ((cArr[2] & 255) >> 6)];
    r[3] = encodeTable[(cArr[2] & 255) & 63];
    return Dest;
}

void b128_e1(char *Dest, const char buf[7]) {
    Dest[0] = (char) ((buf[0] & 255) >> 1);
    /*Dest[1] = (char) (((buf[0] & 1) << 6) | ((buf[1] & 255) >> 2));
    Dest[2] = (char) (((buf[1] & 3) << 5) | ((buf[2] & 255) >> 3));
    Dest[3] = (char) (((buf[2] & 7) << 4) | ((buf[3] & 255) >> 4));
    Dest[4] = (char) (((buf[3] & 15) << 3) | ((buf[4] & 255) >> 5));
    Dest[5] = (char) (((buf[4] & 31) << 2) | ((buf[5] & 255) >> 6));
    Dest[6] = (char) (((buf[5] & 63) << 1) | ((buf[6] & 255) >> 7));*/
    for (int i = 1; i < 7; ++i) {
        Dest[i] = (char) (((buf[i - 1] & b128_e_table_l[i - 1]) << (7 - i)) | ((buf[i] & 255) >> (i + 1)));
    }
    Dest[7] = (char) (buf[6] & 127);
}

void e1(char *r, const char *s, int eCSize) {
    int j = eCSize / 3;
    for (int i = 0; i < j; ++i) {
        e1_1(r + 4 * i, s + 3 * i);
    }
}

void LogArr(JNIEnv *env, char *arr, int size) {
    char *r = NULL;
    strcpyAndCat_auto(&r, "[", "");
    for (int i = 0; i < size; ++i) {
        char *d = NULL;
        m_itoa(&d, (int) arr[i]);
        strcat_auto(&r, d);
        strcat_auto(&r, " ");
    }
    strcat_auto(&r, "]");
    Log(env, r);
}

JNIEXPORT jstring JNICALL Java_com_zhc_tools_floatingboard_JNI_mG
        (JNIEnv *env, jobject obj, jobject ctx) {
    JNIEnv e = *env;
    jclass clz = e->GetObjectClass(env, obj);
    jclass DateClass = e->FindClass(env, "java/util/Date");
    jmethodID DateMId = e->GetMethodID(env, DateClass, "<init>", "()V");
    jobject DateObj = e->NewObject(env, DateClass, DateMId);
    jclass sdfClass = e->FindClass(env, "java/text/SimpleDateFormat");
    jmethodID sdfMId = e->GetMethodID(env, sdfClass, "<init>", "(Ljava/lang/String;)V");
    jobject sdfObj = e->NewObject(env, sdfClass, sdfMId, e->NewStringUTF(env, "yyMMdd"));
    jmethodID sdfFormatMId = e->GetMethodID(env, sdfClass, "format", "(Ljava/util/Date;)Ljava/lang/String;");
    jobject strObj = e->CallObjectMethod(env, sdfObj, sdfFormatMId, DateObj);
    const char *d = e->GetStringUTFChars(env, (jstring) strObj, (jboolean *) 0);
    char r[8];
    e1(r, d, 6);
    char rr[9];
    b128_e1(rr, r);
    rr[8] = r[7];
    for (int i = 0; i < 9; ++i) {
        rr[i] ^= d[i % 6];
    }
    char rrr[12];
    e1(rrr, rr, 9);
    char L[13];
    memset(L, 0, 13);
    for (int i = 0; i < 12; ++i) {
        L[i] = rrr[i];
    }
    if (!strcmp(L, "F2w9DwgIOXAB")) {
        jclass ctxClass = e->GetObjectClass(env, ctx);
        jmethodID ctxMId = e->GetMethodID(env, ctxClass, "finish", "()V");
        e->CallVoidMethod(env, ctx, ctxMId);
        Log(env, "verification.........\n"
                 "高中。拼搏 !\n");
    }
    return e->NewStringUTF(env, L);
}