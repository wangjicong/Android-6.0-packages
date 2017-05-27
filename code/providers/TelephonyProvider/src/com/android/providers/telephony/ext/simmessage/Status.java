package com.android.providers.telephony.ext.simmessage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Status {
    private Status() {
        SetICCStatus(STATUS_UNKNOW);
        SetFirstInit(true);
        SetSimLoadStatus(STATUS_UNKNOW);
    }

    public synchronized static Status GetInstance() {
        if (mInsStatus == null) {
            mInsStatus = new Status();
        }
        return mInsStatus;
    }

    public synchronized static void Release() {
        mInsStatus = null;
    }

    public void SetICCStatus(int nStatus) {
        mIccStatus = nStatus;
    }

    public int GetICCStatus() {
        return mIccStatus;
    }

    public int GetSimLoadStatus() {
        return mSimLoadStatus;
    }

    public void SetSimLoadStatus(int nStatus) {
        mSimLoadStatus = nStatus;
    }

    public boolean IsFirstInit() {
        return mbFirstInit;
    }

    public void SetFirstInit(boolean bFirst) {
        mbFirstInit = bFirst;
    }

    public boolean CanDML() {
        return (IsSimReady() && STATUS_FINISH == GetSimLoadStatus());
    }

    public boolean Waiting(int nSecond) {
        synchronized (this) {
            try {
                wait(nSecond);
                return CanDML();
            } catch (Exception e) {
                return false;
            }
        }
    }

    public boolean IsSimReady() {
        return (GetICCStatus() == TelephonyManager.SIM_STATE_READY);
    }

    public void CheckCanLoader() {
        // avoid load message when sim leak
        if (IsFirstInit()) {
            LoadSimMessageRunnable loadSim = new LoadSimMessageRunnable();
            loadSim.run();
        }
    }


    protected void SendMessage() {
        //Message msg=GetSimLoadHandler().obtainMessage(END_LOAD_SIM_MESSAGE);
        //GetSimLoadHandler().sendMessageDelayed(msg, 100);
    }

    private static final int START_LOAD_SIM_MESSAGE = 0x00000001;
    private static final int END_LOAD_SIM_MESSAGE = 0x00000002;


    public static final int STATUS_UNKNOW = 0xFFFFFFFF;
    public static final int STATUS_LOADING = 0x00000001;
    public static final int STATUS_FINISH = 0x00000002;

    private int mIccStatus;      // SIM Status
    private int mSimLoadStatus;  // SIM sms Load status
    private boolean mbFirstInit;
    private static Status mInsStatus = null;
    private final static String TAG = "Status";

}
