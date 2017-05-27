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

package com.android.launcher3.Folderplugins;

import android.app.AddonManager;
import android.app.SearchManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;
import android.os.TransactionTooLargeException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;

import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.VolteAppsProvider;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.PackageInstallerCompat.PackageInstallInfo;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public class LauncherModelUtils {

    public static final String TAG = "Launcher.Model";


    public static LauncherModelUtils mInstance;

    public static LauncherModelUtils getInstance() {

        if (mInstance == null) {
            mInstance = (LauncherModelUtils) AddonManager.getDefault().getAddon(R.string.launcher_model_utils_addon,
                    LauncherModelUtils.class);
        }

        return mInstance;
    }

    public void loadAppFolderDbInfo(Context context, AppFolderDbInfo folderDbInfo) {
    }

    public void deleteItemFromAppsDatabase(Context context, final ItemInfo item) {

    }

    public void deleteItemsFromAppsDatabase(Context context, final ArrayList<ItemInfo> items) {

    }

    public void addOrMoveItemInAppsDatabase(Context context, ItemInfo item, long container, long screenId, int cellX,
            int cellY) {

    }

    public void addItemToAppsDatabase(Context context, final ItemInfo item, final long container, final long screenId,
            final int cellX, final int cellY) {

    }

    public void moveItemInAppsDatabase(Context context, final ItemInfo item, final long container, final long screenId,
            final int cellX, final int cellY) {
    }

    public void updateItemInAppsDatabaseHelper(Context context, final ContentValues values, final ItemInfo item,
            final String callingFunction) {

    }

    public void updateItemsInAppsDatabaseHelper(Context context, final ArrayList<ContentValues> valuesList,
            final ArrayList<ItemInfo> items, final String callingFunction) {

    }

    public void moveItemsInAppsDatabase(Context context, final ArrayList<ItemInfo> items, final long container,
            final int screen) {
    }

    public void modifyItemInAppsDatabase(Context context, final ItemInfo item, final long container,
            final long screenId, final int cellX, final int cellY, final int spanX, final int spanY) {

    }

    public void updateItemInAppsDatabase(Context context, final ItemInfo item) {

    }

    public AppInfo removeInInstalledApps(String pkg, String cls, ArrayList<AppInfo> installedApps) {
        return null;
    }

    public ArrayList<ItemInfo> loadAppsByScreenId(Context context, long screen) {

        return null;
    }

    public boolean isAddItemToDatabase(ItemInfo item) {
        return (item.container == ItemInfo.NO_ID);
    }

    public ComponentName setComponentName(ItemInfo s) {
        ComponentName cn = null;
        if (s instanceof ShortcutInfo) {
            cn = ((ShortcutInfo) s).getTargetComponent();
        }
        return cn;
    }

}
