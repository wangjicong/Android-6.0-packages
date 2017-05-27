package com.android.launcher3;

import java.util.ArrayList;
import java.util.HashMap;

//import android.app.AddonManager;
import android.content.ComponentName;
import android.util.Log;

public class SprdCmccWorkspaceAddonStub {
    private static final String TAG = "SprdCmccWorkspaceAddonStub";

    private static SprdCmccWorkspaceAddonStub sInstance;
    protected boolean mHasCustomizeData = false;

    public static SprdCmccWorkspaceAddonStub getInstance() {
        if (sInstance == null) {
            sInstance = (SprdCmccWorkspaceAddonStub) SprdAddonUtils.instance(
                    R.string.feature_cmcc_app, SprdCmccWorkspaceAddonStub.class);
            if (sInstance == null) {
                sInstance = new SprdCmccWorkspaceAddonStub();
            }
        }
        return sInstance;
    }

    protected boolean isDefault() {
        Log.d(TAG, "[isDefault]::");
        return mHasCustomizeData;
    }
}
