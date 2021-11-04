package com.nzy.ffmpegplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/**
 * @author niezhiyang
 * since 11/4/21
 * // 音视频同步，一般按照音频为准，因为音频不容易操作
 */
public class MainActivity extends AppCompatActivity {
    private AudioTrack audioTrack;
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
        Utils.copyAssets(this, "Dance.mp3");
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

    public void playVideo(View view) {
        String path = new File(getFilesDir(), "hashiqi.mp4").getAbsolutePath();
        play(path, surface);

    }

    public void playMp3(View view) {
//        String path = new File(getFilesDir(), "Dance.mp3").getAbsolutePath();
        // 可以解码所有的，无论音视频
//        String path = new File(getFilesDir(), "hashiqi.mp4").getAbsolutePath();
        String path = "http://mpge.5nd.com/2015/2015-11-26/69708/1.mp3";
        playSound(path);
    }

    /**
     * 由 Native 来调用的，来初始化音频 AudioTrack
     */

    public void createTrack(int sampleRateInHz, int channals) {
        Toast.makeText(this, "初始化播放器", Toast.LENGTH_SHORT).show();
        int channaleConfig;//通道数

        if (channals == 1) {
            channaleConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (channals == 2) {
            channaleConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            channaleConfig = AudioFormat.CHANNEL_OUT_MONO;
        }
        int buffersize = AudioTrack.getMinBufferSize(sampleRateInHz,
                channaleConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channaleConfig, AudioFormat.ENCODING_PCM_16BIT
                , buffersize, AudioTrack.MODE_STREAM);
        audioTrack.play();

    }

    /**
     * 由 Native 来调用的，播放pcm数据
     */
    public void playTrack(byte[] buffer,int lenth){
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.write(buffer, 0, lenth);
        }
    }

    private native int play(String path, Surface surface);


    private native int playSound(String path);
}
