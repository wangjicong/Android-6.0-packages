package com.android.launcher3.Folderplugins;

import android.app.AddonManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Folder;
import com.android.launcher3.FolderIcon;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.FolderIcon.FolderRingAnimator;
import com.android.launcher3.R;

public class FolderIconPlugin {

    static FolderIconPlugin mInstance;

    public static FolderIconPlugin getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (FolderIconPlugin) AddonManager.getDefault().getAddon(R.string.folder_icon, FolderIconPlugin.class);
        return mInstance;
    }

    public FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group) {
        return null;
    }

    public void bindFolderInfo(FolderInfo folderInfo, FolderInfo mInfo, Launcher mLauncher, Folder mFolder,
            BubbleTextView mFolderName, FolderIcon folderIcon) {

    }

    /*SPRD: bug551836 ,launcher will crash when add application from all app list.@}*/
    public ItemInfo onAlarm(ItemInfo item, ItemInfo mDragInfo, FolderInfo mInfo) {
        // Came from all apps -- make a copy.
        item = ((AppInfo) mDragInfo).makeShortcut();
        item.spanX = 1;
        item.spanY = 1;
        return item;
    }
    /* @} */

    public ItemInfo setItem(DragObject d, FolderInfo mInfo) {
        ItemInfo item = null;
        if (d.dragInfo instanceof AppInfo) {
            // Came from all apps -- make a copy
            item = ((AppInfo) d.dragInfo).makeShortcut();
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
        return item;
    }

    public void setFolderEnable(boolean enable, Folder mFolder) {

    }
}
