package com.nzy.x264.cameraxpush;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.nzy.x264.LivePusher;
import com.nzy.x264.R;

import androidx.appcompat.app.AppCompatActivity;


public class CameraXActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "CameraXActivity";
    //H264码流
    private LivePusher livePusher;
    private TextureView textureView;
    private String url = "rtmp://tx.direct.huya.com/huyalive/1199591667580-1199591667580-0-2399183458616-10057-A-1635401218-1?seq=1635575731795&type=simple";
    VideoChanel videoChanel;

    AudioChannel audioChannel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camerax);
        checkPermission();
        textureView = findViewById(R.id.textureView);
        checkPermission();
        livePusher = new LivePusher();

        livePusher.startLive(url);
        videoChanel = new VideoChanel(this, textureView, livePusher);
        audioChannel = new AudioChannel(44100,2,livePusher);

        findViewById(R.id.bt_start).setOnClickListener(this);
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED|| checkSelfPermission(
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            }, 1);

        }
        return false;
    }

    public void switchCamera(View view) {
        livePusher.switchCamera();
    }


    public void startLive() {
        audioChannel.start();
        videoChanel.startLive();

    }

    public void stopLive(View view) {
        livePusher.stopLive();
    }

    public void toggle(View view) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        livePusher.native_release();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_start:
                Toast.makeText(CameraXActivity.this,"开始推流了",Toast.LENGTH_SHORT).show();
                startLive();
                break;
            default:
        }
    }
}
