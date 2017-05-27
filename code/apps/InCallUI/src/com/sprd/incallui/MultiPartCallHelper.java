package com.sprd.incallui;

import android.app.AddonManager;
import android.content.Context;

import com.android.incallui.Log;
import com.android.incallui.R;

public class MultiPartCallHelper {

    private static final String TAG = MultiPartCallHelper.class.getSimpleName();
    /**
     * MPC (Multi-Part-Call) mode: hang up background call and accept ringing/waiting call.
     */
    public static final int MPC_MODE_HB = 0;

    /**
     * MPC (Multi-Part-Call) mode: hang up foreground call and accept ringing/waiting call.
     */
    public static final int MPC_MODE_HF = 1;

    /**
     * MPC (Multi-Part-Call) mode: hang up background & foreground call and accept ringing/waiting call.
     */
    public static final int MPC_MODE_HBF = 2;

    static MultiPartCallHelper mInstance;

    public static MultiPartCallHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (MultiPartCallHelper) addonManager.getAddon(
                R.string.mutil_part_call_plugin_name, MultiPartCallHelper.class);
        Log.i(TAG, "getInstance [" + mInstance + "]");

        return mInstance;
    }

    public boolean isSupportMultiPartCall() {
        return false;
    }

}
