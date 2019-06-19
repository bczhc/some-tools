//
// Created by zhc-2 on 2019/6/19.
//

#include "com_zhc_qmcflac_qmcflac_Decode_JNI.h"
#include <stdio.h>
#include "qmcLib.h"

JNIEXPORT jint JNICALL Java_com_zhc_qmcflac_qmcflac_1Decode_JNI_decode
        (JNIEnv *env, jobject obj, jstring f, jstring dF) {
    JNIEnv e = *env;
    jclass mClass = e->FindClass(env, "com/zhc/qmcflac/qmcflac_Decode");
    jmethodID mid = e->GetStaticMethodID(env, mClass, "d", "Ljava/lang/String;D");
    decode(f, dF, env, mClass, mid);
//    callMethod(env, mClass, mid, "", 1);
    return (jint) 0;
}