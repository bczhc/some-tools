#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "hicpp-signed-bitwise"

#ifndef BASE128_LIB_H
#define BASE128_LIB_H

#include <unistd.h>
#include <jni.h>

#define ERS 1029
#define DRS 1176


void e1(char *Dest, const char buf[7]);

void d1(char *Dest, const char buf[8]);

int e_1029P(char *Dest, const char buf[ERS], int readSize);

int d_1176P(char *Dest, const char buf[DRS], int readSize);

//char t_b[4][1029] = {{0}};
//char t_e_r[4][DRS] = {{0}};

/*
void *T_fun(void *arg) {
    int i = *((int *) arg);//????
    e_1029P(t_e_r[i], t_b[i], 1029);
    return NULL;
}

int i_t[4] = {0};
*/

/*
int e_4116_TP(char buf[4116], int readSize) {
    pthread_t t[4];
    for (int i = 0; i < 3; ++i) {
        substr2(t_b[i], buf, 1029 * i, 1029);
        i_t[i] = i;
        pthread_create(&(t[i]), NULL, T_fun, &(i_t[i]));
    }
    for (int j = 0; j < 4; ++j) {
        pthread_join(t[j], NULL);
    }
}

*/
int eD(const char *fN, const char *D_fN, JNIEnv *env, jobject obj);

int dD(const char *fN, const char *D_fN, JNIEnv *env, jobject obj);

void NewFileName(char **Dest, const char *filePath);

#pragma clang diagnostic pop

#endif