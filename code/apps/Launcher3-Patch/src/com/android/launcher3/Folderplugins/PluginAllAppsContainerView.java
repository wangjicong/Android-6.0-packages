package com.android.launcher3.Folderplugins;

import android.app.AddonManager;
import com.android.launcher3.R;
import android.view.View;
import java.util.List;
import com.android.launcher3.AppInfo;
import com.android.launcher3.DragSource;
import com.android.launcher3.Folder;
import com.android.launcher3.FolderIcon;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.Launcher;
import android.graphics.Point;
import com.android.launcher3.allapps.AlphabeticalAppsList;

public class PluginAllAppsContainerView {

    static PluginAllAppsContainerView mInstance;

    public static PluginAllAppsContainerView getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (PluginAllAppsContainerView) AddonManager.getDefault().getAddon(
                R.string.feature_jio_allappscontainerview, PluginAllAppsContainerView.class);
        return mInstance;
    }

    public void setApps2Folder(List<AppInfo> apps, List<ItemInfo> items, AlphabeticalAppsList al) {

    }

    public void setApps2Folder(List<ItemInfo> items, AlphabeticalAppsList al) {

    }

    // Drag app from AllApplist folder.
    public void beginDraggingFromFolder(View child, final Launcher mLauncher, Point mIconLastTouchPos,
            AllAppsContainerView allapps) {

    }

    public void enterSpringLoadedIfNeeded(AllAppsContainerView allapps, final Launcher mLauncher) {

    }

    public Folder getOpenFolder(AllAppsGridAdapter mAdapter) {
        return null;
    }
}
