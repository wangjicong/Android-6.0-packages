package com.sprd.videophone.vtmanager;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Surface;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;

import com.android.internal.telephony.RIL;
import android.telephony.TelephonyManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.internal.telephony.RilVideoEx;
import static com.android.internal.telephony.RILConstants.*;

public class VTManager extends Handler {

    private static final String LOG_TAG = "VTManager";
    private static final boolean VDBG = (SystemProperties.getInt("debug.videophone", 0) == 1);

    // message define
    static final int MSG_DOWNLINK_START = 100;
    static final int MSG_INPUT_STARTREAD = 101;

    static final int MSG_UPLINK_START = 200;
    static final int MSG_UPLINK_ENABLE_PIC = 202;

    static final int MSG_RECORDER_ENABLE = 300;
    static final int MSG_RECORDER_DISABLE = 301;

    // const define
    static final int MAX_BUFFER_SIZE = 256*1024;
    static final int MAX_FRAME_SIZE = 176*144*3/2;

    static final int VIDEO_TYPE_H263 = 1;
    static final int VIDEO_TYPE_MPEG4 = 2;

    private static VTManager gInstance = null;
    private VideoCallEngine mVideoCallEngine;
    private int mCameraResolution = RESOLUTION_VGA;
    public boolean mVolteEnable = false; //TelephonyManager.getVolteEnabled();
    private Context mContext;

    public static final int MEDIA_UNSOL_EVENT_READY      = 99;
    public static final int VCE_EVENT_NONE               = 1000;
    public static final int VCE_EVENT_INIT_COMPLETE      = 1001;
    public static final int VCE_EVENT_START_ENC          = 1002;
    public static final int VCE_EVENT_START_DEC          = 1003;
    public static final int VCE_EVENT_STOP_ENC           = 1004;
    public static final int VCE_EVENT_STOP_DEC           = 1005;
    public static final int VCE_EVENT_SHUTDOWN           = 1006;

    public static final int RESOLUTION_720P = 0;
    public static final int RESOLUTION_VGA = 1;
    public static final int RESOLUTION_QVGA = 2;
    public static final int RESOLUTION_CIF = 3;
    public static final int RESOLUTION_QCIF = 4;

    Surface mRemoteSurface;
    Surface mLocalSurface;
    Camera mCamera;

    DownLink mDownLink = new DownLink("DownLink");
    UpLink mUpLink = new UpLink("UpLink");
    // share with native space
    RecorderThread mRecorderThread = null;
    byte[] m_Mpeg4Header = new byte[2048];
    int m_iMpeg4Header_size = 0;

    private int mCodecCount = 0;
    private int mCurrentCodecType = VIDEO_TYPE_H263;
    public enum CodecState  {
        CODEC_IDLE ,
        CODEC_OPEN,
        CODEC_START,
        CODEC_CLOSE;

        public String toString() {
            switch(this) {
                case CODEC_IDLE:
                    return "CODEC_IDLE";
                case CODEC_OPEN:
                    return "CODEC_OPEN";
                case CODEC_START:
                    return "CODEC_START";
                case CODEC_CLOSE:
                    return "CODEC_CLOSE";
            }
            return "CODEC_IDLE";
        }
    };
    private CodecState mCodecState = CodecState.CODEC_IDLE;

    private RIL mCm;
    public  int phoneId  ;

    /* Do not change these values without updating their counterparts
     * in include/media/mediaphone.h
     */
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_SET_VIDEO_SIZE = 2;
    private static final int MEDIA_ERROR = 100;
    private static final int MEDIA_INFO = 200;

    private final static String CAMERA_OPEN_STR = "open_:camera_";
    private final static String CAMERA_CLOSE_STR = "close_:camera_";

    /* SPRD: add for bug479433 @{ */
    public static final int MODEM_TYPE_GSM = 0;
    public static final int MODEM_TYPE_TDSCDMA = 1;
    public static final int MODEM_TYPE_WCDMA = 2;
    public static final int MODEM_TYPE_LTE = 3;
    /* @} */

