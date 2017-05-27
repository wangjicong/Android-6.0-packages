
package com.sprd.firewall;

import java.util.Date;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class FireWallApp extends Application {
    private static final String TAG = "FireWallApp";

    private static final boolean VERBOSE = true;

    private TelephonyManager telMgr;

    static final Object mStartingServiceSync = new Object();

    static PowerManager.WakeLock mStartingService;

    @Override
    public void onCreate() {
        super.onCreate();
        if (VERBOSE)
            Log.v(TAG, "onCreate");
        if (telMgr == null) {
            telMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    public class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            if (incomingNumber == null || incomingNumber.length() == 0) {
                return;
            }
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (VERBOSE) {
                            Log.v(TAG, "CALL_STATE_RINGING");
                            Log.v(TAG, "incomingNumber is " + incomingNumber);
                        }
                        if (PhoneUtils.CheckIsBlockNumber(FireWallApp.this, incomingNumber, false,
                                false)) {
                            if (VERBOSE)
                                Log.v(TAG, "belonged to blacklist so end call");
                            PhoneUtils.getITelephony(telMgr).endCall();
                            PhoneUtils.putToBlockList(FireWallApp.this, incomingNumber,
                                    (new Date()).getTime());
                            Intent intent = new Intent();
                            intent.setClass(FireWallApp.this, ProcessIncomingService.class);
                            intent.putExtra("incomingNumber", incomingNumber);
                            beginStartingService(FireWallApp.this, intent);
                        } else {
                            Log.d(TAG, "not in black list");
                            return;
                        }
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if (VERBOSE)
                            Log.v(TAG, "CALL_STATE_OFFHOOK");
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void beginStartingService(Context context, Intent intent) {
        if (VERBOSE)
            Log.v(TAG, "beginStartingService");
        synchronized (mStartingServiceSync) {
            if (VERBOSE)
                Log.v(TAG, "mStartingServiceSync");
            if (mStartingService == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "StartingAlertService");
                mStartingService.setReferenceCounted(false);
            }
            mStartingService.acquire();
            if (VERBOSE)
                Log.v(TAG, "startService");
            context.startService(intent);
        }
    }

    public static void finishStartingService(ProcessIncomingService processIncomingService,
            int serviceId) {
        if (VERBOSE)
            Log.v(TAG, "finishStartingService start");
        if (mStartingService != null) {
            if (processIncomingService.stopSelfResult(serviceId)) {
                mStartingService.release();
            }
        }
        if (VERBOSE)
            Log.v(TAG, "finishStartingService finish");
    }
}
