package com.nzy.opencv;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 如果直接用jar ，手机必须有opencv的软件
 * 所以采用c
 * 下载地址 https://opencv.org/releases/
 * 1. 准备训练集
 * 2. 训练处模型 存储的权重
 * 3. 继承opencv环境
 * 4. 加载模型
 * 5. 识别
 * <p>
 * google 也为咱们提供了 FaceDetector 来在java层实现的 人脸识别
 * 检测率低，只有50%。
 * <p>
 * <p>
 * <p>
 * <p>
 * 拷入so 在 OpenCv-android-sdk-sdk-native-lib 文件夹放到cpp中的lib中
 * 头文件 OpenCv-android-sdk-sdk-jni-include 放到 cpp的include中
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    static {
        System.loadLibrary("native-lib");
    }

    private CameraHelper cameraHelper;
    int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        checkPermission();
        surfaceView.getHolder().addCallback(this);
        cameraHelper = new CameraHelper(cameraId);
        cameraHelper.setPreviewCallback(this);
        // 只能检测人脸lbpcascade_frontalface

        Utils.copyAssets(this, "lbpcascade_frontalface.xml");

    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);

        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        File file = new File(getFilesDir(), "lbpcascade_frontalface.xml");
        init(file.getAbsolutePath());
        cameraHelper.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraHelper.stopPreview();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // 传给底层
        setSurface(holder.getSurface());
    }



    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        postData(data, CameraHelper.WIDTH, CameraHelper.HEIGHT, cameraId, getFilesDir().getAbsolutePath());

    }

    // 最后一个主要是用来写出转成RGBA的文件
    private native void postData(byte[] data, int width, int height, int cameraId, String path);

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            cameraHelper.switchCamera();
            cameraId = cameraHelper.getCameraId();
        }
        return super.onTouchEvent(event);
    }

    private native void init(String mode);
    private native void setSurface(Surface surface);
}