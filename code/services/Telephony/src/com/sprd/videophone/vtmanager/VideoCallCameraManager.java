package com.sprd.videophone.vtmanager;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.widget.Toast;
import com.android.phone.R;
import com.android.phone.PhoneGlobals;
import com.android.services.telephony.VTManagerProxy;

public class VideoCallCameraManager {
    private static String TAG = VideoCallCameraManager.class.getSimpleName();

    private static final int BRIGHTNESS_CONTRAST_ERROR = -1;
    private static final int START_CAMERA_TIMES = 5;
    private static final int EVENT_DELAYED_CREATE_CAMERA = 999;
    private static final int EVENT_CHANGE_ORIENTATION = 1000;

    public static final String CAMERA_PARAMETERS_BRIGHTNESS = "brightness";
    public static final String CAMERA_PARAMETERS_CONTRAST = "contrast";

    public enum WorkerTaskType {
        NONE, CAMERA_SWITCH, CAMERA_CLOSE, CAMERA_OPEN, VIEW_SWTICH
    };

    /**
     * The camera ID for the front facing camera.
     */
    private String mFrontFacingCameraId;

    /**
     * The camera ID for the rear facing camera.
     */
    private String mRearFacingCameraId;

    private Parameters mParameters;
    private Camera mCamera;
    private Object mCameraLock = new Object();
    private VTManager mVTMgr;
    private VTManagerProxy mVTManagerProxy;
    private Thread mOperateCameraThread;
    private MyOrientationEventListener mOrientationListener;

