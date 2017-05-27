package com.android.server.telecom;

import android.content.Context;
import android.app.AddonManager;

/**
 * Handle registering and unregistering SPRDHUB_PICKUP sensor relating to call state for fading down
 * ringtone to vibrate.
 */

public class FadeDownRingtoneToVibrateHelper extends CallsManagerListenerBase {

    private static final String TAG = "FadeDownRingtoneToVibrateHelper";
    static FadeDownRingtoneToVibrateHelper mInstance;

    public FadeDownRingtoneToVibrateHelper() {}

    public static FadeDownRingtoneToVibrateHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (FadeDownRingtoneToVibrateHelper) addonManager.getAddon(R.string.fade_down_ringtone_to_vibrate_plugin_name,FadeDownRingtoneToVibrateHelper.class);
        Log.d(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public void init (Context context, CallsManager callsManager, Ringer ringer) {
    }
}
