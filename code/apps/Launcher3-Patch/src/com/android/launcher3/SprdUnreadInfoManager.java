package com.android.launcher3;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.AddonManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.util.Log;
import android.view.View;
import android.Manifest.permission;

import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.compat.UserHandleCompat;

// SPRD: bug372523 2014-11-21 Feature show unread mmssms/missed calls info.
public class SprdUnreadInfoManager extends ContentObserver{

    private static final String TAG = "SprdUnreadInfoManager";
    private static final boolean DEBUG = true;

    private WeakReference<Launcher> mLauncherRef;
    private IconCache mIconCache;
    static SprdUnreadInfoManager sInstance;

    public static SprdUnreadInfoManager getInstance() {
        if (sInstance == null)
            sInstance = (SprdUnreadInfoManager) AddonManager.getDefault().getAddon(R.string.unread_info_manager,
                    SprdUnreadInfoManager.class);
        return sInstance;
    }

    public SprdUnreadInfoManager() {
        super(null);
    }
    public SprdUnreadInfoManager(LauncherAppState appState) {
        super(null);
        Log.d(TAG, "SprdUnreadInfoManager");
    }

    public void init(LauncherAppState appState){
    }

    public void registerContentObservers(){
    }

    public boolean isUnreadinfoOn(){
        return false;
    }

    public void bindLauncher(Launcher launcher) {
    }

    public void terminate() {
    }

    /**
     * Note: this method be called in Launcher.onResume to check if there has
     * any unread info need to be updated, then update these unread info all at
     * once.
     */
    public void updateUnreadInfoIfNeeded() {

    }

    /**
     * Note: this method is called by Working thread in LauncherModel before load
     * and bind any icon into Workspace or AppsCustomizePagedView.
     *
     * Prepare the unread data and bitmap and then apply these bitmap when
     * new BubbleTextView need to be created.
     *
     * This method should not be called in UI thread for it manipulate database.
     */
    public void prepareUnreadInfo() {

    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
    }

    public Bitmap getBitmapWithUnreadInfo(ShortcutInfo info, Bitmap originBmp) {
        return originBmp;
    }

    public Bitmap getBitmapWithUnreadInfo(AppInfo info, Bitmap originBmp) {
        return originBmp;
    }

    public boolean updateBubbleTextViewUnreadInfo(BubbleTextView bubble) {
        return false;
    }

    private Bitmap getBitmapWithUnreadInfoInternal(ComponentName cn, Bitmap origin) {
        return origin;
    }

    private boolean hasComponentUnreadInfoChanged(ComponentName cn) {
        return false;
    }

    public void resetComponentsUnreadInfoChangedValue() {

    }

    public void updateUnreadInfo(Workspace wp) {

    }

    public void updateUnreadInfo(Launcher mLauncher,AlphabeticalAppsList mApps,AllAppsContainerView mAllAppsContainerView) {

    }
}
