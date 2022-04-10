package com.example.video_audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class H264Helper implements Runnable {

    private static final long TIMEOUT_US = 10000;
    private ReentrantReadWriteLock lock;
    private final String path;
    private MediaCodec mediaCodec;

    public H264Helper(@NonNull final String path) {
        this.path = path;
    }

    @Override
    public void run() {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(this.path);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        int indexOfTrackVideo = -1;
        int indexOfTrackAudio = -1;
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (indexOfTrackVideo == -1) {
                    indexOfTrackAudio = i;
                } else continue;
            } else if (mime.startsWith("audio/")) {
                if (indexOfTrackAudio == -1) {
                    indexOfTrackAudio = i;
                } else continue;
            }
        }

        if (indexOfTrackAudio != -1) {
            parseAudio(mediaExtractor, indexOfTrackAudio);
        }

        if (indexOfTrackVideo != -1) {
            parseVideo(mediaExtractor, indexOfTrackVideo);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void parseVideo(MediaExtractor mediaExtractor, int indexOfTrackVideo) {

        mediaExtractor.selectTrack(indexOfTrackVideo);
        MediaFormat format = mediaExtractor.getTrackFormat(indexOfTrackVideo);
        int index = -1;
        int sampleDataSize = -1;

        int width = format.getInteger(MediaFormat.KEY_WIDTH);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);

        ByteBuffer inBuffer;
        ByteBuffer outBuffer;

        ByteBuffer byteBuffer = ByteBuffer.allocate(width * height * 3 / 2);
        byte[] bytes = new byte[64 * 1024];
        try {
            MediaCodec mediaCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            mediaCodec.configure(format, null, null, 0);
            mediaCodec.start();

            do {
                sampleDataSize = mediaExtractor.readSampleData(byteBuffer, 0);
                byteBuffer.get(bytes,0,sampleDataSize);
                index = mediaCodec.dequeueInputBuffer(TIMEOUT_US);
                inBuffer = mediaCodec.getInputBuffer(index);
                inBuffer.reset();
                inBuffer.position(0);
                inBuffer.limit(sampleDataSize);
                inBuffer.put(byteBuffer);
                mediaCodec.queueSecureInputBuffer(index, 0, null, TIMEOUT_US, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);


            } while (true);


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void parseAudio(MediaExtractor mediaExtractor, int indexOfTrackAudio) {

    }
}
