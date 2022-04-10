package com.example.video_audio.capture;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.FragmentActivity;

import com.example.video_audio.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CaptureScreenActivity extends FragmentActivity implements View.OnClickListener {


    private static final String[] PERMISSIONS_HERE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_CREATE_CAPTURE = 9527;
    private static final int REQUEST_CODE_PERMISSION = REQUEST_CODE_CREATE_CAPTURE + 1;

    private MediaProjectionManager mediaProjectionManager;

    private TextView mTvTime;
    private TextView mBtnClick;
    private CaptureScreenTask mTask;

    private long mTimeStart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_screeen);
        mTvTime = findViewById(R.id.tv_time);
        mBtnClick = findViewById(R.id.btn_play);
        mBtnClick.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        if (mTask != null && !mTask.isCancelled()) {
            mTask.cancel(false);
            return;
        }
        super.onPause();
    }

    private void startCapture() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (mediaProjectionManager == null)
                mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            if (mTask != null && !mTask.isCancelled()) {
                mTask.cancel(false);
                return;
            }

            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_CREATE_CAPTURE);
        } else {
            Toast.makeText(this, "版本过低，不支持录屏。", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_CREATE_CAPTURE && resultCode == RESULT_OK) {
            mTask = new CaptureScreenTask();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            mTask.execute(new TaskParams(width, height, resultCode, data));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            int result = PermissionChecker.PERMISSION_GRANTED;
            for (int grantResult : grantResults) {
                result |= grantResult;
            }
            if (result == PermissionChecker.PERMISSION_GRANTED) {
                //权限申请成功
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
                startCapture();
            } else {
                //权限申请失败
                Toast.makeText(this, "未获取到所有授权，退出。", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * @return true when have permissions
     */
    private boolean verifyPermission() {
        //1.检测权限
        int permission = PermissionChecker.PERMISSION_GRANTED;
        for (String s : PERMISSIONS_HERE) {
            permission |= ActivityCompat.checkSelfPermission(this, s);
        }
        if (permission != PermissionChecker.PERMISSION_GRANTED) {
            //2.没有权限，弹出对话框申请
            ActivityCompat.requestPermissions(this, PERMISSIONS_HERE, REQUEST_CODE_PERMISSION);
            return false;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:

                if (mTask != null && !mTask.isCancelled()) {
                    mTask.cancel(false);
                } else if (verifyPermission()) {
                    startCapture();
                }
                break;
        }
    }

    private class CaptureScreenTask extends AsyncTask<TaskParams, Void, Exception> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mTimeStart = System.currentTimeMillis();
            mTvTime.setVisibility(View.VISIBLE);
            mBtnClick.setText("停止");
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected Exception doInBackground(TaskParams... params) {

            if (params == null || params.length == 0 || !params[0].checkData())
                return new IllegalArgumentException("No correct size of capture size.");

            MediaCodec mediaCodec;
            Surface surface;
            try {
                mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, params[0].width, params[0].height);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
                format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                surface = mediaCodec.createInputSurface();
                mediaCodec.start();
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
                return e;
            }

            MediaProjection mediaProject = mediaProjectionManager.getMediaProjection(params[0].resultCode, params[0].intent);
            VirtualDisplay virtualDisplay = mediaProject.createVirtualDisplay("capture_screen", params[0].width, params[0].height, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            FileOutputStream fileOutputStream = null;
            byte[] buf = null;
            try {
                fileOutputStream = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/1.h264");
                do {
                    int index = mediaCodec.dequeueOutputBuffer(info, 50000);
                    if (index < 0 || index >= mediaCodec.getOutputBuffers().length) {
                        continue;
                    }
                    if (buf == null || buf.length < info.size) {

                        if (buf != null) {
                            buf = null;
                            System.gc();
                        }
                        buf = new byte[info.size];
                    }
                    ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                    buffer.get(buf, info.offset, info.size);
                    fileOutputStream.write(buf, info.offset, info.size);
                    mediaCodec.releaseOutputBuffer(index, false);

                    publishProgress();
                } while (!isCancelled());
                Log.d(getClass().getSimpleName(), "====~ iscanceled");
//                mediaCodec.stop();
//                mediaCodec.release();
//                virtualDisplay.release();
//                mediaProject.stop();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return e;
            } catch (IOException e) {
                e.printStackTrace();
                return e;
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return e;
                    }
                }
            }

            return new Exception();
        }

        @Override
        protected void onPostExecute(Exception e) {
            Log.d(getClass().getSimpleName(), "====~ onPostExecute");
            mTvTime.setVisibility(View.GONE);
            mBtnClick.setText("启动");
            if (e != null)
                Toast.makeText(CaptureScreenActivity.this, "异常：" + e, Toast.LENGTH_LONG).show();
            super.onPostExecute(e);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            int ms = (int) ((System.currentTimeMillis() - mTimeStart) % 0x7fffffff);
            int seconds = ms / 1000;
            int minutes = seconds / 60;
            seconds %= 60;
            ms %= 1000;

            mTvTime.setText(String.format("%02d:%02d %03d", minutes, seconds, ms));
        }
    }

    private class TaskParams {
        int width, height;
        int resultCode;
        Intent intent;

        public TaskParams(int width, int height, int resultCode, Intent intent) {
            this.width = width;
            this.height = height;
            this.resultCode = resultCode;
            this.intent = intent;
        }

        /**
         * @return 检查数据正确
         */
        boolean checkData() {
            return width > 0 && height > 0 && intent != null && intent.getExtras() != null;
        }
    }

}
