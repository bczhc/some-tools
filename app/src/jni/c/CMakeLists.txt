cmake_minimum_required(VERSION 3.14)

PROJECT(jni C)

INCLUDE_DIRECTORIES(/home/zhc/bin/android-sdk-linux/ndk/21.0.6113669/sysroot/usr/include)

set(CMAKE_C_STANDARD 99)
add_executable(verification verification/verify.c ../zhc.c ./codecs/qmcLib.c floatingboard/fb_tools.c)
add_executable(doJni codecs/doJNI.c)
add_executable(getevent ./getevent/geteventV2.c)