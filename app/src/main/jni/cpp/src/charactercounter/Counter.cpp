//
// Created by root on 2020/8/25.
//
/*
TODO
#include "../jni_h/pers_zhc_tools_jni_JNI_CharactersCounter.h"
#include "../../third_party/my-cpp-lib/zhc.h"
#include "../../third_party/my-cpp-lib/CountCharacters.h"

using namespace bczhc;

ArrayList<CharacterCounter *> list;

JNIEXPORT jint JNICALL Java_pers_zhc_tools_jni_JNI_00024CharactersCounter_createHandler
        (JNIEnv *env, jclass cls) {
    auto *counter = new CharacterCounter;
    list.add(counter);
    return list.length() - 1;
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024CharactersCounter_releaseHandler
        (JNIEnv *env, jclass cls, jint id) {
    delete list.get(id);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024CharactersCounter_count
        (JNIEnv *env, jclass cls, jint id, jstring str) {
    const char *s = env->GetStringUTFChars(str, nullptr);
    list.get(id)->countCharacters(s, env->GetStringUTFLength(str));
    env->ReleaseStringUTFChars(str, s);
}

JNIEXPORT void JNICALL Java_pers_zhc_tools_jni_JNI_00024CharactersCounter_clearResult
        (JNIEnv *env, jclass cls, jint id) {
    list.get(id)->data->clear();
}

JNIEXPORT jstring JNICALL Java_pers_zhc_tools_jni_JNI_00024CharactersCounter_getResultJson
        (JNIEnv * env, jclass cls, jint id) {
    json *j = list.get(id)->getJsonData();
    jstring r = env->NewStringUTF(j->dump().c_str());
    return r;
}
*/
