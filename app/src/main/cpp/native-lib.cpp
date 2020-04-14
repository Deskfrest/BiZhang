#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <iostream>

// 定义LOG类型，便于调试
#ifndef LOG_TAG
#define LOG_TAG "logtest"
#define slogd(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif

using namespace cv;
using namespace std;


extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_bizhang4_ImageOpera_1Opencv_OperaMat_1C(JNIEnv *env, jobject obj,
                                                         jlong mat_Address) {
    Mat *pre_mat=(Mat*)mat_Address;
    if(NULL==pre_mat){

        return -1;
    }
    long resultAddress=(jlong)pre_mat;
    std::string test_s=to_string(resultAddress);
    slogd("address=%s",test_s.c_str());
    //pre_mat->release();
    return resultAddress;
    //阈值取零二值化

    //中值滤波降噪

    //Canny提取边缘

    //
}