cmake_minimum_required(VERSION 3.4.1)
add_subdirectory(librtmp)
message("-------------------")
message(${CMAKE_BUILD_TYPE})
message(${ANDROID_PLATFORM})
message(${CMAKE_SOURCE_DIR})
message(${ANDROID_ABI})
# 把 本目录下的所有的 cpp文件都 引入到 cpp_source 这个变量
file(GLOB cpp_source  *.cpp)

######################### 这就是库文件 和 头文件导入的方式
######################################### x264 ################################
#头文件成功引入 相当于给下面的 target_link_libraries 配置了一个系统路径
#比如 log，会从系统的路径找，所以x264 也会从系统找，如果找不到就从这个路径找，
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)

#弄成 系统变量 x264
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
######################################### faac  ################################
include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)
# faac
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")

add_library(
             native-lib
             SHARED
             # 这里就是引用所有本文件加的cpp
             ${cpp_source}
)

target_link_libraries( # Specifies the target library.
                       native-lib
                       # Links the target library to the log library
                       # included in the NDK.
                        log
                         rtmp
                        x264
                        faac
        )