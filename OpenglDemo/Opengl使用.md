### 使用
1. 在清单文件 写 权限的地方
0x00010000  0x00020000(这个版本是最稳定的)   0x00030000
```
    <uses-feature android:glEsVersion="0x00020000" android:required="true"/>

```
或者在代码中
```
GlSurfaceView.setEGLContextClientVersion(2)
```
2. 使用 android.opengl.GLSurfaceView

view - > cavase -> skia -> opengl -> gpu(渲染)

也就是 cpu 交给 gpu 去渲染
32个纹理——>栅格化（形状）-> 上色 ——> 渲染到显示器

### glsl 插件(写着色器)
可以写 vert 程序

###