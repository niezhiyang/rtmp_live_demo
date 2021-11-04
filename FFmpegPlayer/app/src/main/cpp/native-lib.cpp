#include <jni.h>
#include <string>


extern "C" {
// 因为ffempg都是c写的，咱们的工程是cpp的，所以要加入 extern c，否则找不到，对应的使用方法
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "android/native_window_jni.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
#include <libswresample/swresample.h>

}

#include "android/log.h"

#define TAG "zhiyang"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEFAULT,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)


extern "C"
JNIEXPORT jint JNICALL
Java_com_nzy_ffmpegplayer_MainActivity_play(JNIEnv *env, jobject thiz, jstring _path,
                                            jobject surface) {
    const char *path = env->GetStringUTFChars(_path, NULL);

    // 注册所有的组件，已经过时了，源码说没有必要注册了，已经测过了 可以不写
    avcodec_register_all();
    // 实例化上下文
    AVFormatContext *avFormatContext = avformat_alloc_context();
    // 打开一个文件 或者是 视频流
    int ret = avformat_open_input(&avFormatContext, path, NULL, NULL);
    if (ret != 0) {
        LOGE("打开文件失败 %s", path);
        return -1;
    }
    LOGE("打开文件成功 %s", path);

    // 查找文件的流信息
    int info = avformat_find_stream_info(avFormatContext, NULL);
    if (info < 0) {
        LOGE("查找stream  错误了");
        return -1;
    }
    LOGE("查找stream  成功了");



    // 解封装，找到音频流 和 视频流
    int videoIndex = -1;
    // 流个数，字幕流，音频流 ，视频流，叠加的视频流
    unsigned int streams = avFormatContext->nb_streams;

    for (int i = 0; i < streams; i++) {
        AVStream **pStream = avFormatContext->streams;
        // 遍历找到流
        AVStream *pAvStream = pStream[i];
        // 找到视频流
        if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
        }
    }
    // 找到了视频流
    if (videoIndex == -1) {
        LOGE("没找到视频流的轨道");
        return -1;
    }
    LOGE("找到了视频流轨道 %d  %d", videoIndex, streams);

    // 解析视频流，有可能是H264 有可能是H265，对应的算法

    // 获取解码器上下文
    AVCodecContext *avCodecContext = avFormatContext->streams[videoIndex]->codec;
    // 获取解码器
    AVCodec *avCodec = avcodec_find_decoder(avCodecContext->codec_id);

    // 打开解码器 ,后面的可以是个NULL
    int open = avcodec_open2(avCodecContext, avCodec, NULL);

    if (open != 0) {
        LOGE("打开解码器失败");
        return -1;
    }
    LOGE("打开解码器成功");

    //找到 Surface中的Window 获取界面传下来的surface
    //  在 CmakerList中加入 android 的target_link_libraries 即使 ndk系统目录下的libandroid.so
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    if (!window) {
        LOGE("找不到Native的Window");
        return -1;
    }

    // x264 性能兼容很强，主要是编码 h264，ffmepg 在编码上功能又不强，所以可以集成 x264的
    // avPacket --> CPU(软解码) --> AvFrame(1080*1920) --> 临时 AvFrame(500*400用来适应surface) --> surface(500*400)
    // 三个容器

    //  avPacket ffmpeg自己知道 他的大小
    AVPacket *avPacket = av_packet_alloc();
    //  avFrame   ffmpeg自己知道 他的大小
    AVFrame *avFrame = av_frame_alloc();
    //实例化    适配的frame ，这个 ffmpeg 不知道，所以咱们要算出来他的大小
    AVFrame *adapterFrame = av_frame_alloc();
    // 确定 adapterFrame 跟输入 和 输出surface 有关系
    int width = avCodecContext->width;
    int height = avCodecContext->height;

    //----------------- 输入 -------------------------
    // 输入的    后面的是 是以 几个字节对齐
    // 通过指定像素格式，图像宽高，来计算说需要的内存大小，写 1 证明就是对应的大小
    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, width, height, 1);

    // 实例化一个输入缓冲区
    uint8_t *outbuffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    // 缓冲区 设置给 adapterFrame
    av_image_fill_arrays(adapterFrame->data, adapterFrame->linesize, outbuffer, AV_PIX_FMT_RGBA,
                         width,
                         height, 1);

    //------------------ 转化器 -----------------
    SwsContext *swsContext = sws_getContext(width, height, avCodecContext->pix_fmt,
                                            width, height, AV_PIX_FMT_RGBA, SWS_BICUBIC, NULL, NULL,
                                            NULL);

    // 拿到Window的缓冲区
    int geometry = ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);
    if (geometry != 0) {
        LOGE("找不到windoow的buffer.\n");
        ANativeWindow_release(window);
        return -1;
    }

    while (!av_read_frame(avFormatContext, avPacket)) {
        // 读出来的数据，就是 avPacket，入参出参
        // avPacket->data 就是 h264 二进制 数据 000001 67。。。

        if (avPacket->stream_index == videoIndex) {
            // 视频流的index
            // 解码,因为视频是 h264 要弄成yuv

            // 解码
            // avcodec_send_packet(av)
            // avcodec_receive_frame()
            // 编码下面是成对出现的
            // avcodec_send_frame()
            // avcodec_receive_packet()

            int packetRet = avcodec_send_packet(avCodecContext, avPacket);
            if (packetRet < 0 && packetRet != AVERROR(EAGAIN) && packetRet != AVERROR_EOF) {
                LOGE("解码出错");
                return -1;
            }
            // 执行到这里 avFrame 就是未压缩的 已经解码过的 RGBA avFrame->data 就是picture
            int frameRet = avcodec_receive_frame(avCodecContext, avFrame);
            if (frameRet == AVERROR(EAGAIN)) {
                continue;
            } else if (frameRet < 0) {
                break;
            }
            // 开始转化 到 adapterFrame
            sws_scale(swsContext, avFrame->data, avFrame->linesize, 0, avCodecContext->height,
                      adapterFrame->data, adapterFrame->linesize);
            ANativeWindow_Buffer windowBuffer;
            if (ANativeWindow_lock(window, &windowBuffer, NULL) < 0) {
                LOGE("错误了：不能锁住 window");
            } else {

                //将图像绘制到界面上，注意这里pFrameRGBA一行的像素和windowBuffer一行的像素长度可能不一致
                //需要转换好，否则可能花屏
                uint8_t *dst = (uint8_t *) windowBuffer.bits;
                for (int h = 0; h < height; h++) {
                    memcpy(dst + h * windowBuffer.stride * 4,
                           outbuffer + h * adapterFrame->linesize[0],
                           adapterFrame->linesize[0]);
                }
                // avFrame->pict_type 帧的类型
                switch (avFrame->pict_type) {
                    case AV_PICTURE_TYPE_I:
                        LOGE("I");
                        break;
                    case AV_PICTURE_TYPE_P:
                        LOGE("P");
                        break;
                    case AV_PICTURE_TYPE_B:
                        LOGE("B");
                        break;
                    default:;
                        break;
                }
                ANativeWindow_unlockAndPost(window);
            }
        }
    }


    av_frame_free(&avFrame);
    av_packet_free(&avPacket);
    sws_freeContext(swsContext);
    avcodec_close(avCodecContext);
    avformat_close_input(&avFormatContext);
    avformat_free_context(avFormatContext);

    // 释放
    avformat_free_context(avFormatContext);
    env->ReleaseStringUTFChars(_path, path);
    return 1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nzy_ffmpegplayer_MainActivity_playSound(JNIEnv *env, jobject thiz, jstring _path) {
    const char *path = env->GetStringUTFChars(_path, NULL);

    // 注册所有的组件，已经过时了，源码说没有必要注册了，已经测过了 可以不写
    avcodec_register_all();
    // 实例化上下文
    AVFormatContext *avFormatContext = avformat_alloc_context();
    // 打开一个文件 或者是 视频流
    int ret = avformat_open_input(&avFormatContext, path, NULL, NULL);
    if (ret != 0) {
        LOGE("打开文件失败 %s", path);
        return -1;
    }
    LOGE("打开文件成功 %s", path);

    // 查找文件的流信息
    int info = avformat_find_stream_info(avFormatContext, NULL);
    if (info < 0) {
        LOGE("查找stream  错误了");
        return -1;
    }
    LOGE("查找stream  成功了");



    // 解封装，找到音频流 和 视频流
    int videoIndex = -1;
    // 流个数，字幕流，音频流 ，视频流，叠加的视频流
    unsigned int streams = avFormatContext->nb_streams;

    for (int i = 0; i < streams; i++) {
        AVStream **pStream = avFormatContext->streams;
        // 遍历找到流
        AVStream *pAvStream = pStream[i];
        // 找到视频流
        if (pAvStream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            videoIndex = i;
        }
    }
    // 找到了音频流
    if (videoIndex == -1) {
        LOGE("没找到视频流的轨道");
        return -1;
    }
//    LOGE("找到了视频流轨道 %d  %d", videoIndex, streams);
    // 获取解码器上下文
    AVCodecContext *avCodecContext = avFormatContext->streams[videoIndex]->codec;
    // 获取解码器
    AVCodec *avCodec = avcodec_find_decoder(avCodecContext->codec_id);

    // 打开解码器 ,后面的可以是个NULL
    int open = avcodec_open2(avCodecContext, avCodec, NULL);

    if (open != 0) {
        LOGE("打开解码器失败");
        return -1;
    }
    LOGE("打开解码器成功");
    ///////////////////////////// 上面都一样 //////////////////////////////////
    // avPacket --> CPU(软解码) --> AvFrame  --> 转换成AudioTrack的格式--> AudioTrack 去播放

    //  avPacket ffmpeg自己知道 他的大小
    AVPacket *avPacket = av_packet_alloc();
    //  avFrame   ffmpeg自己知道 他的大小
    AVFrame *avFrame = av_frame_alloc();

    // 获取通道数
    int channals = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);

    jclass pJclass = env->GetObjectClass(thiz);
    jmethodID createTrackId = env->GetMethodID(pJclass, "createTrack", "(II)V");

    jmethodID playTrackId = env->GetMethodID(pJclass, "playTrack", "([BI)V");

    env->CallVoidMethod(thiz, createTrackId, 44100, channals);

    //    1s的pcm个数
    uint8_t *out_buffer = (uint8_t *) av_malloc(44100 * 2);

    //转换器上下文
    SwrContext *swrContext = swr_alloc();
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    enum AVSampleFormat out_formart = AV_SAMPLE_FMT_S16;
    int out_sample_rate = avCodecContext->sample_rate;
    //    设置转换器
    swr_alloc_set_opts(swrContext, out_ch_layout, out_formart, out_sample_rate,
                       avCodecContext->channel_layout, avCodecContext->sample_fmt,
                       avCodecContext->sample_rate, 0, NULL
    );
    swr_init(swrContext);

    while (!av_read_frame(avFormatContext, avPacket)) {
        // 读出来的数据，就是 avPacket，入参出参
        // avPacket->data 就是 h264 二进制 数据 000001 67。。。

        if (avPacket->stream_index == videoIndex) {
            // 视频流的index
            // 解码,因为视频是 h264 要弄成yuv

            // 解码
            // avcodec_send_packet(av)
            // avcodec_receive_frame()
            // 编码下面是成对出现的
            // avcodec_send_frame()
            // avcodec_receive_packet()

            int packetRet = avcodec_send_packet(avCodecContext, avPacket);
            if (packetRet < 0 && packetRet != AVERROR(EAGAIN) && packetRet != AVERROR_EOF) {
                LOGE("解码出错");
                return -1;
            }
            // 执行到这里 avFrame 就是未压缩的 已经解码过的 RGBA avFrame->data 就是picture
            int frameRet = avcodec_receive_frame(avCodecContext, avFrame);
            if (frameRet == AVERROR(EAGAIN)) {
                continue;
            } else if (frameRet < 0) {
                break;
            }
            ///////////////////////////////   audio转化器      /////////////////////////////
            // 重采样，转化
            swr_convert(swrContext, &out_buffer, 44100 * 2,
                        (const uint8_t **)(avFrame->data), avFrame->nb_samples);

            LOGE("解码成功");
            int size = av_samples_get_buffer_size(NULL, channals, avFrame->nb_samples,
                                                  AV_SAMPLE_FMT_S16, 1);
            jbyteArray audioArray = env->NewByteArray(size);
            // 把 c的字节数组 给 jbyteArray
            env->SetByteArrayRegion(audioArray, 0, size,
                                    reinterpret_cast<const jbyte *>(out_buffer));

            env->CallVoidMethod(thiz, playTrackId, audioArray, size);

        }
    }


    av_frame_free(&avFrame);
    av_packet_free(&avPacket);
    swr_free(&swrContext);

    avcodec_close(avCodecContext);
    avformat_close_input(&avFormatContext);
    avformat_free_context(avFormatContext);

    // 释放
    avformat_free_context(avFormatContext);
    env->ReleaseStringUTFChars(_path, path);

    return 0;
}

