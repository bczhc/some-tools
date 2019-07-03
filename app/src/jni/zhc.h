//
// Created by zhc on 2018/5/7
// Created by root on 19-7-3.
//

#ifndef C99_ZHC_H
#define C99_ZHC_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#ifndef ARR_len
#define ARR_len(x) (sizeof(x) / sizeof(x)[0])
#endif
#define dl long long
#define usi unsigned int

void PrintArr(const char arr[], int len);


long long getFileSize(FILE *fp);

void strcat_auto(char **sourceDest, const char *cat_s);

/**
 *
 * @param string s
 * @param s s
 * @return r
 * @example this("123abc123", "23) = 2 this("12342312452312i23ab", "23") = 4
 * usage:
 * int *p = NULL;
 * int t = this(&p, "a1b1c1", "1"); => p[0] = 1, p[1] = 3, p[2] = 5; t = 3;
 */

int cmpCharArray(const char *a1, int a1Len, const char *a2, int a2Len);

#endif //C99_ZHC_H
