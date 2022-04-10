package com.example.video_audio;

public class OpenESLsUtils {

    static {
        System.loadLibrary("native-lib");
    }
    public native boolean init();

}
