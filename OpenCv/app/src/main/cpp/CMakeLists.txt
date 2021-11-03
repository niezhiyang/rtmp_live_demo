
cmake_minimum_required(VERSION 3.10.2)
# 添加头文件
include_directories(include)
#把 本目录 的lib下对应的文件夹设置为系统环境变量
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")
message(-------------)
message(${CMAKE_SOURCE_DIR})

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)

# 这里相当于 给库 起别名
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        native-lib
        # 这个就是静态库 或者 动态库so的名称 libopencv_java4.so
        opencv_java4

        ${log-lib}
         # 使用在android.so
         android

        )