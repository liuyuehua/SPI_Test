#include <jni.h>
#include <string>
#include "spihal.h"

#define  min(a,b) (a<b)?a:b
#define  max(a,b) (a>b)?a:b
#include <android/bitmap.h>
#include <stdlib.h>

#define RGBA_A(p) (((p) & 0xFF000000) >> 24)
#define RGBA_R(p) (((p) & 0x00FF0000) >> 16)
#define RGBA_G(p) (((p) & 0x0000FF00) >>  8)
#define RGBA_B(p)  ((p) & 0x000000FF)
#define MAKE_RGBA(r,g,b,a) (((a) << 24) | ((b) << 16) | ((g) << 8) | (r))
#define COLOR_NUMS 256
#define  COLOR_CHANNELS 3
#define  IMAGE_WIDTH 64
#define  IMAGE_HEIGT 64
#define  RECON_WIDTH 19
#define RECON_HEIGHT 19

static int fd = -1;

extern "C"
jint
Java_com_ihep_lshq_spitest_MainActivity_spiInitJNI(
        JNIEnv* env,
        jobject /* this */) {
    (void*)env;
    if(hal_spi_open(&fd,DEVICE_NAME)!=0) {
        fd = -1;
        return -1;
    }
    hal_spi_init(fd);
    return 0;
}

extern "C"
jint
Java_com_ihep_lshq_spitest_MainActivity_spiCloseJNI(
        JNIEnv* env,
        jobject /* this */) {
    (void*)env;
    if(fd<=0)
        return 0;
    hal_spi_close(fd);
    return fd;
}

extern "C"
jstring
Java_com_ihep_lshq_spitest_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";

    return env->NewStringUTF(hello.c_str());
}
void getArrayRange(jint *arr,int len,jint *pMax,jint *pMin)
{
    jint  minVal = arr[0];
    jint  maxVal = arr[0];
    for(int i=0;i<len;i++)
    {
        jint val = arr[i];
        minVal = min(minVal,val);
        maxVal = max(maxVal,val);
    }
    *pMax = maxVal;
    *pMin = minVal;
}

void FillBitmapWithSameColor(  AndroidBitmapInfo &info,void *pixels,jint pMax,jint pMin)
{
    u_int32_t  val = 0;
    if(pMax)
    {
        val = MAKE_RGBA(128,128,128,128);
    } else
    {
        int len = info.width*info.height* sizeof(uint32_t);
        memset(pixels,0,len);
    }
}
void FillBitmapWithData(  AndroidBitmapInfo &info,jint  *rawData,jbyte  *colorLut,void *pixels,jint pMax,jint pMin,float scaleFactor)
{
    int range = pMax-pMin;
    for (int y = 0; y < info.height; ++y)
    {
        for (int x = 0; x < info.width; ++x)
        {
            int offset = y * info.width + x;
            jint index = (rawData[offset]-pMin)*255/range;
            if(index>255)
            {
                index = 255;
            }
            uint32_t r = (unsigned char)colorLut[index*3];
            uint32_t g = (unsigned char)colorLut[index*3+1];
            uint32_t b = (unsigned char)colorLut[index*3+2];
            int thresh = 128*scaleFactor;
            int a = index>thresh?index:0;
            uint32_t rgba = MAKE_RGBA(r,g,b,a);
            uint32_t *pixel = ((uint32_t *)pixels) + offset;
            *pixel = rgba;
        }
    }
}
extern "C"
void Java_com_ihep_lshq_spitest_SPIInterfaceThread_data2BitmapJNI(
        JNIEnv* env,
        jobject /* this */,
        jintArray   rawData,
        jbyteArray  colorLut,
        jobject zBitmap,
        float scaleFactor)
{
    jint minVal;
    jint maxVal;
    jint *carr = env->GetIntArrayElements(rawData, false);
    jbyte *lut = env->GetByteArrayElements(colorLut, false);


    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(env, zBitmap, &info);
    // Lock the bitmap to get the buffer
    void * pixels = NULL;
    int res = AndroidBitmap_lockPixels(env, zBitmap, &pixels);
    getArrayRange(carr,info.width*info.height,&maxVal,&minVal);
    if(maxVal==minVal)
    {
        FillBitmapWithSameColor(info,pixels,maxVal,minVal);
    } else{
        FillBitmapWithData(info,carr,lut,pixels,maxVal,minVal,scaleFactor);
    }
    AndroidBitmap_unlockPixels(env, zBitmap);
}
extern "C"
void Java_com_ihep_lshq_spitest_SPIInterfaceThread_generateDataJNI(
        JNIEnv* env,
        jobject /* this */,
        jintArray   accumulateArray,
        jshortArray reconArray)
{
    jint minVal;
    jint maxVal;
    int lenAccumulate = env->GetArrayLength(accumulateArray);
    int lenRecon = env->GetArrayLength(reconArray);
    jint *carr = env->GetIntArrayElements(accumulateArray, false);
    jshort *original = env->GetShortArrayElements(reconArray, false);
    for(int i=0;i<5;i++)
    {
        int x = rand()%IMAGE_WIDTH;
        int y = rand()%IMAGE_HEIGT;
        int pos = x+y*IMAGE_WIDTH;
        int val = rand()%255;
        int originalVal = original[pos];
        if(val <= originalVal)
        {
            carr[pos]++;
        }
    }
}
