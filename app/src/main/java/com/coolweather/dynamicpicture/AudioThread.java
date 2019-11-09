package com.coolweather.dynamicpicture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * created by zhangke on 2019/11/9.
 */
public class AudioThread extends Thread {
    private int mSampleRate = 16000;
    private int mBitRate = 48000;
    private boolean isRecording;
    private MediaCodec mAudioCodec;
    private AudioRecord mAudioRecord;
    private MediaMuxerThread mMediaMuxerThread;
    private int minBufferSize;
    private String TAG = "AudioThread";
    private final int TIME_OUTS = 10000;
    private long prevOutputPTSUs = 0L;

    public AudioThread(MediaMuxerThread mediaMuxerThread) {
        this.mMediaMuxerThread = mediaMuxerThread;
    }

    @Override
    public void run() {
        Log.d(TAG, "AudioThread start run");
        byte[] bufferBytes = new byte[minBufferSize];
        int lens = 0;
        Log.d(TAG, "isRecording" + isRecording);
        while (isRecording) {
            Log.d(TAG, "lens.length1:" + lens);
            try {
                lens = mAudioRecord.read(bufferBytes, 0, minBufferSize);
            } catch (Exception e) {

            }
            Log.d(TAG, "lens.length:" + lens);
            if (lens > 0) {
                record(bufferBytes, lens, System.nanoTime() / 1000L);
            }
        }
    }

    private void record(byte[] bufferBytes, int lens, long presentationTimeUs) {
        Log.d(TAG, "start Record");
        int inputBufferIndex = mAudioCodec.dequeueInputBuffer(TIME_OUTS);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mAudioCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            if (inputBuffer != null) {
                inputBuffer.put(bufferBytes);
            }
            if (lens <= 0) {
                mAudioCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
            } else {
                mAudioCodec.queueInputBuffer(inputBufferIndex, 0, lens, presentationTimeUs, 0);
            }
        }

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUTS);
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            Log.i(TAG, "add AudioTrack to muxer");
            mMediaMuxerThread.AudioTrack(mAudioCodec.getOutputFormat());
        }

        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mAudioCodec.getOutputBuffer(outputBufferIndex);
            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                Log.e(TAG, "audio run:BUFFER_FLAG_CODEC_CONFIG");
                bufferInfo.size = 0;
            }
            if (bufferInfo.size >= 0) {
                if (mMediaMuxerThread != null) {
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    bufferInfo.presentationTimeUs = getPTSUs();
                    mMediaMuxerThread.addMutexData(new MutexBean(false, outData, bufferInfo));
                    prevOutputPTSUs = bufferInfo.presentationTimeUs;
                }
            }
            mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);
            bufferInfo = new MediaCodec.BufferInfo();
            outputBufferIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, TIME_OUTS);
        }
    }

    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        return result < prevOutputPTSUs ? prevOutputPTSUs : result;
    }

    public void prepare() {
        initCodec();
        initRecorder();
    }

    private boolean initCodec() {
        Log.d(TAG, "initCodec is invoked");
        try {
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, mSampleRate, 1);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
            //format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            //format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
            mAudioCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mAudioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAudioCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initRecorder() {
        Log.d(TAG, "initRecord is invoked");
        minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.d(TAG, "minBufferSize:" + minBufferSize);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * minBufferSize);
        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.d(TAG, "initRecord:mAudioRecord init failed");
            isRecording = false;
            return false;
        }
        Log.d(TAG, "initRecord succeed");
        isRecording = true;
        mAudioRecord.startRecording();
        return true;
    }


}
