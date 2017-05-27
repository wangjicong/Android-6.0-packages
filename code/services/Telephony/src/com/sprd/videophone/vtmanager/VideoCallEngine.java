package com.sprd.videophone.vtmanager;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class VideoCallEngine {
    private static String TAG = VideoCallEngine.class.getSimpleName();
    private Handler mEventHandler;

    static {
        System.loadLibrary("video_call_engine_jni");
    }

    public VideoCallEngine(Handler eventHandler) {
        mEventHandler = eventHandler;
        Log.i(TAG, "VideoCallEngine create.");
        init();
        postEventFromNative(new WeakReference(this), 0, 0, 0, null);
    }

    private static void postEventFromNative(Object vce_ref,
            int what, int arg1, int arg2, Object obj) {
        if (vce_ref == null) {
            return;
        }
        what = what + 1000;
        Log.i(TAG, "postEventFromNative what=" + what);

        VideoCallEngine vce = (VideoCallEngine) ((WeakReference) vce_ref).get();
        if (vce.mEventHandler != null) {
            Message msg = vce.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            vce.mEventHandler.sendMessage(msg);
        }
    }

    private int mLocalNativeSurfaceTexture; // accessed by native methods
    private int mRemoteNativeSurfaceTexture; // accessed by native methods

    public static native void init();

    public static native void setup(Object weak_this);

    public static native void reset();

    public static native void release();

    public static native void jfinalize();

    public static native void setRemoteSurface(Object surface);

    public static native void setLocalSurface(Object surface);

    public static native void setCamera(Object camera, int resolution);

    public static native void prepare();

    public static native void startUplink();

    public static native void stopUplink();

    public static native void startDownlink();

    public static native void stopDownlink();

    public static native void setUplinkImageFileFD(Object fileDescriptor, long offset, long length);

    public static native void selectRecordSource(int source);

    public static native void selectRecordFileFormat(int format);

    public static native void startRecord();

    public static native void stopRecord();

    public static native void setRecordFileFD(Object fileDescriptor, long offset, long length);

    public static native void setRecordMaxFileSize(long max_filesize_bytes);
}
