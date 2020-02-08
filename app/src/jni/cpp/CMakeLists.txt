cmake_minimum_required(VERSION 3.15)

PROJECT(cpp)

INCLUDE_DIRECTORIES(/home/zhc/bin/android-sdk-linux/ndk-bundle/sysroot/usr/include)

add_executable(pi pi/pi.cpp)
add_executable(input_pen ./motionevent/input_pen.cpp)
add_executable(com_jiagutech_input_InputPen ./motionevent/com_jiagutech_input_InputPen.cpp)
add_executable(JNIHelper ./motionevent/JNIHelper.cpp)
add_executable(android_native_app_glue ./motionevent/android_native_app_glue.cpp)