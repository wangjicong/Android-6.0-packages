package com.android.launcher3.Folderplugins;

import java.util.ArrayList;
import com.android.launcher3.*;
import android.app.AddonManager;
import android.util.Log;

public class AppFolderDbInfo extends ItemInfo {

    static AppFolderDbInfo mInstance;

    public static AppFolderDbInfo getInstance() {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = (AppFolderDbInfo) AddonManager.getDefault().getAddon(R.string.folder_dbinfo, AppFolderDbInfo.class);
        return mInstance;
    }

    public FolderInfo folderInfo;
    public ArrayList<AppDbInfo> children = new ArrayList<AppDbInfo>();

    public void addChild(AppDbInfo item) {
    }

    public boolean removeChild(AppDbInfo item) {
        return false;
    }

    public AppDbInfo removeChild(int index) {
        return null;
    }

    public AppDbInfo getChild(int index) {
        return null;
    }

    @Override
    public String toString() {
        return String
                .format("AppFolderDbInfo[_id=%d, title=%s, screen=%d, cellX=%d, cellY=%d, container=%d, type=%d, child count=%d]",
                        folderInfo.id, folderInfo.title, folderInfo.screenId,
                        folderInfo.cellX, folderInfo.cellY,
                        folderInfo.container, folderInfo.itemType,
                        children.size());
    }

}
