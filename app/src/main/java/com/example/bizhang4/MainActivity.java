package com.example.bizhang4;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import org.opencv.android.OpenCVLoader;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    static {
        System.loadLibrary("native-lib");
    }

    private Bitmap over_bitmap;//处理得到最终Bitmap

    //障碍物判定百分比数据
    private String[]result_percentag_string;
    private double[]result_percentag_double;

    TextureView showCamera_textureview;
    ImageView showChuLiCamera_iamgeview;

    CameraManager cameraManager_open;//摄像头管理
    CameraDevice OcameraDevice;//预开启的摄像头实体
    CameraCaptureSession OcameraCaptureSession;//摄像头会话

    Surface OPreviewSurface;//预览的surface实体

    private ImageReader mPreviewImageReader;//接收预览帧数据，方便后续处理
    private Image image;//ImageReader获取的Image对象

    private Handler chuliBackgroundHandler;
    private HandlerThread chulimBackgroundThread;

    ImageOpera_Opencv imageOpera_opencv;

    NotificationManager manager;//通知管理
    NotificationChannel channel;//渠道
    Notification notification;//通知体对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //获取控件
        showChuLiCamera_iamgeview=findViewById(R.id.showChuLiCamera_iamgeview);
        showCamera_textureview=findViewById(R.id.showCamera_textureview1);
        //实例化图像opencv操作方法
        imageOpera_opencv=new ImageOpera_Opencv();

        //opencv本地库加载成功后再启动摄像头和预览
        if(initOpenCv()){
            startCchuLiBackgroundThread();
            showCamera_textureview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    OPreviewSurface = new Surface(surface);
                    mPreviewImageReader=ImageReader.newInstance(//根据预览的surface大小创建imagereader
                            width,
                            height,
                            ImageFormat.YUV_420_888,
                            2);
                    mPreviewImageReader.setOnImageAvailableListener(onImageAvailableListener,chuliBackgroundHandler);//设置监听
                    openCamera();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
                    return false;
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1, int arg2) { }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture arg0) { }
            });
        }
    }

    //打开相机
    private void openCamera(){
        try {
            //获取相机管理类
            cameraManager_open=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
            //检查权限
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                return;
            }
            //开启后置主摄像头
            cameraManager_open.openCamera("0",cameraDeviceStateCallback,null);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //实现CameraDeviced的StateCallback回调接口
    private final CameraDevice.StateCallback cameraDeviceStateCallback=new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            OcameraDevice=camera;
            startPreview();//开启预览
        }

        @Override
        public void onDisconnected(CameraDevice camera) { }

        @Override
        public void onError(CameraDevice camera, int error) { }
    };

    //开启预览
    private void startPreview(){
        try {
            OcameraDevice.createCaptureSession(
                    Arrays.asList(OPreviewSurface,mPreviewImageReader.getSurface()),
                    cameraCaptureSessionStateCallback,null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    //实现CameraCaptureSession的StateCallback回调接口
    private CameraCaptureSession.StateCallback cameraCaptureSessionStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    OcameraCaptureSession=session;
                    try{
                        CaptureRequest.Builder builder;
                        builder=OcameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        //处理前预览输出的surface
                        builder.addTarget(OPreviewSurface);
                        //设置预览回调的surface
                        builder.addTarget(mPreviewImageReader.getSurface());
                        //发送摄像头图片请求和预览请求
                        OcameraCaptureSession.setRepeatingRequest(builder.build(),null,null);
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) { }
            };

    //回调获取Bitmap以便后续处理
    private ImageReader.OnImageAvailableListener onImageAvailableListener=new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            image=reader.acquireLatestImage();//从流中获取到Image对象
            if(image==null){
                Log.e(TAG,"image未获取到");
                return;
            }
            //Image对象转处理前的Bitmap
            Bitmap cv_preview_bitmap=imageOpera_opencv.getBitmapFromImage(image);
            //处理前Bitmap转处理后Bitmap
            over_bitmap=imageOpera_opencv.OperaBitmap_JAVA_C(cv_preview_bitmap);
            //接收未加权百分比数据
            result_percentag_string=Image_Obstacle_Check.evaluation_Percentage();
            //接收加权计算结果
            result_percentag_double=Image_Obstacle_Check.evaluation_Percentage_Weight();
            retangle_All_Bitmap();
            retangle_Left_Low_Weight_Bitmap();
            retangle_Right_Low_Weight_Bitmap();
            if(Image_Obstacle_Check.check_Obstacle()){
                obstacle_Notification("警告","前方有障碍物");
            }
            //获取到bitmap更新，并通知主线程刷新ImageView
            Message message=new Message();
            message.what=9;
            handler.sendMessage(message);
            //释放资源
            cv_preview_bitmap.recycle();
            image.close();
        }
    };

    //在Bitmap上绘制矩形框中高权值识别敏感区
    //区域坐标:左上(108,300)，右上(612,300)，左下(108.720),右下(612,720)
    private void retangle_All_Bitmap(){
        Canvas canvas=new Canvas(over_bitmap);
        Rect rect=new Rect();
        rect.set(192,500,612,720);
        //设置画笔
        Paint p=new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        Paint p_text=new Paint();
        p_text.setColor(Color.RED);
        p_text.setStyle(Paint.Style.FILL);
        p_text.setTextSize(20);
        //绘制
        canvas.drawRect(rect,p);
        canvas.drawText(result_percentag_string[1]+"%",320,500,p_text);
        canvas.drawText(result_percentag_double[1]+"%",320,520,p_text);
        canvas.drawText("总加权百分比"+result_percentag_double[3]+"%",300,540,p_text);
    }

    //绘制左低权值敏感区
    //区域坐标:左上(108,300)，右上(108+84=192,300)，左下(108.720),右下(108+84=192,720)
    private void retangle_Left_Low_Weight_Bitmap(){
        Canvas canvas=new Canvas(over_bitmap);
        Rect rect=new Rect();
        rect.set(108,500,192,720);
        //设置画笔
        Paint p=new Paint();
        p.setColor(Color.GREEN);
        p.setStyle(Paint.Style.STROKE);
        Paint p_text=new Paint();
        p_text.setColor(Color.GREEN);
        p_text.setStyle(Paint.Style.FILL);
        p_text.setTextSize(20);
        //绘制
        canvas.drawRect(rect,p);
        canvas.drawText(result_percentag_string[0]+"%",108,500,p_text);
        canvas.drawText(result_percentag_double[0]+"%",108,520,p_text);
    }

    //绘制右低权值敏感区
    //区域坐标:左上(612-84=528,300)，右上(612,300)，左下(612-84=528.720),右下(612,720)
    private void retangle_Right_Low_Weight_Bitmap(){
        Canvas canvas=new Canvas(over_bitmap);
        Rect rect=new Rect();
        rect.set(528,500,612,720);
        //设置画笔
        Paint p=new Paint();
        p.setColor(Color.GREEN);
        p.setStyle(Paint.Style.STROKE);
        Paint p_text=new Paint();
        p_text.setColor(Color.GREEN);
        p_text.setStyle(Paint.Style.FILL);
        p_text.setTextSize(20);
        //绘制
        canvas.drawRect(rect,p);
        canvas.drawText(result_percentag_string[2]+"%",528,500,p_text);
        canvas.drawText(result_percentag_double[2]+"%",528,520,p_text);
    }

    /**
     * 发送障碍物识别通知
     * @param strTitle
     * @param strContent
     * Android10.0中为通知引入了渠道概念，需通过建立channel完成渠道设置
     */
    private void obstacle_Notification(String strTitle, String strContent){
        //设置渠道基本属性
        String channelid="warning";
        String channel_name="警告";
        int importance=NotificationManager.IMPORTANCE_HIGH;
        //使用渠道基本属性建立渠道对象
        channel=new NotificationChannel(channelid,channel_name,importance);
        //建立通知管理对象
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //将渠道绑定到通知管理对象中
        manager.createNotificationChannel(channel);
        //设置弹窗通知基本属性
        notification= new NotificationCompat.Builder(MainActivity.this,channelid)
                .setContentTitle(strTitle)
                .setContentText(strContent)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.warn_icon)
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.warn_icon))
                .build();
        //发送通知
        manager.notify(1, notification);
    }

    //加载本地opencv库
    private boolean initOpenCv(){
        boolean success= OpenCVLoader.initDebug();
        if(success){
            Log.e(TAG,"opencv库记载成功");
        }else {
            Log.e(TAG,"opencv库加载失败");
            Toast.makeText(this, "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG).show();
        }
        return success;
    }

    //主线程接收消息并刷新UI
    Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what==9){
                //接收到新的Bitmap后更新ImageView
                showChuLiCamera_iamgeview.setImageBitmap(over_bitmap);
            }
        }
    };

    //为回调ImageReader开启子线程
    private void startCchuLiBackgroundThread() {
        //Log.e(TAG, "开启图像处理的线程");
        if (chulimBackgroundThread == null || chuliBackgroundHandler == null) {
            Log.e(TAG, "startBackgroundThread");
            chulimBackgroundThread = new HandlerThread("ChuLiCameraBackground");
            chulimBackgroundThread.start();
            chuliBackgroundHandler = new Handler(chulimBackgroundThread.getLooper());
        }
    }
}
