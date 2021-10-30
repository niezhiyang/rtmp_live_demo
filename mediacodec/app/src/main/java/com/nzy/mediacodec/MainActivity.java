package com.nzy.mediacodec;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements Camera2Helper.Camera2Listener {
    public static final String TAG = "zhiyang";

    private String url = "rtmp://tx.direct.huya.com/huyalive/1199591667580-1199591667580-0-2399183458616-10057-A-1635401218-1?seq=1635580218767&type=simple";
    private LivePush mLivePush;
    private Camera2Helper mCamera2Helper;
    private TextureView textureView;
    private MediaCodec mediaCodec;
    private long startTime;
    private long timeStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureView);
        checkPermission();
        mCamera2Helper = new Camera2Helper(this,this);
        mLivePush = new LivePush();
    }

    private void initCamera() {

    }


    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] ==PackageManager.PERMISSION_GRANTED){
            Log.i(TAG,"权限有了");
            initCamera();
        }
    }

    public native String stringFromJNI();

    public void startLive(View view) {
        try {
            mCamera2Helper.start(textureView);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mLivePush.startLive(url);
    }

    private byte[] nv21;
    byte[] nv21_rotated;
    byte[] nv12;
    /**
     * 输出的 YUV数据
     * @param y 预览数据，Y分量
     * @param u 预览数据，U分量
     * @param v 预览数据，V分量
     * @param previewSize  预览尺寸
     */
    @Override
    public void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize) {

        // 无论用 Camrae 1 还是 2 都是横着的，所以要旋转

        if (nv21 == null) {
            // 每一行的数据*高度 总的像素值，yuv420 一个像素占用 3/2 个字节
            nv21 = new byte[previewSize.getWidth() * previewSize.getHeight() * 3 / 2];

            nv21_rotated = new byte[previewSize.getWidth() * previewSize.getHeight() * 3 / 2];
        }
        if(mediaCodec ==null){
            initCodec(previewSize);
        }
        Log.e(TAG,"getWidth() : "+previewSize.getWidth() +"-- previewSize.getHeight: "+previewSize.getHeight());
        // 转化成相机原始数据
        ImageUtil.yuvToNv21(y,u,v,nv21,previewSize.getWidth(),previewSize.getHeight());
        // 旋转，YUV420 ，就是 nv21
        ImageUtil.nv21_rotate_to_90(nv21,nv21_rotated,previewSize.getWidth(),previewSize.getHeight());

        // nv12 就是 YUV420
        byte[] temp = ImageUtil.nv21toNV12(nv21_rotated, nv12);
        //输出成H264的码流
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inIndex >= 0) {
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
            byteBuffer.clear();
            byteBuffer.put(temp, 0, temp.length);
            mediaCodec.queueInputBuffer(inIndex, 0, temp.length,
                    0, 0);
        }
        int outIndex = mediaCodec.dequeueOutputBuffer(info, 100000);
        if (outIndex >= 0) {
            if (System.currentTimeMillis() - timeStamp >= 2000) {
                Bundle params = new Bundle();
                //立即刷新 让下一帧是关键帧
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mediaCodec.setParameters(params);
                timeStamp = System.currentTimeMillis();
            }

            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
            byte[] outData = new byte[byteBuffer.remaining()];
            byteBuffer.get(outData);
            // 写在文件中 方便查看
//            FileUtils.writeContent(outData);
//            FileUtils. writeBytes(outData);
            if (startTime == 0) {
                // 微妙转为毫秒
                startTime = info.presentationTimeUs / 1000;
            }
            RTMPPackage rtmpPackage = new RTMPPackage(outData, (info.presentationTimeUs / 1000) - startTime);
            rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_VIDEO);
            mLivePush.addPackage(rtmpPackage);
            mediaCodec.releaseOutputBuffer(outIndex, false);
        }
    }
    private void initCodec(Size size) {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

            final MediaFormat format = MediaFormat.createVideoFormat("video/avc",
                    size.getHeight(), size.getWidth());
            //设置帧率  手动触发一个I帧
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            // 一秒 15帧
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 4000_000);
            //2s一个I帧
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}