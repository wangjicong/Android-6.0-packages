
package com.sprd.firewall;

import java.util.Date;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ReceiverCallState extends BroadcastReceiver {
    private static final String TAG = "ReceiverCallState";

    private static final boolean VERBOSE = true;

    static final Object mStartingServiceSync = new Object();

    static PowerManager.WakeLock mStartingService;

    private Context mContext;

    private Intent mIntent;

    private TelephonyManager telMgr;

    final int answer_type = 2;

    final int AUTO_HANG_UP = 0;

    final int WHITE_LIST = 1;

    final int BLACK_LIST = 2;

    private static CallStateListener mCallStateListener = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (VERBOSE)
            Log.v(TAG, "onReceive");
    }

    public class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if (VERBOSE)
                Log.v(TAG, "incomingNumber is " + incomingNumber);
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (VERBOSE)
                            Log.v(TAG, "CALL_STATE_RINGING");
                        switch (answer_type) {
                            case AUTO_HANG_UP:
                                if (VERBOSE)
                                    Log.v(TAG, "AUTO_HANG_UP");
                                PhoneUtils.getITelephony(telMgr).endCall();
                                break;
                            case WHITE_LIST:
                                if (VERBOSE)
                                    Log.v(TAG, "WHITE_LIST");
                                break;

                            case BLACK_LIST:
                                if (VERBOSE)
                                    Log.v(TAG, "BLACK_LIST model");
                                if (PhoneUtils.CheckIsBlockNumber(mContext, incomingNumber, false,
                                        false)) {

                                    if (VERBOSE)
                                        Log.v(TAG,
                                                "incomingNumber belonged to blacklist so end call");
                                    PhoneUtils.getITelephony(telMgr).endCall();
                                    PhoneUtils.putToBlockList(mContext, incomingNumber,
                                            (new Date()).getTime());

                                    // deal with by service
                                    mIntent.setClass(mContext, ProcessIncomingService.class);
                                    mIntent.putExtra("incomingNumber", incomingNumber);
                                    beginStartingService(mContext, mIntent);
                                } else {
                                    Log.d(TAG, "not in black list");
                                    return;
                                }
                                break;
                        }
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (VERBOSE)
                            Log.v(TAG, "CALL_STATE_OFFHOOK");
                        break;
                }

            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void beginStartingService(Context context, Intent intent) {
        synchronized (mStartingServiceSync) {
            if (mStartingService == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "StartingAlertService");
                mStartingService.setReferenceCounted(false);
            }
            mStartingService.acquire();
            Log.v(TAG, "startService");
            context.startService(intent);
        }
    }

    public static void finishStartingService(ProcessIncomingService processIncomingService,
            int serviceId) {
        synchronized (mStartingServiceSync) {
            if (mStartingService != null) {
                if (processIncomingService.stopSelfResult(serviceId)) {
                    mStartingService.release();
                    mCallStateListener = null;
                }
            }
        }
        Log.v(TAG, "finishStartingService finish");
    }

}
