package com.android.launcher3;

import android.content.Context;
import android.content.Intent;

public class SprdLauncherDrmUtils {
    static SprdLauncherDrmUtils sInstance;

    public static SprdLauncherDrmUtils getInstance() {
        if (sInstance == null) {
            sInstance = (SprdLauncherDrmUtils) SprdAddonUtils.instance(
                    R.string.feature_launcher_drm, SprdLauncherDrmUtils.class);
            if (sInstance == null) {
                return new SprdLauncherDrmUtils();
            }
        }
        return sInstance;
    }

    public SprdLauncherDrmUtils() {
    }

    public void addIntentExtra(Intent intent) {
    }

}
