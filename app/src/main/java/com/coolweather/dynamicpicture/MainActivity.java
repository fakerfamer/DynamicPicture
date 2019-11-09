package com.coolweather.dynamicpicture;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SurfaceView mSurfaceView;
    private Button mCaptureButton;
    private Button mChangeButton;
    private ImageView mThumbView;
    private SurfaceHolder mSurfaceHolder;
    private CameraManager mCameraManager;
    private int cameraId;
    private int BACK_CAM = 0;
    private int FRONT_CAM = 1;
    private CameraDevice mCameraDevice;
    private Handler mMainHandler;
    private Handler mChildHander;
    private Handler mThirdHandler;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    List<Surface> mSurfaceList;
    private CameraCaptureSession mCameraCaptureSession;
    private MediaRecorder mMediaRecorder;
    private String mMediaPath;
    private boolean FLAG_isRecording = false;
    private int width = 1440;
    private int height = 1080;
    private int format = ImageFormat.YUV_420_888;
    private int maxImages = 10;
    private ImageReader mImageReader;
    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    private MediaFormat mMediaFormat;
    private MediaCodec.BufferInfo mBufferInfo;
    private SimpleDateFormat mSimpleDateFormat;
    private FileOutputStream fos;
    private Handler mForthHandler;
    private LinkedList<byte[]> mbyteLinkedList = new LinkedList<>();
    private boolean isRecording = false;
    private byte[] previewData;
    private MediaMuxerThread mMediaMuxerThread;
    private AudioThread mAudioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        intiView();
    }

    private void intiView() {
        Log.d(TAG, "initView");
        mCaptureButton = findViewById(R.id.button_start);
        mChangeButton = findViewById(R.id.change_button);
        mSurfaceView = findViewById(R.id.preview_surface);
        mThumbView = findViewById(R.id.thumb_imageView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaMuxerThread != null) {
                    mMediaMuxerThread.setMediaRecord(false);
                }
            }
        });
        mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
                    } else {
                        openCamera();
                    }
                } else {
                    openCamera();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    private void capture() {
        /*try {
            Log.d(TAG, "start onclick start caoture");
            final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCameraCaptureSession.capture((captureRequestBuilder.build()), null, mChildHander);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }*/
    }

    private void openCamera() {
        mCameraManager = (CameraManager) getSystemService(MainActivity.CAMERA_SERVICE);
        cameraId = BACK_CAM;
        HandlerThread childThread = new HandlerThread("childCamera");
        HandlerThread thirdThread = new HandlerThread("threadCamera");
        thirdThread.start();
        mThirdHandler = new Handler(thirdThread.getLooper());
        childThread.start();
        mChildHander = new Handler(childThread.getLooper());
        mMainHandler = new Handler(getMainLooper());

        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
            mCameraManager.openCamera("" + cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    mCameraDevice = camera;
                    takePreview();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {

                }

                @Override
                public void onError(CameraDevice camera, int error) {

                }
            }, mChildHander);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void takePreview() {
        mImageReader = ImageReader.newInstance(width, height, format, maxImages);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() +
                "/Camera/" + "IMG_" + mSimpleDateFormat.format(new Date()) + ".mp4");
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        HandlerThread childThread1 = new HandlerThread("child");
        childThread1.start();
        mForthHandler = new Handler(childThread1.getLooper());
        boolean isVideoInitialized = initializeVideoEncoder();
        mMediaMuxerThread = new MediaMuxerThread(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() +
                "/Camera/" + "IMG_" + mSimpleDateFormat.format(new Date()) + ".mp4");
        mMediaMuxerThread.prepareMediaMuxer(width, height);
        boolean isAudioInitialized = initializeAudioEncoder();
        Log.d(TAG, "isVideoInitialized" + isVideoInitialized + "isAudioInitialized:" + isAudioInitialized);

        mImageReader.setOnImageAvailableListener(mImageReaderListener, mMainHandler);
        Thread encodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isRecording) {
                    try {
                        //开始视频音频录制
                        encodeVideo(mbyteLinkedList.poll());
                        mAudioThread.start();
                    } catch (Exception e) {
                        Log.d(TAG, "start video and audioEndoder meet some error");
                    }
                }
            }
        }, "encodeThread");
        encodeThread.start();
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
                    mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
                    //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mMainHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, mThirdHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private ImageReader.OnImageAvailableListener mImageReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "mImageReaderListener:" + "Height:" + reader.getHeight() + "WIDTH" + reader.getWidth());
            //reader.acquireNextImage().getPlanes()[1].getBuffer().remaining();
            Image image = reader.acquireNextImage();
            ByteBuffer YBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer UVBuffer = image.getPlanes()[1].getBuffer();
            int YLength = YBuffer.remaining();
            int UVLenth = UVBuffer.remaining();
            YBuffer.position(0);
            UVBuffer.position(0);
            YBuffer.mark();
            UVBuffer.mark();
            int YStride = image.getPlanes()[0].getRowStride();
            int UVStride = image.getPlanes()[1].getRowStride();
            int YPosition = YBuffer.position();
            int UVPosition = UVBuffer.position();
            Log.d(TAG, "onImageAvailable" + ":YLength:" + YLength + "UVLength:" + UVLenth +
                    "YStride:" + YStride + "YPosition:" + YPosition + "YPixelStride:" + image.getPlanes()[0].getPixelStride() + "UVStride:" + UVStride + "UVPixelStride:" + image.getPlanes()[1].getPixelStride());
            previewData = new byte[width * height * 3 / 2];
            if (width != YStride) {
                for (int i = 0; i < height; i++) {
                    YBuffer.position(YPosition + i * YStride);
                    if (YBuffer.remaining() < width) {
                        YBuffer.get(previewData, i * width, YBuffer.remaining());
                    } else {
                        YBuffer.get(previewData, i * width, width);
                    }
                }
            } else {
                YBuffer.get(previewData, 0, YLength);
            }

            if (width != UVStride) {
                for (int i = 0; i < height / 2; i++) {
                    UVBuffer.position(UVPosition + i * UVStride);
                    if (UVBuffer.remaining() < width) {
                        UVBuffer.get(previewData, width * height + i * width, UVBuffer.remaining());
                    } else {
                        UVBuffer.get(previewData, width * height + i * width, width);
                    }
                }
            } else {
                UVBuffer.get(previewData, width * height, UVLenth);
            }
           /* for (int i = height; i < height * 3 / 2; i++) {
                previewData[i * width - 1] = previewData[i * width - 3];
                previewData[i * width] = previewData[i * width - 2];
            }*/
            YBuffer.reset();
            UVBuffer.reset();
