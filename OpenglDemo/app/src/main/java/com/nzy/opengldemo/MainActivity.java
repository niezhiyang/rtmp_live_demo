package com.nzy.opengldemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import com.nzy.opengldemo.CameraMp4.camerafilter.CameraPicMp4Activity;
import com.nzy.opengldemo.camerafilter.FilterActivity;
import com.nzy.opengldemo.simple.SimpleDemoActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int checkWriteStoragePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        if(checkWriteStoragePermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},  100);
        }
    }


    public void startRecode(View view) {
        Intent intent = new Intent(this, CameraPicMp4Activity.class);
        startActivity(intent);
    }

    public void startFilter(View view) {
        Intent intent = new Intent(this, FilterActivity.class);
        startActivity(intent);
    }

    public void startSimple(View view) {
        Intent intent = new Intent(this,  SimpleDemoActivity.class);
        startActivity(intent);
    }
}