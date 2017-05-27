package com.sprd.phone;

import com.android.internal.telephony.TelephonyIntents;
import static com.android.internal.telephony.TelephonyIntents.SECRET_CODE_ACTION;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.net.Uri;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.phone.R;

public class ShowSimLockBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ShowSimLockBroadcastReceiver";

    public ShowSimLockBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String host = null;
        Uri uri = intent.getData();
        if (uri != null) {
            host = uri.getHost();
        } else {
            Log.d(TAG,"uri is null");
            return;
        }
        if("0808".equals(host)){
            handleShowSimlockUnlockBySim(context);
        } else if ("54321".equals(host)) {
            handleOnekeyUnlock(context);
        } else if ("2413".equals(host)) {
            handleShowSimlockUnlockByNv(context);
        } else {
            Log.d(TAG, "Unhandle host[" + host + "]");
        }
    }

    private void handleShowSimlockUnlockBySim(Context context) {
        if (SystemProperties.getBoolean("ro.simlock.unlock.autoshow", true)) {
            Log.d(TAG, "Return for autoshow is turned on.");
            return;
        } else {
            TelephonyManager tm = (TelephonyManager)TelephonyManager.from(context);
            if (tm != null) {
                int simlockSlotFlag = 0;
                int phoneCount = tm.getPhoneCount();
                for (int i = 0; i < phoneCount; i++) {
                    if (tm.checkSimLocked(i)) {
                        simlockSlotFlag |= (1 << i);
                    }
                }
                Log.d(TAG, "simlockSlotFlag = " + simlockSlotFlag);
                if (simlockSlotFlag != 0) {
                    Intent intent = new Intent(TelephonyIntents.SHOW_SIMLOCK_UNLOCK_SCREEN_ACTION);
                    intent.putExtra(TelephonyIntents.EXTRA_SIMLOCK_UNLOCK, simlockSlotFlag);
                    context.sendBroadcast(intent);
                } else {
                    Toast.makeText(context,
                            context.getString(R.string.sim_lock_none),
                            Toast.LENGTH_LONG).show();
                }
            } else {
                    Toast.makeText(context,
                            context.getString(R.string.sim_lock_try_later),
                            Toast.LENGTH_LONG).show();
            }
        }
    }

    private void handleOnekeyUnlock(Context context) {
        if(!SystemProperties.getBoolean("ro.simlock.onekey.lock", false)) {
            Log.d(TAG, "Return for onekey unlock is turn off.");
            return;
        }
        Intent intentForSimLock = new Intent();
        intentForSimLock.setClass(context, ChooseSimLockTypeActivity.class);
        intentForSimLock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentForSimLock);
        Log.d(TAG,"handleSimNetworkLock");
    }

    private void handleShowSimlockUnlockByNv(Context context) {
        if (!SystemProperties.getBoolean("ro.simlock.unlock.bynv", false)) {
            Log.d(TAG, "Return for showing unlock screen by nv is turned off.");
            return;
        } else {
            TelephonyManager tm = (TelephonyManager)TelephonyManager.from(context);
            if (tm != null) {
                boolean isNetworkLock = tm.getSimLockStatus(TelephonyManager.UNLOCK_NETWORK);
                boolean isNetworkSubsetLock = tm.getSimLockStatus(TelephonyManager.UNLOCK_NETWORK_SUBSET);
                boolean isServiceProviderLock = tm.getSimLockStatus(TelephonyManager.UNLOCK_SERVICE_PORIVDER);
                boolean isCorporateLock = tm.getSimLockStatus(TelephonyManager.UNLOCK_CORPORATE);
                boolean isSimLock = tm.getSimLockStatus(TelephonyManager.UNLOCK_SIM);

                if (isNetworkLock || isNetworkSubsetLock || isServiceProviderLock || isCorporateLock || isSimLock) {
                    Intent intent = new Intent(TelephonyIntents.SHOW_SIMLOCK_UNLOCK_SCREEN_BYNV_ACTION);
                    context.sendBroadcast(intent);
                    Log.d(TAG, "handleShowSimlockUnlockByNv has sent broadcast.");
                } else {
                    Toast.makeText(context,
                            context.getString(R.string.simlock_unlocked),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context,
                        context.getString(R.string.sim_lock_try_later),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
