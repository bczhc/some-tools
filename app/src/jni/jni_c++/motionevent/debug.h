//
// Created by root on 2020/1/25.
//

#ifndef CPP_DEBUG_H
#define CPP_DEBUG_H

#endif //CPP_DEBUG_H

#ifndef _DEBUG_H
#define _DEBUG_H

//#include <utils/Log.h>
#include "ALog.h"

#ifdef ALOGD
#define LOGD      ALOGD
#endif
#ifdef ALOGV
#define LOGV      ALOGV
#endif
#ifdef ALOGE
#define LOGE      ALOGE
#endif
#ifdef ALOGI
#define LOGI      ALOGI
#endif

#define LOG_TAG "InputPen"

#endif