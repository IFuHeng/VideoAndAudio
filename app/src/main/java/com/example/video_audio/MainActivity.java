package com.example.video_audio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.content.Context;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_STORAGE = 2;
    GifHelper helper;

    private ImageView mIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Example of a call to a native method
        mIcon = findViewById(R.id.icon);
        Log.d(getClass().getSimpleName(), "====~ intent data = " + getIntent().getData());
        verifyStoragePermission();
        if (getIntent().getData() == null) {
            helper = new GifHelper("/storage/emulated/0/Pictures/dog.gif", mIcon);
            findViewById(R.id.btn_play).setVisibility(View.GONE);
        }
    }


    public void onPlay(View view) {
        if (getIntent().getData() != null) {
            if (helper == null) {
                String path = Utils.turnUri2FilePath(this, getIntent().getData());
                helper = new GifHelper(path, mIcon);
            }
            helper.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (helper != null) {
            helper.start();
        }
    }

    @Override
    protected void onStop() {
        if (helper != null) {
            helper.stop();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (helper != null)
            helper.destroy();
        super.onDestroy();
    }

    private boolean verifyStoragePermission() {
        //1.????????????
        int permission = PermissionChecker.PERMISSION_GRANTED;
        for (String s : PERMISSIONS_STORAGE) {
            permission |= ActivityCompat.checkSelfPermission(this, s);
        }
        if (permission != PermissionChecker.PERMISSION_GRANTED) {
            //2.????????????????????????????????????
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_CODE_STORAGE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE) {
            int result = PermissionChecker.PERMISSION_GRANTED;
            for (int grantResult : grantResults) {
                result |= grantResult;
            }
            if (result == PermissionChecker.PERMISSION_GRANTED) {
                //??????????????????
                Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
            } else {
                //??????????????????
                Toast.makeText(this, "????????????????????????????????????", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


}