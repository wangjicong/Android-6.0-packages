package com.android.launcher3.Folderplugins;

import android.app.AddonManager;

import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsGridAdapter;

public class AllAppsRecyclerViewUtils {

    static AllAppsRecyclerViewUtils sInstance;

    public static AllAppsRecyclerViewUtils getInstance() {
        if (sInstance == null)
            sInstance = (AllAppsRecyclerViewUtils) AddonManager.getDefault().getAddon(
                    R.string.launcher_backup_agent_helper_utils_addon, AllAppsRecyclerViewUtils.class);
        return sInstance;
    }

    public boolean isViewTypeAvaliable(int viewtype) {
        return viewtype == AllAppsGridAdapter.ICON_VIEW_TYPE
                || viewtype == AllAppsGridAdapter.PREDICTION_ICON_VIEW_TYPE;
    }

}
