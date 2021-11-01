package com.nzy.opengldemo.camerafilter;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

/**
 * 下面的三个方法都是 Gl线程
 * @author niezhiyang
 * since 11/1/21
 */
class CameraRender implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraRender";
    private CameraHelper mCameraHelper;
    private CameraView mCameraView;
    private SurfaceTexture mSurfaceTexture;
    // 可以控制N个图层
    private int[] textures;
    float[] mtx = new float[16];
    private ScreenFilter mScreenFilter;


    public CameraRender(CameraView cameraView) {
        mCameraView= cameraView;

        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
        mCameraHelper = new CameraHelper(lifecycleOwner,this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e(TAG,"onSurfaceCreated");
        // 提供一个数组的，在 GPU 创建一个对象
        textures = new int[1];
        // 这里的意思就是当mSurfaceTexture 有数据了，就给到GPU的一个缓存里面
        // 让 SurfaceTexture 与 opengl（GPU）公用一个数据源
        mSurfaceTexture.attachToGLContext(textures[0]);

        mScreenFilter = new ScreenFilter(mCameraView.getContext());
        mSurfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mScreenFilter.setSize(width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 更新摄像头的数据 相当于已经给GPU了,看里面注释，不需要cpu
        /**
         * Update the texture image to the most recent frame from the image stream.  This may only be
         * called while the OpenGL ES context that owns the texture is current on the calling thread.
         * It will implicitly bind its texture to the {@code GL_TEXTURE_EXTERNAL_OES} texture target.
         */
      mSurfaceTexture.updateTexImage();
      // 这里不是数据
      mSurfaceTexture.getTransformMatrix(mtx);
      mScreenFilter.setTransformMatrix(mtx);
      // textures[0] 才是数据
      mScreenFilter.onDraw(textures[0]);
    }

    /**
     * Camera 所有的数据 都在 SurfaceTexture 中，把这里的数据都给 opengl
     * @param output
     */
    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        //mSurfaceTexture.attachToGLContext GPU 和 mSurfaceTexture 数据帮顶在一起
        mSurfaceTexture = output.getSurfaceTexture();



    }

    /**
     * {@link CameraRender#onDrawFrame(GL10)}
     * 当摄像头有数据的时候
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //触发 CameraView 去触发 渲染
        // 当有一帧一帧回调时 , CameraRender 会 onDrawFrame 重新刷新 走
        mCameraView.requestRender();

    }
}
