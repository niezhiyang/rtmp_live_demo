package com.nzy.opengldemo.CameraMp4.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.nzy.opengldemo.CameraMp4.AbstracFboFilter;
import com.nzy.opengldemo.R;

/**
 * @author niezhiyang
 * since 11/2/21
 */
public class BeautyFilter extends AbstracFboFilter {

    //beauty_frag GPU中的变量地址
    private int width;
    private int height   ;
    /**
     * @param context
     */
    public BeautyFilter(Context context) {
        super(context, R.raw.beauty_frag,R.raw.beauty_fragment2);
        // 拿到片源程序中拿到变量引用
        width = GLES20.glGetUniformLocation(mProgram, "width");
        height = GLES20.glGetUniformLocation(mProgram, "height");
    }

    @Override
    public void beforeDraw() {
        super.beforeDraw();
        // 给GPU赋值 片源程序中的值
        GLES20.glUniform1i(width, mWidth);
        GLES20.glUniform1i(height,mHeight);
    }
}
