package com.sprd.settings;

import android.app.AddonManager;
import android.content.Context;
import android.util.Log;
import com.android.settings.R;

public class ApnEditableUtils {

    private static ApnEditableUtils mInstance;
    private static final String LOG_TAG = "ApnEditableUtils";

    public ApnEditableUtils() {
    }

    public static ApnEditableUtils getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (ApnEditableUtils) addonManager.getAddon(
                R.string.feature_apn_editable_plugin, ApnEditableUtils.class);
        Log.d(LOG_TAG, "mInstance = " + mInstance);
        return mInstance;
    }

    public boolean isApnEditable() {
        return true;
    }
}
