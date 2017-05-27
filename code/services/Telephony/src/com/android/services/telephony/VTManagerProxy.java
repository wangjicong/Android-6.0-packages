package com.android.services.telephony;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.telecom.Connection.VideoProvider;
import android.telecom.VideoProfile;
import android.view.Surface;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.PhoneConstants;
import com.sprd.videophone.vtmanager.VTManager;
import com.sprd.videophone.vtmanager.VTManagerUtils;
import com.sprd.videophone.vtmanager.VideoCallCameraManager;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.ims.ImsManager;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;

public class VTManagerProxy {
    private static final String TAG = VTManagerProxy.class.getSimpleName();

    /** phone event code. */
    private static final int EVENT_NEW_RINGING_CONNECTION = 100;
    private static final int EVENT_UNKNOWN_CONNECTION = 101;
    private static final int EVENT_PRECISE_CALL_STATE_CHANGED = 102;
    private static final int EVENT_HANDOVER_STATE_CHANGED = 103;
    private static final int EVENT_CONNECTION_DISCONNECT = 104;

    /** video call provider event code. */
    private static final int EVENT_ON_SET_CAMERA = 200;
    private static final int EVENT_ON_SET_PREVIEW_SURFACE = 201;
    private static final int EVENT_ON_SET_DISPLAY_SURFACE = 202;
    private static final int EVENT_ON_SET_PAUSE_IMAGE = 203;
    private static final int EVENT_ON_SET_DEVICE_ORIENTATION = 204;

    /** video call cp event code. */
    private static final int EVENT_VIDEO_CALL_CODEC = 300;
    private static final int EVENT_VIDEO_CALL_FAIL = 301;
    private static final int EVENT_VIDEO_CALL_FALL_BACK = 302;

    /** video connection event code. */
    private static final int EVENT_CONNECTION_VIDEO_STATE_CHANGED = 400;
    private static final int EVENT_CONNECTION_LOCAL_VIDEO_CAPABILITY_CHANGED = 401;
    private static final int EVENT_CONNECTION_REMOTE_VIDEO_CAPABILITY_CHANGED = 402;
    private static final int EVENT_CONNECTION_VIDEO_PROVIDER_CHANGED = 403;
    private static final int EVENT_CONNECTION_AUDIO_QUALITY_CHANGED = 404;

    private static final int EVENT_VOLTE_CALL_REMOTE_REQUEST_MEDIA_CHANGED = 500;
    private static final int EVENT_VOLTE_CALL_LOCAL_REQUEST_UPGRADE_TO_VIDEO = 501;
    private static final int EVENT_VOLTE_CALL_LOCAL_REQUEST_DOWNGRADE_TO_VOICE = 502;

    private static final Object mLock = new Object();
    private static VTManagerProxy mInstance;

    private Context mContext;
    private VideoCallProvider mLocalRequestProvider;
    private VideoProfile mLoacalRequestProfile;
    private VTManager mVTManager;
    private VideoCallCameraManager mVideoCallCameraManager;
    private Connection mActiveVideoCallConnection;
    private HandlerThread mMediaPhoneThread;

    private Surface mPreviewSurface;
    private Surface mDisplaySurface;
    private String mCameraId;
    private Uri mPauseImage;
    private AlertDialog mFallBackDialog;

    // private boolean mHasCameraPermission = false;
    // private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;

    private VTManagerProxy(Context context) {
        mContext = context;
    }

    public static VTManagerProxy init(Context c) {
        synchronized (mLock) {
            if (mInstance != null) {
                throw new RuntimeException("VTManagerProxy.init() should only be called once");
            }
            mInstance = new VTManagerProxy(c);
            return (VTManagerProxy) mInstance;
        }
    }

    public static VTManagerProxy getInstance() {
        return mInstance;
    }

