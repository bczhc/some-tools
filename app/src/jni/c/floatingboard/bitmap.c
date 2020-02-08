//
// Created by root on 2020/2/8.
//

#include <android/bitmap.h>
#include "./pers_zhc_tools_utils_BitmapUtil.h"

JNIEXPORT int JNICALL Java_pers_zhc_tools_utils_BitmapUtil_getBitmapResolution
        (JNIEnv *env, jobject instance, jobject bitmap, jobject point) {
    JNIEnv e = *env;
    AndroidBitmapInfo info;
    int status = AndroidBitmap_getInfo(env, bitmap, &info);
    jclass cls = e->GetObjectClass(env, point);
    jfieldID fid_x = e->GetFieldID(env, cls, "x", "I");
    jfieldID fix_y = e->GetFieldID(env, cls, "y", "I");
    e->SetIntField(env, point, fid_x, info.width);
    e->SetIntField(env, point, fix_y, info.height);
    return status;
};