    // unsolicited events
    private static final int MEDIA_UNSOL_DATA = 20;
    private static final int MEDIA_UNSOL_CODEC = RIL_UNSOL_VIDEOPHONE_CODEC;
    private static final int MEDIA_UNSOL_STR = RIL_UNSOL_VIDEOPHONE_STRING;
    private static final int MEDIA_UNSOL_REMOTE_VIDEO = RIL_UNSOL_VIDEOPHONE_REMOTE_MEDIA;
    private static final int MEDIA_UNSOL_MM_RING = RIL_UNSOL_VIDEOPHONE_MM_RING;
    private static final int MEDIA_UNSOL_RECORD_VIDEO = RIL_UNSOL_VIDEOPHONE_RECORD_VIDEO;
    private static final int MEDIA_UNSOL_MEDIA_START = RIL_UNSOL_VIDEOPHONE_MEDIA_START;
    private EventHandler mEventHandler;

    // codec request type
    public static final int CODEC_OPEN = 1;
    public static final int CODEC_CLOSE = 2;
    public static final int CODEC_SET_PARAM = 3;

    // for pipe operation
    private Thread mPipeMonitorThread;
    private boolean mStopWaitRequestForAT = false;
    private static final int AT_NONE = 0;
    private static final int AT_TIMEOUT = -1;
    private static final int AT_SELECT_ERR = -2;
    private static final int AT_UPBUFFER_EMPTY = 1;
    private static final int AT_REQUEST_IFRAME = 2;
    private static final int AT_BOTH = 3;
    private native final int native_waitRequestForAT();
    private native final int native_closePipe();
    native final int native_RGB565toYUV420(byte[] pIn, int size,  byte[] pOut, int height, int width);
    native final int native_enableRecord(boolean isEnable, int type, String fileName, int videoType);
    native final int native_writeAudio(Object data, int len, long timeStamp);
    native final int native_writeVideo(byte[] data, int len);

    static {
        System.loadLibrary("vtmanager");
    }

    public  int waitRequestForAT()
    {
        return native_waitRequestForAT() ;
    }
    public VTManager(RIL ril,Context context){
        mCm = ril;
        mContext = context;
        initContext();
        startPipeMonitor();
        gInstance = this;
        log("VTManagerphoneId: "+phoneId );
    }

    public static VTManager getInstance(){
        return gInstance;
    }

    private void initContext() {
        log("initContext() E");
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        mCm.registerForOemHookRaw(mEventHandler, MEDIA_UNSOL_CODEC, null);
        //mCm.registerForOemHookRaw(mEventHandler, RIL_UNSOL_VIDEOPHONE_CODEC, null);
        mCm.registerForOemHookRaw(mEventHandler, MEDIA_UNSOL_STR, null);
        //mCm.registerForOemHookRaw(mEventHandler, RIL_UNSOL_VIDEOPHONE_STRING, null);
        mCm.registerForOemHookRaw(mEventHandler, MEDIA_UNSOL_REMOTE_VIDEO, null);
        //mCm.registerForOemHookRaw(mEventHandler, RIL_UNSOL_VIDEOPHONE_REMOTE_MEDIA, null);

        //mCm.registerForOemHookRaw(mEventHandler, RIL_UNSOL_VIDEOPHONE_MM_RING, null);
        mCm.registerForOemHookRaw(mEventHandler, MEDIA_UNSOL_MM_RING, null);

        mCm.registerForOemHookRaw(mEventHandler, MEDIA_UNSOL_RECORD_VIDEO, null);
        //mCm.registerForOemHookRaw(mEventHandler, RIL_UNSOL_VIDEOPHONE_RECORD_VIDEO, null);
        mCm.registerForOemHookRaw(mEventHandler, MEDIA_UNSOL_MEDIA_START, null);
/*        if(mVolteEnable){
            mVideoState = VideoState.AUDIO_ONLY;
            mVideoCallEngine = new VideoCallEngine(mEventHandler);
            mVideoCallEngine.setup(new WeakReference(mVideoCallEngine));
            initCameraResolution();
        }
*/
        log("initContext() X");
    }

