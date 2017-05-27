package com.android.incallui;

import android.app.AddonManager;
import android.content.Context;

/**
 * Add for shaking phone to start recording.
 */

public class ShakePhoneToStartRecordingHelper {

    private static final String TAG = "ShakePhoneToStartRecordingHelper";
    static ShakePhoneToStartRecordingHelper mInstance;

    public ShakePhoneToStartRecordingHelper() {
    }

    public static ShakePhoneToStartRecordingHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (ShakePhoneToStartRecordingHelper) addonManager.getAddon(
                R.string.shake_phone_to_start_recording_plugin_name,
                ShakePhoneToStartRecordingHelper.class);

        Log.d(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public void init(Context context, CallButtonFragment callButtonFragment) {
    }

    public void unRegisterTriggerRecorderListener() {
    }
}
