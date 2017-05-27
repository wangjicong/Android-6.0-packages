package com.android.providers.telephony.ext.simmessage;


import com.android.internal.telephony.IccCardConstants;
import com.android.providers.telephony.MmsSmsDatabaseHelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ProvidersReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            Log.v(TAG, "onReceiver android.intent.action.BOOT_COMPLETED");

        } else if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
            Log.v(TAG, "onReceiver android.intent.action.SIM_STATE_CHANGED");
            //check SIM card state
//            GetSimState(context);
            //check SIM loader
            String simStates = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simStates)) {
                Log.v(TAG, "INTENT_VALUE_ICC_LOADED");
                ICCMessageManager.getInstance().delayReadIcc();
            }
        }
    }

    public void GetSimState(Context context) {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Log.v(TAG, "GetSimState:[" + manager.getSimState() + "]");
        Status.GetInstance().SetICCStatus(manager.getSimState());
    }

    private static final String TAG = "ProvidersReceiverForSim";
}

