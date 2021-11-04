package com.nzy.ffmpegplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/**
 * @author niezhiyang
 * since 11/4/21
 * // 音视频同步，一般按照音频为准，因为音频不容易操作
 */
public class MainActivity extends AppCompatActivity {
    private Surface surface;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface);
        copyMp4();
        final SurfaceHolder surfaceViewHolder = surfaceView.getHolder();

        surfaceViewHolder.addCallback(new SurfaceHolder.Callback() {


            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //获取文件路径，这里将文件放置在手机根目录下
                surface = surfaceViewHolder.getSurface();

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
    }

    private void copyMp4() {
        Utils.copyAssets(this, "hashiqi.mp4");
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

    public void play(View view) {
        String path = new File(getFilesDir(), "hashiqi.mp4").getAbsolutePath();
        play(path, surface);
    }

    private native int play(String path, Surface surface);


}
