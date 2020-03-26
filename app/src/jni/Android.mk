LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := codecsDo
LOCAL_SRC_FILES := ./c/codecs/codecsDo.c ./zhc.c ./c/codecs/Base128Lib.c ./c/codecs/qmcLib.c ./c/codecs/kwm.c ./jni_help.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := pi
LOCAL_SRC_FILES := ./cpp/pi/pi.cpp
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := FB_tools
LOCAL_SRC_FILES := ./c/floatingboard/fb_tools.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := mallocTest
LOCAL_SRC_FILES := ./c/malloc_test/mallocTest.c
include $(BUILD_SHARED_LIBRARY)
