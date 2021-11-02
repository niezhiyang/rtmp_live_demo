package com.nzy.opengldemo.CameraMp4.camerafilter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.nzy.opengldemo.CameraMp4.RecordButton;
import com.nzy.opengldemo.R;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Camera 滤镜 demo
 * 以前用的是 MediaCodec，会有兼容问题，MediaCodec会有 数据上限的。Dsp芯片的问题
 * 可以直接把摄像头的数据直接 给 gpu ，而不用，让 MediaCodec 经过Cpu给gpu了，opengl 是没有上限的
 * <p>
 * 这个主要是抽取了的东西
 */
public class CameraPicMp4Activity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    private CameraView2 cameraView;
    private RecordButton mBtn_record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fbo);
        cameraView = findViewById(R.id.cameraView);
        mBtn_record = findViewById(R.id.btn_record);
        //速度
        RadioGroup rgSpeed = findViewById(R.id.rg_speed);
        rgSpeed.setOnCheckedChangeListener(this);
        checkPermission();
        initListener();


    }

    private void initListener() {
        mBtn_record.setTouchDelay(300);
        //设置最大录制时间，单位为毫秒
        mBtn_record.setRecordTime(5000);
        //设置最小录制时间，单位为毫秒
        mBtn_record.setMinRecordTime(1000);
        mBtn_record.setRecordButtonListener(new RecordButton.RecordButtonListener() {
            @Override
            public void onClick() {

            }

            @Override
            public void onLongClick() {
                cameraView.startRecord();
            }

            @Override
            public void onLongClickFinish(int result) {
                switch (result) {
                    case RecordButton.NORMAL:
                        cameraView.stopRecord();
                        String path = new File(CameraPicMp4Activity.this.getFilesDir(), "demo.mp4").getAbsolutePath();
                        Toast.makeText(CameraPicMp4Activity.this, "已录制好在：" + path, Toast.LENGTH_SHORT).show();
                        break;
                    case RecordButton.RECORD_SHORT:
                        break;
                    default:
                }
            }
        });
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
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.btn_extra_slow:
                cameraView.setSpeed(CameraView2.Speed.MODE_EXTRA_SLOW);
                break;
            case R.id.btn_slow:
                cameraView.setSpeed(CameraView2.Speed.MODE_SLOW);
                break;
            case R.id.btn_normal:
                cameraView.setSpeed(CameraView2.Speed.MODE_NORMAL);
                break;
            case R.id.btn_fast:
                cameraView.setSpeed(CameraView2.Speed.MODE_FAST);
                break;
            case R.id.btn_extra_fast:
                cameraView.setSpeed(CameraView2.Speed.MODE_EXTRA_FAST);
                break;
        }
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
