package com.android.providers.downloadsplugin;

import java.io.File;

import android.app.Activity;
import android.app.AddonManager;
import android.content.Context;
import com.android.providers.downloads.DownloadReceiver;
import com.android.providers.downloads.DownloadThread;
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
import com.android.providers.downloads.DownloadThread.DownloadInfoDelta;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import com.android.providers.downloads.R;
import android.database.Cursor;

public class DownloadsDRMUtils {

    static DownloadsDRMUtils sInstance;
    private static final String TAG = "DownloadsDRMUtils_BlankClass";

    public static final int DRM_PLUGIN = 0;
    public static final int HELPER_FULLNAME_RETURN = 1;
    public static final int HELPER_SUQENCE_CONTINUE = 2;

    public static DownloadsDRMUtils getInstance(Context context) {
        Log.d(TAG, "DownloadsDRMUtils getInstance");
        /* SPRD 424923 @{ */
        if (sInstance != null) {
            return sInstance;
        }
        /* @} */
        AddonManager addonManager = new AddonManager(context);
        sInstance = (DownloadsDRMUtils) addonManager.getAddon(R.string.downloads_plugin, DownloadsDRMUtils.class);
        Log.d(TAG, "DownloadsDRMUtils getInstance: downloads_plugin = " + context.getString(R.string.downloads_plugin));
        return sInstance;
    }

    public DownloadsDRMUtils() {
    }

    public void deleteDatabase(Context context, Intent intent) {
        return;
    }

    public boolean handleDRMNotificationBroadcast(Context context, Intent intent) {
        return false;
    }

    public String getDRMDisplayName(Cursor cursor, String mimeType) {
        return cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
    }

    public boolean isSupportDRM() {
        return false;
    }

    public String getDRMFileName(String filename, String extension) {
        return (filename + extension);
    }

    public int getDRMSequence(String filename, String extension, int sequence) {
        return DRM_PLUGIN;
    }

    /**
     * Checks if the Media Type needs to be DRM converted
     *
     * @param mimetype Media type of the content
     * @return True if convert is needed else false
     */
    public boolean isDrmConvertNeeded(String mimetype) {
        return false;
    }

    /**
     * Modifies the file extension for a DRM Forward Lock file NOTE: This
     * function shouldn't be called if the file shouldn't be DRM converted
     */
    public String modifyDrmFwLockFileExtension(String filename) {
        return filename;
    }

    public void notifyDownloadCompleted(DownloadThread downloadthread, Context context, DownloadInfoDelta state, int finalStatus, String errorMsg, int numFailed) {
        return;
    }

    public String getDRMPluginMimeType(Context context, File file, String mimeType, Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_TYPE));
    }

    public Intent setDRMPluginIntent(File file, String mimeType) {
        return (new Intent(Intent.ACTION_VIEW));
    }

    public boolean checkDRMFileName(String filename, int sequence, String extension) {
        return false;
    }
}
