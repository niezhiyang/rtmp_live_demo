//
// Created by MOMO on 10/27/21.
//

#include <cstdint>
#include <cstring>
#include "VideoChannel.h"

#include "android/log.h"
#include "JavaCallHelper.h"
#include "librtmp/rtmp.h"

#include "Log.h"

VideoChannel::VideoChannel() {
    LOGE("创建VideoChanle");
}

VideoChannel::~VideoChannel() {
    if (videoCodec) {
        LOGE("释放VideoChanle");
        x264_encoder_close(videoCodec);
        videoCodec = 0;
    }
}

void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {
    // 实例化 x 264
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;
    // YUV420  y是 宽x高，u和v 是 y的 1/4
    ySize = width * height;
    uvSize = width * height / 4;

    if (videoCodec) {
        LOGE("释放了 videoCodec value  %d",videoCodec);
        // 如果以前有，先关闭
//        x264_encoder_close(videoCodec);
        videoCodec = 0;
        LOGE("释放了失败 videoCodec value  %d",videoCodec);
    }
    // 参数 相当于 MediaFormate
    x264_param_t param;
    // 1 编码器速度，速度越大 质量越差
    // 2 越往后越差 ,都是从常量里面拿的
    x264_param_default_preset(&param, "ultrafast", "zerolatency");

    // 编码等级 32 ， 41 是4K
    param.i_level_idc = 32;
    // 格式 是 420
    param.i_csp = X264_CSP_I420;
    param.i_width = width;
    param.i_height = height;

    // 设置B帧,没有B帧 ,在直播中一般没B帧，没有缓存，来一帧就渲染一帧
    param.i_bframe = 0;

    // 码率 控制 方法
    //X264_RC_CQP 0 恒定质量(动态码率)比如一帧时一个黑图 则 码率会非常小
    //X264_RC_CRF 1 恒定码率
    //X264_RC_ABR 2 平均码率 这个是用的最多的               1
    param.rc.i_rc_method = X264_RC_ABR;
    // x264 是 以 K为单位的，所以要
    param.rc.i_bitrate = bitrate / 1024;


    // x264 的单位是自己控制的
    //  1/25 j就是时间
    // 帧率 一秒多少帧 比如25帧
    param.i_fps_num = fps;
    // 1 是 1秒
    param.i_fps_den = 1;
    // 分母 单位是 分子/分母
    param.i_timebase_den = param.i_fps_num;
    // 分子
    param.i_timebase_num = param.i_fps_den;

    // 抖音用的是 ffmpeg.so 编码工程 里面有x264，可以认为 x264是ffmpeg 的一个插件
    // MediaCodec硬解DSP是 主 ，x264 用的是CPU 是副，用到的的是一样多

    // fps 而不是时间戳来计算帧之间的距离
    param.b_vfr_input = 0;
    // 2秒一个I帧
    param.i_keyint_max = fps * 2;
    // 是否把 sps pps 赋值到关键帧前面，不用我们自己去加了，所以I帧之前永远是 sps pps
    param.b_repeat_headers =1;
    
    // 线程 就用一个
    param.i_threads =1; 
    // 编码质量 x264_profile_names ，越往右 越高
    // const char *string = x264_profile_names[0];
    x264_param_apply_profile(&param,"baseline");
    // 打开 编码器
    videoCodec = x264_encoder_open(&param);

    pic_in = new x264_picture_t();
    // 初始化大小 也就是是 一帧数据是固定的。
    x264_picture_alloc(pic_in,X264_CSP_I420,width,height);



}


