package com.sprd.telecom.plugin;

import android.app.AddonManager;
import android.content.Context;

import com.android.server.telecom.Log;
import com.android.server.telecom.R;

public class TelecomPluginsHelper {

    private static final String TAG = "TelecomPluginHelper";
    static TelecomPluginsHelper sInstance;

    public static TelecomPluginsHelper getInstance(Context context) {
        Log.d(TAG, "getInstance...... ");
        if (sInstance == null) {
            AddonManager addonManager = new AddonManager(context);
            sInstance = (TelecomPluginsHelper) addonManager.getAddon(R.string.telecom_plugin_name,
                    TelecomPluginsHelper.class);
            Log.d(TAG, "getInstance [" + sInstance + "]");
        }
        return sInstance;
    }

    public TelecomPluginsHelper() {
    }

    public boolean shouldLogEmergencyCalls(Context context) {
        boolean okToLogEmergencyNumber = context.getResources().getBoolean(
                R.bool.allow_emergency_numbers_in_call_log);
        return okToLogEmergencyNumber;
    }
}
