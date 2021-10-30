package com.nzy.x264.cameraxpush;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.nzy.x264.LivePusher;

/**
 * // 使用 faac 软解 音频数据
 *
 * @author niezhiyang
 * since 10/28/21
 */
public class AudioChannel {
    /**
     * 麦克风 支持的 最小的缓存数据
     */
    private LivePusher livePusher;
    private int sampleRate;
    private int channelConfig;
    private int minBufferSize;
    private byte[] buffer;
    private Handler handler;
    private HandlerThread handlerThread;
    private AudioRecord audioRecord;

    public AudioChannel(int sampleRate, int channels, LivePusher livePusher) {
        this.livePusher = livePusher;
        this.sampleRate = sampleRate;
        //     channels 通道数
        // 如果是双通道
        channelConfig = channels == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        // 数据大小 底层是根据 MediaCodec
        // 如果使用 MediaCodec 编码这个aac ，可以用，软编 faac 作为参考 ，如果硬编不支持则会返回 -1;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channels, channelConfig);
        // 处理软便faac 返回的最小数据
        int inputByteNum = livePusher.initAudioEnc(sampleRate, channels);
        Log.e(CameraXActivity.TAG,"input"+inputByteNum);
        // 输入容器 是 通过 麦克风 的数据
        buffer = new byte[inputByteNum];
        // 选择大的那个
        minBufferSize = Math.max(inputByteNum, minBufferSize);

        HandlerThread handlerThread = new HandlerThread("audio");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());


    }


    public void start() {

        handler.post(new Runnable() {
            @Override
            public void run() {
                // 读取麦克风数据
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
                audioRecord.startRecording();
                while (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                   //   len实际长度len 打印下这个值  0 是 录音不成功
                    int len = audioRecord.read(buffer, 0, buffer.length);
                    Log.i(CameraXActivity.TAG, "录音len: "+len);
                    if (len > 0) {
                        // 推送音频
                        livePusher.sendAudio(buffer, len);
                    }else{
                        Log.e(CameraXActivity.TAG, "录音错误五len: "+len);
                    }
                }
            }
        });
    }
}
