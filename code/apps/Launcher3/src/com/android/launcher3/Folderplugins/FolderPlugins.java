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

public class FolderPlugins {
    public static final String TAG = "FolderPlugins";
    static FolderPlugins mInstance;

    public static FolderPlugins getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (FolderPlugins) AddonManager.getDefault().getAddon(R.string.folder_isenable, FolderPlugins.class);
        return mInstance;
    }

    public boolean isEnable() {
        return false;
    }

    public boolean beginDrag(Folder folder, Object tag, View v, final Launcher mLauncher) {
        if (!v.isInTouchMode()) {
            return false;
        }
        return true;
    }

    public void updateItem(FolderInfo mInfo, Launcher mLauncher) {
        LauncherModel.updateItemInDatabase(mLauncher, mInfo);
    }

    public void saveFolderLocation(Folder folder, FolderInfo mInfo, Launcher launcher) {

    }

    public boolean deferDragView(FolderInfo mInfo, boolean successful, final DragObject d) {
        return false;
    }

    public void updateItemLocationsInDatabase(Folder folder, FolderInfo mInfo, Launcher launcher) {

    }

    public void updateItemLocationsInAppsDatabase(Folder folder, FolderInfo mInfo, Launcher launcher) {

    }
}
