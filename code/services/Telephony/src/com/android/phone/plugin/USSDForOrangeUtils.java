package com.android.phone.plugin;

import android.app.AddonManager;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.android.internal.R;

public class USSDForOrangeUtils {

    private static USSDForOrangeUtils mInstance;
    private static final String LOG_TAG = "USSDUtils";

    public USSDForOrangeUtils() {
        // TODO Auto-generated constructor stub
    }

    public static USSDForOrangeUtils getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = AddonManager.getDefault();
        mInstance = (USSDForOrangeUtils) addonManager.getAddon(
                R.string.ussd_feature_for_orange,
                USSDForOrangeUtils.class);
        Log.d(LOG_TAG, "mInstance = " + mInstance);
        return mInstance;
    }

    public void setSpan(Context context, TextView tv) {
        Log.d(LOG_TAG, "setSpan on the default addon, do nothing");
    }

    public boolean isSupportUSSDCall() {
        return false;
    }
}
