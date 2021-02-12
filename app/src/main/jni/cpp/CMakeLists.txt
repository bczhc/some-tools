CMAKE_MINIMUM_REQUIRED(VERSION 3.10.0)
project(jni)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_STANDARD 17)

add_library(sqlite3 STATIC third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.c)
add_library(
        myLib STATIC
        third_party/my-cpp-lib/math/fourier_series.cpp
        third_party/my-cpp-lib/math/epicycle.cpp
        third_party/my-cpp-lib/math/complex_value.cpp
        third_party/my-cpp-lib/math/complex_integral.cpp
        third_party/my-cpp-lib/app/charactersStatistics/CountCharacters.cpp
        third_party/my-cpp-lib/utf8.cpp
        third_party/my-cpp-lib/concurrent.cpp
        third_party/my-cpp-lib/sqlite3.cpp
        third_party/my-cpp-lib/io.cpp
        third_party/my-cpp-lib/string.cpp
        third_party/my-cpp-lib/app/characterReverser/reverseCharacters.cpp
        third_party/my-cpp-lib/app/stc_flash/stc_flash_lib.cpp
        third_party/my-cpp-lib/utils.cpp
        third_party/my-cpp-lib/file.cpp
        third_party/my-cpp-lib/third_party/crypto-algorithms/sha256.c
        third_party/my-cpp-lib/exception.cpp
        third_party/my-cpp-lib/app/base128/Base128Lib.cpp
)
target_link_libraries(myLib sqlite3)

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
        src/sqlite3/sqlite3.cpp
        src/sqlite3/statement.cpp
        src/sqlite3/cursor.cpp
        src/stc_flash/serial_jni.cpp
        src/stc_flash/stc_flash.cpp
        src/test/Test.cpp
        src/jni_help.cpp
        src/test.cpp
)

target_link_libraries(Main myLib sqlite3)