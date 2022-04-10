#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>

extern "C" {
#include "gif_lib.h"
}

#define ARGB(r, g, b) (0xff000000 | (((r)&0xff)<<16) | (((g)&0xff)<<8) | ((b)&0xff))
#define LOG_TAG "NATIVE_LIB"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGDF(fmt, ...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,fmt,__VA_ARGS__)

struct PlayInfo {
    int totalFrame;
    int curFrame;
    int delay = 100;// millis seconds
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_video_1audio_GifHelper_init(JNIEnv *env, jobject thiz, jstring gif_path) {
    // TODO: implement init()
    const char *filename = env->GetStringUTFChars(gif_path, 0);
    int error;
    GifFileType *gifFile;
    gifFile = DGifOpenFileName(filename, &error);
    DGifSlurp(gifFile);

    LOGDF("====~ INIT GIF,  ExtensionBlockCount = %d ", gifFile->ExtensionBlockCount);
    if (gifFile->ExtensionBlockCount > 0) {
        for (int i = 0; i < gifFile->ExtensionBlockCount; ++i) {
            LOGDF("====~ INIT GIF,   ,ExtensionBlocks =  %d",
                  (gifFile->ExtensionBlocks + i)->Function);
        }
    }
//    LOGDF("====~ INIT GIF,  Function = %d ", gifFile->SavedImages[0].ExtensionBlocks->Function);
//    LOGDF("====~ INIT GIF,  result = %d , gcb = %d ", rlt, gcb->DelayTime);
//    LOGDF("====~ INIT GIF,   ,Function =  %d" ,gifFile->ExtensionBlocks->Function );
    PlayInfo *info = static_cast<PlayInfo *>(malloc(sizeof(PlayInfo)));
    memset(info, 0, sizeof(PlayInfo));
    info->totalFrame = gifFile->ImageCount;
    info->curFrame = 0;
    gifFile->UserData = info;
    //获取延时时间
    GraphicsControlBlock *gbc = new GraphicsControlBlock();
    DGifSavedExtensionToGCB(gifFile, 0, gbc);
    LOGDF("====~ INIT GIF,  DelayTime = %d ", gbc->DelayTime);
    LOGDF("====~ INIT GIF,  DisposalMode = %d ", gbc->DisposalMode);
    LOGDF("====~ INIT GIF,  TransparentColor = %d ", gbc->TransparentColor);
    LOGDF("====~ INIT GIF,  UserInputFlag = %d ", gbc->UserInputFlag ? 1 : 0);
    info->delay = gbc->DelayTime * 10;
    delete gbc;

    env->ReleaseStringUTFChars(gif_path, filename);
    return reinterpret_cast<jlong>(gifFile);
}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_video_1audio_GifHelper_getWidth(JNIEnv *env, jobject thiz, jlong ptr) {
    GifFileType *gifFile = reinterpret_cast<GifFileType *>(ptr);
    return gifFile->SWidth;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_video_1audio_GifHelper_getHeight(JNIEnv *env, jobject thiz, jlong ptr) {
    GifFileType *gifFile = reinterpret_cast<GifFileType *>(ptr);
    return gifFile->SHeight;
}


void drawFrame(GifFileType *gifFile, AndroidBitmapInfo bitmapInfo, void *pixels) {
    PlayInfo *info = static_cast<PlayInfo *>(gifFile->UserData);
    ColorMapObject *defaultColorMap = gifFile->SColorMap;
    SavedImage savedImage = gifFile->SavedImages[info->curFrame];
    GifImageDesc frameInfo = savedImage.ImageDesc;
    ColorMapObject *colorMapObject = frameInfo.ColorMap;

    GraphicsControlBlock *gcb = static_cast<GraphicsControlBlock *>(malloc(
            sizeof(GraphicsControlBlock)));
    int rlt = DGifExtensionToGCB(savedImage.ExtensionBlockCount,
                                 reinterpret_cast<const GifByteType *>(gifFile), gcb);
    LOGDF("====~ START DRAW FRAME,  savedImage.ExtensionBlockCount =  %d",
          savedImage.ExtensionBlockCount);
    if (rlt == GIF_OK) {
//        LOGDF("====~ START DRAW FRAME,  frameInfo is %d , %d, %d" , frameInfo.Width ,frameInfo.Height,frameInfo.Top );
        LOGDF("====~ START DRAW FRAME,  frameInfo is %d , %d, %d", gcb->DelayTime);
    } else {
        LOGD("====~ START DRAW FRAME,  no dcb got");
    }
    free(gcb);

//    LOGDF("====~ START DRAW FRAME,  frameInfo is %d , %d, %d" , frameInfo.Width ,frameInfo.Height,frameInfo.Top );
//    LOGDF("====~ START DRAW FRAME,  saveInmage = %d" , savedImage.RasterBits== nullptr ? 1:0 );
    int *px = (int *) pixels;

//    临时 索引
//    int *line;
//  临时色盘
    GifColorType *tempColorType = (colorMapObject == nullptr) ? defaultColorMap->Colors
                                                              : colorMapObject->Colors;
//    索引
    int pointPixel;
    GifByteType gifByteType;
//操作   解压
    GifColorType gifColorType;
    for (int y = frameInfo.Top; y < frameInfo.Top + frameInfo.Height; ++y) {
//每次遍历行    首地址 传给line
//        line = px;
//        LOGDF("====~ PX address = %d" , px);
        for (int x = frameInfo.Left; x < frameInfo.Left + frameInfo.Width; ++x) {
//            LOGDF("====~ draw x = %d , y = %d" , x, y);
//            定位像素  索引
            pointPixel = (y - frameInfo.Top) * frameInfo.Width + (x - frameInfo.Left);
//            是 1不是2  压缩
            gifByteType = savedImage.RasterBits[pointPixel];
            gifColorType = tempColorType[gifByteType];
//line 进行复制   0  255  屏幕有颜色 line
            px[x] = ARGB(gifColorType.Red, gifColorType.Green, gifColorType.Blue);
        }
//遍历条件     转到下一行
        px = (int *) ((char *) px + bitmapInfo.stride);
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_video_1audio_GifHelper_playFrame(JNIEnv *env, jobject thiz, jlong ptr,
                                              jobject bitmap) {
    // TODO: implement playFrame()
    GifFileType *gifFile = reinterpret_cast<GifFileType *>(ptr);
    PlayInfo *info = static_cast<PlayInfo *>(gifFile->UserData);
    void *pixels;
    AndroidBitmapInfo bitmapInfo;
    AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);

    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    drawFrame(gifFile, bitmapInfo, pixels);
    AndroidBitmap_unlockPixels(env, bitmap);

    info->curFrame++;
    if (info->curFrame >= info->totalFrame - 1) {
        info->curFrame = 0;
    }
    LOGDF("====~DRAW frame : curFrame index = %d", info->curFrame);

    return info->delay;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_video_1audio_GifHelper_destroy(JNIEnv *env, jobject thiz, jlong ptr) {
    GifFileType *gifFile = reinterpret_cast<GifFileType *>(ptr);
    if (gifFile != nullptr) {
        if (gifFile->UserData != nullptr) {
            free(gifFile->UserData);
            gifFile->UserData = nullptr;
        }
        free(gifFile);
        gifFile = nullptr;
    }
    ptr = NULL;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_video_1audio_OpenESLsUtils_init(JNIEnv *env, jobject thiz) {
    // TODO: implement init()

    return 1;
}