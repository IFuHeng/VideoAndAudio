package com.example.video_audio;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import java.io.File;

public class Utils {
    public static String turnUri2FilePath(Activity activity, Uri uri) {
        String filePath = null;
        String[] proj = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
        };
        CursorLoader loader = new CursorLoader(activity, uri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        if (cursor != null) {
            cursor.moveToFirst();
            filePath = cursor.getString(cursor.getColumnIndex(proj[0]));
            if (filePath.indexOf('=') != -1)
                filePath = filePath.substring(filePath.indexOf('=') + 1).trim();

            String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
            Log.d("turnUri2FilePath ", "====~ name  = " + name + " , SIZE = " + size + " , filePath = " + filePath);
            cursor.close();
        }
        return filePath;
    }
}
