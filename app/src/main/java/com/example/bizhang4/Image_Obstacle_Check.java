package com.example.bizhang4;

import org.opencv.core.Mat;
import java.text.DecimalFormat;

/**
 * 图像障碍物检查
 * 像素点计算，障碍物判断
 */
public class Image_Obstacle_Check {
    static String TAG="Image_Obstacle_Check";

    //预定义三个区域总像素数
    static private int left_sum_pixel=84*220;
    static private int high_sum_pixel=432*220;
    static private int right_sum_pixel=84*220;
    //接收三个区域白色像素值数
    static private int left_white_pixel;
    static private int high_white_pixel;
    static private int right_white_pixel;
    //总加权百分比值
    static private double weight_percent;

    /**获取中部的高权值敏感区域像素
     * width：612-108=504
     * height： 720-500=220
     */
    private Mat getMat_High__Pixel(Mat input_mat){
        //设置宽、长，获取通道数
        int real_width=432;
        int real_height=720;
        int channels_input_mat=input_mat.channels();
        //根据图像宽、通道数建立数组，方便处理
        byte[]data=new byte[real_width*channels_input_mat];
        int pv;int num_high=0;
        for(int row=500;row<real_height;row++) {
            //以第row行，第54列开始读取宽度内的一行的像素
            input_mat.get(row, 96, data);
            //Log.e(TAG,"data的长度"+data.length);
            for (int col =96; col < data.length; col++) {
                //读取这个像素
                pv = data[col] & 0xff;
                //判断是否白色像素
                if(pv==255){
                    num_high++;
                }
            }
            input_mat.put(row, 96, data);
        }
        high_white_pixel=num_high;
        return getMat_Right_Pixel(input_mat);
    }

    /**获取左低权值敏感区域像素
     * width：192-108=84
     * height： 720-500=220
     */
    public Mat getMat_Left_Pixel(Mat input_mat){
        //设置宽、长，获取通道数
        int real_width=138;
        int real_height=720;
        int channels_input_mat=input_mat.channels();
        //根据图像宽、通道数建立数组，方便处理
        byte[]data_left=new byte[real_width*channels_input_mat];
        int pv_left;int num_left=0;
        for(int row=500;row<real_height;row++){
            //以第row行，第54列开始读取宽度内的一行的像素
            input_mat.get(row,54,data_left);
            //Log.e(TAG,"data的长度"+data.length);
            for(int col=54;col<data_left.length;col++){
                //读取这个像素
                pv_left=data_left[col]&0xff;
                //判断是否白色像素
                if(pv_left==255){
                    num_left++;
                }
            }
            input_mat.put(row,54,data_left);
        }
        left_white_pixel=num_left;
        return getMat_High__Pixel(input_mat);
    }

    /**获取右低权值敏感区域像素
     * width：612-528=84
     * height：720-500=220
     */
    private Mat getMat_Right_Pixel(Mat input_mat){
        //设置宽、长，获取通道数
        int real_width=804;
        int real_height=720;
        int channels_input_mat=input_mat.channels();
        //根据图像宽、通道数建立数组，方便处理
        byte[]data=new byte[real_width*channels_input_mat];
        int pv_right;int num_right=0;
        for(int row=500;row<real_height;row++){
            //以第row行，第54列开始读取宽度内的一行的像素
            input_mat.get(row,528,data);
            //Log.e(TAG,"data的长度"+data.length);
            for(int col=720;col<data.length;col++){
                //读取这个像素
                pv_right=data[col]&0xff;
                //判断是否白色像素
                if(pv_right==255){
                    num_right++;
                }
            }
            input_mat.put(row,528,data);
        }
        right_white_pixel=num_right;
        return input_mat;
    }

    /**
     * 计算三个区域百分比(非加权）
     */
    static public String[] evaluation_Percentage(){
        String []result_percentag_string=new String[3];
        DecimalFormat decimalFormat=new DecimalFormat("00.00");//格式化保留两位小数
        result_percentag_string[0]=decimalFormat.format(((double) left_white_pixel/left_sum_pixel)*100);
        result_percentag_string[1]=decimalFormat.format(((double) high_white_pixel/high_sum_pixel)*100);
        result_percentag_string[2]=decimalFormat.format(((double)right_white_pixel/right_sum_pixel)*100);
        return result_percentag_string;
    }

    /**
     * 计算三个区域百分比(加权）
     */
    static public double [] evaluation_Percentage_Weight(){
        double []result_percentag_double=new double[4];
        //设定高权值区和低权值区的加权数值
        int low_weight_num=1;
        int hign_weight_num=10;
        //对三个区域像素值进行加权计算
        result_percentag_double[0]=Double.valueOf(
                String.format("%.2f",(double) left_white_pixel/left_sum_pixel*100*low_weight_num));
        result_percentag_double[1]=Double.valueOf(
                String.format("%.2f",(double) high_white_pixel/high_sum_pixel*100*hign_weight_num));
        result_percentag_double[2]=Double.valueOf(
                String.format("%.2f",(double) right_white_pixel/right_sum_pixel*100*low_weight_num));
        //计算加权后的平均百分比
        weight_percent=Double.valueOf(
                String.format("%.2f",(result_percentag_double[0]+result_percentag_double[1]+result_percentag_double[2])/3));
        result_percentag_double[3]=weight_percent;
        return result_percentag_double;
    }

    /**
     * 根据总加权百分比值判断是否存在障碍物
     * @return 存在：true    不存在：false
     */
    static public boolean check_Obstacle(){
        if (weight_percent>=4.5){
            return true;
        }
        return false;
    }
}
