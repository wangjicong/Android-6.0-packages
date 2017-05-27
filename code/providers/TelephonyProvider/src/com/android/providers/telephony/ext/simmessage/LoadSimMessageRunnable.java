package com.android.providers.telephony.ext.simmessage;

import android.util.Log;

public class LoadSimMessageRunnable implements Runnable {
    @Override
    public void run() {
        Log.d(TAG, "Enter LoadSimMessageRunnable ");
        ICCMessageManager.getInstance().InitEnv();
        //Status.GetInstance().SetSimLoadStatus(Status.STATUS_FINISH);
        Status.GetInstance().SetFirstInit(false);
//        Status.GetInstance().SendMessage();
        Log.d(TAG, "Leave   LoadSimMessageRunnable ");
    }

    private static final String TAG = "LoadSimMessageRunnable";
}
