//
// Created by root on 19-7-3.
//

#include "zhc.h"

void PrintArr(const char arr[], int len) {
    int l_ = len - 1;
    printf("[");
    for (int i = 0; i < l_; ++i) {
        printf("%i%c", (int) arr[i], 44);
    }
    printf("%i]___%u", (int) arr[l_ - 1], (l_ + 1));
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

void strcat_auto(char **sourceDest, const char *cat_s) {
    if (*sourceDest == NULL) {
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

int cmpCharArray(const char *a1, const int a1Len, const char *a2, const int a2Len) {
    if (a1Len != a2Len) return 0;
    else {
        for (int i = 0; i < a1Len; ++i) {
            if (a1[i] != a2[i]) return 0;
        }
    }
    return 1;
}