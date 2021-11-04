#include <jni.h>
#include <string>
#include<sys/stat.h>
#include <opencv2/opencv.hpp>
#include "opencv2/opencv.hpp"
#include "android/log.h"
#include <android/native_window_jni.h>

#define TAG "zhiyang"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEFAULT,TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)

using namespace cv;
DetectionBasedTracker *tracker = 0;
ANativeWindow *window = 0;

// 检测器的adapter RecelerView的adapter
class CascadeDetectorAdapter : public DetectionBasedTracker::IDetector {
//
// 1 排除色彩，
// 2 先变成灰度图，
// 3 转成二值化（纯黑白）
// 4.轮廓检测侧（））

public:
    CascadeDetectorAdapter(const Ptr<CascadeClassifier> &detector) : detector(detector) {}

    // 有多少个块
    void detect(const cv::Mat &image, std::vector<cv::Rect> &objects) {
        detector->detectMultiScale(image, objects, scaleFactor, minNeighbours, 0, minObjSize,
                                   maxObjSize);
    }

private:

    CascadeDetectorAdapter();

    Ptr<CascadeClassifier> detector;
};


extern "C" JNIEXPORT jstring JNICALL
Java_com_nzy_opencv_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

static int index = 0;
static int rectIndex = 0;

void savePic2Path(JNIEnv *env, jstring _path, Mat mat, bool isRect) {
    // 把 图片 保存到 files/img 中
    char pic[100];
    char facePic[100];
    const char *path = env->GetStringUTFChars(_path, NULL);

    sprintf(pic, "%s/pic", path);
    sprintf(facePic, "%s/facePic", path);


    int ret = mkdir(pic, 0777);
    int ret2 = mkdir(facePic, 0777);

    char picName[100];
    char faceName[100];

    LOGE("ret = %d", ret);
    // 字符串拼接到p中
    if (isRect) {
        sprintf(faceName, "%s/%d.jpg", facePic, rectIndex++);
        imwrite(faceName, mat);
    } else {
        sprintf(picName, "%s/%d.jpg", pic, index++);
        imwrite(picName, mat);
    }


    env->ReleaseStringUTFChars(_path, path);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_opencv_MainActivity_init(JNIEnv *env, jobject thiz, jstring _model) {
    // 加载模型
    const char *model = env->GetStringUTFChars(_model, NULL);
    // 相当于 CascadeClassifier *classifier = new classifier(model)
    // 检测器，Ptr 叫做智能指针
    // 不用 delete classifier 这个了而已 Ptr 也是继承自  std::shared_ptr
    // 检测器
    Ptr<CascadeClassifier> classifier = makePtr<CascadeClassifier>(model);
    //  CascadeDetectorAdapter *cascadeDetectorAdapter = new CascadeDetectorAdapter()
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(classifier);


    // 跟踪器
    Ptr<CascadeClassifier> classifier1 = makePtr<CascadeClassifier>(model);
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(classifier1);

    // cv::Ptr<IDetector> mainDetector,  是检测器
    // cv::Ptr<IDetector> trackingDetector,  是跟踪器
    // const Parameters& params
    DetectionBasedTracker::Parameters detectorParams;
    tracker = new DetectionBasedTracker(mainDetector, trackingDetector, detectorParams);
    // 开始工作
    tracker->run();


    env->ReleaseStringUTFChars(_model, model);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_opencv_MainActivity_postData(JNIEnv *env, jobject thiz, jbyteArray _data, jint width,
                                          jint height, jint camera_id, jstring _path) {
    jbyte *data = env->GetByteArrayElements(_data, NULL);

    // 分析
    //    Mat *mat = new Mat();
    // YUV420 是
    // Y是height*height
    // U是 Y/4
    // V是 Y/4          传过来的是NV21（摄像头独有的格式）
    Mat src(height + height / 2, width, CV_8UC1, data);

    //InputArray src,
    // OutputArray dst,
    // int code,
    // int dstCn = 0
    // 把 NV21 转成 RGBA
    cvtColor(src, src, COLOR_YUV2BGRA_NV21);
    if (camera_id == 1) {
        // 后置摄像头，需要逆时针旋转90
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        // 左右反转
        flip(src, src, 1);
    } else {
        // 前置摄像头，需要顺时针旋转90
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }

    // 1 。变成灰色
    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);

    // 2 增强对比度
    equalizeHist(gray, gray);


//    savePic2Path(env, _path, gray, false);

    // 3 检测人脸,如果有就以  Rect 形式保存到 vector 中
    // 如果有杯子的训练集，也可以检测杯子，银行卡，等等。只要有训练集都可以

    std::vector<Rect> faces;
    tracker->process(gray);
    tracker->getObjects(faces);
//    LOGE("输出 rect %d", faces.size());
    //遍历矩形这都是能看见人脸的
    for (Rect face :faces) {

        Mat m;
        // 从gray的整张图片，把人脸的部分copy出来给m
        m = gray(face).clone();


        // 训练的样本，的宽高 ，影响识别的快慢,所以小一点，效率会快很多
        // 一般不超过 48
        resize(m, m, Size(40, 40));

        // 把矩形保存的图片保存到 文件中
        // 这个的大小就是 40x40 ，如果放在 resize 之前的话，也就是没有压缩过，那么 就是230x230了（具体看模型）
//        savePic2Path(env, _path, m, true);


        // 画个框框 ，在原图上话
        // Scalar *scalar = new Scalar(255, 0, 0);
        // rectangle(src, face, *scalar);
        Scalar scalar = Scalar(255, 0, 0);
        // 在原图上画 脸的框框
        rectangle(src, face, scalar);


    }
    // 把数据画到 SurfaceView
    //  Mat 和 Bitmap 是可以互转的
    // window 里面有个buffer

    if (window) {
        do {
            if (!window) {
                break;
            }
            // 拿到缓冲区
            ANativeWindow_setBuffersGeometry(window, src.cols, src.rows,
                                             WINDOW_FORMAT_RGBA_8888);
            ANativeWindow_Buffer buffer;
            // 返回 0是成功，成功之后 这里的buffer就有数据了
            int ret = ANativeWindow_lock(window, &buffer, 0);
            if (ret) {
                //失败了释放掉哦
//                ANativeWindow_release(window);
//                window = 0;
                break;
            }
            // 每一行 因为是rgba 的格式，所以 width 乘以4个字节
            int srclineSize = src.cols * 4;

            // 目的数据
            int dstlineSize = buffer.stride * 4;

            // 待显示的缓冲区
            uint8_t *dstData = static_cast<uint8_t *>(buffer.bits);

            for (int i = 0; i < buffer.height; i++) {
                // src.data 图像像素的数据源
                // for 循环行数,每一行添加数据
                memcpy(dstData+i*dstlineSize, src.data+i*srclineSize, srclineSize);
            }
            // 释放锁
            ANativeWindow_unlockAndPost(window);

        } while (0);

    }

    env->ReleaseByteArrayElements(_data, data, 0);

}
extern "C"
JNIEXPORT void JNICALL
Java_com_nzy_opencv_MainActivity_setSurface(JNIEnv *env, jobject thiz, jobject surface) {
    // 渲染Surface的时候 直接找window ,#include "android/native_window.h",
    // 在 CmakerList中加入 android 的链接 即使 ndk系统目录下的android.so
    // 可以理解为 Surface中有一个window，找到这个Window
    LOGE("setSurface %d",window);
    if (window) {
        ANativeWindow_release(window);
        window = 0;
    }

    window = ANativeWindow_fromSurface(env, surface);
    LOGE("setSurface %d",window);

}