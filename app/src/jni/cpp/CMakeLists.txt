CMAKE_MINIMUM_REQUIRED(VERSION 3.14)

PROJECT(cpp)

INCLUDE_DIRECTORIES(/home/zhc/bin/android-sdk-linux/ndk-bundle/sysroot/usr/include)

FILE(GLOB ALL ./src/*.cpp ./src/*/*.cpp)
add_executable(ALL ${ALL})