    public void release() {
        log("release() E");
        mOnCallEventListener = null;
        mCm.unregisterForOemHookRaw(mEventHandler);
/*        mCm.unSetOnVPString(mEventHandler);
        mCm.unSetOnVPRemoteMedia(mEventHandler);
        mCm.unSetOnVPMMRing(mEventHandler);
        mCm.unSetOnVPRecordVideo(mEventHandler);
        mCm.unSetOnVPMediaStart(mEventHandler);
*/
        mStopWaitRequestForAT = true;
        stopDownLink();
        stopUpLink();
        native_closePipe();
        if(mRecorderThread !=null)
        {
            mRecorderThread.ExitRecord(0);
            log("mRecorderThread.quit()"+mRecorderThread.quit());
        }
        log("mDownLink.quit()" + mDownLink.quit());
        log("mUpLink.quit()" + mUpLink.quit());
        if(mVideoCallEngine != null){
            mVideoState = VideoState.AUDIO_ONLY;
            mVideoCallEngine.release();
            mVideoCallEngine = null;
        }
        log("release() X");
    }

    private void startPipeMonitor() {
        /*
        mPipeMonitorThread = new Thread(new Runnable() {
            public void run() {
                log("mPipeMonitorThread E");
                do {
                    int ret = native_waitRequestForAT();
                    if(ret == AT_NONE){
                        log("vt_pipe ret error, exit thread");
                        break;
                    } else {
                        vlog("vt_pipe ret: " + ret);
                    }
                    switch (ret) {
                        case AT_UPBUFFER_EMPTY:
                            //controlIFrame(true, false);
                            if (mUpLink != null) {
                                synchronized(mUpLink.m_pipeLock) {
                                    mUpLink.m_pipeLock.notify();
                                }
                            }
                            break;
                    }
                    if (mStopWaitRequestForAT) {
                        log("mStopWaitRequestForAT");
                        break;
                    }
                } while(true);
                native_closePipe();
                log("mPipeMonitorThread X");
            }
        });
        mPipeMonitorThread.start();
*/
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void vlog(String msg) {
        if (VDBG) Log.d(LOG_TAG, msg);
    }

    public void startDownLink(){
        if(mVolteEnable){
            return;
        }
        log("startDownLink() E mCurrentCodecType ="+ mCurrentCodecType);
        setDecodeType(mCurrentCodecType);
        mDownLink.startWork() ;
        log("startDownLink() X");
    }

    public void stopDownLink(){
        if(mVolteEnable){
            return;
        }
        log("stopDownLink() E");
        mDownLink.stopWork();
        log("stopDownLink() X");
    }
    public void startUpLink(){
        if(mVolteEnable){
            return;
        }
        log("startUpLink() E");
        mUpLink.startWork();
        log("startUpLink() X");
    }

    public void stopUpLink(){
        if(mVolteEnable){
            return;
        }
        log("stopUpLink() E");
        mUpLink.stopWork();
        log("stopUpLink() X");
    }
    public void enablePreviewCallback(boolean enable){
        log("enablePreviewCallback() :"+enable);
        if(mVolteEnable) return;
        mUpLink.enablePreviewCallback(enable);
        log("enablePreviewCallback() X");
    }

    private void setDecodeType(int type){
        if(mVolteEnable) return;
        mDownLink.setDecodeType(type);
    }

    private void setEncodeType(int type){
        if(mVolteEnable) return;
        mUpLink.setEncodeType(type);
    }

    public void enableSubstitutePic(String fn, boolean enable) {
        log("enableSubstitutePic() fn:" + fn + ", enable: " + enable);
        mUpLink.enableSubstitutePic(fn,enable);
    }

    public void enableRecord(boolean bEnable, int type, String fn) {
        if(mVolteEnable) return;
        log("enableRecord() , bEnable: " + bEnable + ", type:" + type + ", fn: " + fn);
        int iEnable = 0;
        if (bEnable) {
            iEnable = 1;
            if (mRecorderThread == null)
            {
                mRecorderThread = new RecorderThread("RecorderThread", mDownLink.mVideoType);
            }
            mRecorderThread.enableRecord(bEnable, type, fn);
        } else {
            if (mRecorderThread != null) {
                mRecorderThread.enableRecord(bEnable, type, fn);
            }
        }
    }

    public void setRemoteSurface(Surface sf){
        log("setRemoteSurface() Surface:" + sf);
        if(mVolteEnable && mVideoCallEngine != null){
            mRemoteSurface = sf;
            mDownLink.setSurface(sf);
            mVideoCallEngine.setRemoteSurface(sf);
            log("setRemoteSurface() mVideoState:" + mVideoState);
        } else {
            if(!isVideoCodecStarted()){
                mRemoteSurface = sf;
                mDownLink.setSurface(sf);
                return;
            }
            if(sf == null){
                stopDownLink();
                mRemoteSurface = sf;
                mDownLink.setSurface(sf);
            } else{
                stopDownLink();
                mRemoteSurface = sf;
                mDownLink.setSurface(sf);
                startDownLink();
            }
        }
    }

    public void setLocalSurface(Surface sf){
        Log.i(LOG_TAG, "setLocalSurface->sf is null " + (sf == null));
        mLocalSurface = sf;
        if(mVolteEnable && mVideoCallEngine != null){
            mVideoCallEngine.setLocalSurface(sf);
            log("setLocalSurface() mVideoState:" + mVideoState);
        } else if(mLocalSurface != null && !mUpLink.isStared()
                && mCodecState == CodecState.CODEC_START){
            startUpLink();
        }

    }

    public void setCamera(Camera cam){
        mCamera = cam;
        mUpLink.setCamera(cam);
        if(mVolteEnable && mVideoCallEngine != null){
            mVideoCallEngine.setCamera(cam, getCameraResolution());
        } else {
            mUpLink.setCamera(cam);
        }
    }

    private void codec(int type, Bundle param, Message result) {
        Log.d(LOG_TAG, "codec " + type);
        if(mVolteEnable){
            return;
        }
        mCm.codecVP(type, param, null);
        //RilVideoEx.codecVP(mCm, type, param, null);
    }

    public void onCodecRequest(int type, int param)
    {
        Log.d(LOG_TAG, "VT_TS onCodecRequest:" + type + ", " + param + ", mCodecState: " + mCodecState);
        if(mVolteEnable){
            return;
        }
        switch (type) {
            case CODEC_OPEN:
                if (mOnCallEventListener != null){
                    mOnCallEventListener.onCallEvent(this, MEDIA_CALLEVENT_CODEC_OPEN, null);
                }
                try {
                    //prepareAsync();
                    mCodecState = CodecState.CODEC_OPEN;
                    Log.d(LOG_TAG, "VT_TS CODEC_OPEN ,mCodecState=" + mCodecState);
                } catch (IllegalStateException ex) {
                    Log.d(LOG_TAG, "prepareAsync fail " + ex);
                }
                //codec(CODEC_OPEN, null, null);
                break;

            case CODEC_SET_PARAM:
                Log.d(LOG_TAG, "VT_TS CODEC_SET_PARAM ,mCodecState=" + mCodecState);
                if (param == 1){
                    if(mOnCallEventListener != null){
                        mOnCallEventListener.onCallEvent(this, MEDIA_CALLEVENT_CODEC_SET_PARAM_DECODER, null);
                    }
                    m_iMpeg4Header_size = 0 ;
                    if(mRemoteSurface != null && !mDownLink.isStared()){
                        startDownLink();
                    }
                } else if(mLocalSurface != null && !mUpLink.isStared()){
                    startUpLink();
                }
                if ((2 == mCodecCount) && (mCodecState != CodecState.CODEC_START)) {
                    try {
                        mCodecState = CodecState.CODEC_START;
                        Log.d(LOG_TAG, "VT_TS mCodecCount is 2 ,mCodecState=" + mCodecState);
                        if (mOnCallEventListener != null) {
                            mOnCallEventListener.onCallEvent(this, MEDIA_CALLEVENT_CODEC_START, null);
                        }
                        if(mRemoteSurface != null && !mDownLink.isStared()){
                            startDownLink();
                        }
                        if(mLocalSurface != null && !mUpLink.isStared()){
                            startUpLink();
                        }
                    } catch (IllegalStateException ex) {
                        Log.d(LOG_TAG, "start fail " + ex);
                    }
                }
                codec(CODEC_SET_PARAM, null, null);
                break;

            case CODEC_CLOSE:
                if (mCodecState == CodecState.CODEC_CLOSE) return;
                try {
                    mCodecState = CodecState.CODEC_CLOSE;
                    mStopWaitRequestForAT = true;
                    stopDownLink();
                    stopUpLink();
                    if (mOnCallEventListener != null){
                        mOnCallEventListener.onCallEvent(this, MEDIA_CALLEVENT_CODEC_CLOSE, null);
                    }
                } catch (IllegalStateException ex) {
                    Log.d(LOG_TAG, "stop fail " + ex);
                }
                codec(CODEC_CLOSE, null, null);
                enableRecord(false, 0, null);
                break;
        }
    }

    public CodecState getCodecState() {
        Log.d(LOG_TAG, "getCodecState(), " + mCodecState);
        return mCodecState;
    }

    private class EventHandler extends Handler {
        private VTManager mMgr;

        public EventHandler(VTManager mgr, Looper looper) {
            super(looper);
            mMgr = mgr;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "handleMessage " + msg);

            AsyncResult ar;
            ar = (AsyncResult) msg.obj;

            switch(msg.what) {
                case MEDIA_PREPARED:
                    //todo: call ril send AT
                    Log.d(LOG_TAG, "receive MEDIA_PREPARED");
                    codec(CODEC_OPEN, null, null);
                    return;

                case MEDIA_UNSOL_DATA: {
                    String indication = (String)ar.result;
                    Log.d(LOG_TAG, "handleMessage(MEDIA_UNSOL_DATA), indication: " + indication);
                    return;
                }

                //case RIL_UNSOL_VIDEOPHONE_CODEC: {
                case MEDIA_UNSOL_CODEC: {
                    if (ar == null) {
                        Log.d(LOG_TAG, "handleMessage(MEDIA_UNSOL_CODEC), ar == null");
                        return;
                    }
                    int[] params = (int[])ar.result;
                    Log.d(LOG_TAG, "handleMessage(MEDIA_UNSOL_CODEC), params: " + params[0] + ", length: " + params.length + ", mCodecCount: " + mCodecCount);
                    if (params[0] == 3){ // config message
                        if (params.length >= 4){
                            if (params[2] == 1) { // decode
                                setDecodeType(params[3]);
                                mCurrentCodecType = params[3] ;
                                mCodecCount++;
                            } else { // encode
                                setEncodeType(params[3]);
                                mCodecCount++;
                            }
                            onCodecRequest(params[0], params[2]);
                        }
                    } else {
                        mCodecCount = 0;
                        onCodecRequest(params[0], 0);
                    }
                    return;
                }

                //case RIL_UNSOL_VIDEOPHONE_STRING: {
                case MEDIA_UNSOL_STR: {
                    if (ar == null) {
                        Log.d(LOG_TAG, "handleMessage(MEDIA_UNSOL_STR), ar == null");
                        return;
                    }

                    String str = (String)ar.result;
                    Log.d(LOG_TAG, "handleMessage(MEDIA_UNSOL_STR), str == " + str);

                    if (str.equals(CAMERA_OPEN_STR)){
                        if (mOnCallEventListener != null){
                            mOnCallEventListener.onCallEvent(mMgr, MEDIA_CALLEVENT_CAMERAOPEN, null);
                        }
                    } else if (str.equals(CAMERA_CLOSE_STR)){
                        if (mOnCallEventListener != null){
                            mOnCallEventListener.onCallEvent(mMgr, MEDIA_CALLEVENT_CAMERACLOSE, null);
                        }
                    } else if (str.length() > 0){
                        if (mOnCallEventListener != null){
                            mOnCallEventListener.onCallEvent(mMgr, MEDIA_CALLEVENT_STRING, str);
                        }
                    }
                    return;
                }

                //case RIL_UNSOL_VIDEOPHONE_REMOTE_MEDIA: {
                case MEDIA_UNSOL_REMOTE_VIDEO: {
                    int[] params = (int[])ar.result;
                    int datatype = params[0];
                    int sw = params[1];
                    int indication = 0;

                    if (params.length > 2)
                        indication = params[2];

                    return;
                }

                case MEDIA_UNSOL_MM_RING: {
                    int[] params = (int[])ar.result;
                    int timer = params[0];
                    return;
                }

                case MEDIA_UNSOL_RECORD_VIDEO: {
                //case RIL_UNSOL_VIDEOPHONE_RECORD_VIDEO: {
                    int[] params = (int[])ar.result;
                    int indication = params[0];
                    return;
                }

                case MEDIA_UNSOL_MEDIA_START: {
                //case RIL_UNSOL_VIDEOPHONE_MEDIA_START: {
                    if (mOnCallEventListener != null){
                        mOnCallEventListener.onCallEvent(mMgr, MEDIA_CALLEVENT_MEDIA_START, null);
                    }
                    return;
                }
                case VCE_EVENT_NONE:{
                    return;
                }
                case VCE_EVENT_INIT_COMPLETE:{
                    mVideoState = VideoState.AUDIO_ONLY;
                    return;
                }
                case VCE_EVENT_START_ENC:{
                    log("VCE_EVENT_START_ENC, mVideoState" + mVideoState);
                    if(!VideoState.isPaused(mVideoState)){
                        mVideoState = mVideoState | VideoState.TX_ENABLED;
                    }
                    return;
                }
                case VCE_EVENT_START_DEC:{
                    log("VCE_EVENT_START_DEC, mVideoState" + mVideoState);
                    if(!VideoState.isPaused(mVideoState)){
                        mVideoState = mVideoState | VideoState.RX_ENABLED;
                    }
                    return;
                }
                case VCE_EVENT_STOP_ENC:{
                    log("VCE_EVENT_STOP_ENC, mVideoState" + mVideoState);
                    mVideoState = mVideoState & ~VideoState.TX_ENABLED;
                    return;
                }
                case VCE_EVENT_STOP_DEC:{
                    log("VCE_EVENT_STOP_ENC, mVideoState" + mVideoState);
                    mVideoState = mVideoState & ~VideoState.RX_ENABLED;
                    return;
                }
                case VCE_EVENT_SHUTDOWN:{
                    mVideoState = VideoState.AUDIO_ONLY;
                    return;
                }

                default:
                    Log.e(LOG_TAG, "Unknown message type " + msg.what);
                    return;
            }
        }
    }

