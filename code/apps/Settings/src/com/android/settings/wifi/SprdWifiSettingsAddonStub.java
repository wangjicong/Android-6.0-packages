/** Created by Spreadst */

package com.android.settings.wifi;

import android.app.AddonManager;
import android.content.Context;

import com.android.settings.R;

public class SprdWifiSettingsAddonStub {
    static SprdWifiSettingsAddonStub sInstance;

    public SprdWifiSettingsAddonStub() {
    }

    public static SprdWifiSettingsAddonStub getInstance(Context context) {
        if (sInstance == null) {
            AddonManager mAddonManager = new AddonManager(context);
            sInstance = (SprdWifiSettingsAddonStub) mAddonManager.getAddon(
                    R.string.feature_wifisettings_addon, SprdWifiSettingsAddonStub.class);
        }
        return sInstance;
    }

    public boolean isSupportCmcc() {
        return false;
    }

    public void startTrustedApListActivty() {
        // empty implementation for WifiSettings
    }

    public void setManulConnectFlags(boolean enable) {
        // empty implementation for WifiSettings
    }

    public boolean isWifiConnectingOrConnected() {
        // empty implementation for WifiSettings
        return false;
    }

    public void initWifiConnectionPolicy() {
        // empty implementation for AdvancedWifiSettings
    }

    public void setMobileToWlanPolicy(String value) {
        // empty implementation for AdvancedWifiSettings
    }

    public int getMobileToWlanPolicy() {
        // empty implementation for AdvancedWifiSettings
        return 0;
    }

    public void resetWifiPolicyDialogFlag() {
        // empty implementation for AdvancedWifiSettings
    }

    public boolean showDialogWhenConnectCMCC() {
        // empty implementation for AdvancedWifiSettings
        return false;
    }

    public void setConnectCmccDialogFlag(boolean enabled) {
        // empty implementation for AdvancedWifiSettings
    }
}
