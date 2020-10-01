CMAKE_MINIMUM_REQUIRED(VERSION 3.14)

PROJECT(cpp)

INCLUDE_DIRECTORIES(/home/zhc/bin/AndroidSdk/ndk/21.3.6528147/sources/cxx-stl/llvm-libc++/include)
INCLUDE_DIRECTORIES(/home/zhc/bin/AndroidSdk/ndk/21.3.6528147/sysroot/usr/include)

FILE(GLOB ALL ./src/*.cpp ./src/*/*.cpp)
add_executable(ALL ${ALL})