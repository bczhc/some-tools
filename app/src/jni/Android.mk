LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := All
LOCAL_CFLAGs	:= -O3 -std=c++11
SRC_LIST :=$(wildcard $(LOCAL_PATH)/cpp/src/*.cpp wildcard $(LOCAL_PATH)/cpp/src/**/*.cpp)
LOCAL_SRC_FILES = $(SRC_LIST:$(LOCAL_PATH)/%=%)
include $(BUILD_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_CFLAGS	:= -O3
#LOCAL_MODULE    := test
#LOCAL_SRC_FILES := cpp/src/test.cpp
#include $(BUILD_SHARED_LIBRARY)
