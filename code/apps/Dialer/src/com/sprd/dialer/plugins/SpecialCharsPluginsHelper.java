package com.sprd.dialer.plugins;

import android.app.AddonManager;
import android.content.Context;

import com.android.dialer.R;

public class SpecialCharsPluginsHelper {
    private static final String TAG = "SpecialCharsPluginsHelper";

    public SpecialCharsPluginsHelper() {
    }

    static SpecialCharsPluginsHelper mInstance;


    public static SpecialCharsPluginsHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (SpecialCharsPluginsHelper) AddonManager.getDefault().getAddon(
                R.string.specail_chars_plugin_name, SpecialCharsPluginsHelper.class);

        return mInstance;
    }

    public boolean handleChars(Context context, String dialString) {
        return handleDmCode(context, dialString);
    }

    private boolean handleDmCode(Context context, String input) {
        return false;
    }

    private boolean handleAgpsCfg(Context context, String input) {
        return false;
    }

    private boolean handleAgpsLogShow(Context context, String input) {
        return false;
    }
}
