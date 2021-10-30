//
// Created by MOMO on 10/28/21.
//

#ifndef X264_LOG_H
#define X264_LOG_H
#include "android/log.h"


#define TAG "zhiyang"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEFAULT,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)

#endif //X264_LOG_H
