/**
 * Created by lshq on 2017/8/13.
 */

package com.ihep.lshq.spitest;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class SPIInterfaceThread<T> extends HandlerThread {
    private static final String TAG = "SPIInterfaceThread";
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private static final int MESSAGE_START = 0;
    private boolean mAcquiring = false;

    private Bitmap mBitmap;
    final  public int CryWidth = 19;
    final  public int CryHeight = 19;
    final  public int ImageWidth = 64;
    final  public int ImageHeight = 64;

    final  public int ColorNums = 256;
    final public  int ColorChannels = 3;
    private  byte [] colorLut = null;
    private  short[] reconArray=null;
    private  int[] accumulatedArray=null;
    private BitmapShowListener<T> mBitmapShowListener;
    private float mScaleFactor=1.0f;

    @Override
    protected void finalize() throws Throwable {
    }
    public interface BitmapShowListener<T> {
        void onBitmapShowListener(T target, Bitmap bitmap);
    }
    public void setBitmapShowListener(BitmapShowListener<T> listener) {
        mBitmapShowListener = listener;
    }
    public SPIInterfaceThread(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        loadColorLut();
        initReconParam();
    }
    public void stopAcquire()
    {
        mAcquiring = false;
    }
    public boolean isAcquiring()
    {
        return  mAcquiring;
    }
    public void setScaleFactor(float factor){mScaleFactor = factor;}

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_START) {
                    T target = (T) msg.obj;
                   resetArray(accumulatedArray);
                    mAcquiring = true;
                    handleRequest(target);
                }
            }
        };
    }
    public void startAcquire(T target) {
      mRequestHandler.obtainMessage(MESSAGE_START, target)
                    .sendToTarget();
    }
    private void handleRequest(final T target) {
        long timeLast = System.currentTimeMillis();
        while(isAcquiring())
        {
            generateDataJNI(accumulatedArray,reconArray);
            try {
                this.sleep(1);
            }
            catch (InterruptedException e)
            {

            }
            finally {

            }

            long timeCurrent = System.currentTimeMillis();
            if(timeCurrent-timeLast>=1000) {
                data2BitmapJNI(accumulatedArray, colorLut, mBitmap,mScaleFactor);
                mResponseHandler.post(new Runnable() {
                    public void run() {
                        if(!mBitmap.isRecycled())
                        {
                            mBitmapShowListener.onBitmapShowListener(target, mBitmap);
                        }

                    }
                });
                timeLast = timeCurrent;
            }
        }
    }

    public void setColorLut(byte []lut)
    {
        if(lut.length!=ColorChannels*ColorNums)
            return;
        colorLut = lut;
    }
    public void setReconArray(short [] array)
    {
        if(array.length!=ImageWidth*ImageHeight)
            return;
        reconArray = array.clone();
    }

    protected void resetArray(int []array)
    {
        for(int i=0;i<array.length;i++)
        {
            array[i] = 0;
        }
    }
    protected void initReconParam()
    {
        mBitmap = Bitmap.createBitmap(ImageWidth,ImageHeight, Bitmap.Config.ARGB_8888);
//        int color = (25<<24)+(25<<16)+(25<<8)+25;
//        mBitmap.setPixel(0,0,color);
        reconArray = new short[ImageHeight*ImageWidth];
        accumulatedArray = new int[ImageHeight*ImageWidth];
    }
    protected void loadColorLut()
    {
        int colorTotalNums = ColorNums*ColorChannels;
        colorLut = new byte[colorTotalNums];
        for(int i=0;i<ColorNums;i++)
        for(int j=0;j<ColorChannels;j++)
        {
            colorLut[i*ColorChannels + j] = (byte)i;
        }
    }
    public native void data2BitmapJNI(  int []   rawData,byte []  colorLut, Bitmap zBitmap,float scaleFactor);
    public native void generateDataJNI(  int []   accumulateArray,short []  reconArray);
}

