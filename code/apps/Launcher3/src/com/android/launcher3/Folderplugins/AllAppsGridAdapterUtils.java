package com.android.launcher3.Folderplugins;

import android.app.AddonManager;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.FolderIcon;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AlphabeticalAppsList;

public class AllAppsGridAdapterUtils {

    static AllAppsGridAdapterUtils sInstance;

    public static AllAppsGridAdapterUtils getInstance() {
        if (sInstance == null)
            sInstance = (AllAppsGridAdapterUtils) AddonManager.getDefault().getAddon(
                    R.string.all_apps_gridadapter_utils_addon, AllAppsGridAdapterUtils.class);
        return sInstance;
    }

    public FolderIcon getFolderIcon(Launcher launcher, ViewGroup parent, View.OnTouchListener touchListener,
            View.OnClickListener iconclicklistener, View.OnLongClickListener iconlongclicklistener) {
        FolderIcon icon = FolderIcon.fromXml(R.layout.all_apps_folder_icon, launcher, parent);
        return icon;
    }

    public void bindFolderInfo(AllAppsGridAdapter.ViewHolder viewholer, AlphabeticalAppsList apps, int position) {
    }

}
