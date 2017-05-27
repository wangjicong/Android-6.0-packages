package com.sprd.incallui;

import com.android.incallui.Log;
import com.android.incallui.R;

import android.content.Context;
import android.app.AddonManager;

/**
 * Support telcel requirements in InCallUI.
 */
public class InCallUITelcelHelper {
    private static final String TAG = "InCallUITelcelHelper";
    static InCallUITelcelHelper mInstance;

    public static InCallUITelcelHelper getsInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (InCallUITelcelHelper) addonManager.getAddon(
                R.string.incallui_telcel_plugin_name, InCallUITelcelHelper.class);
        Log.i(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public InCallUITelcelHelper() {
    }

    public boolean isVoiceClearCodeLabel(Context context, String callStateLabel) {
        return false;
    }

    public boolean isSpecialVoiceClearCode(String number) {
        return false;
    }

}