//开始将数据送给编码器
            image.close();
            Log.d(TAG, "previewData:" + previewData.length);
            mbyteLinkedList.offer(previewData);
            //encodeVideo(previewData);


            previewData = null;
            Log.d(TAG, "image.close:");
        }
    };

    private boolean initializeAudioEncoder() {
        mAudioThread = new AudioThread(mMediaMuxerThread);
        try {
            mAudioThread.prepare();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean initializeVideoEncoder() {
        try {
            mMediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, height * width * 3 * 8 * 15 / 256);
            mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mVideoEncoder = MediaCodec.createEncoderByType("video/avc");
            mVideoEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoEncoder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void encodeVideo(byte[] encodeData) {
        if (encodeData != null) {
            try {
                int inputBufferIndex = mVideoEncoder.dequeueInputBuffer(10000);
                Log.d(TAG, "inputBufferIndex:" + inputBufferIndex + "encodeData:" + encodeData.length);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = mVideoEncoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(encodeData);
                    mVideoEncoder.queueInputBuffer(inputBufferIndex, 0, encodeData.length, System.nanoTime() / 1000L, 0);
                }
                mBufferInfo = new MediaCodec.BufferInfo();
                Log.d("MainActivity", "mBufferInfo == null" + (mBufferInfo == null));
                int outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, 10000);

                Log.d(TAG, "outputBufferIndex: " + outputBufferIndex);

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d(TAG, "doing addVideoTrack---");
                    mMediaMuxerThread.addVideoTrack(mVideoEncoder.getOutputFormat());
                }

                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mVideoEncoder.getOutputBuffer(outputBufferIndex);
                    if (mBufferInfo.flags == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mBufferInfo.size = 0;
                    }
                    Log.d(TAG, "mBufferInfo.size:" + mBufferInfo.size);
                    if (mBufferInfo.size > 0) {
                        byte[] outBuffer = new byte[mBufferInfo.size];
                        outputBuffer.get(outBuffer);
                        /*try {
                            fos.write(outBuffer, 0, outBuffer.length);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                        outputBuffer.position(mBufferInfo.offset);
                        outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                        mBufferInfo.presentationTimeUs = System.nanoTime() / 1000L;
                        mMediaMuxerThread.addMutexData(new MutexBean(true, outBuffer, mBufferInfo));
                    }
                    mVideoEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    mBufferInfo = new MediaCodec.BufferInfo();
                    outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mBufferInfo, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaMuxerThread != null) {
            mMediaMuxerThread.release();
        }
    }
}
