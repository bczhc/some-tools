//
// Created by zhc on 2/2/21.
//

#ifndef BCZHC_SERIAL_SERIAL_JNI_H
#define BCZHC_SERIAL_SERIAL_JNI_H

#include "../../third_party/my-cpp-lib/app/stc_flash/serial.h"
#include <cassert>

namespace bczhc::serial {
    namespace jniImpl {
        Array<uchar> read(JNIEnv *&env, int size, jobject &jniInterface);

        ssize_t write(JNIEnv *&env, uchar *buf, ssize_t size, jobject &jniInterface);

        void setSpeed(JNIEnv *&env, unsigned int speed, jobject jniInterface);

        void close(JNIEnv *&env, jobject jniInterface);

        unsigned int getBaud(JNIEnv *&env, jobject jniInterface);

        void setTimeout(JNIEnv *&env, uint32_t timeout, jobject jniInterface);

        void setParity(JNIEnv *&env, char p, jobject jniInterface);

        char getParity(JNIEnv *&env, jobject jniInterface);

        uint32_t getTimeout(JNIEnv *env, jobject jniInterface);

        void flush(JNIEnv *&env, jobject &jniInterface);
    }

    class SerialJNI : public Serial {
    private:
        JNIEnv *&env;
        jobject &jniInterface;
    public:
        SerialJNI(JNIEnv *&env, jobject &jniInterface);

        [[nodiscard]] Array<uchar> read(ssize_t size) const override;

        ssize_t write(uchar *buf, ssize_t size) const override;

        [[nodiscard]] uint32_t getTimeout() const override;

        void setSpeed(unsigned int speed) override;

        void close() const override;

        [[nodiscard]] unsigned int getBaud() const override;

        void flush() const override;

        void setTimeout(uint32_t t) override;

        void setParity(char p) override;

        [[nodiscard]] char getParity() const override;
    };
}

#endif //BCZHC_SERIAL_SERIAL_JNI_H
