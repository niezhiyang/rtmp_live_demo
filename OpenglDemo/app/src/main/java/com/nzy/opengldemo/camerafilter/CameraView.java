package com.nzy.opengldemo.camerafilter;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * GLES20 所有的代码都写在子线程 ，GLSurfaceView 也是继承  SurfaceView。里面有个Thread
 * @author niezhiyang
 * since 11/1/21
 */
public class CameraView extends GLSurfaceView {
    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 设置版本,这里配置了，清单文件就不用配置了
        setEGLContextClientVersion(2);
        setRenderer(new CameraRender(this));
        // 刷新方式： RENDERMODE_CONTINUOUSLY是自动，随着系统刷新而刷新
        //           RENDERMODE_WHEN_DIRTY 是手动刷新,mGlSurfaceView.requestRender()才去刷新
        // 必须写在 setRenderer 之后
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


    }
}
