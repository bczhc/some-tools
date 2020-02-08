LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := doJNI
LOCAL_SRC_FILES := ./c/codecs/doJNI.c ./zhc.c ./c/codecs/Base128Lib.c ./c/codecs/qmcLib.c ./c/codecs/kwm.c ./jni_help.c
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_MODULE    := pi
LOCAL_SRC_FILES := ./cpp/pi/pi.cpp ./zhc.cpp ./jni_help.cpp
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := a
LOCAL_SRC_FILES := ./c/verification/verify.c ./zhc.c ./c/codecs/qmcLib.c jni_help.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := fb_tools
LOCAL_SRC_FILES := ./c/floatingboard/fb_tools.c jni_help.c ./zhc.c
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_SRC_FILES	:= ./c/floatingboard/bitmap.c
LOCAL_LDLIBS += -ljnigraphics
LOCAL_MODULE	:= bitmap
include $(BUILD_SHARED_LIBRARY)