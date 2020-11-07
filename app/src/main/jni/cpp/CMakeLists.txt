CMAKE_MINIMUM_REQUIRED(VERSION 3.10.2)
project(jni)

add_library(sqlite3 STATIC third_party/my-cpp-lib/third_party/sqlite3-single-c/sqlite3.c)
add_library(myLib STATIC
        third_party/my-cpp-lib/zhc.cpp
        third_party/my-cpp-lib/FourierSeries.cpp
        third_party/my-cpp-lib/Epicycle.cpp
        third_party/my-cpp-lib/ComplexValue.cpp
        third_party/my-cpp-lib/ComplexIntegral.cpp
        third_party/my-cpp-lib/CountCharacters.cpp
        third_party/my-cpp-lib/utf8.cpp
        third_party/my-cpp-lib/Concurrent.cpp
        third_party/my-cpp-lib/Sqlite3.cpp
        third_party/my-cpp-lib/io.cpp
        third_party/my-cpp-lib/String.cpp
        third_party/my-cpp-lib/reverseCharacters.cpp)
target_link_libraries(myLib sqlite3)

file(GLOB sources src/*.cpp src/**/*.cpp)
add_library(Main SHARED ${sources})
target_link_libraries(Main myLib sqlite3)