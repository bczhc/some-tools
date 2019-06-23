//
// Created by zhc-2 on 2019/6/19.
//

#include "com_zhc_qmcflac_qmcflac_Decode_JNI.h"
#include <stdio.h>
#include "qmcLib.h"

JNIEXPORT jint JNICALL Java_com_zhc_qmcflac_qmcflac_1Decode_JNI_decode
        (JNIEnv *env, jobject obj, jstring f, jstring dF) {
    JNIEnv e = *env;
    jclass mClass = e->FindClass(env, "com/zhc/qmcflac/qmcflac_Decode/JNI");
    jmethodID mid = e->GetStaticMethodID(env, mClass, "d", "(Ljava/lang/String;D)V");
    const char *f1 = e->GetStringUTFChars(env, f, (jboolean *) 0);
    const char *f2 = e->GetStringUTFChars(env, dF, (jboolean *) 0);
//    Log(env, f1);
//    Log(env, f2);
    char *sQ = NULL;
    strcat_auto(&sQ, f1);
    strcat_auto(&sQ, " to ");
    strcat_auto(&sQ, f2);
    Log(env, sQ);
    Log(env, "JNI————解码……");
    int rC = decode(f1, f2, env, mClass, mid);
    return (jint) rC;
}