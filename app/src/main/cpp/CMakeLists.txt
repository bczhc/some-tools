CMAKE_MINIMUM_REQUIRED(VERSION 3.10.0)
project(jni)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_STANDARD 17)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR})
add_subdirectory(third_party/libmagic)

include_directories(third_party/jni-lib)
include_directories(.)

add_library(
        myLib-android-dep SHARED
        third_party/jni-lib/third_party/my-cpp-lib/math/complex_integral.cpp
        third_party/jni-lib/third_party/my-cpp-lib/math/complex_value.cpp
        third_party/jni-lib/third_party/my-cpp-lib/math/epicycle.cpp
        third_party/jni-lib/third_party/my-cpp-lib/math/fourier_series.cpp
        third_party/jni-lib/third_party/my-cpp-lib/third_party/crypto-algorithms/sha256.c
        third_party/jni-lib/third_party/my-cpp-lib/concurrent.cpp
        third_party/jni-lib/third_party/my-cpp-lib/app/base128/Base128Lib.cpp
        third_party/jni-lib/third_party/my-cpp-lib/file.cpp
        third_party/jni-lib/third_party/my-cpp-lib/app/stc_flash/stc_flash_lib.cpp
        third_party/jni-lib/third_party/my-cpp-lib/app/stc_flash/serial_linux.cpp
        third_party/jni-lib/third_party/my-cpp-lib/io.cpp
)

add_library(
        Main SHARED
        src/charactercounter/Counter.cpp
        src/codecs/Base128Lib.cpp
        src/codecs/codecsDo.cpp
        src/codecs/kwm.cpp
        src/codecs/qmcLib.cpp
        src/diary/password_digest.cpp
        src/epicycles/calcCall.cpp
        src/floatingboard/fb_tools.cpp
        src/malloc_test/mallocTest.cpp
        src/pi/pi.cpp
        src/stc_flash/serial_jni.cpp
        src/stc_flash/stc_flash.cpp
        src/test/Test.cpp
        src/test.cpp
        src/sys_info/sys_info.cpp
        src/magic/magic.cpp
        src/ucd/ucd_parse.cpp
)

add_subdirectory(third_party/jni-lib)

target_link_libraries(myLib-android-dep myLib)
target_link_libraries(Main myLib myLib-android-dep jni-lib magic)

#set_target_properties(Main myLib PROPERTIES LINK_FLAGS "-Wl,-rpath,./")