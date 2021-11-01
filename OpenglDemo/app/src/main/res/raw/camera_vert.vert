// 把顶点坐标给这个变量， 确定要画画的形状
//字节定义的  4个   数组  矩阵
attribute vec4 vPosition;//0
//cpu
//接收纹理坐标，接收采样器采样图片的坐标  camera
attribute vec4 vCoord;

//   oepngl    camera
uniform mat4 vMatrix;

//传给片元着色器 像素点
varying vec2 aCoord;
void main(){

    //gl_PointSize	点渲染模式，方形点区域渲染像素大小	float
    //gl_Position	顶点位置坐标	vec4
    //gl_FragColor	片元颜色值	vec4
    //gl_FragCoord	片元坐标，单位像素	vec2
    //gl_PointCoord	点渲染模式对应点像素坐标	vec2


    //    gpu  需要渲染的 什么图像   形状
    gl_Position=vPosition;
    //    遍历的   for循环   性能比较低
    aCoord= (vMatrix * vCoord).xy;
}








