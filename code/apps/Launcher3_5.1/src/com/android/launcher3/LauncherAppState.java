/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.PackageInstallerCompat.PackageInstallInfo;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import com.sprd.android.config.OptConfig;//ljb add

// SPRD: bug371310 2014-11-19 Feature circular sliding.
import android.content.SharedPreferences;

public class LauncherAppState implements DeviceProfile.DeviceProfileCallbacks {
    private static final String TAG = "LauncherAppState";

    private static final boolean DEBUG = false;

    // SPRD: bug371310 2014-11-19 Feature circular sliding.
    private static final String CIRCULAR_SLIDING_STATUS = "circular_sliding_status";

    private final AppFilter mAppFilter;
    private final BuildInfo mBuildInfo;
    private final LauncherModel mModel;
    private final IconCache mIconCache;

    private final boolean mIsScreenLarge;
    private final float mScreenDensity;
    private final int mLongPressTimeout = 300;

    private WidgetPreviewLoader.CacheDb mWidgetPreviewCacheDb;
    private boolean mWallpaperChangedSinceLastCheck;

    private static WeakReference<LauncherProvider> sLauncherProvider;
    private static Context sContext;

    private static LauncherAppState INSTANCE;

    private DynamicGrid mDynamicGrid;

    // SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info.
    private SprdUnreadInfoManager mUnreadInfoManager;

