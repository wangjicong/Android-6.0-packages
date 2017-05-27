/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;

import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import android.util.Log;
import android.util.Xml;
import android.util.AttributeSet;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import com.android.internal.util.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Stores the list of all applications for the all apps view.
 */
class AllAppsList {
    public static final int DEFAULT_APPLICATIONS_NUMBER = 42;

    /** The list off all apps. */
    public ArrayList<AppInfo> data =
            new ArrayList<AppInfo>(DEFAULT_APPLICATIONS_NUMBER);
    /** The list of apps that have been added since the last notify() call. */
    public ArrayList<AppInfo> added =
            new ArrayList<AppInfo>(DEFAULT_APPLICATIONS_NUMBER);
    /** The list of apps that have been removed since the last notify() call. */
    public ArrayList<AppInfo> removed = new ArrayList<AppInfo>();
    /** The list of apps that have been modified since the last notify() call. */
    public ArrayList<AppInfo> modified = new ArrayList<AppInfo>();

    private IconCache mIconCache;

    private AppFilter mAppFilter;

    //yanghua@20160627: begin for top packages.
    private static final boolean DEBUG_LOADERS_REORDER = false;
    private static final String TAG_TOPPACKAGES = "toppackages";
    static ArrayList<TopPackage> sTopPackages = null;

    static class TopPackage {
        public TopPackage(String pkgName, String clsName, int index) {
            packageName = pkgName;
            className = clsName;
            order = index;
        }

        String packageName;
        String className;
        int order;
    }
    //yanghua@20160627: end for top packages.

    /**
     * Boring constructor.
     */
    public AllAppsList(IconCache iconCache, AppFilter appFilter) {
        mIconCache = iconCache;
        mAppFilter = appFilter;
    }

    /**
     * Add the supplied ApplicationInfo objects to the list, and enqueue it into the
     * list to broadcast when notify() is called.
     *
     * If the app is already in the list, doesn't add it.
     */
    public void add(AppInfo info) {
        if (mAppFilter != null && !mAppFilter.shouldShowApp(info.componentName)) {
            return;
        }
        if (findActivity(data, info.componentName, info.user)) {
            return;
        }
        data.add(info);
        added.add(info);
    }

    public void clear() {
        data.clear();
        // TODO: do we clear these too?
        added.clear();
        removed.clear();
        modified.clear();
    }

    public int size() {
        return data.size();
    }

    public AppInfo get(int index) {
        return data.get(index);
    }

    /**
     * Add the icons for the supplied apk called packageName.
     */
    public void addPackage(Context context, String packageName, UserHandleCompat user) {
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        final List<LauncherActivityInfoCompat> matches = launcherApps.getActivityList(packageName,
                user);

        for (LauncherActivityInfoCompat info : matches) {
            add(new AppInfo(context, info, user, mIconCache, null));
        }
    }

    /**
     * Remove the apps for the given apk identified by packageName.
     */
    public void removePackage(String packageName, UserHandleCompat user, boolean clearCache) {
        final List<AppInfo> data = this.data;
        for (int i = data.size() - 1; i >= 0; i--) {
            AppInfo info = data.get(i);
            final ComponentName component = info.intent.getComponent();
            if (info.user.equals(user) && packageName.equals(component.getPackageName())) {
                removed.add(info);
                data.remove(i);
            }
        }
        if (clearCache) {
            mIconCache.remove(packageName, user);
        }
    }

