package com.android.launcher3.Folderplugins;

import android.app.AddonManager;

import com.android.launcher3.R;

public class LauncherBackupAgentHelperUtils {

    static LauncherBackupAgentHelperUtils sInstance;

    public static LauncherBackupAgentHelperUtils getInstance() {
        if (sInstance == null)
            sInstance = (LauncherBackupAgentHelperUtils) AddonManager.getDefault().getAddon(
                    R.string.launcher_backup_agent_helper_utils_addon, LauncherBackupAgentHelperUtils.class);
        return sInstance;
    }

    public boolean isRunCustomeClingDismissed() {
        return false;
    }

}
