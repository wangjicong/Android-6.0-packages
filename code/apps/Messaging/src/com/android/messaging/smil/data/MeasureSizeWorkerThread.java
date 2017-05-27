
package com.android.messaging.smil.data;

import com.android.messaging.datamodel.data.DraftMessageData;
import com.android.messaging.smil.ui.SmilMainFragment;
import com.android.messaging.sms.MmsUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class MeasureSizeWorkerThread extends HandlerThread implements Callback {

    private static final String TAG = MeasureSizeWorkerThread.class.getSimpleName();

    private static Handler sMainHandler;
    private Context mContext;
    private static Handler sSubHandler;
    private static MeasureSizeWorkerThread mMeasureSizeWorkerThread;

    public static final int ACTION_QUIT_THREAD = 0x0;
    public static final int ACTION_REFRESH_SIZE = 0x1;

    public static final int MAX_ATTACHMENT_BYTES = 297 * 1024;

    public MeasureSizeWorkerThread(String name) {
        super(name);
    }

    public MeasureSizeWorkerThread(Context context, Handler mainHandler, String name) {
        this(name);
        this.mContext = context;
        this.sMainHandler = mainHandler;
    }

    /**
     * when smil edit start, run this thread.
     * 
     * @param
     */
    public static Handler initMeasureSizeThread(Context context, Handler mainHandler) {

        mMeasureSizeWorkerThread = new MeasureSizeWorkerThread(context, mainHandler,
                "MeasureSizeThread");
        mMeasureSizeWorkerThread.start();
        sSubHandler = new Handler(mMeasureSizeWorkerThread.getLooper(), mMeasureSizeWorkerThread);
        return sSubHandler;
    }

    public static  MeasureSizeWorkerThread getMeasureSizeWorkerThread(){
        return mMeasureSizeWorkerThread;
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d("tim-s", "MeasureSizeWorkerThread.handleMessage ThreadId = "
                + Thread.currentThread().getId());
        switch (msg.what) {
            case ACTION_QUIT_THREAD:
                Log.d("tim-s", "MeasureSizeWorkerThread.handleMessage: ACTION_QUIT_THREAD");
                if (sSubHandler != null && sSubHandler.getLooper() != null) {
                    sSubHandler.getLooper().quit();
                    sSubHandler = null;
                }
                break;

            case ACTION_REFRESH_SIZE:
                Log.d("tim-s", "MeasureSizeWorkerThread.handleMessage: ACTION_REFRESH_SIZE");
                if (sSubHandler != null && sSubHandler.hasMessages(ACTION_REFRESH_SIZE)) {
                    Log.d("tim-s",
                            "MeasureSizeWorkerThread.handleMessage: remove all ACTION_REFRESH_SIZE message.");
                    sSubHandler.removeMessages(ACTION_REFRESH_SIZE);
                }
                measureDraftAttachmentsSize(mContext);
                break;
            default:
                Log.d("tim-s", "MeasureSizeWorkerThread.handleMessage: Unknow message type.");
                break;
        }

        return true;
    }
    
    

    /**
     * measure draft attachments size, the result should be less than 300K.
     * 
     * @param
     */
    private static int measureDraftAttachmentsSize(Context context) {
        /* Modify by SPRD for bug 562014 2016.05.19 Start */
        DraftMessageData draftMsgData = mMessageDateImp.getCurrentDraftMessageData();
        int length = MmsUtils.getAttachmentsLength(context, draftMsgData);
        /* Modify by SPRD for bug 562014 2016.05.19 End */

        if (sMainHandler != null && length > MAX_ATTACHMENT_BYTES) {
            sMainHandler.sendEmptyMessage(SmilMainFragment.OUT_OF_SIZE);
            Log.d("tim-s", "MeasureSizeWorkerThread.measureDraftAttachmentsSize: over 297KB.");
        }
        Log.d("tim-s", "MeasureSizeWorkerThread.measureDraftAttachmentsSize: length = " + length);
        draftMsgData = null;
        return length;
    }
    
    private static DrafTMessageDateImp mMessageDateImp;
    
    public void setDrafTMessageDateImp (DrafTMessageDateImp dateImp){
        mMessageDateImp = dateImp;
    }
    
    public interface DrafTMessageDateImp {

        public DraftMessageData getCurrentDraftMessageData();
    }

}
