/**
 * Create By Shawn.xiao at 2023/05/01
 */
package com.android.mycamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "test0523-MainActivity";

    private GLSurfaceView mGLSurfaceView;

    private Context mContext;

    private GLRender mGLRender;

    private List<Size> mOutputSizes;
    private Size mPictureSize;
    private Size mPreViewSize;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCharacteristics mCharacteristics;

    private ImageReader mImageReader;

    private Surface surface;
    //private SurfaceTexture mSurfaceTexture;

    private int mImageFormat = ImageFormat.JPEG;

    private String mCameraId;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    public final int mMaxImages = 5;
    public final int REQUEST_CAMERA_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWindowFlag();

        setContentView(R.layout.activity_main);

        mContext = this;
        initView();
    }

    private void setWindowFlag() {
        Window window = getWindow();
        //隐藏顶部 StatuBar状态栏
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //隐藏底部 NavigationBar导航栏
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    private void initView() {
        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        //设置GLES版本
        mGLSurfaceView.setEGLContextClientVersion(3);
        //创建Render对象，并将其设置到GLSurfaceView
        mGLRender = new GLRender(mContext);
        mGLSurfaceView.setRenderer(mGLRender);
        /**
         * 刷新方式
         * RENDERMODE_WHEN_DIRTY，手动刷新，调用requestRender()
         * RENDERMODE_CONTINUOUSLY，自动刷新，GPU一个fence，目前较为普遍的16ms自动回调一次onDrawFrame()
         *
         * 如果不设置，默认是自动刷新
         * */
        /*mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);*/
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //应该放在SurfaceTexture销毁的地方，暂时先放在这里
        closeCamera();
        stopBackgroundThread();
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            //do nothing frist shawn
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    /**
     * Opens the camera
     */
    public void openCamera() {
        //检查权限
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        startBackgroundThread();
        //配置相机参数
        setUpCameraOutputs();

        try {
            Log.v(TAG, "openCamera mCameraId=" + mCameraId);
            //打开相机，设置状态回调
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up member variables related to camera.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs() {
        try {
            Log.v(TAG, "setUpCameraOutputs E");
            mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            mCameraId = CameraUtils.getInstance().getCameraId();
            mCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);

            mOutputSizes = CameraUtils.getInstance().getCameraOutputSizes(mCharacteristics, mCameraId, SurfaceTexture.class);
            mPictureSize = mOutputSizes.get(0);

            //PreviewSize应该是通过PictureSize和Screensize从mOutputSizes中找到比率匹配的Size数组，然后选最大或最合适的那个Size
            //这里先写成固定值实现功能
            mPreViewSize = new Size(1440,1080);
            mGLRender.initData(mPreViewSize);

            mImageReader = ImageReader.newInstance(mPictureSize.getWidth(), mPictureSize.getHeight(), mImageFormat, mMaxImages);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
            Log.v(TAG, "setUpCameraOutputs X");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //打开相机的状态回调
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.v(TAG, "mStateCallback onOpened()");

            // This method is called when the camera is opened，we start camera preview here.
            try {
                //从GLRender中获取到SurfaceTexture，也就是GLRender里创建的用于预览显示的纹理对象
                SurfaceTexture surfaceTexture = mGLRender.getSurfaceTexture();
                if (surfaceTexture == null) {
                    return;
                }
                surfaceTexture.setDefaultBufferSize(1440, 1080);
                //设置帧回调监听
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    //每一帧回调都会走下面重写的函数
                    @Override
                    public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                        //请求绘制每一帧数据
                        mGLSurfaceView.requestRender();
                    }
                });
                //使用使用获取到的SurfaceTexture创建一个Surface
                surface = new Surface(surfaceTexture);

                mCameraDevice = cameraDevice;
                mPreviewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(surface);
                mPreviewRequest = mPreviewRequestBuilder.build();

                //传入Surface数组和创建CaptureSession的状态回调
                cameraDevice.createCaptureSession(Arrays.asList(surface), sessionsStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "Open Camera Failed!");
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.v(TAG, "mStateCallback onDisconnected()");
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.v(TAG, "mStateCallback onError()");
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }
    };

    CameraCaptureSession.StateCallback sessionsStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (null == mCameraDevice) return;

            mCaptureSession = session;
            try {
                // Auto focus should be continuous for camera preview.
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                mPreviewRequest = mPreviewRequestBuilder.build();
                Log.v(TAG, "sessionsStateCallback setRepeatingRequest()");
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.d(TAG, "相机访问异常");
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.d(TAG, "onConfigureFailed!");
        }
    };

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            Log.v(TAG, "onCaptureProgressed onCaptureProgressed()");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            Log.v(TAG, "onCaptureProgressed onCaptureCompleted()");
        }
    };


    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.v(TAG, "onImageAvailable()");
        }
    };

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }
}