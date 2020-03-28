LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := All
SRC_LIST :=$(wildcard $(LOCAL_PATH)/cpp/src/*.cpp wildcard $(LOCAL_PATH)/cpp/src/**/*.cpp)
LOCAL_SRC_FILES = $(SRC_LIST:$(LOCAL_PATH)/%=%)
include $(BUILD_SHARED_LIBRARY)