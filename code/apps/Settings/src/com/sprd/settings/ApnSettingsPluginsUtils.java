package com.sprd.settings;

import android.app.AddonManager;
import android.content.Context;
import android.util.Log;
import com.android.settings.R;

public class ApnSettingsPluginsUtils {

    private static ApnSettingsPluginsUtils mInstance;
    private static final String LOG_TAG = "ApnSettingsPluginsUtils";

    public ApnSettingsPluginsUtils() {
    }

    public static ApnSettingsPluginsUtils getInstance(Context context) {
        if (mInstance != null) return mInstance;
        Log.d(LOG_TAG,"mInstance==null context.getPackageName(): " + context.getPackageName());
        AddonManager addonManager = new AddonManager(context);
        mInstance = (ApnSettingsPluginsUtils) addonManager.getAddon(
                R.string.feature_apn_settings, ApnSettingsPluginsUtils.class);
        return mInstance;
    }

    public boolean apnSettingVisibility(String mccmnc, String type, boolean isSupportVolte) {
         return true;
    }

    public boolean getIsNeedSetChecked(String mccmnc, String apn) {
        return true;
    }
}