
package com.android.services.telephony.plugin;

import com.android.internal.telephony.Connection;
import android.app.Activity;
import com.android.internal.telephony.Call;

import android.app.AddonManager;
import android.content.Context;
import android.content.Intent;
import com.android.internal.R;
import com.android.phone.PhoneGlobals;
import com.android.internal.telephony.CallManager;

import android.util.Log;
import android.telephony.Rlog;

public class BlockIncomingCallNotifierUtils {

    private static final String LOGTAG = "BlockIncomingCallNotifierUtils";
    static BlockIncomingCallNotifierUtils sInstance;

    public static BlockIncomingCallNotifierUtils getInstance(Context context) {
        if (sInstance != null) return sInstance;
        Log.d(LOGTAG, "BlockIncomingCallNotifierUtils getInstance");
        AddonManager addonManager = new AddonManager(context);
        sInstance = (BlockIncomingCallNotifierUtils) addonManager.getAddon(
                R.string.feature_Firewall_phone_intercept, BlockIncomingCallNotifierUtils.class);
        Log.d(LOGTAG,
                "BlockIncomingCallNotifierUtils getInstance: plugin = "
                        + context.getString(R.string.feature_Firewall_phone_intercept));
        return sInstance;
    }

    public BlockIncomingCallNotifierUtils() {
    }

    public void plugin_phone(Connection connection, Call call) {

        Log.d(LOGTAG, "Don't join AddonBlockIncomingCallNotifierUtils ");
    }

}
