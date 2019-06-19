//
// Created by zhc on 2019/5/7.
//
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

char *ToUpperCase(char *Dest, const char *string) {
    char *p = Dest;
    int len = strlen(string);
    char r[len + 1];
    int i = 0;
    while (1) {
        r[i] = (char) toupper((int) string[i]);
        if (string[i] == '\0') break;
        i++;
    }
    strcpy(p, r);
    return Dest;
}

void PrintArr(const char arr[], int len) {
    int l_ = len - 1;
    printf("[");
    for (int i = 0; i < l_; ++i) {
        printf("%i%c", (int) arr[i], 44);
    }
    printf("%i]___%u", (int) arr[l_ - 1], (l_ + 1));
}

dl m_pow(const dl base, const dl exponent) {
    dl r = 1LL;
    for (int i = 0; i < exponent; ++i) {
        r *= base;
    }
    return r;
}

int BinToDec(const char *NumStr) {
    int r = 0;
    int j = 0;
    for (int i = strlen(NumStr) - 1; i >= 0; --i) {
        r += (NumStr[i] == '0' ? 0 : 1) * m_pow(2, j);
        j++;
    }
    return r;
}

void printArr(const char *a, const int length) {
    int l = length;
    printf("[");
    for (int i = 0; i < l; ++i) {
        printf("%i", (int) a[i]);
        if (i != l - 1) {
            printf(",");
        }
    }
    printf("]\n");
}

char *substring(char *Dest, const char *source, const int beginIndex, const int endIndex) {
    char *r = Dest;
    strncpy(r, source + beginIndex, (size_t) (endIndex - beginIndex));
    return Dest;
}

void substr(char **Dest, const char *source, const int from, int length) {
    *Dest = (char *) malloc((size_t) length + 1);
    strncpy(*Dest, source + from, (size_t) length);
}

char *substr2(char *Dest, const char *source, const int from, int length) {
    char *r = Dest;
    strncpy(r, source + from, (size_t) length);
    return Dest;
}


long long getFileSize(FILE *fp) {
    long long sz;
    fseek(fp, 0L, SEEK_END);
    sz = (long long) ftell(fp);
    if (sz == -1) {
//        sz = _ftelli64(fp);
        printf("Get file size error.\n");
    }
    fseek(fp, 0L, SEEK_SET);
    return sz;
}

int getIntegerLen(const int x) {
    int n = x;
    int r = 0;
    while (1) {
        int b = n / 10;
        r++;
        n = b;
        if (!b) break;
    }
    return r;
}

void Scanf(char **Dest) {
    char c;
    int i = 1;
    while (1) {
        scanf("%c", &c);
        *Dest = (char *) realloc(*Dest, (size_t) i);
        if (c == 0x0A) {
            (*Dest)[i - 1] = 0x0;
            break;
        }
        (*Dest)[i - 1] = c;
        ++i;
    }
}

void strcat_auto(char **DestOrSource, const char *str) {
    usi strL = strlen(str);
    usi S_L = strlen(*DestOrSource);
    char a1[strL + 1], a2[S_L + 1];
    strcpy(a1, str);
    strcpy(a2, *DestOrSource);
    *DestOrSource = NULL;
    *DestOrSource = (char *) malloc((size_t) (strL + S_L + 1));
    strcpy(*DestOrSource, a1);
    strcat(*DestOrSource, a2);
}

void charToCharPtr(char **Dest, const char c) {
    *Dest = NULL;
    *Dest = (char *) malloc((size_t) 2);
    (*Dest)[0] = c;
}

/**
 *
 * @param string s
 * @param s s
 * @return r
 * @example this("123abc123", "23) = 2  this("12342312452312i23ab", "23") = 4
 */
usi strInStrCount(const char *string, const char *s) {
    usi c = 0;
    usi stringL = strlen(string), sL = strlen(s);
    usi forI = stringL - sL + 1;
    if (stringL < sL) {
//        free((void *) forI);
        return 0;
    } else {
        for (int i = 0; i < forI; ++i) {
            int b = 1;
            for (int j = 0; j < sL; ++j) {
                b &= (string[i + j] == s[j]);
            }
            if (b) ++c;
        }
    }
    return c;
}

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
 */
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
}

int Str_Cmp_nMatchCase(const char *a, const char *b) {
    char t1[strlen(a) + 1];
    char t2[strlen(a) + 1];
    ToUpperCase(t1, a);
    ToUpperCase(t2, b);
    return strcmp(t1, t2) ? 0 : 1;
}

void m_itoa(char **Dest, const int i) {
    int I_L = getIntegerLen(i);
    *Dest = (char *) malloc((size_t) (I_L + 1));
    int d_i = 0;
    for (int j = I_L - 1; j >= 0; --j) {
        (*Dest)[d_i] = (int) (((long) i) / ((long) m_pow(10LL, j)) % 10) + 48;
        ++d_i;
    }
    (*Dest)[d_i] = 0;
}