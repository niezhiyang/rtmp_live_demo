package com.nzy.opengldemo.CameraMp4;

import android.content.Context;
import android.opengl.GLES20;

/**
 * @author niezhiyang
 * since 11/1/21
 */
public class AbstracFboFilter extends AbstractFilter {
    public int[] frameBuffer;
    public int[] frameTextures;

    /**
     * @param context
     * @param vertexShaderId   顶点坐标程序
     * @param fragmentShaderId 片源着色程序
     */
    public AbstracFboFilter(Context context, int vertexShaderId, int fragmentShaderId) {
        super(context, vertexShaderId, fragmentShaderId);
    }


    @Override
    public void setSize(int width, int height) {
        super.setSize(width,height);
        releaseFrame();
        // 实例化 FBO ，让摄像头的数据 先缓存到 FBO，
        // 1. 创建 FBO
        frameBuffer = new int[1];
        GLES20.glGenBuffers(1, frameBuffer, 0);

        // 2. 生成一个纹理
        frameTextures = new int[1];

        // 生成纹理的个数
        GLES20.glGenTextures(frameTextures.length, frameTextures, 0);

        // 3. 配置纹理
        for (int i = 0; i < frameTextures.length; i++) {
            // 绑定纹理，开始操作纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTextures[i]);

            // 具体看本目录下的 锯齿.jpg
            //左面的图为 GL_LINEAR 的效果，是放大模糊的效果。
            //右面的图是 GL_NEAREST 的效果，锯齿比较明显， 但没有模糊。

            //放大过滤,
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_NEAREST);
            //缩小过滤
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);


            // 告诉GPU 操作纹理完事了
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        ///////////////////////////////////////////////
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameTextures[0]);
        /**
         * 指定一个二维的纹理图片
         * level
         *     指定细节级别，0级表示基本图像，n级则表示Mipmap缩小n级之后的图像（缩小2^n）
         * internalformat
         *     指定纹理内部格式，必须是下列符号常量之一：GL_ALPHA，GL_LUMINANCE，GL_LUMINANCE_ALPHA，GL_RGB，GL_RGBA。
         * width height
         *     指定纹理图像的宽高，所有实现都支持宽高至少为64 纹素的2D纹理图像和宽高至少为16 纹素的立方体贴图纹理图像 。
         * border
         *     指定边框的宽度。必须为0。
         * format
         *     指定纹理数据的格式。必须匹配internalformat。下面的符号值被接受：GL_ALPHA，GL_RGB，GL_RGBA，GL_LUMINANCE，和GL_LUMINANCE_ALPHA。
         * type
         *     指定纹理数据的数据类型。下面的符号值被接受：GL_UNSIGNED_BYTE，GL_UNSIGNED_SHORT_5_6_5，GL_UNSIGNED_SHORT_4_4_4_4，和GL_UNSIGNED_SHORT_5_5_5_1。
         * data
         *     指定一个指向内存中图像数据的指针。
         */

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                null);
        // 要开始GPU的 FBO数据
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        //绑定 真正发生绑定   fbo  和 纹理  (图层)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
                frameTextures[0],
                0);


        // 释放 52行
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        // 释放 75行
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        // 生成一个图层

    }
    @Override
    public int onDraw(int texture) {
        // 此时已经绑定了 ,把生成的 图层返回回去
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,frameBuffer[0]);
        super.onDraw(texture);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);

        return frameTextures[0];

    }
    private void releaseFrame() {
        if (frameTextures != null) {
            GLES20.glDeleteTextures(1, frameTextures, 0);
            frameTextures = null;
        }

        if (frameBuffer != null) {
            GLES20.glDeleteFramebuffers(1, frameBuffer, 0);
        }
    }
}
