package com.nzy.x264;

import android.util.Log;
import android.view.SurfaceHolder;


public class LivePusher {


    static {
        System.loadLibrary("native-lib");
    }


    public LivePusher() {
        native_init();
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
    }

    public void switchCamera() {
    }
    private void onPrepare(boolean isConnect) {
        //通知UI
    }
    public void startLive(String path) {
        native_start(path);
    }

    public void stopLive(){
        native_stop();


    }

//    jni回调java层的方法  byte[] data    char *data
    private void postData(byte[] data) {
        Log.i("rtmp", "postData: "+data.length);
        FileUtils.writeBytes(data);
        FileUtils.writeContent(data);
    }

    public native void native_init();

    public native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);

    public native void native_start(String path);

    public native int initAudioEnc(int sampleRate, int channels);

    public native void native_pushVideo(byte[] data);

    public native void native_stop();

    public native void native_release();

    public native void sendAudio(byte[] buffer, int len);



}
