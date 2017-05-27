
package com.sprd.dialer;

import com.android.dialer.R;

import android.util.Log;
import android.app.AddonManager;
import android.content.Context;

public class DialerPlugin {

    private static final String TAG = "DialerPlugin";
    static DialerPlugin sInstance;

    public static DialerPlugin getInstance() {
        log("getInstance()");
        if (sInstance == null) {
            AddonManager addonManager = AddonManager.getDefault();
            sInstance = (DialerPlugin) addonManager.getAddon(R.string.dialer_plugin,
                    DialerPlugin.class);
            log("getInstance [" + sInstance + "]");
        }
        return sInstance;
    }

    public DialerPlugin() {
    }

    public boolean callLogDetailShowDuration() {
        log("callLogDetailShowDuration");
        return true;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