void VideoChannel::encodeData(int8_t *data) {
    //    容器   y的数据
    memcpy(pic_in->img.plane[0], data, ySize);
    // y的数据 和 Camera2 差不多，也是三个通道
    // Y 取值 就是 height * width,上面都是Y，U和V 交叉存储，V在前面 U 在后面，交叉存储
    // U
    // V
    for (int i = 0; i < uvSize; ++i) {
        //v数据
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);
        //间隔1个字节取一个数据
        //u数据
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2+1 );
    }
    //编码出了几个 nal （暂时理解为帧）  1   pi_nal  1  永远是1
    int pi_nal;
    //  编码出了几帧
    //编码出的数据 H264
    x264_nal_t *pp_nals;
    //编码输出的参数
    x264_picture_t pic_out;
    // 可以 解码 好几帧， yuv 转化成 h264数据
    x264_encoder_encode(videoCodec, &pp_nals, &pi_nal, pic_in, &pic_out);
    LOGE("videoCodec value  %d",videoCodec);
    //sps数据  最多就 30 个字节
    uint8_t sps[100];
    uint8_t pps[100];


    int sps_len, pps_len;
    LOGE("编码出的帧数  %d",pi_nal);
    if (pi_nal > 0) {
        for (int i = 0; i < pi_nal; ++i) {
            LOGE("输出索引:  %d  输出长度 %d",i,pi_nal);
            // 回调给java 写数据，方便观察
            // javaCallHelper->postH264(reinterpret_cast<char *>(pp_nals[i].p_payload), pp_nals[i].i_payload);

            // 使用 MediaCodec sps pps 是在一块的，一帧中
            // x264 是 分开的每一帧
            // 要一起发送，所以需要找到这两帧，合并起来再发送
            if (pp_nals[i].i_type == NAL_SPS) {
                // 缓存 sps
                sps_len = pp_nals[i].i_payload - 4;
                memcpy(sps, pp_nals[i].p_payload + 4, sps_len);
            }  else if (pp_nals[i].i_type == NAL_PPS) {
                //  pps  这里 拼接 sps ，直接发送
                pps_len = pp_nals[i].i_payload - 4;
                memcpy(pps, pp_nals[i].p_payload + 4, pps_len);
                // 发送出去
                sendSpsPps(sps, pps, sps_len, pps_len);
            } else{
                // 因为 x264 已经在每个I帧之前加了sps pps 了，所以这里是所有的I，P，B(直播基本没有B帧)
                sendFrame(pp_nals[i].i_type,pp_nals[i].i_payload,pp_nals[i].p_payload);
            }
        }
    }
    LOGE("pi_nal  %d",pi_nal);

}

void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {

    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 13 + sps_len + 3 + pps_len;
    RTMPPacket_Alloc(packet, bodysize);
    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    //composition time 0x000000
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //版本
    packet->m_body[i++] = 0x01;
    //编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;

    //整个sps
    packet->m_body[i++] = 0xE1;
    //sps长度
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = sps_len & 0xff;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(&packet->m_body[i], pps, pps_len);


    //视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    //随意分配一个管道（尽量避开rtmp.c中使用的）
    packet->m_nChannel = 10;
    //sps pps没有时间戳
    packet->m_nTimeStamp = 0;
    //不使用绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    if (this->callback) {
        this->callback(packet);
    }
}


void VideoChannel::setCallback(VideoChannel::VideoCallback callback) {
 this->callback = callback;
}

void VideoChannel::sendFrame(int type, int payload, uint8_t *p_payload) {
    //去掉 00 00 00 01 / 00 00 01
    if (p_payload[2] == 0x00){
        payload -= 4;
        p_payload += 4;
    } else if(p_payload[2] == 0x01){
        payload -= 3;
        p_payload += 3;
    }
    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 9 + payload;
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);
//    int type = payload[0] & 0x1f;
    packet->m_body[0] = 0x27;
    //关键帧
    if (type == NAL_SLICE_IDR) {
        LOGE("关键帧");
        packet->m_body[0] = 0x17;
    }
    //类型
    packet->m_body[1] = 0x01;
    //时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //数据长度 int 4个字节 相当于把int转成4个字节的byte数组
    packet->m_body[5] = (payload >> 24) & 0xff;
    packet->m_body[6] = (payload >> 16) & 0xff;
    packet->m_body[7] = (payload >> 8) & 0xff;
    packet->m_body[8] = (payload) & 0xff;

    //图片数据
    memcpy(&packet->m_body[9],p_payload,  payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodysize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callback(packet);
}



