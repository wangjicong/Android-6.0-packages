package com.android.server.telecom;

import android.content.Context;
import android.app.AddonManager;

/**
 * Handle registering and unregistering accelerometer sensor relating to call state for muting incoming call.
 */

public class FlipToMuteHelper extends CallsManagerListenerBase {

    private static final String TAG = "FlipToMuteHelper";
    static FlipToMuteHelper mInstance;

    public FlipToMuteHelper() {
    }

    public static FlipToMuteHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (FlipToMuteHelper) addonManager.getAddon(
                R.string.flip_to_mute_plugin_name, FlipToMuteHelper.class);
        Log.d(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public void init(Context context, CallsManager callsManager, Ringer ringer) {
    }
}