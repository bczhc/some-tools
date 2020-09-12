LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := sqlite3
LOCAL_CFLAGS := -O3
LOCAL_SRC_FILES := $(LOCAL_PATH)/cpp/third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.c
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := myLib
LOCAL_CFLAGS := -O3
LOCAL_SRC_FILES := cpp/third_party/my-cpp-lib/zhc.cpp\
				   cpp/third_party/my-cpp-lib/FourierSeries.cpp\
                   cpp/third_party/my-cpp-lib/Epicycle.cpp\
                   cpp/third_party/my-cpp-lib/ComplexValue.cpp\
                   cpp/third_party/my-cpp-lib/ComplexIntegral.cpp\
                   cpp/third_party/my-cpp-lib/CountCharacters.cpp\
                   cpp/third_party/my-cpp-lib/utf8.cpp
LOCAL_SHARED_LIBRARIES := sqlite3
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := All
LOCAL_CPPFLAGS := -O3
MY_SRC_LIST := $(wildcard $(LOCAL_PATH)/cpp/src/*.cpp wildcard $(LOCAL_PATH)/cpp/src/**/*.cpp)
LOCAL_SRC_FILES := $(MY_SRC_LIST:$(LOCAL_PATH)/%=%)
LOCAL_SHARED_LIBRARIES := myLib sqlite3
include $(BUILD_SHARED_LIBRARY)
