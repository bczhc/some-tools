//
// Created by root on 2020/9/26.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_JniTest.h"
#include "../../third_party/my-cpp-lib/concurrent.h"
#include "../jni_help.h"
#include <cerrno>
#include "../../third_party/my-cpp-lib/app/stc_flash/stc_flash_lib.h"

void init(int fd) {
    termios options{};
    tcgetattr(fd, &options);
    // set up raw mode / no echo / binary
    options.c_cflag |= (tcflag_t) (CLOCAL | CREAD);
    options.c_lflag &= (tcflag_t) ~(ICANON | ECHO | ECHOE | ECHOK | ECHONL |
                                    ISIG | IEXTEN); //|ECHOPRT

    options.c_oflag &= (tcflag_t) ~(OPOST);
    options.c_iflag &= (tcflag_t) ~(INLCR | IGNCR | ICRNL | IGNBRK);
#ifdef IUCLC
    options.c_iflag &= (tcflag_t) ~IUCLC;
#endif
#ifdef PARMRK
    options.c_iflag &= (tcflag_t) ~PARMRK;
#endif
    tcsetattr(fd, TCSANOW, &options);
}

void setSpeed(int fd, unsigned int speed) {
    unsigned int speedArr[] = {B0, B50, B75, B110, B134, B150, B200, B300, B600, B1200, B1800,
                               B2400, B4800, B9600,
                               B19200, B38400, B57600, B115200, B230400, B460800, B500000, B576000,
                               B921600,
                               B1000000, B1152000, B1500000, B2000000, B2500000, B3000000, B3500000,
                               B4000000};
    unsigned int nameArr[] = {0, 50, 75, 110, 134, 150, 200, 300, 600, 1200, 1800, 2400, 4800, 9600,
                              19200, 38400,
                              57600, 115200, 230400, 460800, 500000, 576000, 921600, 1000000,
                              1152000, 1500000,
                              2000000, 2500000, 3000000, 3500000, 4000000};
    termios opt{};
    tcgetattr(fd, &opt);
    int len = ARR_SIZE(speedArr);
    for (int i = 0; i < len; i++) {
        if (speed == nameArr[i]) {
            tcflush(fd, TCIOFLUSH);
            unsigned int rate = speedArr[i];
            cfsetispeed(&opt, rate);
            cfsetospeed(&opt, rate);
            int status = tcsetattr(fd, TCSANOW, &opt);
            if (status != 0) {
                int e = errno;
                printf("%i\n", e);
                throw String("tcsetattr fd1");
            }
            return;
        }
        tcflush(fd, TCIOFLUSH);
    }
    throw String("Setting speed failed.");
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024JniTest_call
        (JNIEnv *env, jclass) {
}