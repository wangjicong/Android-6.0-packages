package com.sprd.incallui;

import com.android.incallui.R;

import android.app.AddonManager;
import android.content.Context;
import android.util.Log;

public class ExplicitCallTransferPluginHelper {
    static ExplicitCallTransferPluginHelper sInstance;
    private static final String TAG = "[ExplicitCallTransferPlugin]";
    private static final boolean DBG = false;

    public static ExplicitCallTransferPluginHelper getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }

        if (DBG) Log.d(TAG, "getInstance()      sInstance is NULL. We have to create one!");

        AddonManager addonManager = new AddonManager(context);
        sInstance = (ExplicitCallTransferPluginHelper) addonManager.getAddon(
                R.string.feature_explicit_call_transfer_plugin_package_name,
                ExplicitCallTransferPluginHelper.class);

        if (DBG) Log.d(TAG, "getInstance()      sInstance = " + sInstance);
        return sInstance;
    }

    public ExplicitCallTransferPluginHelper(){
        // default
    }

    public boolean isExplicitCallTransferPlugin() {
        return false;
    }
}
