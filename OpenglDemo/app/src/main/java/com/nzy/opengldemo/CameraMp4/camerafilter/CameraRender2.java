package com.nzy.opengldemo.CameraMp4.camerafilter;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.nzy.opengldemo.CameraMp4.MediaRecorder;
import com.nzy.opengldemo.CameraMp4.RecordFilter;
import com.nzy.opengldemo.CameraMp4.filter.BeautyFilter;
import com.nzy.opengldemo.CameraMp4.filter.CameraFilter;
import com.nzy.opengldemo.CameraMp4.filter.SoulFilter;
import com.nzy.opengldemo.R;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

/**
 * 下面的三个方法都是 Gl线程
 *
 * @author niezhiyang
 * since 11/1/21
 */
class CameraRender2 implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "CameraRender";
    private CameraHelper2 mCameraHelper;
    private CameraView2 mCameraView;
    private SurfaceTexture mCameraTexture;
    // 可以控制N个图层
    private int[] textures;
    float[] mtx = new float[16];
    private CameraFilter mCarmeraFilter;
    private RecordFilter mRecordFilter;
    private SoulFilter mSoulFilter;
    private BeautyFilter mBeautyFilter;
    private MediaRecorder mRecorder;


    public CameraRender2(CameraView2 cameraView) {
        mCameraView = cameraView;

        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
        mCameraHelper = new CameraHelper2(lifecycleOwner, this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e(TAG, "onSurfaceCreated");
        // 提供一个数组的，在 GPU 创建一个对象
        textures = new int[1];
        // 这里的意思就是当mSurfaceTexture 有数据了，就给到GPU的一个缓存里面
        // 让 SurfaceTexture 与 opengl（GPU）公用一个数据源
        mCameraTexture.attachToGLContext(textures[0]);

        // 滤镜路径id
        mCarmeraFilter = new CameraFilter(mCameraView.getContext(),R.raw.camera_frag_cool);

        mCameraTexture.setOnFrameAvailableListener(this);
        mRecordFilter = new RecordFilter(mCameraView.getContext());
        String path = new File(mCameraView.getContext().getFilesDir(),"demo.mp4").getAbsolutePath();
        mRecorder = new MediaRecorder(mCameraView.getContext(), path,
                EGL14.eglGetCurrentContext(),
                480, 640);

        mSoulFilter = new SoulFilter(mCameraView.getContext());
        mBeautyFilter = new BeautyFilter(mCameraView.getContext());

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mRecordFilter.setSize(width, height);
        mCarmeraFilter.setSize(width, height);
        mSoulFilter.setSize(width, height);
        if(mBeautyFilter!=null){
            mBeautyFilter.setSize(width, height);
        }

    }

    @Override
    public void onDrawFrame(GL10 gl) {

        /**
         * Update the texture image to the most recent frame from the image stream.  This may only be
         * called while the OpenGL ES context that owns the texture is current on the calling thread.
         * It will implicitly bind its texture to the {@code GL_TEXTURE_EXTERNAL_OES} texture target.
         */
        // 更新摄像头的数据 相当于已经给GPU了,看里面注释，不需要cpu
        mCameraTexture.updateTexImage();
        // 这里不是数据
        mCameraTexture.getTransformMatrix(mtx);
        mCarmeraFilter.setTransformMatrix(mtx);
        // textures[0] 才是数据
        // 这个id 就是 过滤后的数据
        int id = mCarmeraFilter.onDraw(textures[0]);


        // 此时 mCameraTexture SurfaceView是没有数据的，摄像头给了Fbo
        // 渲染到 再下一个图层
        id = mSoulFilter.onDraw(id);
        id = mRecordFilter.onDraw(id);
        if (mBeautyFilter != null) {
//            行     打开 还是不打开 美颜滤镜    资源泄露
            id = mBeautyFilter.onDraw(id);
        }
        // 拿到了fbo的引用   可以  编码视频   输出  直播推理
        mRecorder.fireFrame(id,mCameraTexture.getTimestamp());




    }

    /**
     * Camera 所有的数据 都在 SurfaceTexture 中，把这里的数据都给 opengl
     *
     * @param output
     */
    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        //mSurfaceTexture.attachToGLContext GPU 和 mSurfaceTexture 数据帮顶在一起
        mCameraTexture = output.getSurfaceTexture();


    }

    /**
     * {@link CameraRender2#onDrawFrame(GL10)}
     * 当摄像头有数据的时候
     *
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //触发 CameraView 去触发 渲染
        // 当有一帧一帧回调时 , CameraRender 会 onDrawFrame 重新刷新 走
        mCameraView.requestRender();

    }
    public void startRecord(float speed) {
        try {
            mRecorder.start(speed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stopRecord() {
        mRecorder.stop();
    }

    public void enableBeauty(final boolean isChecked) {

        mCameraView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (isChecked) {
                    mBeautyFilter = new BeautyFilter(mCameraView.getContext());
                    mBeautyFilter.setSize(mCameraView.getWidth(), mCameraView.getHeight());
                }else {
                    mBeautyFilter.release();
                    mBeautyFilter = null;
                }
            }
        });
//        Opengl 线程  来做   fbo
    }
}