    private String mCameraId;
    private int mCameraTimes = START_CAMERA_TIMES;
    private int mCameraNumbers = 0;
    private boolean mThreadRunning;
    private int mWidth = 176;
    private int mHeight = 144;
    private int mDeviceRotation = 0;
    private boolean mIsFirstInit = true;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "The message: " + msg.what);
            switch (msg.what) {
                case EVENT_DELAYED_CREATE_CAMERA:
                    initCameraAndStartPreview();
                    break;
                case EVENT_CHANGE_ORIENTATION:
                    handleOrientationChange();
                    break;
                default:
                    Log.w(TAG, "unsupport message: " + msg.what);
                    break;
            }
        }
    };

    public VideoCallCameraManager(VTManager vTMgr, Context context, VTManagerProxy vtManagerProxy) {
        mVTMgr = vTMgr;
        initializeCameraList(context);
        mVTManagerProxy = vtManagerProxy;
        mOrientationListener = new MyOrientationEventListener(context);
        mOrientationListener.enable();
    }

    /**
     * Get the Camera object which video call using.
     */
    public Camera getCamera() {
        return mCamera;
    }

    /**
     * Get the Camera-related parameters from Camera object.
     */
    public int getCameraParameters(boolean isBrightness) {
        if (null != mCamera) {
            String key = isBrightness ? CAMERA_PARAMETERS_BRIGHTNESS : CAMERA_PARAMETERS_CONTRAST;
            mParameters = mCamera.getParameters();
            int parameters = Integer.parseInt(mParameters.get(key));
            Log.i(TAG, "getCameraParameters(), key: " + key + "  value:" + parameters);
            return parameters;
        }
        Log.e(TAG, "getCameraParameters()... ERROR");
        return BRIGHTNESS_CONTRAST_ERROR;
    }

    /**
     * Gets the used camera ID.
     */
    private String getCamerID() {
        Log.d(TAG, "getCamerID(): " + mCameraId);
        return mCameraId;
    }

    public void setCameraID(String id) {
        mCameraId = id;
    }

    public boolean isSameCamera(String id) {
        if (mCameraId != null && mCameraId.equals(id)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * For example, suppose the natural orientation of the device is portrait. The device is rotated
     * 270 degrees clockwise, so the device orientation is 270. Suppose a back-facing camera sensor
     * is mounted in landscape and the top side of the camera sensor is aligned with the right edge
     * of the display in natural orientation. So the camera orientation is 90. The rotation should
     * be set to 0 (270 + 90).
     */
    private int getSensorRotation(String cameraId) {
        int cameraRotation;
        if (cameraId != null && cameraId.equals(mRearFacingCameraId)) {
            cameraRotation = mDeviceRotation + 90;
        } else {
            if (mDeviceRotation >= 270) {
                cameraRotation = 270 + mDeviceRotation;
            } else {
                cameraRotation = 270 - mDeviceRotation;
            }
        }
        Log.i(TAG, "getSensorRotation()->cameraId:" + cameraId + " cameraRotation:"
                + cameraRotation
                + " mDeviceRotation:" + mDeviceRotation);
        return cameraRotation;
    }

    /**
     * Set the Camera-related parameters.
     */
    public void setCameraParameters(boolean isBrightness, int value) {
        if (null != mCamera) {
            String key = isBrightness ? CAMERA_PARAMETERS_BRIGHTNESS : CAMERA_PARAMETERS_CONTRAST;
            mParameters = mCamera.getParameters();
            mParameters.set(key, value);
            mCamera.setParameters(mParameters);
            Log.i(TAG, "setCameraParameters(), key: " + key + "  value:" + value);
        }
        Log.e(TAG, "setCameraParameters()... ERROR");
    }

    /**
     * indicate async task is running, should disable camera-relative operations.
     */
    private boolean isThreadRunning() {
        return mThreadRunning;
    }

    /**
     * Creates a new Camera object to access a particular hardware camera, And starts capturing and
     * drawing preview frames to the screen.
     */
    public void initCameraAndStartPreview() {
        if (mOperateCameraThread != null) {
            try {
                mOperateCameraThread.join();
            } catch (InterruptedException ex) {
                Log.d(TAG, "mOperateCameraThread.quit() exception " + ex);
            }
        }
        mOperateCameraThread = new Thread(new Runnable() {
            public void run() {
                if (null != mVTMgr && mVTMgr.mVolteEnable) {
                    mThreadRunning = true;
                }
                Log.i(TAG, "mOperateCameraThread start. ");
                openCamera();
                if (null != mVTMgr) {
                    if (!mVTMgr.mVolteEnable) {
                        mVTMgr.setCamera(mCamera);
                    }
                    Log.d(TAG, "mVTMgr is not null.");
                    if (mVTMgr.mVolteEnable) {
                        mThreadRunning = false;
                    }
                    if (mVTMgr.mLocalSurface != null) {
                        startCameraPreView();
                    }
                }
                Log.d(TAG, "mOperateCameraThread end. ");
            }
        });
        mOperateCameraThread.start();
    }

    /**
     * Creates a new Camera object to access a particular hardware camera. If the same camera is
     * opened by other applications, this will throw a RuntimeException.
     */
    private void openCamera() {
        try {
            synchronized (mCameraLock) {
                if (mCamera == null) {
                    // If the activity is paused and resumed, camera device has been
                    // released and we need to open the camera.
                    String cameraId = getCamerID();
                    mCamera = Camera.open(Integer.valueOf(cameraId));
                    Camera.Parameters params = mCamera.getParameters();
                    params.set("sensor-rot", getSensorRotation(cameraId));
                    params.set("sensor-orient", 1);
                    params.set("ycbcr", 1);// ensure yuv sequence of camera preview
                    // data
                    if (mVTMgr != null && mVTMgr.mVolteEnable) {
                        switch (mVTMgr.getCameraResolution()) {
                            case VTManager.RESOLUTION_720P:
                                params.setPreviewSize(1280, 720);
                                break;
                            case VTManager.RESOLUTION_VGA:
                                params.setPreviewSize(640, 480);
                                break;
                            case VTManager.RESOLUTION_QVGA:
                                params.setPreviewSize(320, 240);
                                break;
                            case VTManager.RESOLUTION_CIF:
                                params.setPreviewSize(352, 288);
                                break;
                            case VTManager.RESOLUTION_QCIF:
                                params.setPreviewSize(176, 144);
                                break;
                            default:
                                break;
                        }
                        params.setPreviewFrameRate(30);
                    } else {
                        params.setPreviewSize(mWidth, mHeight);
                        params.setPreviewFrameRate(10);
                    }
                    mCamera.setParameters(params);
                    if (mVTMgr != null) {
                        if (mVTMgr.mVolteEnable) {
                            mCamera.unlock();
                        } else {
                            mVTMgr.setCamera(mCamera);
                        }
                        if (mVTMgr.mLocalSurface == null && !mVTMgr.mVolteEnable) {
                            setPreviewSize(mWidth, mHeight);
                        }
                    }
                    mIsFirstInit = false;
                    Log.i(TAG, "openCamera(), mCamera: " + mCamera);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Open Camera Fail: " + e);
            closeCamera();
            if (mIsFirstInit) {
                if (mCameraTimes >= 0) {
                    mHandler.removeMessages(EVENT_DELAYED_CREATE_CAMERA);
                    mHandler.sendEmptyMessageDelayed(EVENT_DELAYED_CREATE_CAMERA, 700);
                    mCameraTimes--;
                    Log.d(TAG, "mCameraTimes: " + mCameraTimes);
                } else {
                    Log.d(TAG, "Camera start progrom exit.");
                    mHandler.removeMessages(EVENT_DELAYED_CREATE_CAMERA);
                }
            }
        }
    }

    /**
     * Disconnects and releases the Camera object resources.
     */
    private void closeCamera() {
        Log.i(TAG, "closeCamera");
        mHandler.removeMessages(EVENT_DELAYED_CREATE_CAMERA);
        if (mCamera == null) {
            Log.e(TAG, "already stopped.");
            return;
        }

        synchronized (mCameraLock) {
            Log.i(TAG, "close camera and get lock.");
            try {
                if (null != mVTMgr) {
                    if (mVTMgr.mVolteEnable) {
                        mCamera.reconnect();
                    }
                    mVTMgr.enablePreviewCallback(false);
                    mVTMgr.setCamera(null);
                }
                Camera.Parameters params = mCamera.getParameters();
                params.set("ycbcr", 0);
                mCamera.setParameters(params);
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /* SPRD: add the judge camera for bug 408181 @{ */
    public void handleSetCameraPreSurface(Surface surface) {
        if (surface != null) {
            if (mCamera == null) {
                openVideoCamera();
            }
            startCameraPreView();
        } else {
            stopCameraPreView();
        }
    }

    /* @} */
    /**
     * Starts capturing and drawing preview frames to the screen.
     */
    public void startCameraPreView() {
        if (mVTMgr == null || mVTMgr.mLocalSurface == null || mCamera == null) {
            Log.w(TAG, "startCameraPreView->mVTMgr:" + mVTMgr + " mLocalSurface:"
                    + mVTMgr.mLocalSurface
                    + " mCamera:" + mCamera);
            return;
        }
        if (null != mVTMgr && mVTMgr.mVolteEnable && mThreadRunning) {
            Log.w(TAG, "startCameraPreView->mThreadRunning:" + mThreadRunning);
            return;
        }
        try {
            synchronized (mCameraLock) {
                Log.i(TAG, "get camera lock in surface create");
                if (mCamera != null) {
                    if (mVTMgr != null && mVTMgr.mVolteEnable) {
                        mCamera.reconnect();
                    }
                    //SPRD:add updateCameraPara() for bug495583
                    updateCameraPara();
                    if (mCamera.previewEnabled()) {
                        Log.i(TAG, "surfaceCreated setPreviewSurface. ");
                        mCamera.stopPreview();
                        mCamera.setPreviewSurface(mVTMgr.mLocalSurface);
                        mCamera.startPreview();
                        if (mVTMgr != null)
                            mVTMgr.enablePreviewCallback(true);
                    } else {
                        Log.i(TAG, "surfaceCreated startPreview. ");
                        Log.i(TAG, "mVTMgr.mLocalSurface is null " + (mVTMgr.mLocalSurface == null)
                                + mVTMgr.mLocalSurface);
                        mCamera.setPreviewSurface(mVTMgr.mLocalSurface);
                        mCamera.startPreview();
                        if (mVTMgr != null)
                            mVTMgr.enablePreviewCallback(true);
                    }
                    if (mVTMgr != null && mVTMgr.mVolteEnable) {
                        mCamera.unlock();
                        mVTMgr.setCamera(mCamera);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "startCameraPreView Fail: " + e);
            closeCamera();
        }
    }

    public void startCameraPreViewBackground() {
        if (mVTMgr == null || mVTMgr.mLocalSurface == null || mCamera == null) {
            if (mVTMgr != null) {
                Log.w(TAG, "startCameraPreViewBackground->mVTMgr:" + mVTMgr + " mLocalSurface:"
                        + mVTMgr.mLocalSurface
                        + " mCamera:" + mCamera);
            }
            else {
                Log.w(TAG, "startCameraPreViewBackground->mVTMgr:" + mVTMgr + " mCamera:" + mCamera);
            }
            return;
        }
        try {
            synchronized (mCameraLock) {
                Log.i(TAG, "get camera lock in surface create");
                if (mCamera != null) {
                    if (mVTMgr != null && mVTMgr.mVolteEnable) {
                        mCamera.reconnect();
                    }
                    //SPRD:add updateCameraPara() for bug495583
                    updateCameraPara();
                    if (mCamera.previewEnabled()) {
                        Log.i(TAG, "surfaceCreated setPreviewSurface. ");
                        mCamera.stopPreview();
                        mCamera.setPreviewSurface(mVTMgr.mLocalSurface);
                        mCamera.startPreview();
                        if (mVTMgr != null)
                            mVTMgr.enablePreviewCallback(true);
                    } else {
                        Log.i(TAG, "surfaceCreated startPreview. ");
                        Log.i(TAG, "mVTMgr.mLocalSurface is null " + (mVTMgr.mLocalSurface == null)
                                + mVTMgr.mLocalSurface);
                        mCamera.setPreviewSurface(mVTMgr.mLocalSurface);
                        mCamera.startPreview();
                        if (mVTMgr != null)
                            mVTMgr.enablePreviewCallback(true);
                    }
                    if (mVTMgr != null && mVTMgr.mVolteEnable) {
                        mCamera.unlock();
                        mVTMgr.setCamera(mCamera);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "startCameraPreView Fail: " + e);
            closeCamera();
        }
    }

    /**
     * Stops capturing and drawing preview frames to the surface, and resets the camera for a future
     * call to {@link #startPreview()}.
     */
    public void stopCameraPreView() {
        synchronized (mCameraLock) {
            if (mCamera != null) {
                try {
                    Log.i(TAG, "stopCameraPreView.");
                    if (mVTMgr != null && mVTMgr.mVolteEnable) {
                        mCamera.reconnect();
                    }
                    if (mCamera.previewEnabled()) {
                        mCamera.stopPreview();
                    }
                    mCamera.setPreviewSurface((Surface) null);
                    mCamera.startPreview();
                    if (mVTMgr != null && mVTMgr.mVolteEnable) {
                        mCamera.unlock();
                        mVTMgr.setCamera(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "setPreviewSurface failed, " + e);
                }
            }
        }
    }

    /**
     * Return current preview state.
     */
    public boolean isCameraPreviewing() {
        if (mCamera != null) {
            synchronized (mCameraLock) {
                boolean isPreviewing = false;
                try {
                    if (mCamera.previewEnabled()) {
                        Log.i(TAG, "isCameraPreviewing: true");
                        isPreviewing = true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "isCameraPreviewing, camera exception " + e);
                }
                return isPreviewing;
            }
        } else {
            return false;
        }
    }

    /**
     * Determine whether the camera type can switch.
     */
    private boolean canSwitchCameras() {
        Log.d(TAG, "canSwitchCameras()...");
        if (mCameraNumbers < 2) {
            return false;
        }
        return true;
    }

    public void setPreviewSize(int width, int height) {
        Log.i(TAG, "setPreviewSize-> width:" + width + " height=" + height);
        VTManagerProxy.getInstance().setPreviewSize(width, height);
    }

    public void openVideoCamera() {
        operateCamera(WorkerTaskType.CAMERA_OPEN);
    }

    public void switchVideoCamera() {
        operateCamera(WorkerTaskType.CAMERA_SWITCH);
    }

    public void releaseVideoCamera() {
        // SPRD: remove camera orientationListener for bug 427421
        // mOrientationListener.disable();
        closeCamera();
    }

    public void closeVideoCamera() {
        operateCamera(WorkerTaskType.CAMERA_CLOSE);
    }

    public void handleSetCamera(String cameraId) {
        Log.i(TAG, "handleSetCamera()->isFirstInit:" + mIsFirstInit + " cameraId=" + cameraId
                + " mCameraId=" + mCameraId);
        if (cameraId == null) {
            closeVideoCamera();
            setCameraID(null);
        } else {
            if (mIsFirstInit) {
                setCameraID(cameraId);
                initCameraAndStartPreview();
            } else if (isSameCamera(cameraId)) {
                onSetSameCameraId();
            } else {
                if (mCameraId == null) {
                    setCameraID(cameraId);
                    openVideoCamera();
                } else {
                    setCameraID(cameraId);
                    switchVideoCamera();
                }
            }
        }
    }

    /**
     * Determine whether the camera is opened.
     */
    public boolean isCameraOpened() {
        return (mCamera != null);
    }

    public void onSetSameCameraId() {
        if (isCameraPreviewing()) {
            Log.i(TAG, "onSetSameCameraId() E, Camera is previewing");
            return;
        } else if (isCameraOpened()) {
            startCameraPreView();
        } else {
            openVideoCamera();
        }
    }

    public void onSetDeviceRotation(int rotation) {
    }

    /**
     * When user click menu to control the camera, this method will be called.
     */
    private void operateCamera(final WorkerTaskType type) {
        /* SPRD: modify for bug520684 @{ */
        if (mThreadRunning) {
            Log.e(TAG, "operateCamera(), work task is locked!");
            return;
        }
        /* @} */
        mOperateCameraThread = new Thread(new Runnable() {
            public void run() {
                Log.i(TAG, "operateCamera() E, type: " + type);
                mThreadRunning = true;
                mVTManagerProxy.setCameraSwitching(true);
                if (WorkerTaskType.CAMERA_CLOSE == type || WorkerTaskType.CAMERA_OPEN == type) {
                    if (mVTMgr != null) {
                        mVTMgr.stopUpLink();
                    }
                    if (WorkerTaskType.CAMERA_CLOSE == type) {
                        closeCamera();
                    } else if (WorkerTaskType.CAMERA_OPEN == type) {
                        openCamera();
                        if (mVTMgr != null && mVTMgr.mVolteEnable) {
                            startCameraPreViewBackground();
                        } else {
                            startCameraPreView();
                        }
                        if (mVTMgr != null) {
                            mVTMgr.startUpLink();
                        }
                    }

                    if (mVTMgr != null) {
                        mVTMgr.controlLocalVideo(isCameraPreviewing(), false);
                        // TODO:SPRD
                        /*
                         * mVTMgr.enableSubstitutePic(VideoCallUtils.getSubstitutePic(true,
                         * PhoneGlobals.getInstance().mVideoCallHSP.mUsedPhoneId,
                         * PhoneGlobals.getInstance().getApplicationContext()) ,
                         * !isCameraAvailable());
                         */
                    }
                } else if (WorkerTaskType.CAMERA_SWITCH == type) {
                    if (mVTMgr != null)
                        mVTMgr.stopUpLink();
                    closeCamera();
                    openCamera();
                    if (null != mVTMgr) {
                        if (mVTMgr != null && mVTMgr.mVolteEnable) {
                            startCameraPreViewBackground();
                        } else {
                            mVTMgr.setCamera(getCamera());
                            startCameraPreView();
                        }
                        mVTMgr.startUpLink();
                    }
                }
                mVTManagerProxy.setCameraSwitching(false);
                Log.i(TAG, "closeOrSwitchCamera() X");
                mThreadRunning = false;
                return;
            }
        });

        Log.i(TAG, "mOperateCameraThread: " + mOperateCameraThread);
        mOperateCameraThread.start();
    }

    /**
     * Get the camera ID and aspect ratio for the front and rear cameras.
     * @param context The context.
     */
    private void initializeCameraList(Context context) {
        if (context == null) {
            return;
        }

        CameraManager cameraManager = null;
        try {
            cameraManager = (CameraManager) context.getSystemService(
                    Context.CAMERA_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Could not get camera service.");
            return;
        }

        if (cameraManager == null) {
            return;
        }

        String[] cameraIds = {};
        try {
            cameraIds = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Could not access camera: " + e);
            // Camera disabled by device policy.
            return;
        }

        for (int i = 0; i < cameraIds.length; i++) {
            CameraCharacteristics c = null;
            try {
                c = cameraManager.getCameraCharacteristics(cameraIds[i]);
            } catch (IllegalArgumentException e) {
                // Device Id is unknown.
                Log.w(TAG, "initializeCameraList fail: " + e);
            } catch (CameraAccessException e) {
                // Camera disabled by device policy.
                Log.w(TAG, "initializeCameraList fail: " + e);
            }
            if (c != null) {
                int facingCharacteristic = c.get(CameraCharacteristics.LENS_FACING);
                if (facingCharacteristic == CameraCharacteristics.LENS_FACING_FRONT) {
                    mFrontFacingCameraId = cameraIds[i];
                } else if (facingCharacteristic == CameraCharacteristics.LENS_FACING_BACK) {
                    mRearFacingCameraId = cameraIds[i];
                }
            }
        }
        Log.i(TAG, "initializeCameraList->mFrontFacingCameraId:" + mFrontFacingCameraId
                + "  mRearFacingCameraId:" + mRearFacingCameraId);
    }

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            int displayOrientation = mDeviceRotation;
            if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                if (orientation >= 350 || orientation <= 10) {
                    displayOrientation = 0;
                } else if (orientation >= 80 && orientation <= 100) {
                    displayOrientation = 90;
                } else if (orientation >= 160 && orientation <= 280) {
                    displayOrientation = 270;
                }
            }
            if (displayOrientation != mDeviceRotation
                    && displayOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                Log.i(TAG, "onOrientationChanged: " + displayOrientation);
                mDeviceRotation = displayOrientation;
                mHandler.removeMessages(EVENT_CHANGE_ORIENTATION);
                mHandler.sendEmptyMessageDelayed(EVENT_CHANGE_ORIENTATION, 500);
            }
        }
    }

    private void handleOrientationChange() {
        Log.i(TAG, "handleOrientationChange->mCamera:" + mCamera);
        // SPRD:add startCameraPreView for bug541022
        startCameraPreView();
    }

    private void updateCameraPara() {
        Log.i(TAG, "updateCameraPara->mCamera:" + mCamera);
        if (mCamera == null) {
            return;
        }
        Camera.Parameters params = mCamera.getParameters();
        params.set("sensor-rot", getSensorRotation(mCameraId));
        mCamera.setParameters(params);
    }
}
