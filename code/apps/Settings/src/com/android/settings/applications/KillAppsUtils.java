/** Create by Spreadst */
package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.AddonManager;
import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.R;

public class KillAppsUtils {

    static KillAppsUtils sInstance;

    public KillAppsUtils() {
    }

    public static KillAppsUtils getInstance() {
        if (sInstance != null) return sInstance;
        sInstance = (KillAppsUtils) AddonManager.getDefault().getAddon(R.string.feature_kill_allapps,
                KillAppsUtils.class);
        return sInstance;
    }

    public boolean isSupportKillAllApps() {
        return false;
    }

    public void startKillAllApps(Context context, ActivityManager am, PackageManager pm) {
    }
}

