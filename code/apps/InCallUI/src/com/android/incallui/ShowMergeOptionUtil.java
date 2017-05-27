package com.android.incallui;

import android.app.AddonManager;
import android.content.Context;

public class ShowMergeOptionUtil {

    private static final String TAG = "ShowMergeOptionUtil";

    static ShowMergeOptionUtil sInstance;

    public ShowMergeOptionUtil() {}

    public static  ShowMergeOptionUtil getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        AddonManager addonManager = new AddonManager(context);
        sInstance = (ShowMergeOptionUtil) addonManager.getAddon(R.string.show_merge_call_button,
                ShowMergeOptionUtil.class);

        Log.d(TAG, "getInstance [" + sInstance + "]");
        return sInstance;
    }

    public boolean showMergeButton(Call call) {
        return call.can(
                    android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE);
    }

    public void showToast(Context context) {
    }

    public boolean shouldBreak() {
        return false;
    }
}
