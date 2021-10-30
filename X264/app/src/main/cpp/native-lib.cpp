#include <jni.h>
#include <string>
#include <pthread.h>
#include "VideoChannel.h"
#include "safe_queue.h"

extern "C" {
#include "librtmp/rtmp.h"
}

#include "JavaCallHelper.h"
#include "Log.h"
#include "AudioChannel.h"


JavaCallHelper *helper = 0;

VideoChannel *videoChannel = 0;
AudioChannel *audioChannel = 0;

// 是否已经开始
int isStart = false;
//记录子线程的对象
pthread_t pid;

//记录一个开始推流的时间
uint32_t start_time;
//推流标志位
int readyPushing = 0;
//阻塞式队列
SafeQueue<RTMPPacket *> packets;

RTMP *rtmp = 0;

// 虚拟机引用
JavaVM *javaVm = 0;

// 从 JavaCannel
void callback(RTMPPacket *packet) {
    if (packet) {
        // 直播中 如果堆积的太多，就直接清空掉，那最新的就可以了，这个如果过多 ，会有延时
        // 也不怕 packets 过大
        if (packets.size() > 50) {
            packets.clear();
        }
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}

// 固定的函数，可以拿到 JavaVm 虚拟机给你的
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVm = vm;
    LOGE("保存虚拟机的引用");
    return JNI_VERSION_1_4;
}

void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = 0;
    }
}

void *start(void *args) {
    LOGE("准备开始链接");
    // 开启 RTMP
    char *url = static_cast<char *>(args);
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("rtmp创建失败");
            break;
        }
        RTMP_Init(rtmp);
        //设置超时时间 5s
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("rtmp设置地址失败:%s", url);
            break;
        }
        //开启输出模式
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            LOGE("rtmp连接地址失败:%s", url);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);

        LOGE("rtmp连接成功----------->:%s", url);
        if (!ret) {
            LOGE("rtmp连接流失败:%s", url);
            break;
        }




        //准备好了 可以开始推流了
        readyPushing = 1;
        //记录一个开始推流的时间
        start_time = RTMP_GetTime();
        packets.setWork(1);

        // 音频的头 需要链接到服务器 第一个发送的
        RTMPPacket *packetAudioHead = audioChannel->getAudioConfig();
        callback(packetAudioHead);


        RTMPPacket *packet = 0;
        //循环从队列取包 然后发送
        while (isStart) {
            packets.pop(packet);
            if (!isStart) {
                break;
            }
            if (!packet) {
                continue;
            }
            // 给rtmp的流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            //发送包 1:加入队列发送
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("发送数据失败");
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    return 0;

}

extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_x264_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
    // 在 C 中不需要 加 （），构造函数
    helper = new JavaCallHelper(javaVm, env, thiz);
    videoChannel = new VideoChannel();
    videoChannel->setCallback(callback);
    videoChannel->javaCallHelper = helper;


}extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_x264_LivePusher_native_1start(JNIEnv *env, jobject thiz, jstring _path) {
    if (isStart) {
        return;
    }
    // 因为 path 是 const ，所以又找了一个 url 变量
    const char *path = env->GetStringUTFChars(_path, NULL);
    char *url = new char[strlen(path) + 1];
    stpcpy(url, path);

    // 标志 表示开始直播
    isStart = 1;

    // 开启子线程
    pthread_create(&pid, 0, start, url);

    env->ReleaseStringUTFChars(_path, path);

}
extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_x264_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject thiz, jint width,
                                                     jint height,
                                                     jint fps,
                                                     jint bitrate) {

    if (videoChannel) {
        videoChannel->setVideoEncInfo(width, height, fps, bitrate);
    }

}



// 一帧数据 yuv数据
extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_x264_LivePusher_native_1pushVideo(JNIEnv *env, jobject thiz, jbyteArray _data) {
    if (!videoChannel || !readyPushing) {
        return;
    }
    jbyte *data = env->GetByteArrayElements(_data, NULL);
    videoChannel->encodeData(data);
    env->ReleaseByteArrayElements(_data, data, 0);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_x264_LivePusher_native_1stop(JNIEnv *env, jobject thiz) {

}

extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_x264_LivePusher_native_1release(JNIEnv *env, jobject thiz) {
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = 0;
    }
    if (helper) {
        delete (helper);
        helper = 0;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nzy_x264_LivePusher_initAudioEnc(JNIEnv *env, jobject thiz, jint sample_rate,
                                          jint channels) {
    audioChannel = new AudioChannel();
    audioChannel->setCallback(callback);
    audioChannel->openCodec(sample_rate,channels);

    return audioChannel->inputByteNum;
}extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_x264_LivePusher_sendAudio(JNIEnv *env, jobject thiz, jbyteArray buffer, jint len) {
    if(audioChannel){
        jbyte *data = env->GetByteArrayElements(buffer, NULL);
        audioChannel->encode(reinterpret_cast<int32_t *>(data), len/2);
        env->ReleaseByteArrayElements(buffer,data,0);
    }
}