//
// Created by root on 2020/1/25.
//

#ifndef CPP_ALOG_H
#define CPP_ALOG_H

#endif //CPP_ALOG_H

#pragma once

#include<android/log.h>

#define LOG_TAG "debug log"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args)