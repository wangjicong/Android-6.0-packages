package com.android.services.telephony.plugin;

import android.app.AddonManager;
import android.content.Context;
import android.util.Log;

import com.android.phone.R;

public class MMICompleteUtils {
    static MMICompleteUtils sInstance;

    public static MMICompleteUtils getInstance(Context context) {
        if (sInstance != null) return sInstance;
        AddonManager addonManager = new AddonManager(context);
        sInstance = (MMICompleteUtils) addonManager.getAddon(
                R.string.feature_support_MMIdialog_dismiss, MMICompleteUtils.class);
        return sInstance;
    }

    public MMICompleteUtils() {
    }

    public boolean isDismissMMIDialog() {
        Log.i("MMICompleteUtils", "isDismissMMIDialog=false");
        return false;
    }
}