    /**
     * Used to listen to events from {@link #mPhoneBase}.
     */
    public void registerForMessages(GSMPhone phone) {
        phone.registerForNewRingingConnection(
                mHandler, EVENT_NEW_RINGING_CONNECTION, null);
        phone.registerForUnknownConnection(
                mHandler, EVENT_UNKNOWN_CONNECTION, null);
        phone.registerForPreciseCallStateChanged(
                mHandler, EVENT_PRECISE_CALL_STATE_CHANGED, null);
        phone.registerForHandoverStateChanged(
                mHandler, EVENT_HANDOVER_STATE_CHANGED, null);
        phone.registerForDisconnect(mHandler, EVENT_CONNECTION_DISCONNECT, null);
        phone.registerForVideoCallCodec(mHandler, EVENT_VIDEO_CALL_CODEC, phone);
        phone.registerForVideoCallFail(mHandler, EVENT_VIDEO_CALL_FAIL, phone);
        phone.registerForVideoCallFallBack(mHandler, EVENT_VIDEO_CALL_FALL_BACK, phone);
    }

    /**
     * Used to listen to events.
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            /* SPRD: add for VoLTE@{*/
            if(ImsManager.isVolteEnabledByPlatform(mContext)){
                log("handleMessage, this is volte product, msg.what=" + msg.what);
                return;
            }
            /* @} */
            switch (msg.what) {
                case EVENT_NEW_RINGING_CONNECTION:
                    handleNewRingingConnection((AsyncResult) msg.obj);
                    break;
                case EVENT_UNKNOWN_CONNECTION:
                    handleNewUnknownConnection((AsyncResult) msg.obj);
                    break;
                case EVENT_PRECISE_CALL_STATE_CHANGED:
                    handleCallStateChanged((AsyncResult) msg.obj);
                    break;
                case EVENT_HANDOVER_STATE_CHANGED:
                    handleHandoverStateChanged((AsyncResult) msg.obj);
                    break;
                case EVENT_CONNECTION_DISCONNECT:
                    handleDisconnect((AsyncResult) msg.obj);
                    break;
                case EVENT_ON_SET_CAMERA:
                    handleSetCamera((String) msg.obj);
                    break;
                case EVENT_ON_SET_PREVIEW_SURFACE:
                    handleSetPreviewSurface((Surface) msg.obj);
                    break;
                case EVENT_ON_SET_DISPLAY_SURFACE:
                    handleSetDisplaySurface((Surface) msg.obj);
                    break;
                case EVENT_ON_SET_PAUSE_IMAGE:
                    log("handleMessage:what = EVENT_ON_SET_PAUSE_IMAGE ->mPauseImage="
                            + mPauseImage);
                    // mPauseImage = (String)msg.obj;
                    break;
                case EVENT_ON_SET_DEVICE_ORIENTATION:
                    handleSetDeviceOrientation((Integer) msg.obj);
                    break;
                case EVENT_VIDEO_CALL_CODEC:
                    handleVideoCallCodecEvent((AsyncResult) msg.obj);
                    break;
                case EVENT_VIDEO_CALL_FAIL:
                    handleVideoCallFail((AsyncResult) msg.obj);
                    break;
                case EVENT_VIDEO_CALL_FALL_BACK:
                    handleVideoCallFallBack((AsyncResult) msg.obj);
                    break;
                case EVENT_CONNECTION_VIDEO_STATE_CHANGED:
                    handleVideoStateChanged((Integer) msg.obj);
                    break;
                case EVENT_CONNECTION_LOCAL_VIDEO_CAPABILITY_CHANGED:
                    handlLocalVideoCapabilityChanged((Boolean) msg.obj);
                    break;
                case EVENT_CONNECTION_REMOTE_VIDEO_CAPABILITY_CHANGED:
                    handleRemoteVideoCapabilityChanged((Boolean) msg.obj);
                    break;
                case EVENT_CONNECTION_VIDEO_PROVIDER_CHANGED:
                    handleVideoProviderChanged((VideoProvider) msg.obj);
                    break;
                case EVENT_CONNECTION_AUDIO_QUALITY_CHANGED:
                    handleAudioQualityChanged((Integer) msg.obj);
                    break;
                case EVENT_VOLTE_CALL_LOCAL_REQUEST_UPGRADE_TO_VIDEO:
                    handleLocalRequestMediaChange((Connection) msg.obj, true);
                    break;
                case EVENT_VOLTE_CALL_LOCAL_REQUEST_DOWNGRADE_TO_VOICE:
                    handleLocalRequestMediaChange((Connection) msg.obj, false);
                    break;
                default:
                    log("handleMessage,unkwon message:what =" + msg.what);
                    break;
            }
        }
    };

    public void initVideoCallProvider(Connection connection) {
        if (connection != null) {
            log("initVideoCallProvider,connection=" + connection);
            /* SPRD: add for VoLTE@{*/
            if(connection instanceof ImsPhoneConnection){
                return;
             }
            /* @} */
            VideoCallProvider vtProvider = new VideoCallProvider(connection);
            connection.setVideoProvider(vtProvider);
            // final boolean hasReadCallLogPermission =
            // hasPermission(mContext.getApplicationContext(), "android.permission.CAMERA");
            // if (!mHasCameraPermission && hasReadCallLogPermission) {
            // requestPermissions(new String[] {"android.permission.CAMERA"},
            // CAMERA_PERMISSION_REQUEST_CODE);
            // }
            // mHasCameraPermission = hasReadCallLogPermission;
        }
    }

    /**
     * Verifies the incoming call and triggers sending the incoming-call intent to Telecom.
     * @param asyncResult The result object from the new ringing event.
     */
    private void handleNewRingingConnection(AsyncResult asyncResult) {
        log("handleNewRingingConnection,asyncResult.result=" + asyncResult.result);
        /* SPRD:  dismissFallBackDialog when NewRingingConnection for fix bug 509266 @{ */
        dismissFallBackDialog();
        /* @} */
        Connection connection = (Connection) asyncResult.result;
        if (connection != null && VideoProfile.isBidirectional(connection.getVideoState())) {
            onVTConnectionEstablished(connection);
        }
    }

    private void handleNewUnknownConnection(AsyncResult asyncResult) {
        log("handleNewUnknownConnection->asyncResult.result=" + asyncResult.result);
        if (!(asyncResult.result instanceof Connection)) {
            log("handleNewUnknownConnection called with non-Connection object");
            return;
        }
        Connection connection = (Connection) asyncResult.result;
        if (connection != null) {
            Call call = connection.getCall();
            if (call != null && call.getState().isAlive()) {

            }
        }
    }

    private void handleHandoverStateChanged(AsyncResult asyncResult) {
        log("handleHandoverStateChanged->asyncResult.result=" + asyncResult.result);
        if (!(asyncResult.result instanceof Connection)) {
            log("handleHandoverStateChanged called with non-Connection object");
            return;
        }
        Connection connection = (Connection) asyncResult.result;
        if (connection != null) {
            Call call = connection.getCall();
            if (call != null && call.getState().isAlive()) {

            }
        }
    }

    private void handleCallStateChanged(AsyncResult asyncResult) {
        log("handleCallStateChanged:->asyncResult.result=" + asyncResult.result);
        if (!(asyncResult.result instanceof GSMPhone)) {
            log("handleCallStateChanged called  with non-GSMPhone object");
            return;
        }
        GSMPhone phone = (GSMPhone) asyncResult.result;
        if (phone != null && phone.getState() == PhoneConstants.State.OFFHOOK) {
            Call call = (Call) phone.getForegroundCall();
            if (call != null) {
                Connection connection = (Connection) call.getLatestConnection();
                if (connection != null && VideoProfile.isBidirectional(connection.getVideoState())) {
                    onVTConnectionEstablished(connection);
                }
            }
        }
    }

    private void handleDisconnect(AsyncResult asyncResult) {
        log("handleDisconnect->asyncResult.result=" + asyncResult.result);
        if (!(asyncResult.result instanceof Connection)) {
            log("handleDisconnect called with non-Connection object");
            return;
        }
        Connection connection = (Connection) asyncResult.result;
        if (connection != null) {
            Call call = connection.getCall();
            if (call != null &&
                    isActiveVideoCallConnection(connection)) {
                onVTConnectionDisconnected();
            }
        }
    }

    public void onVTConnectionEstablished(Connection connection) {
        log("onVTConnectionEstablished");
        GSMPhone phone = null;
        Call call = connection.getCall();
        if (call != null && (call.isRinging() || call.isDialingOrAlerting())) {
            phone = (GSMPhone) call.getPhone();
        }

        if (isVideoCallAlive() || phone == null) {
            log("onVTConnectionEstablished->Don't create VTManager cause : isVideoCallAlive()="
                    + isVideoCallAlive() + " phone=" + phone);
            return;
        }
        mActiveVideoCallConnection = connection;
        mActiveVideoCallConnection.addListener(new VideoConnectionListener());
        if (mVTManager == null) {
            final Object syncObj = new Object();

            final RIL ril = (RIL) phone.mCi;
            mMediaPhoneThread = new HandlerThread("VTManager") {
                protected void onLooperPrepared() {
                    log("create mVTManager");
                    synchronized (syncObj) {
                        mVTManager = new VTManager(ril, mContext);
                        syncObj.notifyAll();
                    }
                    log("create mVTManager done");
                }
            };
            mMediaPhoneThread.start();
            log("before wait mVTManager");
            synchronized (syncObj) {
                try {
                    syncObj.wait(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mVideoCallCameraManager = new VideoCallCameraManager(mVTManager, mContext, this);
            log("after wait mVTManager, mVTManager is null?:" + (mVTManager == null));
            log("wait mVTManager done.");
        }
    }

    public void onVTConnectionDisconnected() {
        if (!isVideoCallAlive()) {
            log("No active video call!");
            return;
        }
        if (mVTManager != null) {
            mVTManager.release();
            mVideoCallCameraManager.releaseVideoCamera();
            mVideoCallCameraManager = null;
            mVTManager = null;
            mActiveVideoCallConnection = null;
            log("onVTConnectionDisconnected::mMediaPhoneThread.quit(): " + mMediaPhoneThread.quit());
        }
    }

    private void handleSetCamera(String cameraId) {
        log("handleSetCamera->cameraId=" + cameraId);
        mCameraId = cameraId;
        if (mVideoCallCameraManager == null) {
            log("handleSetCamera mVideoCallCameraManager is null!");
            return;
        }
        mVideoCallCameraManager.handleSetCamera(cameraId);
        updateSessionModificationState();//bug493552
    }

    /* SPRD:bug493552 @{ */
    private void updateSessionModificationState() {
        if (mActiveVideoCallConnection == null) {
            log("updateSessionModificationState mActiveVideoCallConnection is null!");
            return;
        }
        android.telecom.Connection.VideoProvider vtProvider = mActiveVideoCallConnection
                .getVideoProvider();
        if (vtProvider != null) {
            log("updateSessionModificationState receiveSessionModifyResponse");
            vtProvider.receiveSessionModifyResponse(VideoProvider.SESSION_MODIFY_REQUEST_SUCCESS,
                    null, null);
        }
    }

    /* @} */

    private void handleSetPreviewSurface(Surface surface) {
        log("handleSetPreviewSurface->Surface=" + surface);
        mPreviewSurface = surface;
        if (mVTManager != null) {
            if (mVTManager.mVolteEnable && surface == null) {
                log("handleSetPreviewSurface->Clean up camera object");
                mVTManager.setCamera(null);
            }
            mVTManager.setLocalSurface(mPreviewSurface);
        }
        if (mVideoCallCameraManager == null) {
            log("handleSetPreviewSurface-->mVideoCallCameraManager is null");
            if (mVTManager != null && mVTManager.mVolteEnable) {
                mVTManager.setLocalSurface(mPreviewSurface);
            }
            return;
        }
        /* SPRD: add handleSetCameraPreSurface for bug 408181 @{ */
        mVideoCallCameraManager.handleSetCameraPreSurface(mPreviewSurface);
        /* @} */
    }

    private void handleSetDisplaySurface(Surface surface) {
        log("handleSetDisplaySurface->Surface=" + surface);
        mDisplaySurface = surface;
        if (mVTManager != null) {
            mVTManager.setRemoteSurface(mDisplaySurface);
        }
        if (surface != null) {
            setPreviewSize(176, 144);
        }
    }

    private void handleSetDeviceOrientation(Integer rotation) {
        log("handleSetDeviceOrientation->rotation=" + rotation);
        if (mVideoCallCameraManager == null) {
            log("handleSetDeviceOrientation-->mVideoCallCameraManager is null");
            return;
        }
        if (rotation != null) {
            mVideoCallCameraManager.onSetDeviceRotation(rotation.intValue());
        }
    }

    private void handleVideoCallCodecEvent(AsyncResult asyncResult) {
        log("handleVideoCallCodecEvent->asyncResult=" + asyncResult);
        if (asyncResult != null) {
            GSMPhone phone = (GSMPhone) asyncResult.userObj;
            int[] params = (int[]) asyncResult.result;
            log("handleVideoCallCodecEvent->params=" + params[0]);
            if (phone != null && params[0] == 1) {
                phone.codecVP(params[0], null);
            }
        }
    }

    private void handleVideoCallFail(AsyncResult asyncResult) {
        log("handleVideoCallFail->asyncResult=" + asyncResult);
        if (asyncResult != null) {
            GSMPhone phone = (GSMPhone) asyncResult.userObj;
            AsyncResult arO = (AsyncResult) asyncResult.result;
            AsyncResult ar = (AsyncResult) arO.result;
            boolean isIncomingCall = arO.userObj != null ? (((Integer) arO.userObj == 1) ? true
                    : false) : false;
            String number = null;
            Integer cause = null;
            if (ar != null) {
                number = ar.userObj != null ? (String) ar.userObj : null;
                cause = ar.result != null ? (Integer) ar.result : null;
                log("handleVideoCallFail, number: " + number + ", cause: " + cause);
            }
            if (isIncomingCall || number == null || cause == null) {
                log("handleVideoCallFail->don't show fail message because: isIncomingCall:"
                        + isIncomingCall +
                        " or number/cause is null.");
                return;
            }
            onVideoCallFailOrFallBack();
            VTManagerUtils.showVideoCallFailToast(mContext, cause.intValue());
            if (cause.intValue() == VTManagerUtils.VIDEO_CALL_NO_SERVICE ||
                    cause.intValue() == VTManagerUtils.VIDEO_CALL_CAPABILITY_NOT_AUTHORIZED) {
                showFallBackDialog(number, cause.intValue(), phone);
            }
        }
    }

    private void handleVideoCallFallBack(AsyncResult asyncResult) {
        log("handleVideoCallFallBack->asyncResult=" + asyncResult);
        if (asyncResult != null) {
            GSMPhone phone = (GSMPhone) asyncResult.userObj;
            AsyncResult arO = (AsyncResult) asyncResult.result;
            AsyncResult ar = (AsyncResult) arO.result;
            boolean isIncomingCall = arO.userObj != null ? (((Integer) arO.userObj == 1) ? true
                    : false) : false;
            String number = null;
            Integer cause = null;
            if (ar != null) {
                number = ar.userObj != null ? (String) ar.userObj : null;
                cause = ar.result != null ? (Integer) ar.result : null;
                log("handleVideoCallFail, number: " + number + ", cause: " + cause);
            }
            if (isIncomingCall || number == null || cause == null) {
                log("handleVideoCallFail->don't show fail message because: isIncomingCall:"
                        + isIncomingCall +
                        " or number/cause is null.");
                return;
            }
            onVideoCallFailOrFallBack();
            showFallBackDialog(number, cause.intValue(), phone);
        }
    }

    private void showFallBackDialog(String number, int cause, GSMPhone phone) {
        dismissFallBackDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext.getApplicationContext());
        mFallBackDialog = builder.setView(VTManagerUtils.getVideoCallFallBackView
                (mContext.getApplicationContext(), number, cause, phone)).create();
        VTManagerUtils.initVideoCallFallBackDialog(mFallBackDialog,
                mContext.getApplicationContext(), number, cause, phone);
        mFallBackDialog.show();
    }

    public void dismissFallBackDialog() {
        if (mFallBackDialog != null) {
            mFallBackDialog.dismiss();
        }
    }

    private void onVideoCallFailOrFallBack() {
        if (mActiveVideoCallConnection != null) {
            Call call = mActiveVideoCallConnection.getCall();
            log("onVideoCallFailOrFallBack->hangup alive call.");
            call.hangupIfAlive();
        }
    }

    private void handleVideoStateChanged(Integer videoState) {
        log("handleVideoStateChanged->vdieoState=" + videoState);
        if (videoState != null && VideoProfile.isAudioOnly(videoState.intValue())) {
            onVTConnectionDisconnected();
        }
    }

    private void handlLocalVideoCapabilityChanged(Boolean capability) {
        log("handlLocalVideoCapabilityChanged->capability=" + capability);
    }

    private void handleRemoteVideoCapabilityChanged(Boolean capability) {
        log("handleRemoteVideoCapabilityChanged->capability=" + capability);
    }

    private void handleVideoProviderChanged(VideoProvider videoProvider) {
        log("handleVideoProviderChanged->VideoProvider=" + videoProvider);
    }

    private void handleAudioQualityChanged(Integer quality) {
        log("handleAudioQualityChanged->quality=" + quality);
    }

    public boolean isVideoCallAlive() {
        return mActiveVideoCallConnection != null;
    }

    public boolean isActiveVideoCallConnection(Connection connection) {
        return isVideoCallAlive() && mActiveVideoCallConnection.equals(connection);
    }

    public void setPreviewSize(int width, int height) {
        if (mActiveVideoCallConnection != null) {
            android.telecom.Connection.VideoProvider vp = mActiveVideoCallConnection
                    .getVideoProvider();
            if (vp != null) {
                log("setPreviewSize->width=" + width + " height=" + height);
                VideoProfile.CameraCapabilities cc = new VideoProfile.CameraCapabilities(width,
                        width, false, 0);
                vp.changeCameraCapabilities(cc);
            }
        }
    }

    public void setCameraSwitching(boolean isSwitching) {

        if (mActiveVideoCallConnection != null) {
            android.telecom.Connection.VideoProvider vp = mActiveVideoCallConnection
                    .getVideoProvider();
            if (vp != null) {
                log("setCameraSwitching->isSwitching=" + isSwitching);
                VideoProfile.CameraCapabilities cc = new VideoProfile.CameraCapabilities(0, 0,
                        false, 0,isSwitching);//SPRD:modify for bug493880
                vp.changeCameraCapabilities(cc);
            }
        }
    };

    public void handleLocalRequestMediaChange(Connection connection, boolean upgradeToVideo) {
        log("handleLocalRequestMediaChange->connection:" + connection + "   upgradeToVideo:"
                + upgradeToVideo);
        GSMPhone phone = null;
        if (connection != null) {
            Call call = connection.getCall();
            if (call != null) {
                phone = (GSMPhone) call.getPhone();
            }
            if (phone == null) {
                log("handleLocalRequestMediaChange->Don't send request because phone is null!");
                return;
            }
            phone.requestVolteCallMediaChange(!upgradeToVideo, null);
        }
    }

    /**
     * Instantiates an instance of the VideoCallProvider
     */
    class VideoCallProvider extends android.telecom.Connection.VideoProvider {
        private Connection mConnection;

        VideoCallProvider(Connection connection) {
            mConnection = connection;
        }

        /**
         * Sets the camera to be used for video recording in a video call.
         * @param cameraId The id of the camera.
         */
        @Override
        public void onSetCamera(String cameraId) {
            mHandler.obtainMessage(EVENT_ON_SET_CAMERA, cameraId).sendToTarget();
        }

        /**
         * Sets the surface to be used for displaying a preview of what the user's camera is
         * currently capturing. When video transmission is enabled, this is the video signal which
         * is sent to the remote device.
         * @param surface The surface.
         */
        @Override
        public void onSetPreviewSurface(Surface surface) {
            mHandler.obtainMessage(EVENT_ON_SET_PREVIEW_SURFACE, surface).sendToTarget();
        }

        /**
         * Sets the surface to be used for displaying the video received from the remote device.
         * @param surface The surface.
         */
        @Override
        public void onSetDisplaySurface(Surface surface) {
            mHandler.obtainMessage(EVENT_ON_SET_DISPLAY_SURFACE, surface).sendToTarget();
        }

        /**
         * Sets the device orientation, in degrees. Assumes that a standard portrait orientation of
         * the device is 0 degrees.
         * @param rotation The device orientation, in degrees.
         */
        @Override
        public void onSetDeviceOrientation(int rotation) {
            mHandler.obtainMessage(EVENT_ON_SET_DEVICE_ORIENTATION, new Integer(rotation))
                    .sendToTarget();
        }

        /**
         * Sets camera zoom ratio.
         * @param value The camera zoom ratio.
         */
        @Override
        public void onSetZoom(float value) {

        }

        /**
         * Issues a request to modify the properties of the current session. The request is sent to
         * the remote device where it it handled by the In-Call UI. Some examples of session
         * modification requests: upgrade call from audio to video, downgrade call from video to
         * audio, pause video.
         * @param requestProfile The requested call video properties.
         */
        @Override
        public void onSendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile) {
            log("onSendSessionModifyRequest->fromProfile:" + fromProfile + "  toProfile ="
                    + toProfile);

            if ((fromProfile == null || toProfile == null)
                    || (fromProfile.getVideoState() == toProfile.getVideoState())) {
                log("onSendSessionModifyRequest->fromProfile = toProfile");
                return;
            }

            mLocalRequestProvider = this;
            mLoacalRequestProfile = toProfile;

            if (toProfile.getVideoState() == VideoProfile.STATE_BIDIRECTIONAL) {
                mHandler.obtainMessage(EVENT_VOLTE_CALL_LOCAL_REQUEST_UPGRADE_TO_VIDEO, mConnection)
                        .sendToTarget();
            } else {
                mHandler.obtainMessage(EVENT_VOLTE_CALL_LOCAL_REQUEST_DOWNGRADE_TO_VOICE,
                        mConnection).sendToTarget();
            }

            /*
             * log("onSendSessionModifyRequest->requestProfile:"+requestProfile); if(requestProfile
             * != null){
             * log("onSendSessionModifyRequest->requestVideoState:"+requestProfile.getVideoState()
             * +" connectionVideoState:"+mConnection.getVideoState());
             * if(requestProfile.getVideoState() != mConnection.getVideoState()){
             * mLocalRequestProvider = this; mLoacalRequestProfile = requestProfile;
             * if(requestProfile.getVideoState() == VideoProfile.BIDIRECTIONAL){
             * mHandler.obtainMessage(EVENT_VOLTE_CALL_LOCAL_REQUEST_UPGRADE_TO_VIDEO,
             * mConnection).sendToTarget(); } else {
             * mHandler.obtainMessage(EVENT_VOLTE_CALL_LOCAL_REQUEST_DOWNGRADE_TO_VOICE,
             * mConnection).sendToTarget(); } } }
             */
        }

        /**
         * Provides a response to a request to change the current call session video properties.
         * This is in response to a request the InCall UI has received via the InCall UI.
         * @param responseProfile The response call video properties.
         */
        @Override
        public void onSendSessionModifyResponse(VideoProfile responseProfile) {

        }

        /**
         * Issues a request to the video provider to retrieve the camera capabilities. Camera
         * capabilities are reported back to the caller via the In-Call UI.
         */
        @Override
        public void onRequestCameraCapabilities() {

        }

        /**
         * Issues a request to the video telephony framework to retrieve the cumulative data usage
         * for the current call. Data usage is reported back to the caller via the InCall UI.
         */
        // @Override
        public void onRequestCallDataUsage() {

        }

        @Override
        public void onRequestConnectionDataUsage() {

        }

        /**
         * Provides the video telephony framework with the URI of an image to be displayed to remote
         * devices when the video signal is paused.
         * @param uri URI of image to display.
         */
        @Override
        public void onSetPauseImage(Uri uri) {
            mHandler.obtainMessage(EVENT_ON_SET_PAUSE_IMAGE, uri).sendToTarget();
        }

    }

    /**
     * Listener for listening to events in the {@link com.android.internal.telephony.Connection}.
     */
    class VideoConnectionListener extends com.android.internal.telephony.Connection.ListenerBase {
        VideoConnectionListener() {
        }

        @Override
        public void onVideoStateChanged(int videoState) {
            mHandler.obtainMessage(EVENT_CONNECTION_VIDEO_STATE_CHANGED, new Integer(videoState))
                    .sendToTarget();
        }

        /**
         * The {@link com.android.internal.telephony.Connection} has reported a change in local
         * video capability.
         * @param capable True if capable.
         */
        @Override
        public void onLocalVideoCapabilityChanged(boolean capable) {
            mHandler.obtainMessage(EVENT_CONNECTION_LOCAL_VIDEO_CAPABILITY_CHANGED,
                    new Boolean(capable)).sendToTarget();
        }

        /**
         * The {@link com.android.internal.telephony.Connection} has reported a change in remote
         * video capability.
         * @param capable True if capable.
         */
        @Override
        public void onRemoteVideoCapabilityChanged(boolean capable) {
            mHandler.obtainMessage(EVENT_CONNECTION_REMOTE_VIDEO_CAPABILITY_CHANGED,
                    new Boolean(capable)).sendToTarget();
        }

        /**
         * The {@link com.android.internal.telephony.Connection} has reported a change in the video
         * call provider.
         * @param videoProvider The video call provider.
         */
        @Override
        public void onVideoProviderChanged(VideoProvider videoProvider) {
            mHandler.obtainMessage(EVENT_CONNECTION_VIDEO_PROVIDER_CHANGED, videoProvider)
                    .sendToTarget();
        }

        /**
         * Used by the {@link com.android.internal.telephony.Connection} to report a change in the
         * audio quality for the current call.
         * @param audioQuality The audio quality.
         */
        @Override
        public void onAudioQualityChanged(int audioQuality) {
            mHandler.obtainMessage(EVENT_CONNECTION_AUDIO_QUALITY_CHANGED,
                    new Integer(audioQuality)).sendToTarget();
        }
    };

    private void log(String string) {
        android.util.Log.i(TAG, string);
    }

    public boolean hasPermission(Context context, String permission) {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
    /*
     * public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
     * grantResults) { if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) { if (grantResults.length
     * >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) { mHasCameraPermission = true;
     * } } }
     */
}
