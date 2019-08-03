//
// Created by root on 19-8-3.
//

#include "com_zhc_tools_pi_PiJNI.h"
#include "../zhc.h"



//#include <iostream>

//using namespace std;

void o(JNIEnv *env, jobject obj, char *str) {
    /*jclass clz = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(clz, "o", "Landroid/widget/EditText;");
    jobject fO = env->GetObjectField(obj, fid);
    jclass oClz = env->GetObjectClass(fO);
    jmethodID oMid = env->GetMethodID(oClz, "setText", "(Ljava/lang/String;)V");
    jstring s = env->NewStringUTF("Hello!");
    env->CallVoidMethod(fO, oMid, s);*/
    jclass clz = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(clz, "O", "(Ljava/lang/String;)V");
    jstring s = env->NewStringUTF(str);
    env->CallVoidMethod(obj, mid, s);
}

void pi(int bN, JNIEnv *env, jobject obj) {
    char p3[3];
    p3[0] = '3';
    p3[1] = '.';
    p3[2] = 0;
    o(env, obj, p3);
    long a[2] = {956, 80}, b[2] = {57121, 25}, i = 0, j, k, p, q, r, s = 2, t, u, v, N, M = 10000;
//    printf("%9cMachin%6cpi=16arctan(1/5)-4arctan(1/239)\nPlease input a number.\n", 32, 32);
    N = bN;
    /*cin >> N, */
    N = N / 4 + 3;
    char *re = nullptr;
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
        m_itoa(&re, (int) pi[i]);
        o(env, obj, re);
    }
    delete[]pi, delete[]e;/* cin.ignore(), cin.ignore();*/
}

JNIEXPORT void JNICALL Java_com_zhc_tools_pi_PiJNI_gen
        (JNIEnv *env, jobject obj, jint i) {
    pi((int) i, env, obj);
}