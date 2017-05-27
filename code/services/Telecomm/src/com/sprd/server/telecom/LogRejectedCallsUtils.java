/** Created by Spreadst */
package com.sprd.server.telecom;

import com.android.server.telecom.R;

import android.app.AddonManager;
import android.content.Context;
import android.provider.CallLog.Calls;
import com.android.server.telecom.CallState;
import android.telecom.DisconnectCause;

public class LogRejectedCallsUtils {

    static LogRejectedCallsUtils sInstance;

    public static LogRejectedCallsUtils getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        sInstance = (LogRejectedCallsUtils) addonManager.getAddon(
                R.string.log_rejected_calls_plugin, LogRejectedCallsUtils.class);
        return sInstance;
    }

    public LogRejectedCallsUtils() {
    }

    public int getCallLogType(boolean isIncoming, int disconnectCause) {
        int type;
        if (!isIncoming) {
            type = Calls.OUTGOING_TYPE;
        } else if (disconnectCause == DisconnectCause.MISSED) {
            type = Calls.MISSED_TYPE;
        } else {
            type = Calls.INCOMING_TYPE;
        }
        return type;
    }

    public boolean shouldShowMissedCallNotification(int disConnectCause,
            int oldState, int newState) {
        if (oldState == CallState.RINGING && newState == CallState.DISCONNECTED
                && (disConnectCause == DisconnectCause.MISSED)) {
            return true;
        } else {
            return false;
        }
    }
}
