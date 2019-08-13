LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := doJNI
LOCAL_SRC_FILES := ./c/codecs/doJNI.c ./zhc.c ./c/codecs/Base128Lib.c ./c/codecs/qmcLib.c ./c/codecs/kwm.c
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE    := pi
LOCAL_SRC_FILES := ./jni_c++/pi/pi.cpp ./zhc.cpp
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := a
LOCAL_SRC_FILES := ./c/verification/verify.c ./zhc.c ./c/codecs/qmcLib.c
include $(BUILD_SHARED_LIBRARY)