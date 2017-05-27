package com.android.launcher3.Folderplugins;

import java.lang.ref.WeakReference;
import android.app.AddonManager;

import com.android.launcher3.LauncherProvider;
import com.android.launcher3.VolteAppsProvider;

import com.android.launcher3.R;

public class LauncherAppStateUtils {

    static LauncherAppStateUtils sInstance;

    public static LauncherAppStateUtils getInstance() {
        if (sInstance == null)
            sInstance = (LauncherAppStateUtils) AddonManager.getDefault().getAddon(
                    R.string.launcher_app_state_utils_addon, LauncherAppStateUtils.class);
        return sInstance;
    }

    public void setLauncherCustomeProvider(VolteAppsProvider provider) {
    }

    public VolteAppsProvider getLauncherAppsCustomeProvider() {
        return null;
    }

}
