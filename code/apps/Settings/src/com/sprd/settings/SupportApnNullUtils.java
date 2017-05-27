package com.sprd.settings;

import android.app.AddonManager;
import android.content.Context;
import android.util.Log;
import com.android.settings.R;

public class SupportApnNullUtils {

    private static SupportApnNullUtils mInstance;
    private static final String LOG_TAG = "SupportApnNullUtils";

    public SupportApnNullUtils() {
    }

    public static SupportApnNullUtils getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (SupportApnNullUtils) addonManager.getAddon(
                R.string.feature_support_apn_null, SupportApnNullUtils.class);
        Log.d(LOG_TAG, "mInstance = " + mInstance);
        return mInstance;
    }

    public boolean IsSupportApnNull() {
        Log.d(LOG_TAG, "IsSupportApnNull: false");
        return false;
    }

}