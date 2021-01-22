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
)
target_link_libraries(myLib sqlite3)

file(GLOB sources src/*.cpp src/**/*.cpp)
add_library(Main SHARED ${sources})
target_link_libraries(Main myLib sqlite3)