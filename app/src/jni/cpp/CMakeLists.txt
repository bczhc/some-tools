cmake_minimum_required(VERSION 3.15)

PROJECT(cpp)

INCLUDE_DIRECTORIES(/home/zhc/bin/android-sdk-linux/ndk-bundle/sysroot/usr/include)

add_executable(pi pi/pi.cpp)
