//
// Created by root on 19-8-13.
//

#include "../../zhc.h"
#include "./com_zhc_tools_floatingboard_JNI.h"
#include "../codecs/qmcLib.h"
#include <stdio.h>

char encodeTable[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
char b128_e_table_l[] = {1, 3, 7, 15, 31, 63, 127};

char *e1_1(char *Dest, const char cArr[3]) {
    char *r = Dest;
    r[0] = encodeTable[(cArr[0] & 255) >> 2];
    r[1] = encodeTable[(((cArr[0] & 255) & 3) << 4) | ((cArr[1] & 255) >> 4)];
    r[2] = encodeTable[(((cArr[1] & 255) & 15) << 2) | ((cArr[2] & 255) >> 6)];
    r[3] = encodeTable[(cArr[2] & 255) & 63];
    return Dest;
}

void ab_ks(char *Dest, const char *src, int size, int o) {
    for (int i = 0; i < size; ++i) Dest[i] = (char) (97 + (src[i] - 97 + o) % 26);
}

void b128_e1(char *Dest, const char buf[7]) {
    Dest[0] = (char) ((buf[0] & 255) >> 1);
    for (int i = 1; i < 7; ++i)
        Dest[i] = (char) (((buf[i - 1] & b128_e_table_l[i - 1]) << (7 - i)) | ((buf[i] & 255) >> (i + 1)));
    Dest[7] = (char) (buf[6] & 127);
}

void b128_d1(char *Dest, const char buf[8]) {
    for (int i = 0; i < 7; ++i) Dest[i] = (char) (((buf[i] & 255) << (i + 1)) | ((buf[i + 1] & 255) >> (6 - i)));
}

void e1(char *r, const char *s, int eCSize) {
    int j = eCSize / 3;
    for (int i = 0; i < j; ++i) e1_1(r + 4 * i, s + 3 * i);
}

void ee(char Dest[13], const char *s) {
    char r[8];
    e1(r, s, 6);
    char rr[8];
    unsigned int o = 0;
    for (int i = 0; i < 6; ++i) o ^= (unsigned int) s[i];
    ab_ks(rr, r, 8, o);
    char rrr[9];
    b128_d1(rrr, rr);
    rrr[7] = 0, rrr[8] = 0;
    for (int i = 0; i < 7; ++i) rrr[i] ^= s[i];
    e1(Dest, rrr, 9);
    Dest[10] = '=', Dest[11] = Dest[10], Dest[12] = Dest[11] - '=';;
}

JNIEXPORT jstring JNICALL Java_com_zhc_tools_floatingboard_JNI_mG
        (JNIEnv *env, jobject obj, jobject ctx) {
    JNIEnv e = *env;
    jclass DateClass = e->FindClass(env, "java/util/Date");
    jmethodID DateMId = e->GetMethodID(env, DateClass, "<init>", "()V");
    jobject DateObj = e->NewObject(env, DateClass, DateMId);
    jclass sdfClass = e->FindClass(env, "java/text/SimpleDateFormat");
    jmethodID sdfMId = e->GetMethodID(env, sdfClass, "<init>", "(Ljava/lang/String;)V");
    jobject sdfObj = e->NewObject(env, sdfClass, sdfMId, e->NewStringUTF(env, "yyMMdd"));
    jmethodID sdfFormatMId = e->GetMethodID(env, sdfClass, "format", "(Ljava/util/Date;)Ljava/lang/String;");
    jobject strObj = e->CallObjectMethod(env, sdfObj, sdfFormatMId, DateObj);
    const char *d = e->GetStringUTFChars(env, (jstring) strObj, (jboolean *) 0);
    char R[13];
    ee(R, d);
//    Log(env, R);
    jclass ctxClass = e->GetObjectClass(env, ctx);
    //TODO GetExternalFilePath
    /*jclass EClass = e->FindClass(env, "android/os/Environment");
    jmethodID EMId = e->GetStaticMethodID(env, EClass, "getExternalStorageDirectory", "()Ljava/io/File;");
    jobject F = e->CallStaticObjectMethod(env, EClass, EMId);
    jclass tSClass = e->GetObjectClass(env, F);
    jmethodID tSMId = e->GetMethodID(env, tSClass, "toString", "()Ljava/lang/String;");
    jstring fStr = (jstring) e->CallObjectMethod(env, F, tSMId);
    Log(env, e->NewStringUTF(env, fStr));*/
    char *cS = NULL;
    char qS[12];
    qS[11] = '\0';
    qS[0] = 'r', qS[1] = 'm', qS[2] = 'J', qS[3] = 'f', qS[4] = 'o', qS[5] = 'g', qS[6] = qS[4], qS[7] = 'V', qS[8] = '+';
    qS[9] = 'Q', qS[10] = '=';
    strcpyAndCat_auto(&cS, qS, "=");
//    Log(env, cS);
//    Log(env, R);
    if (!strcmp(R, cS)) {
        jmethodID ctxMId = e->GetMethodID(env, ctxClass, "finish", "()V");
        e->CallVoidMethod(env, ctx, ctxMId);
        Log(env, "verification.........\n"
                 "高中。拼搏 !\n");
    } else {
        jmethodID sCMId = e->GetMethodID(env, ctxClass, "setContentView", "(I)V");
        jclass RClass = e->FindClass(env, "com/zhc/tools/R$layout");
        jfieldID f = e->GetStaticFieldID(env, RClass, "tools_activity_main", "I");
        jint cI = e->GetStaticIntField(env, RClass, f);
        e->CallVoidMethod(env, ctx, sCMId, cI);
    }
    return e->NewStringUTF(env, R);
}