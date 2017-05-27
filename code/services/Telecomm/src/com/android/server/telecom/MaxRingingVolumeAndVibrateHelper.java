package com.android.server.telecom;

import android.app.AddonManager;
import android.content.Context;

/**
 * Handle registering and unregistering POCKET_MODE sensor relating to call state for
 * MaxRingingVolume and vibrate.
 */
public class MaxRingingVolumeAndVibrateHelper extends CallsManagerListenerBase {

    private static final String TAG = "MaxRingingVolumeAndVibrateHelper";
    static MaxRingingVolumeAndVibrateHelper mInstance;

    public MaxRingingVolumeAndVibrateHelper() {
    }

    public static MaxRingingVolumeAndVibrateHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (MaxRingingVolumeAndVibrateHelper) addonManager.getAddon(
                R.string.max_ringing_volume_plugin_name, MaxRingingVolumeAndVibrateHelper.class);

        Log.d(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public void init(Context context, CallsManager callsManager, Ringer ringer) {
    }
}