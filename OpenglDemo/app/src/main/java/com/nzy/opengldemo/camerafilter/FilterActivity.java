package com.nzy.opengldemo.camerafilter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import com.nzy.opengldemo.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Camera 滤镜 demo
 * 以前用的是 MediaCodec，会有兼容问题，MediaCodec会有 数据上限的。Dsp芯片的问题
 * 可以直接把摄像头的数据直接 给 gpu ，而不用，让 MediaCodec 经过Cpu给gpu了，opengl 是没有上限的
 */
public class FilterActivity extends AppCompatActivity {
    CameraView gLSurfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        check();
        gLSurfaceView = findViewById(R.id.gLSurfaceView);
    }

    private void check() {
        int checkWriteStoragePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        if(checkWriteStoragePermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},  100);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        gLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gLSurfaceView.onPause();
    }

    public void grey(View view) {
    }
}