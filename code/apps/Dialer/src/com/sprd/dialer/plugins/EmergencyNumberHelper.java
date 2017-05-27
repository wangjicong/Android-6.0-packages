package com.sprd.dialer.plugins;

import android.app.AddonManager;
import android.content.Context;

import com.android.dialer.R;

public class EmergencyNumberHelper {
    private static final String TAG = "EmergencyNumberHelper";

    public EmergencyNumberHelper() {
    }

    static EmergencyNumberHelper mInstance;


    public static EmergencyNumberHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (EmergencyNumberHelper) AddonManager.getDefault().getAddon(
                R.string.emergency_number_plugin_name, EmergencyNumberHelper.class);

        return mInstance;
    }

    public String getEmergencyNumber(Context context, String displayNumber,
            String detailsNumber) {
        return displayNumber;
    }
}
