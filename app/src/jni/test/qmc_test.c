//
// Created by root on 19-7-6.
//
#include "../qmcLib.h"

int main(int argc, char **argv) {
    FILE *fp, *fpO;
    if ((fp = fopen(argv[1], "rb")) == NULL) return -1;
    if ((fpO = fopen(argv[2], "wb")) == NULL) return -1;
    char c[1024] = {0};
    dl fL = getFileSize(fp), a = fL / 1024;
    if (!fL)
        return 0;
    usi p = fL / 20480;
    int b = (int) (fL % 1024);
    for (int j = 0; j < a; ++j) {
        fread(c, 1024, 1, fp);
        for (int k = 0; k < 1024; ++k) {
            c[k] ^= nextMask_();
        }
        fwrite(c, 1024, 1, fpO);
        if (!(j % p)) printf("%f\n", ((double) j) / (double) a * 100);
    }
    if (b) {
        fread(c, b, 1, fp);
        for (int j = 0; j < b; ++j) {
            c[j] ^= nextMask_();
        }
        fwrite(c, b, 1, fpO);
    }
    fclose(fp);
    fclose(fpO);
    system("md5sum -b ./r.flac");
    return 0;
}