    public static final int MEDIA_CALLEVENT_CAMERACLOSE = 100;
    public static final int MEDIA_CALLEVENT_CAMERAOPEN = 101;
    public static final int MEDIA_CALLEVENT_STRING = 102;
    public static final int MEDIA_CALLEVENT_CODEC_OPEN = 103;
    public static final int MEDIA_CALLEVENT_CODEC_SET_PARAM_DECODER = 104;
    public static final int MEDIA_CALLEVENT_CODEC_SET_PARAM_ENCODER = 105;
    public static final int MEDIA_CALLEVENT_CODEC_START = 106;
    public static final int MEDIA_CALLEVENT_CODEC_CLOSE = 107;
    public static final int MEDIA_CALLEVENT_MEDIA_START = 108;

    /**
     * Interface definition of a callback to be invoked to communicate some
     * info and/or warning about the h324 or call control.
     */
    public interface OnCallEventListener
    {
        boolean onCallEvent(VTManager vtmgr, int what, Object extra);
    }

    /**
     * Register a callback to be invoked when an info/warning is available.
     *
     * @param listener the callback that will be run
     */
    public void setOnCallEventListener(OnCallEventListener listener)
    {
        mOnCallEventListener = listener;
    }

    private OnCallEventListener mOnCallEventListener;


