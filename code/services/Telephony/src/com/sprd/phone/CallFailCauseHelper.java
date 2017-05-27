package com.sprd.phone;

import android.app.AddonManager;
import android.content.Context;

import com.android.phone.R;
import com.android.services.telephony.Log;

/**
 * Handle call fail cause
 */
public class CallFailCauseHelper {
    private static String TAG = "CallFailCauseHelper";
    static CallFailCauseHelper mInstance;

    public CallFailCauseHelper() {

    }

    public static CallFailCauseHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (CallFailCauseHelper) addonManager
                .getAddon(R.string.call_fail_cause_plugin_name, CallFailCauseHelper.class);
        Log.i(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public boolean isSupportCallFailCause() {
        return false;
    }

    public void parserAttributeValue(Context context) {

    }

    public String getAttributeValue(Integer causeNumber) {
        return "";
    }

}
