package com.example.bizhang4;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.io.ByteArrayOutputStream;

public class ImageOpera_Opencv {
    String TAG="ImageOpera_Opencv";
    //实例化障碍物检查类
    static Image_Obstacle_Check image_obstacle_check=new Image_Obstacle_Check();
    //声明C++方法
    //public native ImageData ImageProJNI(long mat_addres);
    public native long OperaMat_C(long mat_addres);

    //中值滤波降噪
    private Mat mediaBlur_Opera(Mat pre_mat){
        Mat over_mat=new Mat();
        Imgproc.medianBlur(pre_mat,over_mat,3);
        return over_mat;
    }

    //对图像进行Canny边缘处理
    private Mat Canny_Opera(Mat pre_mat){
        Imgproc.GaussianBlur(pre_mat,pre_mat,new Size(3,3),0);
        //Mat temp_mat=new Mat();
        //Imgproc.cvtColor(pre_mat,temp_mat,Imgproc.COLOR_BGR2GRAY);
        Mat over_mat=new Mat();
        Imgproc.Canny(pre_mat,over_mat,75,225,3,true);
        //传入最终处理的边缘图像，进行像素值统计
        over_mat=image_obstacle_check.getMat_Left_Pixel(over_mat);
        pre_mat.release();
        return over_mat;
    }

    //对图像进行二值化
    private Mat Threshold(Mat mat){
        /********普通阈值二值化********/
        Mat temp_mat=new Mat();
        Imgproc.cvtColor(mat,temp_mat,Imgproc.COLOR_BGR2GRAY);
        Mat over_mat=new Mat();
        //阈值取零
        Imgproc.threshold(temp_mat,over_mat,35,255,
                Imgproc.THRESH_TOZERO);
        //中值滤波降噪
        over_mat=mediaBlur_Opera(over_mat);
        /********普通阈值二值化********/
        temp_mat.release();
        return Canny_Opera(over_mat);
    }

    //调用java层/C++层——opencv方法-处理获取到的Bitmap并返回处理后的Bitmap
    public Bitmap OperaBitmap_JAVA_C(Bitmap bitmap){
        Mat pre_mat=new Mat();
        Utils.bitmapToMat(bitmap,pre_mat);
        //Mat over_mat_threshold=Threshold(pre_mat);
        //java层调用opencv方法处理
        Mat over_mat=Threshold(pre_mat);
        //c++层调用opencv方法处理
        //Mat over_mat=OperaBitmap_C(pre_mat);
        //Log.e(TAG,"over_mat111:"+over_mat.cols()+" "+over_mat.rows());
        Bitmap over_bitmap=Bitmap.createBitmap(over_mat.cols(),over_mat.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(over_mat,over_bitmap);
        pre_mat.release();
        over_mat.release();
        return over_bitmap;
    }

    /**
     * 将YUV数据封装为Bitmap
     * @param data    nv21格式数组
     * @param width   格式化后的宽度
     * @param height  格式化后的长度
     * @return 完成基础封装转化的Bitmap图像
     */
    private static Bitmap getBitmapImageFromYUV(byte[] data, int width, int height) {
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 80, baos);
        byte[] jdata = baos.toByteArray();
        BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
        bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
        return bmp;
    }

    /**
     * 从Image对象中提取YUV并调用自定义方法进行图像提取封装
     * @param image ImageReader读取到的图像
     * @return 提取到的Bitmap格式图像
     */
    public Bitmap getBitmapFromImage(Image image) {
        int w = image.getWidth(), h = image.getHeight();
        int i420Size = w * h * 3 / 2;
        Image.Plane[] planes = image.getPlanes();
        int remaining0 = planes[0].getBuffer().remaining();
        int remaining1 = planes[1].getBuffer().remaining();
        int remaining2 = planes[2].getBuffer().remaining();
        //获取pixelStride，可能跟width相等，可能不相等
        int pixelStride = planes[2].getPixelStride();
        int rowOffest = planes[2].getRowStride();
        byte[] nv21 = new byte[i420Size];
        byte[] yRawSrcBytes = new byte[remaining0];
        byte[] uRawSrcBytes = new byte[remaining1];
        byte[] vRawSrcBytes = new byte[remaining2];
        planes[0].getBuffer().get(yRawSrcBytes);
        planes[1].getBuffer().get(uRawSrcBytes);
        planes[2].getBuffer().get(vRawSrcBytes);
        if (pixelStride == w) {
            //两者相等，说明每个YUV块紧密相连，可以直接拷贝
            System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h);
            System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1);
        } else {
            byte[] ySrcBytes = new byte[w * h];
            byte[] vSrcBytes = new byte[w * h / 2 - 1];
            for (int row = 0; row < h; row++) {
                //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w);
                //y执行两次，uv执行一次
                if (row % 2 == 0) {
                    //最后一行需要减一
                    if (row == h - 2) {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w - 1);
                    } else {
                        System.arraycopy(vRawSrcBytes, rowOffest * row / 2, vSrcBytes, w * row / 2, w);
                    }
                }
            }
            System.arraycopy(ySrcBytes, 0, nv21, 0, w * h);
            System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1);
        }
        Bitmap bm = getBitmapImageFromYUV(nv21, w, h);
        Matrix m = new Matrix();
        m.setRotate(90, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        Bitmap result = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
        return result;
    }
}