    public void sendString(String str) {
        Log.d(LOG_TAG, "sendString");

        //mCm.sendVPString(str, null);
        RilVideoEx.sendVPString(mCm, str, null);
    }

    public void controlLocalVideo(boolean bEnable, boolean bReplaceImg) {
        Log.d(LOG_TAG, "controlLocalVideo");

        if (bReplaceImg){
            if (bEnable)
                sendString("open_:camera_");
            else
                sendString("close_:camera_");
        }

        //mCm.controlVPLocalMedia(1, bEnable?1:0, false, null);
        RilVideoEx.controlVPLocalMedia(mCm, 1, bEnable?1:0, false, null);
    }

    public void controlLocalAudio(boolean bEnable) {
        Log.d(LOG_TAG, "controlLocalAudio");

        //mCm.controlVPLocalMedia(0, bEnable?1:0, false, null);
        RilVideoEx.controlVPLocalMedia(mCm, 0, bEnable?1:0, false, null);
    }

    void controlIFrame(boolean bReport, boolean bRequest) {
        Log.d(LOG_TAG, "controlIFrame, bReport: " + bReport + ", bRequest: " + bRequest);

        //mCm.controlIFrame(bReport, bRequest, null);
        RilVideoEx.controlIFrame(mCm, bReport, bRequest, null);
    }

