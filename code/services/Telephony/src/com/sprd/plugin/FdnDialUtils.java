package com.sprd.plugin;

import com.android.phone.R;
import android.app.AddonManager;
import android.content.Context;
import android.content.Intent;

public class FdnDialUtils {
    static FdnDialUtils sInstance;

    public static FdnDialUtils getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        sInstance = (FdnDialUtils) addonManager.getAddon(R.string.feature_support_fdn_dial,
                FdnDialUtils.class);
        return sInstance;
    }

    public FdnDialUtils() {
    }

    public String getAction() {
        String action = Intent.ACTION_CALL_PRIVILEGED;
        return action;
    }
}
