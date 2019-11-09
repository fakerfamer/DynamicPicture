package com.coolweather.dynamicpicture;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * created by zhangke on 2019/11/8.
 */
public class MutexBean {
    private ByteBuffer byteBuffer;
    private MediaCodec.BufferInfo bufferInfo;
    private boolean isVedio;

    public MutexBean(boolean isVedio, byte[] bytes, MediaCodec.BufferInfo bufferInfo) {
        this.isVedio = isVedio;
        this.byteBuffer = ByteBuffer.wrap(bytes);
        this.bufferInfo = bufferInfo;
    }

    public boolean isVedio() {
        return isVedio;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return bufferInfo;
    }
}