    public static LauncherAppState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return sContext;
    }

    public static void setApplicationContext(Context context) {
        if (sContext != null) {
            Log.w(Launcher.TAG, "setApplicationContext called twice! old=" + sContext + " new=" + context);
        }
        sContext = context.getApplicationContext();
    }

    private LauncherAppState() {
        if (sContext == null) {
            throw new IllegalStateException("LauncherAppState inited before app context set");
        }

        Log.v(Launcher.TAG, "LauncherAppState inited");

        if (sContext.getResources().getBoolean(R.bool.debug_memory_enabled)) {
            MemoryTracker.startTrackingMe(sContext, "L");
        }

        // set sIsScreenXLarge and mScreenDensity *before* creating icon cache
        mIsScreenLarge = isScreenLarge(sContext.getResources());
        mScreenDensity = sContext.getResources().getDisplayMetrics().density;

        recreateWidgetPreviewDb();
        mIconCache = new IconCache(sContext);

        mAppFilter = AppFilter.loadByName(sContext.getString(R.string.app_filter_class));
        mBuildInfo = BuildInfo.loadByName(sContext.getString(R.string.build_info_class));
        mModel = new LauncherModel(this, mIconCache, mAppFilter);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
        launcherApps.addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        sContext.registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        sContext.registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        //Kalyy 20160727
        if(OptConfig.SUNVOV_STK_NAME_SUPPORT){
            filter = new IntentFilter();
            filter.addAction("launcher3.updateStkName.action");
        }
        //Kalyy 20160727
        sContext.registerReceiver(mModel, filter);

        // Register for changes to the favorites
        ContentResolver resolver = sContext.getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);

        // SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info.
        mUnreadInfoManager = new SprdUnreadInfoManager(this);
    }

    public void recreateWidgetPreviewDb() {
        if (mWidgetPreviewCacheDb != null) {
            mWidgetPreviewCacheDb.close();
        }
        mWidgetPreviewCacheDb = new WidgetPreviewLoader.CacheDb(sContext);
    }

    /**
     * Call from Application.onTerminate(), which is not guaranteed to ever be called.
     */
    public void onTerminate() {
        sContext.unregisterReceiver(mModel);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
        launcherApps.removeOnAppsChangedCallback(mModel);
        PackageInstallerCompat.getInstance(sContext).onStop();

        ContentResolver resolver = sContext.getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);

        // SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info.
        mUnreadInfoManager.terminate();
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            // If the database has ever changed, then we really need to force a reload of the
            // workspace on the next load
            mModel.resetLoadedState(false, true);
            mModel.startLoaderFromBackground();
        }
    };

    LauncherModel setLauncher(Launcher launcher) {
        mModel.initialize(launcher);

        // SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info.
        mUnreadInfoManager.bindLauncher(launcher);

        // SPRD: bug377994 2014-12-06 Feature disable Stats default.
        // SPRD: bug393450 2015-01-12 Feature refactor dynamic controller.
        SprdDynamicController.getInstance().setLauncher(launcher);
        return mModel;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    LauncherModel getModel() {
        return mModel;
    }

    boolean shouldShowAppOrWidgetProvider(ComponentName componentName) {
        return mAppFilter == null || mAppFilter.shouldShowApp(componentName);
    }

    WidgetPreviewLoader.CacheDb getWidgetPreviewCacheDb() {
        return mWidgetPreviewCacheDb;
    }

    static void setLauncherProvider(LauncherProvider provider) {
        sLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    static LauncherProvider getLauncherProvider() {
        return sLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return LauncherFiles.SHARED_PREFERENCES_KEY;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    DeviceProfile initDynamicGrid(Context context) {
        mDynamicGrid = createDynamicGrid(context, mDynamicGrid);
        mDynamicGrid.getDeviceProfile().addCallback(this);
        return mDynamicGrid.getDeviceProfile();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    static DynamicGrid createDynamicGrid(Context context, DynamicGrid dynamicGrid) {
        // Determine the dynamic grid properties
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point realSize = new Point();
        display.getRealSize(realSize);
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        if (dynamicGrid == null) {
            Point smallestSize = new Point();
            Point largestSize = new Point();
            display.getCurrentSizeRange(smallestSize, largestSize);

            dynamicGrid = new DynamicGrid(context,
                    context.getResources(),
                    Math.min(smallestSize.x, smallestSize.y),
                    Math.min(largestSize.x, largestSize.y),
                    realSize.x, realSize.y,
                    dm.widthPixels, dm.heightPixels);
        }

        // Update the icon size
        DeviceProfile grid = dynamicGrid.getDeviceProfile();
        grid.updateFromConfiguration(context, context.getResources(),
                realSize.x, realSize.y,
                dm.widthPixels, dm.heightPixels);
        return dynamicGrid;
    }

    public DynamicGrid getDynamicGrid() {
        return mDynamicGrid;
    }

    public boolean isScreenLarge() {
        return mIsScreenLarge;
    }

    // Need a version that doesn't require an instance of LauncherAppState for the wallpaper picker
    public static boolean isScreenLarge(Resources res) {
        return res.getBoolean(R.bool.is_large_tablet);
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    public float getScreenDensity() {
        return mScreenDensity;
    }

    public int getLongPressTimeout() {
        return mLongPressTimeout;
    }

    public void onWallpaperChanged() {
        mWallpaperChangedSinceLastCheck = true;
    }

    public boolean hasWallpaperChangedSinceLastCheck() {
        boolean result = mWallpaperChangedSinceLastCheck;
        mWallpaperChangedSinceLastCheck = false;
        return result;
    }

    @Override
    public void onAvailableSizeChanged(DeviceProfile grid) {
        Utilities.setIconSize(grid.iconSizePx);
    }

    public static boolean isDisableAllApps() {
        // Returns false on non-dogfood builds.
        return getInstance().mBuildInfo.isDogfoodBuild() &&
                Utilities.isPropertyEnabled(Launcher.DISABLE_ALL_APPS_PROPERTY);
    }

    public static boolean isDogfoodBuild() {
        return getInstance().mBuildInfo.isDogfoodBuild();
    }

    public void setPackageState(ArrayList<PackageInstallInfo> installInfo) {
        mModel.setPackageState(installInfo);
    }

    /**
     * Updates the icons and label of all icons for the provided package name.
     */
    public void updatePackageBadge(String packageName) {
        mModel.updatePackageBadge(packageName);
    }

    /* SPRD: bug371310 2014-11-19 Feature circular sliding. @{ */
    public static String getCircularSlidingStatusPreferenceKey() {
        return CIRCULAR_SLIDING_STATUS;
    }

    public boolean getCircularSlidingStatus() {
        SharedPreferences sp = sContext.getSharedPreferences(getSharedPreferencesKey(), Context.MODE_PRIVATE);
        return sp.getBoolean(CIRCULAR_SLIDING_STATUS, true);	
        
    }

    public void setCircularSlidingStatus(boolean status) {
        SharedPreferences sp = sContext.getSharedPreferences(getSharedPreferencesKey(), Context.MODE_PRIVATE);
        sp.edit().putBoolean(CIRCULAR_SLIDING_STATUS, status).commit();
    }
    /* SPRD: bug371310 2014-11-19 Feature circular sliding. @} */

    /* SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info. @{ */
    public SprdUnreadInfoManager getUnreadInfoManager() {
        return mUnreadInfoManager;
    }
    /* SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info. @} */
}
