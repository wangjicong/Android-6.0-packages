package com.android.sprdlauncher3;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;

import com.sprd.ext.FeatureOption;

import java.io.IOException;
import java.io.InputStream;

class WallpaperManagerController {

    /**
     * The method {@link #setStream} should be adapted to different android version.
     * Calling wallpaperManager.setStream(InputStream) as default here.
     * For instance, wallpaperManager.setStream(data, visibleCropHint,
     * allowBackup, whichWallpaper) should be called in android N.
     *
     * @throws IOException If an error occurs when attempting to set the wallpaper
     * based on the provided image data.
     */
    public static void setStream(Context context, final InputStream data, Rect visibleCropHint,
            boolean allowBackup, int whichWallpaper) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        wallpaperManager.setStream(data);
    }

    /**
     * The method {@link #clear} should be adapted to different android version.
     * Calling wallpaperManager.clear() as default here.
     * For instance, wallpaperManager.clear(whichWallpaper) should be called in android N.
     *
     * @throws IOException If an error occurs reverting to the built-in
     * wallpaper.
     */
    public static void clear(Context context, int whichWallpaper) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        wallpaperManager.clear();
    }
}

/**
 * Utility class used to help set lockscreen wallpapers.
 */
public class NycWallpaperUtils {

    /**
     * Flag: set or retrieve the general system wallpaper.
     */
    public static final int FLAG_SYSTEM = 1 << 0;

    /**
     * Flag: set or retrieve the lock-screen-specific wallpaper.
     */
    public static final int FLAG_LOCK = 1 << 1;

    /**
     * Android 7.0
     */
    public static final int SDK_VERSION_N = 24;

    public static final boolean ATLEAST_N = Build.VERSION.SDK_INT >= SDK_VERSION_N;

    public static final boolean SUPPORT_LOCK_WALLPAPER = ATLEAST_N || FeatureOption.SPRD_LOCK_WALLPAPER_SUPPORT;

    /**
     * Calls cropTask.execute(), once the user has selected which wallpaper to set. On pre-N
     * devices, the prompt is not displayed since there is no API to set the lockscreen wallpaper.
     */
    public static void executeCropTaskAfterPrompt(
            Context context, final AsyncTask<Integer, ?, ?> cropTask,
            DialogInterface.OnCancelListener onCancelListener) {
        if (SUPPORT_LOCK_WALLPAPER) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.wallpaper_instructions)
                    .setItems(R.array.which_wallpaper_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int selectedItemIndex) {
                            int whichWallpaper;
                            if (selectedItemIndex == 0) {
                                whichWallpaper = FLAG_SYSTEM;
                            } else if (selectedItemIndex == 1) {
                                whichWallpaper = FLAG_LOCK;
                            } else {
                                whichWallpaper = FLAG_SYSTEM | FLAG_LOCK;
                            }
                            cropTask.execute(whichWallpaper);
                        }
                    })
                    .setOnCancelListener(onCancelListener)
                    .show();
        } else {
            cropTask.execute(FLAG_SYSTEM);
        }
    }

    public static void setStream(Context context, final InputStream data, Rect visibleCropHint,
            boolean allowBackup, int whichWallpaper) throws IOException {
        if (SUPPORT_LOCK_WALLPAPER) {
            WallpaperManagerController.setStream(context, data, visibleCropHint, allowBackup, whichWallpaper);
        } else {
            // Fall back to previous implementation (set system)
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            wallpaperManager.setStream(data);
        }
    }

    public static void clear(Context context, int whichWallpaper) throws IOException {
        if (SUPPORT_LOCK_WALLPAPER) {
            WallpaperManagerController.clear(context, whichWallpaper);
        } else {
            // Fall back to previous implementation (clear system)
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            wallpaperManager.clear();
        }
    }
}