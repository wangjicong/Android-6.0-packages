package com.android.providers.downloadsplugin;

import java.io.File;

import android.app.Activity;
import android.app.AddonManager;
import android.content.Context;


import android.content.Intent;
import android.database.Cursor;
import static android.app.DownloadManager.COLUMN_LOCAL_FILENAME;
import static android.app.DownloadManager.COLUMN_LOCAL_URI;
import static android.app.DownloadManager.COLUMN_MEDIA_TYPE;
import static android.app.DownloadManager.COLUMN_URI;
import static android.app.DownloadManager.COLUMN_TITLE;
import static android.provider.Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI;
import android.util.Log;
import static com.android.providers.downloads.Constants.TAG;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import com.android.providers.downloads.R;


public class OpenHelperDRMUtil {

    static OpenHelperDRMUtil sInstance;
    private static final String TAG = "OpenHelperDRMUtil";
    public static final int DRM_PLUGIN = 0;
    public static final int HELPER_FULLNAME_RETURN = 1;
    public static final int HELPER_SUQENCE_CONTINUE = 2;

    public static OpenHelperDRMUtil getInstance(Context context) {
        if (sInstance != null) return sInstance;
        Log.d(TAG, "OpenHelperDRMUtil getInstance");
        AddonManager addonManager = new AddonManager(context);
        sInstance = (OpenHelperDRMUtil) addonManager.getAddon(R.string.openhelper_plugin, OpenHelperDRMUtil.class);
        Log.d(TAG, "OpenHelperDRMUtil getInstance: openhelper_plugin = " + context.getString(R.string.openhelper_plugin));
        return sInstance;
    }

    public OpenHelperDRMUtil() {
    }

    public String getDRMPluginMimeType(Context context, File file, String mimeType, Cursor cursor) {
        Log.d(TAG, "getDRMPluginMimeType enter " );
        return cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_TYPE));
    }

    public Intent setDRMPluginIntent(File file, String mimeType) {
        Log.d(TAG, "setDRMPluginIntent enter " );
        return (new Intent(Intent.ACTION_VIEW));
    }
}
