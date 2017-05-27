package com.sprd.settings;

import android.app.AddonManager;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.android.settings.R;

public class ApnUtils {

    private static ApnUtils mInstance;
    private static final String LOG_TAG = "ApnUtils";

    public ApnUtils() {
    }

    public static ApnUtils getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (ApnUtils) addonManager.getAddon(
                R.string.feature_apn_utils_plugin, ApnUtils.class);
        Log.d(LOG_TAG, "mInstance = " + mInstance);
        return mInstance;
    }

    /**
     * Get the EDITABLE value from the cursor.
     *
     * @param c the cursor of the APN
     * @return true if this APN is editable
     */
    public boolean getEditable(Cursor c) {
        return true;
    }
}
