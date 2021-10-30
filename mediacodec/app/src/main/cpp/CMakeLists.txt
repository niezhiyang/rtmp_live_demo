
cmake_minimum_required(VERSION 3.10.2)
# 添加librtmp文件夹
add_subdirectory(librtmp)

#定义 本文件下的所有的 的 cpp 引入到一个 native_lib 变量中
file(GLOB native_source *.cpp)

add_library( # Sets the name of the library.
        native-lib

        SHARED

        # 这里可以直接使用这个变量引入，不用一个个的cpp去添加
        ${native_source}
        )


find_library(
        log-lib

        log)

target_link_libraries(
        native-lib

        ${log-lib}
        # 把 rtmp 链接到 咱们打包的so中，这里的名字 就是 librtmp 文件夹下的 CmakeLists.txt 中的 打包的静态库
        rtmp
)