package com.nzy.opengldemo.simple;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.nzy.opengldemo.R;
import com.nzy.opengldemo.simple.TriangleRender;

import androidx.appcompat.app.AppCompatActivity;

public class SimpleDemoActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_opengl);
        glSurfaceView = findViewById(R.id.gLSurfaceView);
        // 使用 2 版本
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(new TriangleRender());
        //设置为手动刷新
        // 1. RENDERMODE_CONTINUOUSLY 随着系统而刷新
        // 2. RENDERMODE_WHEN_DIRTY 手动刷新,mGlSurfaceView.requestRender()才去刷新
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }
}