package com.example.video_audio;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

public class GifHelper implements Runnable {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private Thread mThread;

    private final int width;
    private final int height;
    private final ImageView imageView;

    String fileName;
    private final long mNativePtr;
    Bitmap mBitmap;
//    MyHandler handler;

    public GifHelper(String fileName, ImageView imageView) {
        this.fileName = fileName;
        this.imageView = imageView;
        mNativePtr = init(fileName);

        width = getWidth(mNativePtr);
        height = getHeight(mNativePtr);
        Log.d(getClass().getSimpleName(), "====~mWidth = " + getWidth(mNativePtr));
        Log.d(getClass().getSimpleName(), "====~mHeight = " + getHeight(mNativePtr));
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        imageView.setImageBitmap(mBitmap);
//        handler = new MyHandler(imageView);
    }

    public void start() {
        stop();

        mThread = new Thread(this);
        mThread.start();
    }

    public void stop() {
        if (mThread != null)
            mThread.interrupt();
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            long startTime = System.currentTimeMillis();
            int delay = playFrame(mNativePtr, mBitmap);
//        Log.d(getClass().getSimpleName(), "====~ one frame cost " + (System.currentTimeMillis() - startTime) + " ms");
            imageView.postInvalidate();
            long costToast = System.currentTimeMillis() - startTime;
            if (costToast < delay) {
                try {
                    Thread.sleep(delay - costToast);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private native long init(String gifPath);

    private native int getWidth(long ptr);

    private native int getHeight(long ptr);


    /**
     * @param ptr
     * @param bitmap
     * @return 下一帧间隔
     */
    private native int playFrame(long ptr, Bitmap bitmap);

    private native int destroy(long ptr);

    public void destroy() {
        destroy(mNativePtr);
    }

//    private static class MyHandler extends Handler {
//        private final WeakReference<ImageView> weakReference;
//
//        MyHandler(ImageView thiz) {
//            weakReference = new WeakReference<>(thiz);
//        }
//
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            if (weakReference.get() == null)
//                return;
//            super.handleMessage(msg);
//        }
//    }
}