    public boolean isVideoCodecStarted(){
        return getCodecState() == CodecState.CODEC_START;
    }

    public void initCameraResolution(){
        SharedPreferences sharePref = PreferenceManager.getDefaultSharedPreferences(
                mContext.getApplicationContext());
        mCameraResolution = sharePref.getInt("vt_resolution", RESOLUTION_VGA);
        Log.i(LOG_TAG, "initCameraResolution():"+mCameraResolution);
    }

    public int getCameraResolution(){
        Log.i(LOG_TAG, "getCameraResolution():"+mCameraResolution);
        return mCameraResolution;
    }

    private int mVideoState = VideoState.AUDIO_ONLY;
    /**
    * The video state of the call, stored as a bit-field describing whether video transmission and
    * receipt it enabled, as well as whether the video is currently muted.
    */
    private static class VideoState {
        /**
         * Call is currently in an audio-only mode with no video transmission or receipt.
         */
        public static final int AUDIO_ONLY = 0x0;

        /**
         * Video transmission is enabled.
         */
        public static final int TX_ENABLED = 0x1;

        /**
         * Video reception is enabled.
         */
        public static final int RX_ENABLED = 0x2;

        /**
         * Video signal is bi-directional.
         */
        public static final int BIDIRECTIONAL = TX_ENABLED | RX_ENABLED;