    /**
     * Add and remove icons for this package which has been updated.
     */
    public void updatePackage(Context context, String packageName, UserHandleCompat user) {
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        final List<LauncherActivityInfoCompat> matches = launcherApps.getActivityList(packageName,
                user);
        if (matches.size() > 0) {
            // Find disabled/removed activities and remove them from data and add them
            // to the removed list.
            for (int i = data.size() - 1; i >= 0; i--) {
                final AppInfo applicationInfo = data.get(i);
                final ComponentName component = applicationInfo.intent.getComponent();
                if (user.equals(applicationInfo.user)
                        && packageName.equals(component.getPackageName())) {
                    if (!findActivity(matches, component)) {
                        removed.add(applicationInfo);
                        mIconCache.remove(component, user);
                        data.remove(i);
                    }
                }
            }

            // Find enabled activities and add them to the adapter
            // Also updates existing activities with new labels/icons
            for (final LauncherActivityInfoCompat info : matches) {
                AppInfo applicationInfo = findApplicationInfoLocked(
                        info.getComponentName().getPackageName(), user,
                        info.getComponentName().getClassName());
                if (applicationInfo == null) {
                    add(new AppInfo(context, info, user, mIconCache, null));
                } else {
                    mIconCache.remove(applicationInfo.componentName, user);
                    mIconCache.getTitleAndIcon(applicationInfo, info, null);
                    modified.add(applicationInfo);
                }
            }
        } else {
            // Remove all data for this package.
            for (int i = data.size() - 1; i >= 0; i--) {
                final AppInfo applicationInfo = data.get(i);
                final ComponentName component = applicationInfo.intent.getComponent();
                if (user.equals(applicationInfo.user)
                        && packageName.equals(component.getPackageName())) {
                    removed.add(applicationInfo);
                    mIconCache.remove(component, user);
                    data.remove(i);
                }
            }
        }
    }


