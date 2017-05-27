package com.android.launcher3.Folderplugins;

import java.util.ArrayList;

import android.app.AddonManager;
import android.util.Log;
import android.view.View;

import com.android.launcher3.Folder;
import com.android.launcher3.AppInfo;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.DropTarget.DragObject;
import java.util.List;
import java.util.HashMap;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.model.AppNameComparator;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.AlphabeticalAppsList.FastScrollSectionInfo;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import android.support.v7.widget.RecyclerView;
import com.android.launcher3.allapps.AlphabeticalAppsList.SectionInfo;
import com.android.launcher3.R;

public class PluginAlphabeticalApplist {
    public static final String TAG = "PluginAlphabeticalApplist";
    static PluginAlphabeticalApplist mInstance;

    public static PluginAlphabeticalApplist getInstance() {
        if (mInstance != null) {
            return mInstance;
        }

        mInstance = (PluginAlphabeticalApplist) AddonManager.getDefault().getAddon(
                R.string.jio_folder_AlphabeticalAppsList, PluginAlphabeticalApplist.class);
        return mInstance;
    }

    public void setApps2Folder(AlphabeticalAppsList al, List<ItemInfo> items,
            HashMap<ComponentKey, ItemInfo> componentToAppMap) {

    }

    public void setApps2Folder(AlphabeticalAppsList al, List<AppInfo> apps, List<ItemInfo> items,
            HashMap<ComponentKey, ItemInfo> mComponentToAppMap) {

    }

    public void addApps(AlphabeticalAppsList al, List<AppInfo> apps, List<ItemInfo> items) {

    }

    public void addApps(AlphabeticalAppsList al, List<ItemInfo> items, int dummy) {

    }

    public void updateApps(AlphabeticalAppsList al, List<AppInfo> apps, List<ItemInfo> items,
            HashMap<ComponentKey, ItemInfo> mComponentToAppMap) {
    }

    public void updateApps(AlphabeticalAppsList al, List<ItemInfo> items, int dummy,
            HashMap<ComponentKey, ItemInfo> mComponentToAppMap) {
    }

    /**
     * Updates internals when the set of apps are updated.
     */
    public void onAppsUpdated(List<ItemInfo> mApps, HashMap<ComponentKey, ItemInfo> mComponentToAppMap,
            AppNameComparator mAppNameComparator, Launcher mLauncher, AlphabeticalAppsList al) {
    }

    /**
     * Updates the set of filtered apps with the current filter. At this point,
     * we expect mCachedSectionNames to have been calculated for the set of all
     * apps in mApps.
     */
    public void updateAdapterItems(AlphabeticalAppsList al, List<ItemInfo> mFilteredApps,
            List<FastScrollSectionInfo> mFastScrollerSections, List<AdapterItem> mAdapterItems,
            List<SectionInfo> mSections, List<AppInfo> mPredictedApps, List<ComponentKey> mPredictedAppComponents,
            HashMap<ComponentKey, ItemInfo> mComponentToAppMap, Launcher mLauncher, int mNumPredictedAppsPerRow,
            int mNumAppsPerRow, int mNumAppRowsInAdapter, RecyclerView.Adapter mAdapter,
            ArrayList<ComponentKey> mSearchResults, List<ItemInfo> mApps) {

    }

    public List<ItemInfo> getFiltersAppInfos(ArrayList<ComponentKey> mSearchResults, List<ItemInfo> mApps,
            HashMap<ComponentKey, ItemInfo> mComponentToAppMap) {
        return null;
    }

    public int setItemViewType(ItemInfo appInfo) {
        // TODO Auto-generated method stub
        return AllAppsGridAdapter.ICON_VIEW_TYPE;
    }
}
