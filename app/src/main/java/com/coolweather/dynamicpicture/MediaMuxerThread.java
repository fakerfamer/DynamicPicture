package com.coolweather.dynamicpicture;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * created by zhangke on 2019/11/8.
 */
public class MediaMuxerThread extends Thread {
    private Queue<MutexBean> mMutexBeanQueue;
    private boolean isRecording;
    private String path;
    private int mVideoTrack;
    private MediaMuxer mMediaMuxer;
    private boolean isMediaStart;
    private String TAG = "MediaMuxerThread";
    private int mAudioTrack;

    public MediaMuxerThread(String path) {
        this.isRecording = true;
        this.path = path;
        mMutexBeanQueue = new ArrayBlockingQueue<>(100);
    }

    public void setMediaRecord(boolean isRecording){
        this.isRecording = isRecording;
    }

    public void prepareMediaMuxer(int width, int height) {
        try {
            mVideoTrack = -1;
            mMediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {

        }
    }

    private void startMediaMutex() {
        mMediaMuxer.start();
        isMediaStart = true;
        start();
    }

    public void run(){
        while(true){
            if(!mMutexBeanQueue.isEmpty()){
                MutexBean data = mMutexBeanQueue.poll();
                if(data.isVedio()){
                    mMediaMuxer.writeSampleData(mVideoTrack, data.getByteBuffer(), data.getBufferInfo());
                }else{
                    mMediaMuxer.writeSampleData(mAudioTrack, data.getByteBuffer(), data.getBufferInfo());
                }
            }else{
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!isRecording){
                    break;
                }
            }
        }
        Log.d(TAG, "release is invoked--");
        release();
    }

    public void  addVideoTrack(MediaFormat mediaFormat){
        if(mMediaMuxer == null){
            return;
        }
        mVideoTrack = mMediaMuxer.addTrack(mediaFormat);
        startMediaMutex();
    }
    public void release() {
        if(mMediaMuxer != null && isMediaStart){
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    public void addMutexData(MutexBean mutexBean) {
        mMutexBeanQueue.offer(mutexBean);
    }

    public void AudioTrack(MediaFormat format) {
        if(format == null){
            Log.e(TAG, "AudioFormat is null, return");
            return;
        }
        mAudioTrack = mMediaMuxer.addTrack(format);
        startMediaMutex();
    }
}
