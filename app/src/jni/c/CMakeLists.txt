cmake_minimum_required(VERSION 3.14)

PROJECT(jni C)

INCLUDE_DIRECTORIES(/home/zhc/bin/android-sdk-linux/ndk-bundle/sysroot/usr/include)

set(CMAKE_C_STANDARD 99)
add_executable(verification verification/verify.c ../zhc.c ./codecs/qmcLib.c floatingboard/fb_tools.c)
add_executable(doJni codecs/codecsDo.c codecs/kwm.c codecs/Base128Lib.c)
