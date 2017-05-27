package com.android.incallui;

import android.app.AddonManager;
import android.content.Context;

/*
* Add for hands-free switch to headset
* */
public class SpeakerToHeadsetHelper {

    private static final String TAG = "SpeakerToHeadsetHelper";
    static SpeakerToHeadsetHelper mInstance;

    public SpeakerToHeadsetHelper() {
    }

    public static SpeakerToHeadsetHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (SpeakerToHeadsetHelper) addonManager.getAddon(
                R.string.speaker_switch_to_headset_plugin_name, SpeakerToHeadsetHelper.class);
        Log.d(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public void init(Context context, CallButtonFragment callButtonFragment) {
    }

    public void unRegisterSpeakerTriggerListener() {
    }
}