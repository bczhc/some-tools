# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.15

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:


#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:


# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list


# Suppress display of executed commands.
$(VERBOSE).SILENT:


# A target that is always out of date.
cmake_force:

.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /home/zhc/bin/clion-2019.3.2/bin/cmake/linux/bin/cmake

# The command to remove a file.
RM = /home/zhc/bin/clion-2019.3.2/bin/cmake/linux/bin/cmake -E remove -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /home/zhc/code/code/Android/QMCFLAC/app/src/jni

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /home/zhc/code/code/Android/QMCFLAC/app/src/jni/cmake-build-debug

# Include any dependencies generated for this target.
include CMakeFiles/jni_help_cpp.dir/depend.make

# Include the progress variables for this target.
include CMakeFiles/jni_help_cpp.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/jni_help_cpp.dir/flags.make

CMakeFiles/jni_help_cpp.dir/jni_help.cpp.o: CMakeFiles/jni_help_cpp.dir/flags.make
CMakeFiles/jni_help_cpp.dir/jni_help.cpp.o: ../jni_help.cpp
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --progress-dir=/home/zhc/code/code/Android/QMCFLAC/app/src/jni/cmake-build-debug/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building CXX object CMakeFiles/jni_help_cpp.dir/jni_help.cpp.o"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -o CMakeFiles/jni_help_cpp.dir/jni_help.cpp.o -c /home/zhc/code/code/Android/QMCFLAC/app/src/jni/jni_help.cpp

CMakeFiles/jni_help_cpp.dir/jni_help.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/jni_help_cpp.dir/jni_help.cpp.i"
	/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -E /home/zhc/code/code/Android/QMCFLAC/app/src/jni/jni_help.cpp > CMakeFiles/jni_help_cpp.dir/jni_help.cpp.i

CMakeFiles/jni_help_cpp.dir/jni_help.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/jni_help_cpp.dir/jni_help.cpp.s"
	/usr/bin/c++ $(CXX_DEFINES) $(CXX_INCLUDES) $(CXX_FLAGS) -S /home/zhc/code/code/Android/QMCFLAC/app/src/jni/jni_help.cpp -o CMakeFiles/jni_help_cpp.dir/jni_help.cpp.s

# Object files for target jni_help_cpp
jni_help_cpp_OBJECTS = \
"CMakeFiles/jni_help_cpp.dir/jni_help.cpp.o"

# External object files for target jni_help_cpp
jni_help_cpp_EXTERNAL_OBJECTS =

jni_help_cpp: CMakeFiles/jni_help_cpp.dir/jni_help.cpp.o
jni_help_cpp: CMakeFiles/jni_help_cpp.dir/build.make
jni_help_cpp: CMakeFiles/jni_help_cpp.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green --bold --progress-dir=/home/zhc/code/code/Android/QMCFLAC/app/src/jni/cmake-build-debug/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Linking CXX executable jni_help_cpp"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/jni_help_cpp.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/jni_help_cpp.dir/build: jni_help_cpp

.PHONY : CMakeFiles/jni_help_cpp.dir/build

CMakeFiles/jni_help_cpp.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/jni_help_cpp.dir/cmake_clean.cmake
.PHONY : CMakeFiles/jni_help_cpp.dir/clean

CMakeFiles/jni_help_cpp.dir/depend:
	cd /home/zhc/code/code/Android/QMCFLAC/app/src/jni/cmake-build-debug && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /home/zhc/code/code/Android/QMCFLAC/app/src/jni /home/zhc/code/code/Android/QMCFLAC/app/src/jni /home/zhc/code/code/Android/QMCFLAC/app/src/jni/cmake-build-debug /home/zhc/code/code/Android/QMCFLAC/app/src/jni/cmake-build-debug /home/zhc/code/code/Android/QMCFLAC/app/src/jni/cmake-build-debug/CMakeFiles/jni_help_cpp.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/jni_help_cpp.dir/depend
