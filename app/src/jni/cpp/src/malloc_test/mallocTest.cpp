//
// Created by root on 2020/3/26.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_MAllocTest.h"
#include <stdlib.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma clang diagnostic ignored "-Wunused-result"
JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024MAllocTest_alloc
        (JNIEnv *env, jclass cls, jlong lo) {
    char *p = (char *) malloc((size_t) lo);
    for (jlong i = 0; i < lo; ++i) {
        p[i] = 0;
    }
    return (jlong) (void *) p;
}
#pragma clang diagnostic pop