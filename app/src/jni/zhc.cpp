//
// Created by root on 19-7-3.
//

#include "zhc.h"

char *ToUpperCase(char *Dest, const char *string) {
    char *p = Dest;
    int len = strlen(string);
    char r[len + 1];
    int i = 0;
    while (true) {
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
    while (true) {
        int b = n / 10;
        r++;
        n = b;
        if (!b) break;
    }
    return r;
}

int getLongLen(const long x) {
    long n = x;
    int r = 0;
    while (true) {
        long b = n / 10;
        r++;
        n = b;
        if (!b) break;
    }
    return r;
}

void Scanf(char **Dest) {
    char c;
    int i = 1;
    while (true) {
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

void strcpyAndCat_auto(char **Dest, const char *cpy_s, const char *cat_s) {
    *Dest = nullptr;
    int cpy_s_len = strlen(cpy_s);
    int cat_s_len = strlen(cat_s);
    size_t size = cpy_s_len + cat_s_len + 1;
    *Dest = (char *) malloc(size);
    strcpy(*Dest, cpy_s);
    strcat(*Dest, cat_s);
    (*Dest)[size - 1] = '\0';
}

void strcat_auto(char **sourceDest, const char *cat_s) {
    if (*sourceDest == nullptr) {
        *sourceDest = (char *) malloc(1);
        (*sourceDest)[0] = 0;
    }
    int sourceLen = strlen(*sourceDest);
    char cloneSource[sourceLen + 1];
    strcpy(cloneSource, *sourceDest);
    size_t size = sourceLen + strlen(cat_s) + 1;
    *sourceDest = (char *) malloc(size);
    strcpy(*sourceDest, cloneSource);
    strcat(*sourceDest, cat_s);
    (*sourceDest)[size - 1] = '\0';
}

void charToCharPtr(char **Dest, const char c) {
    *Dest = nullptr;
    *Dest = (char *) malloc((size_t) 2);
    (*Dest)[0] = c;
}

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
usi strInStrCount(int **Dest, const char *string, const char *s) {
    usi c = 0;
    usi stringL = strlen(string), sL = strlen(s);
    usi forI = stringL - sL + 1;
    *Dest = nullptr;
    if (stringL < sL) {
//        free((void *) forI);
        return 0;
    } else {
        for (int i = 0; i < forI; ++i) {
            int b = 1;
            for (int j = 0; j < sL; ++j) {
                b &= (string[i + j] == s[j]);
            }
            if (b) {
                *Dest = (int *) realloc(*Dest, (size_t) (4 * (++c)));
                (*Dest)[c - 1] = i;
            }
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

/*void m_lltoa(char **Dest, const dl int ll) {

}*/

void m_ltoa(char **Dest, const long i) {
    int I_L = getLongLen(i);
    *Dest = (char *) malloc((size_t) (I_L + 1));
    long d_i = 0;
    for (long j = I_L - 1; j >= 0; --j) {
        (*Dest)[d_i] = (int) (((long long) i) / ((long long) m_pow(10LL, j)) % 10) + 48;
        ++d_i;
    }
    (*Dest)[d_i] = 0;
}

int split(char ***Dest, const char *SourceString, const char *SplitStr) {
    int *pos = nullptr;
    int posL = strInStrCount(&pos, SourceString, SplitStr);
    usi srcLen = strlen(SourceString), splitStrLen = strlen(SplitStr), toP = srcLen - splitStrLen;
    int lastIndex = 0;
    *Dest = (char **) malloc((size_t) (sizeof(char *) * (posL + 1)));
    for (int i = 0; i < posL; ++i) {
        int sL = 0;
        for (int j = lastIndex; j < pos[i]; ++j) {
            sL = pos[i] - lastIndex + 2;
            (*Dest)[i] = (char *) malloc((size_t) (sL));
            (*Dest)[i][j - lastIndex] = SourceString[j];
        }
        (*Dest)[i][pos[i]] = 0;
        lastIndex = pos[i];
    }
    return posL;
}

int cmpIntArray(const int *a1, const int a1Len, const int *a2, const int a2Len) {
    if (a1Len != a2Len) return 0;
    else {
        for (int i = 0; i < a1Len; ++i) {
            if (a1[i] != a2[i]) return 0;
        }
    }
    return 1;
}

int cmpCharArray(const char *a1, const int a1Len, const char *a2, const int a2Len) {
    if (a1Len != a2Len) return 0;
    else {
        for (int i = 0; i < a1Len; ++i) {
            if (a1[i] != a2[i]) return 0;
        }
    }
    return 1;
}

int charArrToInt(const char *s, size_t size) {
    int r = 0;
    for (int i = 0; i < size; ++i) {
        r += ((usi) s[i] - 48) * m_pow(10LL, size - i - 1);
    }
    return r;
}