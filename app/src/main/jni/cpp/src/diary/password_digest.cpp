//
// Created by bczhc on 2/8/21.
//

#include "../jni_h/pers_zhc_tools_jni_JNI_Diary.h"
extern "C" {
#include "third_party/my-cpp-lib/third_party/crypto-algorithms/sha256.h"
}
#include "third_party/my-cpp-lib/string.hpp"
#include "third_party/my-cpp-lib/array.hpp"
#include "third_party/my-cpp-lib/app/base128/Base128Lib.h"
#include <cassert>

using namespace bczhc;

Array<uchar> sha256Encode(uchar *data, size_t size) {
    assert(sizeof(uchar) == sizeof(BYTE));
    Array<uchar> buf(SHA256_BLOCK_SIZE);
    SHA256_CTX ctx{};
    sha256_init(&ctx);
    sha256_update(&ctx, (BYTE *) data, size);
    sha256_final(&ctx, buf.data());
    return buf;
}

Array<uchar> sha256Encode(const String &str) {
    return sha256Encode((uchar *) str.getCString(), str.length());
}

String hexArrToStr(const uchar *a, size_t size) {
    String s;
    for (size_t i = 0; i < size; ++i) {
        String hex = String::toString(a[i], 16);
        if (hex.length() == 1) hex = String("0") + hex;
        s += hex;
    }
    return s;
}

String sha256EncodeToString(const String &str) {
    const Array<uchar> a = sha256Encode(str);
    return hexArrToStr(a.data(), a.length());
}

String encode(const String &str) {
    auto s = sha256EncodeToString(str);
    String s2;
    for (int i = (int) s.length() - 1; i >= 0; --i) {
        s2 += s[i];
    }
    String salt = "bczhc";
    s2 += salt;
    auto a = sha256Encode(s2);
    Array<uchar> b(a.length() + 3);
    int i;
    for (i = 0; i < a.length(); ++i) {
        b[i] = a[i];
    }
    char c[] = {0x12, 0x34, 0x56};
    int o = i;
    for (; i < b.length(); ++i) {
        b[i] = c[i - o];
    }
    Array<uchar> d(40);
    for (i = 0; i < b.length() / 7; ++i) encode7bytes(d.data() + i * 8, b.data() + i * 7);
    return sha256EncodeToString(hexArrToStr(d.data(), d.length()) + salt);
}

JNIEXPORT jstring JNICALL Java_pers_zhc_tools_jni_JNI_00024Diary_myDigest
        (JNIEnv *env, jclass, jstring js) {
    const char *s = env->GetStringUTFChars(js, nullptr);
    auto r = encode(s);
    env->ReleaseStringUTFChars(js, s);
    return env->NewStringUTF(r.getCString());
}