package com.nzy.opengldemo.CameraMp4.camerafilter;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * GLES20 所有的代码都写在子线程 ，GLSurfaceView 也是继承  SurfaceView。里面有个Thread
 * @author niezhiyang
 * since 11/1/21
 */
public class CameraView2 extends GLSurfaceView {
    private CameraRender2 renderer;
    public CameraView2(Context context) {
        super(context);
    }

    public CameraView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 设置版本,这里配置了，清单文件就不用配置了
        setEGLContextClientVersion(2);
        renderer = new CameraRender2(this);
//        opengl  有讲究
        setRenderer(renderer);
        // 刷新方式： RENDERMODE_CONTINUOUSLY是自动，随着系统刷新而刷新
        //           RENDERMODE_WHEN_DIRTY 是手动刷新,mGlSurfaceView.requestRender()才去刷新
        // 必须写在 setRenderer 之后
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


    }
    private Speed mSpeed = Speed.MODE_NORMAL;

    public enum Speed {
        MODE_EXTRA_SLOW, MODE_SLOW, MODE_NORMAL, MODE_FAST, MODE_EXTRA_FAST
    }
    public void setSpeed(Speed speed) {
        this.mSpeed = speed;
    }

    public void startRecord(){
        //速度  时间/速度 speed小于就是放慢 大于1就是加快
        float speed = 1.f;
        switch (mSpeed) {
            case MODE_EXTRA_SLOW:
                speed = 0.3f;
                break;
            case MODE_SLOW:
                speed = 0.5f;
                break;
            case MODE_NORMAL:
                speed = 1.f;
                break;
            case MODE_FAST:
                speed = 2.f;
                break;
            case MODE_EXTRA_FAST:
                speed = 3.f;
                break;
        }
        renderer.startRecord(speed);
    }
    public void stopRecord(){
        renderer.stopRecord();
    }
}
