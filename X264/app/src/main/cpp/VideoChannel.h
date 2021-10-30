//
// Created by MOMO on 10/27/21.
//

#ifndef TOUPING_VIDEOCHANNEL_H
#define TOUPING_VIDEOCHANNEL_H

#include "x264/armeabi-v7a/include/x264.h"
#include "JavaCallHelper.h"
#include "librtmp/rtmp.h"



class VideoChannel {
    // 函数 指针
    typedef void (*VideoCallback)(RTMPPacket *packet);
public:
    VideoChannel();

    virtual ~VideoChannel();

    // 创建 编码器
    void setVideoEncInfo(int width,int height,int fps,int bitrate);

    // 真正编码一帧的数据
    void encodeData(int8_t *data);

    JavaCallHelper *javaCallHelper;

    void sendSpsPps(uint8_t sps[100], uint8_t pps[100], int len, int len1);

    void setCallback(VideoCallback callback) ;

private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
    int ySize;
    int uvSize;
    x264_t *videoCodec;
    x264_picture_t  *pic_in = 0;
    VideoCallback callback;


    void sendFrame(int type, int payload, uint8_t *payload1);
};


#endif //TOUPING_VIDEOCHANNEL_H
