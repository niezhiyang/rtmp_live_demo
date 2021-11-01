package com.nzy.opengldemo.simple;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author niezhiyang
 * since 10/30/21
 */
public class BgRender implements GLSurfaceView.Renderer {
    // cpu 调用 opengl 调用 gpu ，绘制
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // gl 初始化
        // 清除之前的数据，类似于 canvas.restore()
        // 都是静态的API
        GLES20.glClearColor(0,0,0,1);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // 横竖屏，画中画 改变的时候,通知改变
        GLES20.glViewport(0,0,width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
      // 渲染 1. 手动刷新，2 随着系统 的刷新而刷新 比如 60Hz
        GLES20.glClearColor(0.12f,0.3f,0,1);
    }
}
