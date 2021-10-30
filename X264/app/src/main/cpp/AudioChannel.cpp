//
// Created by MOMO on 10/28/21.
//

#include <malloc.h>
#include <cstring>
#include "AudioChannel.h"
#include "Log.h"

AudioChannel::AudioChannel() {

}

AudioChannel::~AudioChannel() {

}

void AudioChannel::openCodec(int sampleRate, int channels) {

    //maxOutputBytes 编码之后输出最大数据
    // 输入样本的容器大小
    unsigned long inputSamples;
//    实例化faac编码器
    /**
     unsigned long   nSampleRate,        // 采样率，单位是bps
    unsigned long   nChannels,          // 声道，1为单声道，2为双声道
    unsigned long   &nInputSamples,     // 传引用，得到每次调用编码时所应接收的原始数据长度
    unsigned long   &nMaxOutputBytes    // 传引用，得到每次调用编码时生成的AAC数据的最大长度
     */
    // 打开faac 编码器   后面 两个 执行完 就有值了，输出的
    codec=  faacEncOpen(sampleRate, channels, &inputSamples, &maxOutputBytes);

    // 因为是双通道哦 输入容器的大小
    inputByteNum = inputSamples * 2;

    // 实例化输出的容器
    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));
    LOGE("初始化-----------》%d  inputByteNum %d  maxOutputBytes:%d ",codec,inputByteNum,maxOutputBytes);
    //参数
    faacEncConfigurationPtr configurationPtr = faacEncGetCurrentConfiguration(codec);
    /* Bitstream output format (0 = Raw; 1 = ADTS) */
    // 输出 aac 裸流数据
    configurationPtr->mpegVersion = MPEG4;
    //    编码等级
    configurationPtr->aacObjectType = LOW;
    //输出aac裸流数据
    configurationPtr->outputFormat = 0;
    //采样位数
    configurationPtr->inputFormat = FAAC_INPUT_16BIT;
    //    将我们的配置生效
    faacEncSetConfiguration(codec, configurationPtr);
}

void AudioChannel::encode(int32_t *data, int len) {
    // 编码，这个data 就是原始数据，麦克风数据

    // 编码完成，
    int bytelen=faacEncEncode(codec, data, len, outputBuffer, maxOutputBytes);
    // 此时 outputBuffer 就已经是压缩 编码的数据了
    if (bytelen > 0) {
        LOGE("发送音频数据 %d", outputBuffer);
        //  拼装packet  数据
        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet, bytelen + 2);
        packet->m_body[0] = 0xAF;//
        packet->m_body[1] = 0x01;
        memcpy(&packet->m_body[2], outputBuffer, bytelen);
        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = bytelen + 2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        callback(packet);
        LOGE("发送音频数据结束 %d", outputBuffer);
    }
}

RTMPPacket *AudioChannel::getAudioConfig() {
    // 发送头帧 类似于视频的 sps 和 pps , 这个头帧是在链接成功服务器之后立马发送额的
    u_char *buf;
    u_long len;
    //头帧的内容   {0x12 0x08}
    faacEncGetDecoderSpecificInfo(codec, &buf, &len);
    //头帧的  rtmpdump  实时录制  实时给时间戳
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, len + 2);

    packet->m_body[0] = 0xAF;
    packet->m_body[1] = 0x00;
    memcpy(&packet->m_body[2], buf, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = len + 2;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}

void AudioChannel::setCallback(Callback callback) {
    this->callback = callback;

}



