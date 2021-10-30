package com.nzy.mediacodecpush;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

public class ScreenLive extends Thread {
    private static final String TAG = "ScreenLive";

    static {
        System.loadLibrary("native-lib");
    }
    private boolean isLiving;
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();
    private String url;
    private MediaProjection mediaProjection;
    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        LiveTaskManager.getInstance().execute(this);
    }
    public void addPackage(RTMPPackage rtmpPackage) {
        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }

    @Override
    public void run() {
        //1推送到
        if (!connect(url)) {
            Log.i("david", "run: ----------->推送失败");
            return;
        }

        VideoCodec videoCodec = new VideoCodec(this);
        videoCodec.startLive(mediaProjection);
        AudioCodec audioCodec = new AudioCodec(this);
        audioCodec.startLive();
        isLiving = true;
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "取出数据" );
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i(TAG, "run: ----------->推送 "+ rtmpPackage.getBuffer().length);
                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer()
                        .length, rtmpPackage.getTms(), rtmpPackage.getType());
            }
        }
    }
    private native boolean sendData(byte[] data, int len, long tms, int type);

    private native boolean connect(String url);
}
