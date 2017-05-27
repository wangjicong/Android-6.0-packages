package com.sprd.incallui;

import android.app.AddonManager;
import android.content.Context;

import com.android.incallui.CallCardFragment;
import com.android.incallui.Log;
import com.android.incallui.R;

/**
 * Make a call to the contact recently added to FDN list, Contact name should show FDN list name.
 */
public class FdnListNameHelper {
    private static final String TAG = "FdnListNameHelper";
    static FdnListNameHelper mInstance;

    public static FdnListNameHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        mInstance = (FdnListNameHelper) addonManager.getAddon(R.string.fdn_list_name_plugin_name,
                FdnListNameHelper.class);
        Log.i(TAG, "getInstance [" + mInstance + "]");
        return mInstance;
    }

    public FdnListNameHelper() {

    }

    public boolean isSupportFdnListName(int subId) {
        return false;
    }

    public void setFDNListName(String number, boolean nameIsNumber, String name,
                               String label, CallCardFragment cardFragment, int subId) {

    }
}