        /**
         * Video is paused.
         */
        public static final int PAUSED = 0x4;

        /**
         * Whether the video state is audio only.
         * @param videoState The video state.
         * @return Returns true if the video state is audio only.
         */
        public static boolean isAudioOnly(int videoState) {
            return !hasState(videoState, TX_ENABLED) && !hasState(videoState, RX_ENABLED);
        }

        /**
         * Whether the video transmission is enabled.
         * @param videoState The video state.
         * @return Returns true if the video transmission is enabled.
         */
        public static boolean isTransmissionEnabled(int videoState) {
            return hasState(videoState, TX_ENABLED);
        }

        /**
         * Whether the video reception is enabled.
         * @param videoState The video state.
         * @return Returns true if the video transmission is enabled.
         */
        public static boolean isReceptionEnabled(int videoState) {
            return hasState(videoState, RX_ENABLED);
        }

        /**
         * Whether the video signal is bi-directional.
         * @param videoState
         * @return Returns true if the video signal is bi-directional.
         */
        public static boolean isBidirectional(int videoState) {
            return hasState(videoState, BIDIRECTIONAL);
        }

        /**
         * Whether the video is paused.
         * @param videoState The video state.
         * @return Returns true if the video is paused.
         */
        public static boolean isPaused(int videoState) {
            return hasState(videoState, PAUSED);
        }

        /**
         * Determines if a specified state is set in a videoState bit-mask.
         *
         * @param videoState The video state bit-mask.
         * @param state The state to check.
         * @return {@code True} if the state is set.
         * {@hide}
         */
        private static boolean hasState(int videoState, int state) {
            return (videoState & state) == state;
        }
    }
}

