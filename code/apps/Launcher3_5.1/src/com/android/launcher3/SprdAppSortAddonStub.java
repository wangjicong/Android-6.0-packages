package com.android.launcher3;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ComponentName;
import android.util.Log;

// SPRD: bug375932 2014-12-02 Feature customize app icon sort.
public class SprdAppSortAddonStub {

    private static final String TAG = "SprdAppSortAddonStub";

    private static SprdAppSortAddonStub sInstance;
    protected boolean mHasCustomizeData = false;

    public static SprdAppSortAddonStub getInstance() {
        if (sInstance == null) {
            // SPRD: bug395717 2015-01-19 Feature to support device not with addon.
            sInstance = (SprdAppSortAddonStub) SprdAddonUtils.instance(
                    R.string.feature_app_sort, SprdAppSortAddonStub.class);
            if (sInstance == null) {
                sInstance = new SprdAppSortAddonStub();
            }
        }
        return sInstance;
    }

    public final boolean hasCustomizeData() {
        return mHasCustomizeData;
    }

    protected void onSortApps(ArrayList<ComponentName> componentNames) {
        // empty implementation
        Log.d(TAG, "onSortApps empty implementation.");
    }

    public final void sortApps(ArrayList<AppInfo> apps) {
        if (!hasCustomizeData()) {
            return;
        }

        ArrayList<ComponentName> sortedCNs = new ArrayList<ComponentName>();
        HashMap<ComponentName, AppInfo> maps = new HashMap<ComponentName, AppInfo>();

        for (AppInfo appInfo : apps) {
            sortedCNs.add(appInfo.componentName);
            maps.put(appInfo.componentName, appInfo);
        }

        onSortApps(sortedCNs);

        // refresh mApps
        apps.clear();
        for (ComponentName cn : sortedCNs) {
            apps.add(maps.get(cn));
        }
    }

}
