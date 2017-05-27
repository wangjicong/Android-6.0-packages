package com.android.launcher3.Folderplugins;

import java.util.ArrayList;

import android.app.AddonManager;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Folder;
import com.android.launcher3.FolderIcon;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.IconCache;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Workspace;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.util.LongArrayMap;

public class LauncherUtils {

    static LauncherUtils sInstance;

    public static LauncherUtils getInstance() {
        if (sInstance == null)
            sInstance = (LauncherUtils) AddonManager.getDefault().getAddon(R.string.launcher_utils_addon,
                    LauncherUtils.class);
        return sInstance;
    }

    public FolderIcon addCustomeFolder(CellLayout layout, long container, final long screenId, int cellX, int cellY,
            FolderInfo info, Workspace mWorkspace, IconCache iconcache, boolean isWorkspaceLocked, Launcher launcher,
            LongArrayMap<FolderInfo> folderarray) {
        return null;
    }

    public void createCustomeShortCut(ItemInfo info, BubbleTextView favorite, IconCache iconcache) {
        favorite.applyFromShortcutInfo((ShortcutInfo) info, iconcache);
    }

    public void showCustomeFolderCling(FolderInfo info, Launcher launcher) {
    }

    public boolean isCellLayoutParam(ViewGroup.LayoutParams param) {
        // Use orignal source code by default,so return true
        return true;
    }

    public boolean isFoldeIconFromAppList(FolderInfo info) {
        // Use orignal source code by default,so return false
        return false;
    }

    public void bindAllApplications2CustomeFolder(AllAppsContainerView appview, ArrayList<AppInfo> apps,
            ArrayList<ItemInfo> items, LauncherCallbacks launchercallbacks) {
    }

    public void showFirstVolteRunCustomeClings(Launcher launcher) {
    }

    public void showFolderVisiable(Folder folder, FolderIcon icon, Launcher launcher) {
        // folder.setVisibility(View.VISIBLE);
        folder.animateOpen();
        launcher.growAndFadeOutFolderIcon(icon);
    }

    public void closeFolderInAllApp(Launcher launcher, AllAppsContainerView mAppsView) {

    }

    public void showCling(Launcher launcher, FolderInfo info) {

    }

    public boolean isInstanceofLayoutParams(FolderIcon folderIcon) {
        return true;
    }

    public void bindAllApplications2Folder(final ArrayList<AppInfo> apps, final ArrayList<ItemInfo> items,
            AllAppsContainerView mAppsView, LauncherCallbacks mLauncherCallbacks) {

    }
}
