/** Created by Spreadst */
package com.sprd.server.telecom;

import com.android.server.telecom.R;

import android.app.AddonManager;
import android.content.Context;

public class CallLogUtils {
    static CallLogUtils sInstance;


    public static CallLogUtils getInstance(Context context) {
        if (sInstance != null) return sInstance;
        AddonManager addonManager = new AddonManager(context);
        sInstance = (CallLogUtils) addonManager.getAddon(R.string.call_log_plugin,CallLogUtils.class);
        return sInstance;
    }

    public CallLogUtils() {
    }

    public boolean okToLogEmergencyNumber() {
    	  //qiuyaobo,20160720, sprd modify, begin
        //return false;
        return true;
        //qiuyaobo,20160720, sprd modify, end
    }
}
