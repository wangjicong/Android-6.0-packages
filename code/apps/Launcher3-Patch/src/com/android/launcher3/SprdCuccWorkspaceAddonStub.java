package com.android.launcher3;

import java.util.ArrayList;
import java.util.HashMap;

//import android.app.AddonManager;
import android.content.ComponentName;
import android.util.Log;

public class SprdCuccWorkspaceAddonStub {
    private static final String TAG = "SprdCmccWorkspaceAddonStub";

    private static SprdCuccWorkspaceAddonStub sInstance;
    protected boolean mHasCustomizeData = false;

    public static SprdCuccWorkspaceAddonStub getInstance() {
        if (sInstance == null) {
            sInstance = (SprdCuccWorkspaceAddonStub) SprdAddonUtils.instance(
                    R.string.feature_cucc_app, SprdCuccWorkspaceAddonStub.class);
            if (sInstance == null) {
                sInstance = new SprdCuccWorkspaceAddonStub();
            }
        }
        return sInstance;
    }

    protected boolean isDefault() {
        Log.d(TAG, "[isDefault]::");
        return mHasCustomizeData;
    }
}
