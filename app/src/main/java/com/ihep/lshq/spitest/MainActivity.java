package com.ihep.lshq.spitest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ihep.lshq.opengl.DefaultCameraRenderer;
import com.ihep.lshq.opengl.TextureViewGLWrapper;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.Arrays;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private TextView infoText;
    private boolean spiOpened = false;
    private ImageView mImageView;
    final  public int CryWidth = 19;
    final  public int CryHeight = 19;
    final  public int ImageWidth = 64;
    final  public int ImageHeight = 64;
    final  public int ColorNums = 256;
    final public  int ColorChannels = 3;
    private  byte [] colorLut;
    private  short[] reconArray;
    private String TAG = "SPITest";
    private SPIInterfaceThread<ImageView> mSPIInterface;

    private boolean canOpenCamera = false;
    private CameraManager cameraManager= null;
    private CameraDevice cameraDevice = null;
    private CameraCaptureSession mSession = null;
    private Surface surface = null;

    ///用于显示相机的TextureView，基于opengl实现
    private TextureView textureView;
    ///用于储存相机图像的纹理
    private SurfaceTexture surfaceTexture;
    ///控制绘制的流程
    private  TextureViewGLWrapper textureViewGLWrapper;
    ///用来实际进行相机的图像绘制的
    private DefaultCameraRenderer defaultCameraRenderer;

    private float mScaleFactor = 1.0f;
    private  ScaleGestureDetector.OnScaleGestureListener mOnScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mSPIInterface.setScaleFactor(mScaleFactor);
            return true;
        }
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };
    private  ScaleGestureDetector mScaleGestureDetector = null;
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener(){

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mScaleGestureDetector.onTouchEvent(event);
        }
    };
    protected void finalize( )
    {
        spiCloseJNI();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoText = (TextView) findViewById(R.id.info_text);
        mImageView = (ImageView) findViewById(R.id.imageview_recon);
        textureView = (TextureView)findViewById(R.id.texture_view);

        loadColorLut();
        loadReconParam();

        ///创建数据接收线程
        Handler responseHandler = new Handler();
        mSPIInterface = new SPIInterfaceThread<>(responseHandler);
        mSPIInterface.setColorLut(colorLut);
        mSPIInterface.setReconArray(reconArray);
        mSPIInterface.setBitmapShowListener(new SPIInterfaceThread.BitmapShowListener<ImageView>() {
            @Override
            public void onBitmapShowListener(ImageView target, Bitmap bitmap) {
                defaultCameraRenderer.updateBitmap(bitmap);
            }
        });
        mSPIInterface.start();
        mSPIInterface.getLooper();

        defaultCameraRenderer = new DefaultCameraRenderer(this);
        textureViewGLWrapper = new TextureViewGLWrapper(defaultCameraRenderer);
        textureViewGLWrapper.setListener(new TextureViewGLWrapper.EGLSurfaceTextureListener() {
            @Override
            public void onSurfaceTextureReady(SurfaceTexture texture) {
                surfaceTexture = texture;
                ///openCamera();
            }
        },new Handler(Looper.getMainLooper()));

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                textureViewGLWrapper.onSurfaceTextureAvailable(surface, width, height);
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                textureViewGLWrapper.onSurfaceTextureSizeChanged(surface, width, height);
            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                textureViewGLWrapper.onSurfaceTextureDestroyed(surface);
                return true;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                textureViewGLWrapper.onSurfaceTextureUpdated(surface);
            }
        });
        mScaleGestureDetector = new ScaleGestureDetector(this,mOnScaleGestureListener);
        textureView.setOnTouchListener(mOnTouchListener);
        requestForPermission();

    }
    @Override
    protected void onResume()
    {
        super.onResume();
        ///openCamera();
    }
    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }
    public void onOpenSPI(View view) {
        if(spiOpened)
        {
            infoText.setText("spi is opened!");
            return;
        }
        int ret = spiInitJNI();
        if(ret==0)
        {
            spiOpened = true;
            infoText.setText("open spi port successed!");
        }
        else
        {
            infoText.setText("failed to open spi port!");
        }
        openCamera();
        mSPIInterface.startAcquire(mImageView);
    }
    protected void requestForPermission()
    {
        RxPermissions rxPermission = new RxPermissions(MainActivity.this);
        rxPermission.requestEach(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        canOpenCamera = permission.granted;
                        if (permission.granted) {
                            Log.d(TAG, permission.name + " is granted.");
                            openCamera();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            Log.d(TAG, permission.name + " is denied. More info should be provided.");
                            finish();
                        } else {
                            Log.d(TAG, permission.name + " is denied.");
                            finish();
                        }
                    }
                });
    }
    public void onCloseSPI(View view) {
        if(mSPIInterface.isAcquiring())
            mSPIInterface.stopAcquire();
        if(spiOpened)
        {
            spiOpened = false;
            spiCloseJNI();
            infoText.setText("close spi port successed!");
        }
        closeCamera();
        mSPIInterface.stopAcquire();
    }
    protected void loadReconParam()
    {
        reconArray = new short[ImageHeight*ImageWidth];
        short smoothData1[][] = new short[CryHeight][CryWidth];
        short smoothData2[][]= new short[CryHeight][ImageWidth];
        InputStream is =  this.getApplicationContext().getResources().openRawResource(R.raw.recon);
        DataInputStream dataInputStream = new DataInputStream(is);
        try {
            for (int i = 0; i < CryHeight; i++)
                for (int j = 0; j < CryWidth; j++) {
                    byte temp = dataInputStream.readByte();
                    smoothData1[i][j] = (short)(temp&0xFF);
                }
        }
        catch (java.io.IOException e)
        {
            Log.e(TAG, "loadReconParam: "+e.toString());
        }
        for(int i=0;i<CryHeight;i++)
        {
        for(int j=0;j<ImageWidth;j++)
        {
            double orignalX = j*(CryWidth-1)*1.0f/(ImageWidth-1);
            int floor = (int)Math.floor(orignalX);
            int ceil = (int)Math.ceil(orignalX);
            double distance = ceil-orignalX;
            smoothData2[i][j] =(short)( smoothData1[i][floor]*(distance) + smoothData1[i][ceil]*(1-distance)) ;
        }
    }
        for(int i=0;i<ImageHeight;i++)
        {
            for(int j=0;j<ImageWidth;j++)
            {
                double orignalY = i*(CryHeight-1)*1.0f/(ImageHeight-1);
                int floor = (int)Math.floor(orignalY);
                int ceil = (int)Math.ceil(orignalY);
                double distance = ceil-orignalY;
                reconArray[i*ImageWidth+j] =(short)( smoothData2[floor][j]*(distance) + smoothData2[ceil][j]*(1-distance)) ;
            }
        }
    }
    protected void loadColorLut()
    {
        int colorTotalNums = ColorNums*ColorChannels;
        colorLut = new byte[colorTotalNums];
        InputStream is =  this.getApplicationContext().getResources().openRawResource(R.raw.colortable);
        DataInputStream dataInputStream = new DataInputStream(is);
        try {
            for (int i = 0; i < colorTotalNums; i++) {
                byte val = dataInputStream.readByte();
               /// int index = i%ColorNums*ColorChannels + i/ColorNums;
                colorLut[i] = val;
            }
        }
        catch (java.io.IOException e)
        {
            Log.e(TAG, "loadColorLut: "+e.toString());
        }
    }
    protected void openCamera() {
        if (!canOpenCamera) return;
        if (!textureView.isAvailable()) return;
        if (surfaceTexture == null) return;
        if (cameraDevice != null) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            finish();
            return;
        }
        cameraManager =(CameraManager)getSystemService(Context.CAMERA_SERVICE);
        CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                surface = new Surface(surfaceTexture);
                surfaceTexture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
                try {
                    final CaptureRequest.Builder req = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    req.addTarget(surface);
                    camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                req.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                req.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                                req.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
                                session.setRepeatingRequest(req.build(), null, null);
                                mSession = session;
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            };
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "onConfigure Failed");
                        }
                    }, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {

            }
            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
                Log.e(TAG,"camera open failed");
            }
        };
        try{
            cameraManager.openCamera("0",stateCallback,null);
        }catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if(mSession!=null) {
            mSession.close();
            mSession = null;
        }
        if(cameraDevice!=null)
        {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native int spiInitJNI();
    public native int spiCloseJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

}
