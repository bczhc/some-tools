//
// Created by bczhc on 7/4/21.
//

#include <sys/sysinfo.h>
#include "../jni_h/pers_zhc_tools_jni_JNI_SysInfo.h"

JNIEXPORT jlong JNICALL Java_pers_zhc_tools_jni_JNI_00024SysInfo_getUptime
        (JNIEnv *, jclass) {
    struct sysinfo info{};
    sysinfo(&info);
    return (jlong) info.uptime;
};

JNIEXPORT jshort JNICALL Java_pers_zhc_tools_jni_JNI_00024SysInfo_getProcessesCount
        (JNIEnv *, jclass) {
    struct sysinfo info{};
    sysinfo(&info);
    return (jshort) info.procs;
}

