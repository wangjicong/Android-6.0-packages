package com.android.providers.downloads.ui.uiplugin;

import java.io.File;

import android.app.AddonManager;
import android.content.Context;
import com.android.providers.downloads.ui.R;
import com.android.providers.downloadsplugin.OpenHelperDRMUtil;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class DownloaduiDRMUtils {

    static DownloaduiDRMUtils sInstance;
    private static final String TAG = "DownloaduiDRMUtils";

    public static DownloaduiDRMUtils getInstance(Context context) {
        if (sInstance != null) return sInstance;
        Log.d(TAG, "DownloaduiDRMUtils getInstance");
        AddonManager addonManager = new AddonManager(context);
        sInstance = (DownloaduiDRMUtils) addonManager.getAddon(R.string.downloadui_downloadsuccess, DownloaduiDRMUtils.class);
        Log.d(TAG, "DownloaduiDRMUtils getInstance: plugin = " + context.getString(R.string.downloadui_downloadsuccess));
        return sInstance;
    }

    public DownloaduiDRMUtils() {
    }

    public boolean isDRMDownloadSuccess(long id, Context uiContext){
        Log.d(TAG, "isDRMDownloadSuccess enter");
        return false;
    }
}
