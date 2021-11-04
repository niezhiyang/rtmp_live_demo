cmake_minimum_required(VERSION 3.10.2)
include_directories(include)
project("ffmpegpalyer")


#添加一个系统查找目录
#添加系统环境变量
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")
add_library( # Sets the name of the library.
        native-lib

        SHARED
        native-lib.cpp )


find_library(
        log-lib
        log )


target_link_libraries( # Specifies the target library.
        native-lib
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale
        # 系统 android.so 主要用Window.jni
        android
        ${log-lib} )