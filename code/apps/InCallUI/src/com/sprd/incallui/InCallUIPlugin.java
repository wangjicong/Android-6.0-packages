package com.sprd.incallui;

import com.android.dialer.R;
import com.android.phone.common.animation.AnimUtils;

import android.util.Log;
import android.app.AddonManager;
import android.widget.TextView;

public class InCallUIPlugin {

    private static final String TAG = "InCallUIPlugin";
    static InCallUIPlugin sInstance;

    public static InCallUIPlugin getInstance() {
        log("getInstance()");
        if (sInstance == null) {
            AddonManager addonManager = AddonManager.getDefault();
            sInstance = (InCallUIPlugin) addonManager.getAddon(R.string.incallui_plugin, InCallUIPlugin.class);
            log("getInstance [" + sInstance + "]");
        }
        return sInstance;
    }

    public InCallUIPlugin() {
    }

    public void setPrimaryCallElapsedTime(TextView elapsedTimeT) {
        log("setPrimaryCallElapsedTime");
        AnimUtils.fadeOut(elapsedTimeT, AnimUtils.DEFAULT_DURATION);
    }

    public boolean isOnlyDislpayActiveCall() {
        log("isOnlyDislpayActiveCall");
        return false;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}