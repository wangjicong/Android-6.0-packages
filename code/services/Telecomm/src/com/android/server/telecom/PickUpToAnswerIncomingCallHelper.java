package com.android.server.telecom;

import android.app.AddonManager;
import android.content.Context;

/**
 * Handle registering and unregistering SPRDHUB_PICKUP sensor relating to call state for answer
 * incoming call automatically.
 */
public class PickUpToAnswerIncomingCallHelper extends CallsManagerListenerBase {

    private static final String TAG = "PickUpToAnswerIncomingCallHelper";
    static PickUpToAnswerIncomingCallHelper mInstance;

    public PickUpToAnswerIncomingCallHelper() {
    }

    public static PickUpToAnswerIncomingCallHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (PickUpToAnswerIncomingCallHelper) addonManager.getAddon(
                R.string.pickup_to_answer_incoming_call_plugin_name,
                PickUpToAnswerIncomingCallHelper.class);

        Log.d(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public void init(Context context, CallsManager callsManager) {
    }
}