package com.sprd.ext;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.sprdlauncher3.LauncherModel;
import com.android.sprdlauncher3.LauncherSettings;
import com.android.sprdlauncher3.compat.UserHandleCompat;

import android.view.Window;
import android.view.WindowManager;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.util.Log;

/**
 * Created by SPRD on 11/14/16.
 */

public class UtilitiesExt {
    private static final String TAG = "UtilitiesExt";
    //SPRD add for SPRD_SETTINGS_ACTIVITY_SUPPORT start {
    /**
     *
     * @param context  to getContentResolver
     * @param key  Preference key
     *  @param defaultVal if get value is null, return defaultVal
     */
    public static boolean getLauncherSettingsBoolean(Context context, String key, boolean defaultVal) {
        Bundle extras = new Bundle();
        extras.putBoolean(LauncherSettings.Settings.EXTRA_DEFAULT_VALUE, defaultVal);
        Bundle bundle = context.getContentResolver().call(
                LauncherSettings.Settings.CONTENT_URI,
                LauncherSettings.Settings.METHOD_GET_BOOLEAN,
                key, extras);

        if(bundle == null){
            return defaultVal;
        }
        return bundle.getBoolean(LauncherSettings.Settings.EXTRA_VALUE);
    }

    public static String getLauncherSettingsString(Context context, String key, String defaultVal) {
        Bundle extras = new Bundle();
        extras.putString(LauncherSettings.Settings.EXTRA_DEFAULT_VALUE, defaultVal);
        Bundle bundle = context.getContentResolver().call(
                LauncherSettings.Settings.CONTENT_URI,
                LauncherSettings.Settings.METHOD_GET_STRING,
                key, extras);

        if(bundle == null){
            return defaultVal;
        }
        return bundle.getString(LauncherSettings.Settings.EXTRA_VALUE);
    }
    //end }

    public static boolean isAppInstalled(Context context, ComponentName component) {
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        return LauncherModel.isValidPackageActivity(context, component, user);
    }

    public static CharSequence getAppLabelByPackageName(Context context, String pkgName) {
        boolean ret = false;
        ApplicationInfo info = null;
        PackageManager pm = context.getPackageManager();

        if (!TextUtils.isEmpty(pkgName)) {
            try {
                info = pm.getApplicationInfo(pkgName, 0);
                ret = info != null;
            } catch (PackageManager.NameNotFoundException e) {
                LogUtils.w(TAG, "getAppLabelByPackageName, get app label failed, pkgName:" + pkgName);
            }
        }

        return ret ? info.loadLabel(pm) : null;
    }

    public static void closeCursorSilently(Cursor cursor) {
        try {
            if (cursor != null) cursor.close();
        } catch (Throwable t) {
            LogUtils.w(TAG, "fail to close", t);
        }
    }
    

    public static void enableFullScreenMode(Activity activity,boolean enable) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (enable && (winParams.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN)
                != WindowManager.LayoutParams.FLAG_FULLSCREEN) {
            winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            win.setAttributes(winParams);
        } else if (!enable && (winParams.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN)
                == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
            winParams.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
            win.setAttributes(winParams);
        }
    }
    
}
