//
// Created by root on 2020/1/25.
//

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/epoll.h>
#include <sys/wait.h>
#include <sys/un.h>
#include <cstddef>
#include <linux/input.h>

#include "input_pen.h"
#include "debug.h"



//驱动路径
#define DEV_PATH "/dev/input/event1"

//最大侦听
#define MAX_EVENTS 1

//epoll_wait的最大时间
#define EPOLL_SLEEP 200

//线程是否由pthread创建，否则由JVM创建
#define USING_PTHREAD 0


#if USING_PTHREAD
#define LOGD(fmt, arg...) do{printf(fmt"\n", ##arg);}while(0)
#define LOGE LOGD
#endif

/**************************************************************************************/

//static variable

//当前驱动文件指针
static int fd = 0;
//epoll文件指针
static int epoll_fd = 0;
//epoll_event数组
static struct epoll_event events[MAX_EVENTS];
//回调函数
static input_callback *callbacks = NULL;

//标记线程是否已启动
static unsigned char start_flag = 0;
//标记线程是否已退出
static unsigned char exit_flag = 0;
//线程锁
static pthread_mutex_t exit_mutex;

/**************************************************************************************/


//set non-blocking for fd
static int set_non_blocking(int fd) {
    int opts;

    opts = fcntl(fd, F_GETFL);
    if (opts < 0) {
        LOGE("fcntl F_GETFL error: %s", strerror(errno));
        return -1;
    }
    opts = (opts | O_NONBLOCK);
    if (fcntl(fd, F_SETFL, opts) < 0) {
        LOGE("fcntl F_SETFL error: %s", strerror(errno));
        return -1;
    }

    return 0;
}

//register epoll events for fd
static void epoll_register( int  epoll_fd, int  fd ) {
    struct epoll_event  ev;
    int         ret;

    ev.events  = EPOLLIN;//interested in receiving data
    ev.data.fd = fd;

    do {
        //register events for fd
        ret = epoll_ctl( epoll_fd, EPOLL_CTL_ADD, fd, &ev );
    } while (ret < 0 && errno == EINTR);

}



//remove epoll events for fd
static void epoll_unregister( int  epoll_fd, int  fd ) {
    int  ret;
    do {
        ret = epoll_ctl( epoll_fd, EPOLL_CTL_DEL, fd, NULL );
    } while (ret < 0 && errno == EINTR);

}


//通知退出线程，由其他线程调用
static void thread_cancel() {
    LOGD("thread_cancel");
    pthread_mutex_lock(&exit_mutex);

    exit_flag = 1;

    pthread_mutex_unlock(&exit_mutex);
}

//停止线程，由本线程调用
static void thread_exit() {
    unsigned char flag ;


    pthread_mutex_lock(&exit_mutex);

    flag = exit_flag;

    pthread_mutex_unlock(&exit_mutex);

    if (flag == 1) {
        LOGD("thread_exit");
        //close devices
        close(fd);

        //clean variablies
        fd = 0;
        epoll_fd = 0;
        start_flag = 0;
        exit_flag = 0;
        //release thread resources
        if (callbacks != NULL && callbacks->detach_thread_cb != NULL) {
            callbacks->detach_thread_cb();
            LOGD("callbacks->detach_thread_cb();\n");
        }

        //exit current thread
        pthread_exit(NULL);


    }
}


//线程运行函数
#if USING_PTHREAD
static void *run(void *args) {
#else
static void run(void *args) {

#endif
    int n = 0;
    int i = 0;
    int res;
    struct input_event event;

    LOGD("run...");
    while (1) {

        thread_exit();//每次检测是否要退出运行


        n = epoll_wait(epoll_fd, events, MAX_EVENTS, EPOLL_SLEEP);//检测是否有事件发生
        if (n == -1) {
            LOGE("epoll_wait error:%s", strerror(errno));
            continue;
        }

        for (i = 0; i < n; i++) {
            if (events[i].data.fd == fd) { //有读事件发生
                res = read(fd, &event, sizeof(event));
                if (res < (int)sizeof(event)) {
                    LOGE("could not get event\n");
                    continue;
                }

#if (!USING_PTHREAD)
                //把input_event的数据回调到java层
                if (callbacks != NULL && callbacks->get_event_cb != NULL) {
                    callbacks->get_event_cb(event.type, event.code, event.value);
                }
#else
                //printf("[%8ld.%06ld] ", event.time.tv_sec, event.time.tv_usec);
                if (event.type == EV_ABS) {
                    printf("%04x %04x %08x\n", event.type, event.code, event.value);
                }
#endif
            }
        }
    }
#if USING_PTHREAD
    return NULL;
#else
    return ;
#endif

}


//初始化函数
unsigned char input_pen_init(input_callback *cb) {

    pthread_t thread;

    LOGD("input_pen_init");
    if (start_flag) {
        return 1;
    }

    //callbacks
    callbacks = cb;

    //open device
    fd = open(DEV_PATH, O_RDWR);
    if (fd < 0) {
        LOGE("open device failed!\n");
        return 0;
    }

    //create epoll
    epoll_fd = epoll_create(MAX_EVENTS);
    if (epoll_fd == -1) {
        LOGE("epoll_create failed!\n");
        return 0;
    }
    //set non-blocking
    set_non_blocking(fd);

    //epoll register
    epoll_register(epoll_fd, fd);

    //mutex
    if (pthread_mutex_init(&exit_mutex, NULL) != 0) {
        LOGE("pthread_mutex_initn failed!");
        return 0;
    }

    //create thread
#if USING_PTHREAD
    if (pthread_create(&thread, NULL, run, (void *)NULL) != 0) {
        LOGE("pthread_create failed!\n");
        return 0;
    }
#else
    if (callbacks != NULL && callbacks->create_thread_cb != NULL) {
        thread = callbacks->create_thread_cb("input_pen_thread", run, NULL);
        if (thread == 0) {
            LOGE("create thread failed!\n");
            return 0;
        }

        start_flag = 1;
        LOGD("input_pen_init success!");
        return 1;

    }
#endif



    return 0;

}

//退出函数
void input_pen_exit() {
    thread_cancel();
}

#if USING_PTHREAD
int main() {
    int count = 0;
    input_pen_init(NULL);

    while (1) {
        sleep(1);
        count ++;
        if (count == 20) {
            thread_cancel();
            sleep(1);
            break;
        }
    }
    return 0;
}
#endif