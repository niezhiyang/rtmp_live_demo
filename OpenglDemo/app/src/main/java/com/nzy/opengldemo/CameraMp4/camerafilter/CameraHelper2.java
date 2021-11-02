package com.nzy.opengldemo.CameraMp4.camerafilter;

import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

/**
 * @author niezhiyang
 * since 11/1/21
 */
public class CameraHelper2 {

    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.FRONT;
    private Preview.OnPreviewOutputUpdateListener listener;


    public CameraHelper2(LifecycleOwner lifecycleOwner, Preview.OnPreviewOutputUpdateListener listener) {
        this.listener = listener;
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        // 打开摄像头
        CameraX.bindToLifecycle(lifecycleOwner, getPreView());

//        直播camerax  打开的
    }
    private Preview getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        // 也是找到最合适的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                //前置或者后置摄像头
                .setLensFacing(currentFacing)
                .build();

        // 预览
        Preview preview = new Preview(previewConfig);
        // 预览 得到数据的监听，
        // PreviewOutput.getSurfaceTexture 所有的数据都在这个里面
        preview.setOnPreviewOutputUpdateListener(listener);

        return preview;
    }
}
