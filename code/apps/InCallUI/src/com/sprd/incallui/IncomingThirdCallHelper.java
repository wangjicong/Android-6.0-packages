package com.sprd.incallui;

import com.android.incallui.Log;
import com.android.incallui.NeededForReflection;
import com.android.incallui.R;

import android.content.Context;
import android.app.AddonManager;

/**
 * Deal Incoming third call issue
 */
@NeededForReflection
public class IncomingThirdCallHelper {
    private static final String TAG = "IncomingThirdCallHelper";
    static IncomingThirdCallHelper mInstance;

    public static IncomingThirdCallHelper getsInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (IncomingThirdCallHelper) addonManager.getAddon(
                R.string.incoming_third_call_plugin_name,
                IncomingThirdCallHelper.class);
        Log.i(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public IncomingThirdCallHelper() {
    }

    public void dismissHangupCallDialog() {
        /* SPRD: Do nothing @{ */
    }
    public void dealIncomingThirdCall(Context context, boolean show) {
        /* SPRD: Do nothing @{ */
    }

    public boolean isSupportIncomingThirdCall() {
        return false;
    }

}
