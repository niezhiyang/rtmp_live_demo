package com.nzy.mediacodec;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author niezhiyang
 * since 10/30/21
 */
public class LivePush extends Thread {
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();
    /**
     * 直播的地址
     */
    private String url;

    static {
        System.loadLibrary("native-lib");
    }

    private boolean isLiving;

    /**
     * 在队列里面添加 rtmpPackage
     *
     * @param rtmpPackage
     */
    public void addPackage(RTMPPackage rtmpPackage) {
        queue.add(rtmpPackage);
    }

    /**
     * 开始直播
     * @param url
     */
    public void startLive(String url) {
        this.url = url;
        start();
    }

    /**
     * 关闭直播
     */
    public void stopLive(){
        isLiving = false;
    }

    @Override
    public void run() {
        super.run();
        if (!connect(url)) {
            Log.i(MainActivity.TAG, "run: ----------->推送失败");
            return;
        }
        isLiving = true;

        while (isLiving){
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(MainActivity.TAG, "取出数据" );
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i(MainActivity.TAG, "run: ----------->推送 "+ rtmpPackage.getBuffer().length);
                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer()
                        .length, rtmpPackage.getTms(), rtmpPackage.getType());
            }
        }
    }

    /**
     * 链接服务器
     * @param url
     * @return
     */
    private native boolean connect(String url);

    /**
     * 发送数据
     * @param data
     * @param len
     * @param tms
     * @param type
     * @return
     */
    private native boolean sendData(byte[] data, int len, long tms, int type);
}
