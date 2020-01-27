//
// Created by root on 2020/1/25.
//

#include<stdio.h>
#include<stdlib.h>
#include<unistd.h>
#include<pthread.h>
#include <string.h>
#include<jni.h>
#include<android/log.h>

#include <stdint.h>
#include <dirent.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/inotify.h>
#include <sys/limits.h>
#include <sys/poll.h>
#include <linux/input.h>
#include <errno.h>

//#include "getevent.h"


#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "native-activity", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "native-activity", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "native-activity", __VA_ARGS__))

static const char *TAG = "getevent";
static const char *device_path = "/dev/input/event4";

//全局变量
JavaVM *g_jvm = NULL;
JNIEnv *g_env = NULL;
jobject g_obj = NULL;

jclass native_clazz;
jmethodID callback;


/**将JAVA字符串数组转C char字符数组（俗称字符串）的数组**/
char *jstringToChar(JNIEnv *env, jstring jstr) {
    char *rtn = NULL;
    jclass clsstring = (*env)->FindClass(env, "java/lang/String");
    jstring strencode = (*env)->NewStringUTF(env, "GB2312");//转换成Cstring的GB2312，兼容ISO8859-1
    //jmethodID   (*GetMethodID)(JNIEnv*, jclass, const char*, const char*);第二个参数是方法名，第三个参数是getBytes方法签名
    //获得签名：javap -s java/lang/String:   (Ljava/lang/String;)[B
    jmethodID mid = (*env)->GetMethodID(env, clsstring, "getBytes", "(Ljava/lang/String;)[B");
    //等价于调用这个方法String.getByte("GB2312");
    //将jstring转换成字节数组
    //用Java的String类getByte方法将jstring转换为Cstring的字节数组
    jbyteArray barr = (jbyteArray) (*env)->CallObjectMethod(env, jstr, mid, strencode);
    jsize alen = (*env)->GetArrayLength(env, barr);
    jbyte *ba = (*env)->GetByteArrayElements(env, barr, JNI_FALSE);
    LOGI("alen=%d\n", alen);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1 + 128);
        LOGI("rtn address == %p", &rtn);//输出rtn地址
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;            //"\0"
    }
    (*env)->ReleaseByteArrayElements(env, barr, ba, 0);
    return rtn;
}


JNIEXPORT void Java_com_cjz_jnigetevent_NativeEventCallBack_setJNIEnv(JNIEnv *env, jobject obj) {
    //保存全局JVM以便在子线程中使用
    (*env)->GetJavaVM(env, &g_jvm);
    //不能直接赋值(g_obj = obj)
    g_obj = (*env)->NewGlobalRef(env, obj);
}

/**使用jni执行getevent数据获取操作**/
JNIEXPORT void JNICALL
Java_com_cjz_jnigetevent_NativeEventCallBack_startTrace(JNIEnv *env, jobject thiz, jstring path) {
    device_path = jstringToChar(env, path);
    int lastX = -1, lastY = -1;
    int fd = -1;
    struct input_absinfo absI;
    fd_set rds;
    fd = open(device_path, O_RDONLY);
    if (fd < 0) {
        LOGI("main init: device open failure");
    }
    //保存全局JVM以便在子线程中使用
    (*env)->GetJavaVM(env, &g_jvm);
    //不能直接赋值(g_obj = obj)
    g_obj = (*env)->NewGlobalRef(env, thiz);
    g_env = env;
    {
        native_clazz = (*g_env)->GetObjectClass(g_env, g_obj);
        callback = (*g_env)->GetMethodID(g_env, native_clazz, "callback", "(IIIIII)V"); //参数为6个int，返回值为void
        //测试一下：
        //(*g_env)->CallVoidMethod(g_env, g_obj, callback, 0, 0, 0, 0, 0, 0);
    }
    LOGI("main init -1");


    while (1 && fd != 0) {
        FD_ZERO(&rds);
        FD_SET(fd, &rds);
        /*调用select检查是否能够从/dev/input/event4设备读取数据*/
        int ret = select(fd + 1, &rds, NULL, NULL, NULL);
        if (ret < 0) {
            continue;
        } else if (FD_ISSET(fd, &rds)) {
            //得到X轴的abs信息
            int x, y, pressure;
            struct input_event event;
            ioctl(fd, EVIOCGABS(ABS_X), &absI);
            //printf("x abs lastest value=%d\n",absI.value);
            //printf("x abs min=%d\n",absI.minimum);
            //printf("x abs max=%d\n",absI.maximum);
            x = absI.value;
            //得到y轴的abs信息
            ioctl(fd, EVIOCGABS(ABS_Y), &absI);
            //printf("y abs lastest value=%d\n",absI.value);
            //printf("y abs min=%d\n",absI.minimum);
            //printf("y abs max=%d\n",absI.maximum);
            y = absI.value;

            //得到按压轴的abs信息
            ioctl(fd, EVIOCGABS(ABS_PRESSURE), &absI);
            //printf("pressure abs lastest value=%d\n",absI.value);
            //printf("pressure abs min=%d\n",absI.minimum);
            //printf("pressure abs max=%d\n",absI.maximum);
            pressure = absI.value;
            /*从fd中读取sizeof(struct input_event)那么多字节的数据，放到event结构体变量的内存地址的内存块中
              字节和该数据结构对此，因此可以直接通过这个数据结构方便地读出数据*/
            read(fd, &event, sizeof(struct input_event));


            (*g_env)->CallVoidMethod(g_env, g_obj, callback, x, y, pressure, event.type, event.code, event.value);
            lastX = x;
            lastY = y;
        }

    }
}