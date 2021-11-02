package com.nzy.opengldemo.CameraMp4;

import android.content.Context;
import android.opengl.GLES20;

import com.nzy.opengldemo.R;

/**
 * 这个类就是拿到摄像头的数据而创建 已经渲染到 FBO 里面了
 *
 * @author niezhiyang
 * since 11/1/21
 */
public class CameraFilter extends AbstracFboFilter {
    private float[] mtx;
    private int vMatrix;

    /**
     * @param context
     * @param fragmentShaderId 片源着色程序
     */
    public CameraFilter(Context context, int fragmentShaderId) {
        // 传入顶点坐标
        super(context,  R.raw.camera_vert, fragmentShaderId);
        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
    }


    @Override
    public void beforeDraw() {
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);
    }

    public void setTransformMatrix(float[] mtx) {
        this.mtx = mtx;
    }

}
