/*
 * Copyright (C) 2013 Spreadtrum Communications Inc.
 *
 */

package com.sprd.incallui;

import com.android.incallui.Call;
import com.android.incallui.R;
import com.android.internal.telephony.Phone;

import android.telecom.PhoneAccountHandle;
import android.telecom.Call.Details;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.app.AddonManager;
import android.content.Context;
import android.graphics.drawable.Drawable;

public class InCallUIHdVoicePlugin {

    private static final String TAG = "[InCallUIHdVoicePlugin]";
    static InCallUIHdVoicePlugin sInstance;

    public static InCallUIHdVoicePlugin getInstance() {
        log("getInstance()");
        if (sInstance == null) {
            AddonManager addonManager = AddonManager.getDefault();
            sInstance = (InCallUIHdVoicePlugin) addonManager.getAddon(R.string.incallui_hd_voice_plugin, InCallUIHdVoicePlugin.class);
            log("getInstance ["+sInstance+"]");
        }
        return sInstance;
    }

    public InCallUIHdVoicePlugin() {
    }

    public Drawable getCallStateIcon(Context context) {
        log("getCallStateIcon");
        return context.getDrawable(R.drawable.ic_hd_24dp);
    }

    public void removeHdVoiceIcon () {
        //do nothing
    }

    public void showHdAudioIndicator(ImageView view, Call call, Context context) {
        if (call == null) {
            return;
        }
        view.setBackground(getCallStateIcon(context));
        boolean shouldShowHdAudioIndicator = (call != null && call.hasProperty(Details.PROPERTY_HIGH_DEF_AUDIO));
        view.setVisibility(shouldShowHdAudioIndicator ? View.VISIBLE : View.GONE);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
