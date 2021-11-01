package com.nzy.opengldemo.CameraMp4;

import android.content.Context;
import android.opengl.GLES20;

/**
 * @author niezhiyang
 * since 11/1/21
 */
public class CameraFilter extends AbstractFilter{
    private float[] mtx;
    private int vMatrix;
    public CameraFilter(Context context, int fragRawId) {
        super(context, fragRawId);
        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");

    }

    @Override
    public void beforeDraw() {
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);
    }
}
