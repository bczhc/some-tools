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

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
#ifndef ARR_len
#define ARR_len(x) sizeof(x) / sizeof(x)[0]
#endif
#define dl long long
#define usi unsigned int
#ifdef __cplusplus
extern "C" {
#endif

char *ToUpperCase(char *Dest, const char *string);

void PrintArr(const char arr[], int len);

dl m_pow(const dl base, const dl exponent);

int BinToDec(const char *NumStr);

void printArr(const char *a, const int length);

char *substring(char *Dest, const char *source, const int beginIndex, const int endIndex);

void substr(char **Dest, const char *source, const int from, int length);

char *substr2(char *Dest, const char *source, const int from, int length);


long long getFileSize(FILE *fp);

int getIntegerLen(const int x);

int getLongLen(const long x);

void Scanf(char **Dest);

void strcpyAndCat_auto(char **Dest, const char *cpy_s, int cpy_s_length, const char *cat_s, int cat_s_length);

void strcat_auto(char **sourceDest, const char *cat_s);

void charToCharPtr(char **Dest, const char c);
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
usi strInStrCount(int **Dest, const char *string, const char *s);

/**
 * String split
 * @param Dest Dest
 * @param str String
 * @param splitChar as separation
 * @use
 * void ***r = NULL;
 * split(&r, str1, str2);
 * int i = *((int *) (r[0][0]));// element count
 * char **R = ((char **) ((char ***) r)[1]); //char * result
 * for (int j = 0; j < i; ++j) {
        printf("%s\n", R[j]);
    }
 *//*
void split(void ****Dest, char *str, const char *splitChar) {
    *Dest = (void ***) malloc((size_t) (sizeof(char **) * 2));
    ((*Dest)[0]) = (void **) malloc((size_t) (sizeof(int *) * 1));
    (*Dest)[0][0] = (void *) malloc((size_t) (size_t) (sizeof(int)));
    (*Dest)[1] = (void **) malloc((size_t) (sizeof(void *) * 3));
    char *r = NULL;
    usi sS = strlen(str) + 1, splitChar_s = strlen(splitChar) + 1;
    char str_charArr[sS], splitChar_charArr[splitChar_s];
    for (int j = 0; j < sS - 1; ++j) {
        str_charArr[j] = str[j];
    }
    str_charArr[sS - 1] = 0x0;
    for (int j = 0; j < splitChar_s - 1; ++j) {
        splitChar_charArr[j] = splitChar[j];
    }
    splitChar_charArr[splitChar_s - 1] = 0x0;
    usi eC = strInStrCount(str, splitChar) + 1;
    if (str[0] == splitChar[0]) eC--;
    if (str[sS - 2] == splitChar[splitChar_s - 2]) eC--;
    if (eC != 1) {
        goto n;
    }
    *((int *) ((*Dest)[0][0])) = (int) 0;
    strcpy((*Dest)[1][0], "");
    return;
    n:
    *((int *) ((*Dest)[0][0])) = (int) eC;
    (*Dest)[1] = (void **) malloc((size_t) (sizeof(char *) * eC));
    r = strtok(str_charArr, splitChar_charArr);
    int a_i = 0;
    while (r != NULL) {
        int rS = strlen(r) + 1;
        (*Dest)[1][a_i] = ((char *) malloc((size_t) rS));
        strcpy((*Dest)[1][a_i], r);
        r = strtok(NULL, splitChar_charArr);
        ++a_i;
    }
}*/

int Str_Cmp_nMatchCase(const char *a, const char *b);

void m_itoa(char **Dest, const int i);

/*void m_lltoa(char **Dest, const dl int ll) {

}*/

void m_ltoa(char **Dest, const long l);

int split(char ***Dest, const char *SourceString, const char *SplitStr);

int cmpIntArray(const int *a1, const int a1Len, const int *a2, const int a2Len);

int cmpCharArray(const char *a1, const int a1Len, const char *a2, const int a2Len);

int charArrToInt(const char *s, size_t size);

int getBiggerNum(const int a, const int b);

int firstIndexOf(const char *s, const int s_len, const char c);

char m_itoc(const int i);

int m_ctoi(const char c);

int Split(char ***dst, const char *s, int s_length, const char *separatorString, int separatorString_length);
#ifdef __cplusplus
}
#endif
#ifdef __cplusplus

#include <iostream>

using namespace std;

template<class T>
class List {
private:
    T *p;
    int32_t length{};
    int32_t typeSize{};
    bool hasFreed = false;
public:
    List();

    T add(T a);

    T remove(T a);

    T get(int32_t index);

    void release();

    /*~List() {
        if (!hasFreed)
            this->release();
    }*/

    int32_t getLength();

    void foreach(void (*callback)(T));
};

#endif

#endif //C99_ZHC_H
