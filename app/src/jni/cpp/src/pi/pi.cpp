//
// Created by root on 19-8-3.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_Pi.h"

void Callback(JNIEnv *env, jobject callback, int a) {
    jclass clz = env->GetObjectClass(callback);
    jmethodID mid = env->GetMethodID(clz, "callback", "(I)V");
    env->CallVoidMethod(callback, mid, (jint) a);
    env->DeleteLocalRef(clz);
}

void pi(JNIEnv *env, int bN, jobject callback) {
    bN -= bN % 4;
//    o(env, obj, p3);
    long a[2] = {956, 80}, b[2] = {57121, 25}, i = 0, j, k, p, q, r, s = 2, t, u, v, N, M = 10000;
//    printf("%9cMachin%6cpi=16arctan(1/5)-4arctan(1/239)\nPlease input a number.\n", 32, 32);
    N = bN;
    /*cin >> N, */
    N = N / 4 + 3;
    long *pi = new long[N], *e = new long[N];
    while (i < N)pi[i++] = 0;
    while (--s + 1) {
        for (*e = a[k = s], i = N; --i;)e[i] = 0;
        for (q = 1; j = i - 1, i < N; e[i] ? 0 : ++i, q += 2, k = !k)
            for (r = v = 0; ++j < N; pi[j] += k ? u : -u)
                u = (t = v * M + (e[j] = (p = r * M + e[j]) / b[s])) / q, r = p % b[s], v = t % q;
    }
    while (--i)(pi[i] = (t = pi[i] + s) % M) < 0 ? pi[i] += M, s = t / M - 1 : s = t / M;
    for (; ++i < N - 2;) {
        Callback(env, callback, (jint) pi[i]);
    }
    delete[]pi, delete[]e;
}

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024Pi_gen
        (JNIEnv *env, jclass cls, jint bN, jobject callback) {
    pi(env, bN, callback);
}

#pragma clang diagnostic pop