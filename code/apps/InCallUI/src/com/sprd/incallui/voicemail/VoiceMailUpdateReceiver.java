package com.sprd.incallui.voicemail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.incallui.CallerInfoUtils;

import android.text.TextUtils;
import android.util.Log;

public class VoiceMailUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "VoiceMailUpdateReceiver";
    private static final String ACTION_SIM_STATE_CHANGED
            = "android.intent.action.SIM_STATE_CHANGED";
    private static final String ACTION_VM_SETTING_CHANGED
            = "android.intent.action.VM_SETTING_CHANGED";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive Action: " + action);
        if (ACTION_SIM_STATE_CHANGED.equals(action)) {
            String simStatus = intent.getStringExtra("ss");
            if (TextUtils.equals(simStatus, "LOADED") || TextUtils.equals(simStatus, "ABSENT")) {
                refreshVoiceMailCache();
            }
        } else if (ACTION_VM_SETTING_CHANGED.equals(action)) {
            Log.d(TAG, "refresh voice mail cache, reason: SETTING");
            refreshVoiceMailCache();
        } else {
            Log.w(TAG, "onReceive: can not handle: " + intent);
        }
    }

    private void refreshVoiceMailCache() {
        CallerInfoUtils.refreshVoiceMailCache();
    }
}