    /**
     * Returns whether <em>apps</em> contains <em>component</em>.
     */
    private static boolean findActivity(List<LauncherActivityInfoCompat> apps,
            ComponentName component) {
        for (LauncherActivityInfoCompat info : apps) {
            if (info.getComponentName().equals(component)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Query the launcher apps service for whether the supplied package has
     * MAIN/LAUNCHER activities in the supplied package.
     */
    static boolean packageHasActivities(Context context, String packageName,
            UserHandleCompat user) {
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        return launcherApps.getActivityList(packageName, user).size() > 0;
    }

    /**
     * Returns whether <em>apps</em> contains <em>component</em>.
     */
    private static boolean findActivity(ArrayList<AppInfo> apps, ComponentName component,
            UserHandleCompat user) {
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            final AppInfo info = apps.get(i);
            if (info.user.equals(user) && info.componentName.equals(component)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find an ApplicationInfo object for the given packageName and className.
     */
    private AppInfo findApplicationInfoLocked(String packageName, UserHandleCompat user,
            String className) {
        for (AppInfo info: data) {
            final ComponentName component = info.intent.getComponent();
            if (user.equals(info.user) && packageName.equals(component.getPackageName())
                    && className.equals(component.getClassName())) {
                return info;
            }
        }
        return null;
    }

    //yanghua@20160627: Load the default set of default top packages from an xml file.
    /**
     *
     * @param context
     * @return true if load successful.
     */
    static boolean loadTopPackage(final Context context) {
        boolean bRet = false;
        if (sTopPackages != null) {
            return bRet;
        }

        sTopPackages = new ArrayList<TopPackage>();

        try {
            XmlResourceParser parser = context.getResources().getXml(R.xml.default_toppackage);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            XmlUtils.beginDocument(parser, TAG_TOPPACKAGES);

            final int depth = parser.getDepth();

            int type = -1;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                    && type != XmlPullParser.END_DOCUMENT) {

                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TopPackage);

                sTopPackages.add(new TopPackage(a.getString(R.styleable.TopPackage_topPackageName),
                        a.getString(R.styleable.TopPackage_topClassName), a.getInt(
                                R.styleable.TopPackage_topOrder, 0)));

                Log.d("yanghua", "loadTopPackage: packageName = "
                        + a.getString(R.styleable.TopPackage_topPackageName)
                        + ", className = "
                        + a.getString(R.styleable.TopPackage_topClassName));

                a.recycle();
            }
        } catch (XmlPullParserException e) {
            Log.d("yanghua", "Got XmlPullParserException while parsing toppackage.", e);
        } catch (IOException e) {
            Log.d("yanghua", "Got IOException while parsing toppackage.", e);
        }

        return bRet;
    }

    /**
     * M: Reorder all apps index according to TopPackages.
     */
    void reorderApplist() {
        //final long sortTime = DEBUG_LOADERS_REORDER ? SystemClock.uptimeMillis() : 0;

        if (sTopPackages == null || sTopPackages.isEmpty()) {
            return;
        }
        ensureTopPackageOrdered();

        final ArrayList<AppInfo> dataReorder = new ArrayList<AppInfo>(
                DEFAULT_APPLICATIONS_NUMBER);

        for (TopPackage tp : sTopPackages) {
            int loop = 0;
            for (AppInfo ai : added) {
                if (DEBUG_LOADERS_REORDER) {
                    Log.d("yanghua", "reorderApplist: remove loop = " + loop);
                }

                if (ai.componentName.getPackageName().equals(tp.packageName)
                        && ai.componentName.getClassName().equals(tp.className)) {
                    if (DEBUG_LOADERS_REORDER) {
                        Log.d("yanghua", "reorderApplist: remove packageName = "
                                + ai.componentName.getPackageName());
                    }
                    data.remove(ai);
                    dataReorder.add(ai);
                    dumpData();
                    break;
                }
                loop++;
            }
        }

        for (TopPackage tp : sTopPackages) {
            int loop = 0;
            int newIndex = 0;
            for (AppInfo ai : dataReorder) {
                if (DEBUG_LOADERS_REORDER) {
                    Log.d("yanghua", "reorderApplist: added loop = " + loop + ", packageName = "
                            + ai.componentName.getPackageName());
                }

                if (ai.componentName.getPackageName().equals(tp.packageName)
                        && ai.componentName.getClassName().equals(tp.className)) {
                    newIndex = Math.min(Math.max(tp.order, 0), added.size());
                    if (DEBUG_LOADERS_REORDER) {
                        Log.d("yanghua", "reorderApplist: added newIndex = " + newIndex);
                    }
                    /// M: make sure the array list not out of bound
                    if (newIndex < data.size()) {
                        data.add(newIndex, ai);
                    } else {
                        data.add(ai);
                    }
                    dumpData();
                    break;
                }
                loop++;
            }
        }

        if (added.size() == data.size()) {
            added = (ArrayList<AppInfo>) data.clone();
            Log.d("yanghua", "reorderApplist added.size() == data.size()");
        }

        if (DEBUG_LOADERS_REORDER) {
            //Log.d("yanghua", "sort and reorder took " + (SystemClock.uptimeMillis() - sortTime) + "ms");
        }
    }

    /**
     * Dump application informations in data.
     */
    void dumpData() {
        int loop2 = 0;
        for (AppInfo ai : data) {
            if (DEBUG_LOADERS_REORDER) {
                Log.d("yanghua", "reorderApplist data loop2 = " + loop2);
                Log.d("yanghua", "reorderApplist data packageName = "
                        + ai.componentName.getPackageName());
            }
            loop2++;
        }
    }

    /*
     * M: ensure the items from top_package.xml is in order,
     * for some special case of top_package.xml will make the arraylist out of bound.
     */

    static void ensureTopPackageOrdered() {
        ArrayList<TopPackage> tpOrderList = new ArrayList<TopPackage>(DEFAULT_APPLICATIONS_NUMBER);
        boolean bFirst = true;
        for (TopPackage tp : sTopPackages) {
            if (bFirst) {
                tpOrderList.add(tp);
                bFirst = false;
            } else {
                for (int i = tpOrderList.size() - 1; i >= 0; i--) {
                    TopPackage tpItor = tpOrderList.get(i);
                    if (0 == i) {
                        if (tp.order < tpOrderList.get(0).order) {
                            tpOrderList.add(0, tp);
                        } else {
                            tpOrderList.add(1, tp);
                        }
                        break;
                    }

                    if ((tp.order < tpOrderList.get(i).order)
                        && (tp.order >= tpOrderList.get(i - 1).order)) {
                        tpOrderList.add(i, tp);
                        break;
                    } else if (tp.order > tpOrderList.get(i).order) {
                        tpOrderList.add(i + 1, tp);
                        break;
                    }
                }
            }
        }

        if (sTopPackages.size() == tpOrderList.size()) {
            sTopPackages = (ArrayList<TopPackage>) tpOrderList.clone();
            tpOrderList = null;
            Log.d("yanghua", "ensureTopPackageOrdered done");
        } else {
            Log.d("yanghua", "some mistake may occur when ensureTopPackageOrdered");
        }
    }
    //yanghua@20160627: Load the default set of default top packages from an xml file.
}
