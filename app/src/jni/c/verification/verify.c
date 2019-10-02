//
// Created by root on 19-8-13.
//

#include "../../zhc.h"
#include "./com_zhc_tools_floatingboard_JNI.h"
#include "../codecs/qmcLib.h"
#include <stdio.h>
#include "jni.h"
#include "../../jni_help.h"


int cpD(const char *d1, const char *d2) {
    char ***s = (char ***) malloc((size_t) (sizeof(char **) * 2));
    for (int i = 0; i < 2; ++i) {
        s[i] = (char **) malloc((size_t) (sizeof(char *) * 3));
        for (int j = 0; j < 3; ++j) {
            s[i][j] = (char *) malloc((size_t) 2);
        }
    }
    for (int l = 0; l < 2; ++l) {
        for (int k = 0; k < 3; ++k) {
            for (int i = 0; i < 2; ++i) {
                s[l][k][i] = l ? d2[i + 2 * k] : d1[i + 2 * k];
            }
        }
    }
    for (int m = 0; m < 3; ++m) {
        int i1 = charArrToInt(s[0][m], 2), i2 = charArrToInt(s[1][m], 2);
        if (i1 != i2) return i1 < i2 ? -1 : 1;
    }
    return strcmp(d1, d2);
}

char decodeTable[128] = {0};
char encodeTable[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
char ksTable[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
char b128_e_table_l[] = {1, 3, 7, 15, 31, 63, 127};

void aA0_ks(char *Dest, const char *src, int size, int o) {
    for (int i = 0; i < size; ++i) {
        int tableI = 0;
        for (int j = 0; j < 62; ++j) if (ksTable[j] == src[i]) tableI = j;
        Dest[i] = (char) ksTable[(tableI + o) % 62];
    }
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


//  char encodeTable[] = "1029384756<>qpwoeirutyalskdjfhgmznxbcv!@#$%^&*()_+/~`,.?\"\';:Ab";

char *e1_64(char *Dest, const char cArr[3]) {
    char *r = Dest;
    r[0] = encodeTable[(cArr[0] & 255) >> 2];
    r[1] = encodeTable[(((cArr[0] & 255) & 3) << 4) | ((cArr[1] & 255) >> 4)];
    r[2] = encodeTable[(((cArr[1] & 255) & 15) << 2) | ((cArr[2] & 255) >> 6)];
    r[3] = encodeTable[(cArr[2] & 255) & 63];
    return Dest;
}

void d1_64(char *Dest, const char cArr[4]) {
    char *r = Dest;
    r[0] = (decodeTable[cArr[0]] << 2) | (decodeTable[cArr[1]] >> 4);
    r[1] = (decodeTable[cArr[1]] << 4) | (decodeTable[cArr[2]] >> 2);
    r[2] = (decodeTable[cArr[2]] << 6) | decodeTable[cArr[3]];
}

char *d1_EQ_M_64(char *Dest, const char cArr[4], int eqM_C) {
    char *r = Dest;
    if (eqM_C == 2) {
        r[0] = (decodeTable[cArr[0]] << 2) | (decodeTable[cArr[1]] >> 4);
        return r;
    } else {
        r[0] = (decodeTable[cArr[0]] << 2) | (decodeTable[cArr[1]] >> 4);
        r[1] = (decodeTable[cArr[1]] << 4) | (decodeTable[cArr[2]] >> 2);
        return r;
    }
}

void initDT() {
    for (int i = 65; i < 91; ++i) {
        decodeTable[i] = i - 65;
    }
    for (int j = 97; j < 123; ++j) {
        decodeTable[j] = j - 71;
    }
    for (int k = 48; k < 59; ++k) {
        decodeTable[k] = k + 4;
    }
    decodeTable['+'] = 62;
    decodeTable['/'] = 63;
}

void eD_64(char **Dest, const char *s, size_t sSize, JNIEnv *env) {
    int a = sSize, b = a % 3, t = a / 3;
    size_t size = t * 4 + (b ? 4 : 0) + 1;
    *Dest = (char *) malloc(size);
    memset(*Dest, 0, size);
//    (*Dest)[size - 1] = 0;
    char r[4] = {0};
    for (int i = 0; i < t; ++i) {
        e1_64(r, s + 3 * i);
        for (int j = 0; j < 4; ++j) {
//            printf("%c", r[j]);
            (*Dest)[i * 4 + j] = r[j];
//            LogArr(env, "*Dest 1", *Dest, size);
        }
    }
    if (b) {
        char n[3] = {0};
        for (int i = 0; i < b; ++i) {
            n[i] = s[3 * t + i];
        }
        e1_64(r, n);
        for (int k = 0; k < b + 1; ++k) {
//            printf("%c", r[k]);
            (*Dest)[size - 4 + k - 1] = r[k];
//            LogArr(env, "*Dest 2", *Dest, size);
        }
        for (int j = 0; j < 3 - b; ++j) {
//            printf("%c", '=');
        }
        for (int l = size - 1; l > size - 1 - (3 - b); --l) {
            (*Dest)[l - 1] = '=';
//            LogArr(env, "*Dest 3", *Dest, size);
        }
    }
}

void dD_64(char **Dest, const char *s, usi sSize) {
    int a = sSize, t = a / 4;
    int eqMC = (s[a - 1] == '=') + (s[a - 2] == '=');
    int d = a - eqMC;
    int b = d / 4;
    char r[3] = {0};
    char c2S[] = {0, 0};
    if (eqMC) {
        for (int i = 0; i < b; ++i) {
            d1_64(r, s + 4 * i);
            for (int j = 0; j < 3; ++j) {
                c2S[0] = r[j];
                strcat_auto(Dest, c2S);
//                printf("%c", r[j]);
            }
        }
        d1_EQ_M_64(r, s + b * 4, eqMC);
        for (int l = 0; l < 3 - eqMC; ++l) {
            c2S[0] = r[l];
            strcat_auto(Dest, c2S);
//            printf("%c", r[l]);
        }
    } else {
        for (int i = 0; i < t; ++i) {
            d1_64(r, s + 4 * i);
            for (int j = 0; j < 3; ++j) {
                c2S[0] = r[j];
                strcat_auto(Dest, c2S);
//                printf("%c", r[j]);
            }
        }
    }
}

void ee(char **Dest, const char *s, JNIEnv *env) {
    /*char r[8];
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
    Dest[10] = '=', Dest[11] = Dest[10], Dest[12] = Dest[11] - '=';;*/
    char *r1_t = NULL;
    size_t ss = strlen(s);
    eD_64(&r1_t, s, ss, env);
//    LogArr(env, "r1_t", r1_t, strlen(r1_t));
    int *p = NULL;
    usi eqN = strInStrCount(&p, r1_t, "=");
    {
        char *z = NULL;
        m_itoa(&z, eqN);
//        Log(env, "eqN", z);
        free(z);
    }
    free(p);
    int r1S = strlen(r1_t) + 1 - eqN;
    {
        char *z = NULL;
        m_itoa(&z, r1S);
//        Log(env, "rqS", z);
        free(z);
    }
    char r1[r1S];
    for (int k = 0; k < r1S; ++k) {
        if (r1_t[k] == '=') break;
        r1[k] = r1_t[k];
    }
    r1[r1S - 1] = 0;
    size_t strLen = strlen(r1);
//    LogArr(env, "r1", r1, strLen);
//    Log(env, "r1", r1);
    char r2[strLen + 1];
    memset(r2, 0, strLen + 1);
    usi o = 0;
    for (int i = 0; i < strLen; ++i) {
        o ^= (usi) r1[i];
    }
    aA0_ks(r2, r1, strLen, o);
//    LogArr(env, "r2", r2, strLen + 1);
//    Log(env, "r2", r2);
    size_t r2StrLen = strlen(r2);
    size_t b128SrcSize = r2StrLen % 8 ? ((r2StrLen / 8 + 1) * 8) : r2StrLen;
    size_t b128DestSize = r2StrLen / 8 * 7 + (r2StrLen % 8 ? 7 : 0);
    char b128Src[b128SrcSize];
    memset(b128Src, 0, b128SrcSize);
    for (int j = 0; j < r2StrLen; ++j) {
        b128Src[j] = r2[j];
    }
    char b128Dest[b128DestSize];
    memset(b128Dest, 0, b128DestSize);
    for (int f = 0; f < b128SrcSize / 8; ++f) {
        b128_d1(b128Dest + 7 * f, b128Src + 8 * f);
    }
    {
        char *z = NULL;
        m_itoa(&z, b128DestSize);
//        Log(env, "b128DestSize", z);
        free(z);
    }
//    LogArr(env, "b128Dest", b128Dest, b128DestSize);
    eD_64(Dest, b128Dest, b128DestSize, env);
//    LogArr(env, "b128Dest-", b128Dest, b128DestSize);
}

JNIEXPORT jint JNICALL Java_pers_zhc_tools_floatingboard_JNI_mG
        (JNIEnv *env, jobject obj, jobject ctx, jstring iStr) {
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
//    Log(env, R);
//    Log(env, d);
    jclass ctxClass = e->GetObjectClass(env, ctx);
    //TODO GetExternalFilePath
    /*jclass EClass = e->FindClass(env, "android/os/Environment");
    jmethodID EMId = e->GetStaticMethodID(env, EClass, "getExternalStorageDirectory", "()Ljava/io/File;");
    jobject F = e->CallStaticObjectMethod(env, EClass, EMId);
    jclass tSClass = e->GetObjectClass(env, F);
    jmethodID tSMId = e->GetMethodID(env, tSClass, "toString", "()Ljava/lang/String;");
    jstring fStr = (jstring) e->CallObjectMethod(env, F, tSMId);
    Log(env, e->NewStringUTF(env, fStr));*/

    /*if (!strcmp(R, cS) || (d[4] == '2')) {
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
    }*/
    const char *s = e->GetStringUTFChars(env, iStr, (jboolean *) 0);
//    LogArr(env, "", s, 22);
//    Log(env, s);
    int *p = NULL;
    int n = strInStrCount(&p, s, "-");
    if (n != 1) return (jint) 1;
    int i = p[0];
    free(p);
    char **str = (char **) malloc((size_t) (sizeof(char *) * 2));
    size_t str1S = (strlen(s) - i);
    str[0] = (char *) malloc(i + 1), str[1] = (char *) malloc(str1S);
    for (int j = 0; j < i; ++j) {
        str[0][j] = s[j];
    }
    for (int k = 0; k < str1S - 1; ++k) {
        str[1][k] = s[k + i + 1];
    }
    str[0][i] = 0, str[1][str1S - 1] = 0;
    char *r = NULL;
    initDT();
    dD_64(&r, str[0], i);
    char *rr = NULL;
    ee(&rr, r, env);
//    LogArr(env, "rr", rr, 14);
//    Log(env, "str[1]", str[1]);
//    Log(env, "rr", rr);
//    Log(env, "r", r);
    if (!strcmp(str[1], rr)) {
//        Log(env, "", "验证通过");
        if (cpD(r, d) >= 0) {
            jmethodID sCMId = e->GetMethodID(env, ctxClass, "setContentView", "(I)V");
            jclass RClass = e->FindClass(env, "pers/zhc/tools/R$layout");
            jfieldID f = e->GetStaticFieldID(env, RClass, "tools_activity_main", "I");
            jint cI = e->GetStaticIntField(env, RClass, f);
//            Log(env, "sCV...");
            e->CallVoidMethod(env, ctx, sCMId, cI);
//            Log(env, "sCVOk");
            return 0;
        } else {
            Log(env, "vF", "不支持了，高中……2");
            jmethodID sCMId = e->GetMethodID(env, ctxClass, "setContentView", "(I)V");
            jclass RClass = e->FindClass(env, "pers/zhc/tools/R$layout");
            jfieldID f = e->GetStaticFieldID(env, RClass, "v_f_activity", "I");
            jint cI = e->GetStaticIntField(env, RClass, f);
            //            Log(env, "sCV...");
            e->CallVoidMethod(env, ctx, sCMId, cI);
            Log(env, "vF", "不支持了，高中……");
            return 1;
        }
    }
    free(r);
    free(rr);
    return 2;
}

int main(int argc, char **argv) {
//    if (argc != 2) return argc;
    const char *s = "MTkwODI1-jzsvGT4h1g==";
    int *p = NULL;
    int n = strInStrCount(&p, s, "-");
    if (n != 1) return (jint) 1;
    int i = p[0];
    free(p);
    char **str = (char **) malloc((size_t) (sizeof(char *) * 2));
    size_t str1S = (strlen(s) - i);
    str[0] = (char *) malloc(i + 1), str[1] = (char *) malloc(str1S);
    for (int j = 0; j < i; ++j) {
        str[0][j] = s[j];
    }
    for (int k = 0; k < str1S - 1; ++k) {
        str[1][k] = s[k + i + 1];
    }
    str[0][i] = 0, str[1][str1S - 1] = 0;
    char *r = NULL;
    initDT();
    dD_64(&r, str[0], i);
    char *rr = NULL;
    ee(&rr, r, NULL);
//        printf("%s\n", rr);
    printArr(rr, strlen(rr) + 2);
}