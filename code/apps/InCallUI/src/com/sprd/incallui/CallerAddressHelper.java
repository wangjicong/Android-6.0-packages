package com.sprd.incallui;

import com.android.incallui.Log;
import com.android.incallui.R;

import android.content.Context;
import android.app.AddonManager;

/**
 * get caller address for phone number
 */
public class CallerAddressHelper {
    private static final String TAG = "CallerAddressHelper";
    static CallerAddressHelper mInstance;

    public static CallerAddressHelper getsInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (CallerAddressHelper) addonManager.getAddon(R.string.caller_address_plugin_name, CallerAddressHelper.class);
        Log.i(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public CallerAddressHelper() {
    }

    public boolean isSupportCallerAddress() {
        return false;
    }

    public String getCallerAddress(Context context, String number) {
        return "";
    }
}
