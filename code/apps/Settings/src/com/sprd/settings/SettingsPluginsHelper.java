package com.sprd.settings;

import android.app.AddonManager;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.sim.SimDialogActivity;

public class SettingsPluginsHelper {
    static SettingsPluginsHelper mInstance;
    public SettingsPluginsHelper() {
    }

    public static SettingsPluginsHelper getInstance() {
        if (mInstance == null) {
            mInstance = (SettingsPluginsHelper) AddonManager.getDefault()
                    .getAddon(R.string.feature_support_settings_addon,
                            SettingsPluginsHelper.class);
        }
        return mInstance;
    }

    public boolean displayConfirmDataDialog(Context context,
           SubscriptionInfo subscriptionInfo,int currentDataSubId,SimDialogActivity simDialog) {
        return false;
    }
}