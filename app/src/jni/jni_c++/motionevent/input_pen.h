//
// Created by root on 2020/1/25.
//

#ifndef CPP_INPUT_PEN_H
#define CPP_INPUT_PEN_H

#endif //CPP_INPUT_PEN_H


#ifndef _INPUT_PEN_H
#define _INPUT_PEN_H


#include <pthread.h>
#include <linux/input.h>
#include <sys/types.h>
#include <linux/types.h>

#ifdef _cplusplus
extern "C" {
#endif

//获取input_event数据的方法指针，由App层提供
typedef void (*get_event_callback)(__u16 type, __u16 code, __s32 value );

//创建线程的函数指针，通过Java虚拟机创建
typedef pthread_t (*create_thread_callback)(const char* name, void (*start)(void *), void* arg);

//释放线程资源的函数指针
typedef int (*detach_thread_callback)(void);

//回调函数结构体
typedef struct {
    get_event_callback get_event_cb;
    create_thread_callback create_thread_cb;
    detach_thread_callback detach_thread_cb;
} input_callback;

/*******************************************************************/
//public methods

//初始化函数
unsigned char input_pen_init(input_callback *callback);

//退出函数
void input_pen_exit();


/*******************************************************************/


#ifdef _cplusplus
}
#endif

#endif