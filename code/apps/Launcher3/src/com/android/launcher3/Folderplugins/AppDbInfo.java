package com.android.launcher3.Folderplugins;

import com.android.launcher3.*;

public class AppDbInfo extends ItemInfo {
    public String pkgName;
    public String clsName;

    public AppInfo appInfo;

    @Override
    public String toString() {
        return String.format(
                "AppDbInfo[_id=%d, pkg=\"%s\", cls=\"%s\", screen=%d, cellX=%d, cellY=%d, container=%d, type=%d]", id,
                pkgName, clsName, screenId, cellX, cellY, container, itemType);
    }
}
