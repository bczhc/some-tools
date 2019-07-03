#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma ide diagnostic ignored "hicpp-signed-bitwise"

#include <unistd.h>
#include "./zhc.h"
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
int eD(const char *fN, const char *D_fN, JNIEnv *env, jobject obj, jmethodID mid);

int dD(const char *fN, const char *D_fN, JNIEnv *env, jobject obj, jmethodID mid);

/*char *NewFileName(char *Dest, const char *filePath) {
    char *r = Dest;
    char newFN[strlen(filePath) + 5];
    int x = 2;
    while (1) {
        char *xS = NULL;
//        itoa(x, xS, 10);
        m_itoa(&xS, x);
        strcpy(newFN, filePath);
        strcat(newFN, " (");
        strcat(newFN, xS);
        strcat(newFN, ")");
        if (access(newFN, F_OK) == EOF) break;
        x++;
        free(xS);
    }
    strcpy(r, newFN);
    return Dest;
}*/


#pragma clang diagnostic pop