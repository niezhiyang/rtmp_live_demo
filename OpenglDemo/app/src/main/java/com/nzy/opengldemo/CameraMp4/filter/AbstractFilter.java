package com.nzy.opengldemo.CameraMp4.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.nzy.opengldemo.camerafilter.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author niezhiyang
 * since 11/1/21
 *
 * 顶点程序 确定形状
 * 片源程序 是着色的
 */
public  class AbstractFilter {
    private final int vCoord;
    private final int vTexture;
    public   int mProgram;
    private int vPosition;
    // 顶点着色器
    // 片源着色器
    // 整个屏幕的四个坐标，

    /**
     * OpenGl 坐标
     * 世界坐标系
     */
    float[] VERTEX = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };
    /**
     * 纹理坐标系
     * 左上是0.0
     * 向下是正
     */
    float[] TEXTURE = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };
    // 纹理坐标
    FloatBuffer textureBuffer;
    // 顶点坐标缓存区
    FloatBuffer vertexBuffer;
    protected int mWidth;
    protected int mHeight;
    private int mFfagRawId;
    private int vertexShaderId;

    /**
     *
     * @param context
     * @param vertexShaderId 顶点坐标程序
     * @param fragmentShaderId 片源着色程序
     */
   public AbstractFilter(Context context, int vertexShaderId, int fragmentShaderId){
       mFfagRawId =fragmentShaderId ;
       // 申请连续的 4个字节 4(个左边)*4(每个坐标是float，4个字节)*2(代表有x和y)
       ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 4 * 2);
       // 重新整理一下
       buffer.order(ByteOrder.nativeOrder());
       // 转成 floatbuffer
       vertexBuffer =  buffer.asFloatBuffer();
       vertexBuffer.clear();
       // 赋值坐标到里面
       vertexBuffer.put(VERTEX);

       textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
               .asFloatBuffer();
       textureBuffer.clear();
       textureBuffer.put(TEXTURE);

       // 顶点着色器程序
       String vertexShader = OpenGLUtils.readRawTextFile(context,  vertexShaderId );
       // 灰色，取平均 rgb 取平均）
//       String fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag_grey);
//       String fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag_warmth);
//       String fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag_cool);
//       String fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag_rotate);
       String fragShader = OpenGLUtils.readRawTextFile(context, mFfagRawId);

       // mProgram 相当于GPU中的一个值，在 cpu中是没用的
       mProgram = OpenGLUtils.loadProgram(vertexShader, fragShader);



       // 给 GPU 中的 vPosition 赋值
       vPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
       //接收纹理坐标，接收采样器采样图片的坐标
       vCoord = GLES20.glGetAttribLocation(mProgram, "vCoord");
       //采样点的坐标
       vTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
       //变换矩阵， 需要将原本的vCoord（01,11,00,10） 与矩阵相乘
       // 因为是摄像头所以需要这个矩阵
       //vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");





   }

    public  void beforeDraw(){};

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;

    }


    /**
     * 开始渲染
     * @param texture
     */
    public int onDraw(int texture) {
       // opengl 范围
        GLES20.glViewport(0,0,mWidth,mHeight);
        // 使用程序
        GLES20.glUseProgram(mProgram);
        // 定位到0，从0开始读取
        vertexBuffer.position(0);

        // 绑定
        //     index   指定要修改的通用顶点属性的索引。
        //     size  指定每个通用顶点属性的组件数。
        //      type  指定数组中每个组件的数据类型。
        //      接受符号常量GL_FLOAT  GL_BYTE，GL_UNSIGNED_BYTE，GL_SHORT，GL_UNSIGNED_SHORT或GL_FIXED。 初始值为GL_FLOAT。
        //      normalized    指定在访问定点数据值时是应将其标准化（GL_TRUE）还是直接转换为定点值（GL_FALSE）。
        GLES20.glVertexAttribPointer(vPosition,2,GLES20.GL_FLOAT, false,0,vertexBuffer);
        //        生效
        GLES20.glEnableVertexAttribArray(vPosition);


        // 形状
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT,
                false, 0, textureBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);
        // 因为摄像头设置的数据是第0层，所以激活这个图层
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);



        //生成一个采样
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(vTexture, 0);

        beforeDraw();

        //通知画画 渲染到屏幕了 如果是 FBO 的话，是直接给了FBO
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);

        return texture;

    }


    public void release() {

    }
}
