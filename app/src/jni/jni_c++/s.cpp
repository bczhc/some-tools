//
// Created by root on 19-8-4.
//

#include <cstring>
#include "../zhc.h"

void pi(char **Dest, int bN) {
    bN -= bN % 4;
    *Dest = (char *) malloc((size_t) bN + 3);
    char p3[] = "3.";
//    o(env, obj, p3);
    strcpy(*Dest, p3);
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
        strcat(*Dest, re);
    }
    delete[]pi, delete[]e;
}

int main() {
    char *r = nullptr;
    pi(&r, 4);
    printf("%s\n", r);
}