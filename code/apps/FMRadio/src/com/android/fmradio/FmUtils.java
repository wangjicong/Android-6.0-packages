/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.fmradio;

import android.content.Context;
import android.content.SharedPreferences;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
* SPRD:
*
* @{
*/
import java.io.File;
import java.io.IOException;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.SystemProperties;
import android.provider.Settings;

import com.sprd.android.config.OptConfig;

/**
* @}
*/

/**
 * This class provider interface to compute station and frequency, get project
 * string
 */
public class FmUtils {
    private static final String TAG = "FmUtils";

    /**
     * SPRD: bug490888 Seek station with 50kHz step.
     * Original Android code:
    // FM station variables
    public static final int DEFAULT_STATION = 1000;
    public static final float DEFAULT_STATION_FLOAT = computeFrequency(DEFAULT_STATION);
    // maximum station frequency
    private static final int HIGHEST_STATION = 1080;
    // minimum station frequency
    private static final int LOWEST_STATION = 875;
    // station step
    private static final int STEP = 1;
    // convert rate
    private static final int CONVERT_RATE = 10;
     * @{
     */
    public static final boolean support50ksearch = "1".equals(SystemProperties.get("persist.support.50ksearch", "0"));
    // FM station variables
    public static final int DEFAULT_STATION = support50ksearch ? 10000 : 1000;
    // convert rate
    public static final int CONVERT_RATE = support50ksearch ? 100 :10;
    public static final float DEFAULT_STATION_FLOAT = computeFrequency(DEFAULT_STATION);
    // maximum station frequency
    //sunvov:dlj add for change toast for fm88.0~107.9 161012 start
    private static final int HIGHEST_STATION = OptConfig.SUN_SUBCUSTOM_S7358_HWD_MITO_QHD || OptConfig.SUNVOV_CUSTOM_C7368_BP_MITO_FWVGA ? (support50ksearch ? 10790 : 1079) : (support50ksearch ? 10800 : 1080);
    // minimum station frequency
    private static final int LOWEST_STATION = OptConfig.SUN_SUBCUSTOM_S7358_HWD_MITO_QHD || OptConfig.SUNVOV_CUSTOM_C7368_BP_MITO_FWVGA ? (support50ksearch ? 8800 : 880) : (support50ksearch ? 8750 : 875);
    //sunvov:dlj add for change toast for fm88.0~107.9 161012 end
    // station step
    private static final int STEP = support50ksearch ? 5 : 1;
    /**
     * @}
     */


    // minimum storage space for record (512KB).
    // Need to check before starting recording and during recording to avoid
    // recording keeps going but there is no free space in sdcard.
    public static final long LOW_SPACE_THRESHOLD = 512 * 1024;
    // Different city may have different RDS information.
    // We define 100 miles (160934.4m) to distinguish the cities.
    public static final double LOCATION_DISTANCE_EXCEED = 160934.4;
    private static final String FM_LOCATION_LATITUDE = "fm_location_latitude";
    private static final String FM_LOCATION_LONGITUDE = "fm_location_longitude";
    private static final String FM_IS_FIRST_TIME_PLAY = "fm_is_first_time_play";
    private static final String FM_IS_SPEAKER_MODE = "fm_is_speaker_mode";
    private static final String FM_IS_FIRST_ENTER_STATION_LIST = "fm_is_first_enter_station_list";
    // StorageManager For FM record
    private static StorageManager sStorageManager = null;

    public static float getHighestFrequency() {
        return computeFrequency(HIGHEST_STATION);
    }

    /**
     * Whether the frequency is valid.
     *
     * @param station The FM station
     *
     * @return true if the frequency is in the valid scale, otherwise return
     *         false
     */
    public static boolean isValidStation(int station) {
        boolean isValid = (station >= LOWEST_STATION && station <= HIGHEST_STATION);
        return isValid;
    }

    /**
     * Compute increase station frequency
     *
     * @param station The station frequency
     *
     * @return station The frequency after increased
     */
    public static int computeIncreaseStation(int station) {
        int result = station + STEP;
        if (result > HIGHEST_STATION) {
            result = LOWEST_STATION;
        }
        return result;
    }

    /**
     * Compute decrease station frequency
     *
     * @param station The station frequency
     *
     * @return station The frequency after decreased
     */
    public static int computeDecreaseStation(int station) {
        int result = station - STEP;
        if (result < LOWEST_STATION) {
            result = HIGHEST_STATION;
        }
        return result;
    }

    /**
     * Compute station value with given frequency
     *
     * @param frequency The station frequency
     *
     * @return station The result value
     */
    public static int computeStation(float frequency) {
        return (int) (frequency * CONVERT_RATE);
    }

    /**
     * Compute frequency value with given station
     *
     * @param station The station value
     *
     * @return station The frequency
     */
    public static float computeFrequency(int station) {
        return (float) station / CONVERT_RATE;
    }

    /**
     * According station to get frequency string
     *
     * @param station for 100KZ, range 875-1080
     *
     * @return string like 87.5
     */
    public static String formatStation(int station) {
        float frequency = (float) station / CONVERT_RATE;
        /**
         * SPRD: bug490888 Seek station with 50kHz step.
         * Original Android code:
        DecimalFormat decimalFormat = new DecimalFormat("0.0");
         * @{
         */
        DecimalFormat decimalFormat;
        if (support50ksearch) {
            decimalFormat = new DecimalFormat("0.00");
        } else {
            decimalFormat = new DecimalFormat("0.0");
        }
        /**
         * @}
         */
        decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        return decimalFormat.format(frequency);
    }

    /**
     * Get the phone storage path
     *
     * @return The phone storage path
     */
    public static String getDefaultStoragePath() {
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStoragePathState())) {
                String selectLocation = getGlobalStorageType();
                if(STORAGE_DEVICE.equals(selectLocation)){
                    return Environment.getInternalStoragePath().getCanonicalPath();
                } else {
                    return Environment.getExternalStoragePath().getCanonicalPath();
                }
            } else {
                return Environment.getInternalStoragePath().getCanonicalPath();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error when getStoragePath ");
            return null;
        }

    }
    /**
     * SPRD: bug469133, return storage path type according to the priority settings.
     * @{
     */
    private static final String STORAGE_DEVICE = "storate_device";
    private static final String STORAGE_SDCARD = "storate_sdcard";
    private static final String DEFAULT_STORAGE_LOCATION = "persist.sys.storageLocation";

    private static String getGlobalStorageType() {
        String selectedLocation = SystemProperties.get(DEFAULT_STORAGE_LOCATION, STORAGE_DEVICE);
        if (selectedLocation.equals(STORAGE_SDCARD)) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStoragePathState())) {
                SystemProperties.set(DEFAULT_STORAGE_LOCATION, STORAGE_DEVICE);
                Log.d(TAG, "SDcard is removed");
                return STORAGE_DEVICE;
            }
        }
        return selectedLocation;
    }
    /**
     * @}
     */
    /**
     * Get the default storage state
     *
     * @return The default storage state
     */
    public static String getDefaultStorageState(Context context) {
        ensureStorageManager(context);
        String state = sStorageManager.getVolumeState(getDefaultStoragePath());
        return state;
    }

    private static void ensureStorageManager(Context context) {
        if (sStorageManager == null) {
            sStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        }
    }

    /**
     * Get the FM play list path
     *
     * @param context The context
     *
     * @return The FM play list path
     */
    public static String getPlaylistPath(Context context) {
        ensureStorageManager(context);
        String[] externalStoragePaths = sStorageManager.getVolumePaths();
        String path = externalStoragePaths[0] + "/Playlists/";
        return path;
    }

    /**
     * Check if has enough space for record
     *
     * @param recordingSdcard The recording sdcard path
     *
     * @return true if has enough space for record
     */
    public static boolean hasEnoughSpace(String recordingSdcard) {
        boolean ret = false;
        try {
            StatFs fs = new StatFs(recordingSdcard);
            long blocks = fs.getAvailableBlocks();
            long blockSize = fs.getBlockSize();
            long spaceLeft = blocks * blockSize;
            ret = spaceLeft > LOW_SPACE_THRESHOLD ? true : false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "hasEnoughSpace, sdcard may be unmounted:" + recordingSdcard);
        }
        return ret;
    }

    /**
     * Get the latest searched location
     * @return the list of latitude and longitude
     */
    public static double[] getLastSearchedLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String strLatitude = prefs.getString(FM_LOCATION_LATITUDE, "0.0");
        String strLongitude = prefs.getString(FM_LOCATION_LONGITUDE, "0.0");
        double latitude = Double.valueOf(strLatitude);
        double longitude = Double.valueOf(strLongitude);
        return new double[] { latitude, longitude };
    }

    /**
     * Set the last searched location
     */
    public static void setLastSearchedLocation(Context context, double latitude, double longitude) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String strLatitude = Double.valueOf(latitude).toString();
        String strLongitude = Double.valueOf(longitude).toString();
        editor.putString(FM_LOCATION_LATITUDE, strLatitude);
        editor.putString(FM_LOCATION_LONGITUDE, strLongitude);
        editor.commit();
    }

    /**
     * check it is the first time to use Fm
     */
    public static boolean isFirstTimePlayFm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstTime = prefs.getBoolean(FM_IS_FIRST_TIME_PLAY, true);
        return isFirstTime;
    }

    /**
     * Called when first time play FM.
     * @param context The context
     */
    public static void setIsFirstTimePlayFm(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FM_IS_FIRST_TIME_PLAY, false);
        editor.commit();
    }

    /**
     * check it is the first time enter into station list page
     */
    public static boolean isFirstEnterStationList(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isFirstEnter = prefs.getBoolean(FM_IS_FIRST_ENTER_STATION_LIST, true);
        if (isFirstEnter) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(FM_IS_FIRST_ENTER_STATION_LIST, false);
            editor.commit();
        }
        return isFirstEnter;
    }

    /**
     * Create the notification large icon bitmap from layout
     * @param c The context
     * @param text The frequency text
     * @return The large icon bitmap with frequency text
     */
    public static Bitmap createNotificationLargeIcon(Context c, String text) {
        Resources res = c.getResources();
        int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        LinearLayout iconLayout = new LinearLayout(c);
        iconLayout.setOrientation(LinearLayout.VERTICAL);
        iconLayout.setBackgroundColor(c.getResources().getColor(R.color.theme_primary_color));
        iconLayout.setDrawingCacheEnabled(true);
        iconLayout.layout(0, 0, width, height);
        TextView iconText = new TextView(c);
        /**
         * SPRD: bug523048, textsize is too large.
         * Original Android code:
        iconText.setTextSize(24.0f);
         * @{
         */
        iconText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.0f);
        /**
         * @}
         */
        iconText.setTextColor(res.getColor(R.color.theme_title_color));
        iconText.setText(text);
        iconText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int left = (int) ((width - iconText.getMeasuredWidth()) * 0.5);
        int top = (int) ((height - iconText.getMeasuredHeight()) * 0.5);
        iconText.layout(left, top, iconText.getMeasuredWidth() + left,
                iconText.getMeasuredHeight() + top);
        iconLayout.addView(iconText);
        iconLayout.layout(0, 0, width, height);

        iconLayout.buildDrawingCache();
        Bitmap largeIcon = Bitmap.createBitmap(iconLayout.getDrawingCache());
        iconLayout.destroyDrawingCache();
        return largeIcon;
    }

    /**
     * Get whether speaker mode is in use when audio focus lost.
     * @param context the Context
     * @return true for speaker mode, false for non speaker mode
     */
    public static boolean getIsSpeakerModeOnFocusLost(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getBoolean(FM_IS_SPEAKER_MODE, false);
    }

    /**
     * Set whether speaker mode is in use.
     * @param context the Context
     * @param isSpeaker speaker state
     */
    public static void setIsSpeakerModeOnFocusLost(Context context, boolean isSpeaker) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FM_IS_SPEAKER_MODE, isSpeaker);
        editor.commit();
    }

    /**
     * SPRD: bug495859, request runtime permissions
     *
     */
    public static final int FM_PERMISSIONS_REQUEST_CODE = 2016;

    public static boolean hasLocationPermission(Context context) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public static boolean checkBuildRecordingPermission(Context context) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        int numPermissionsToRequest = 0;
        boolean requestMicrophonePermission = false;
        boolean requestStoragePermission = false;

        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicrophonePermission = true;
            numPermissionsToRequest++;
        }

        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission = true;
            numPermissionsToRequest++;
        }
        String[] permissionsToRequest = new String[numPermissionsToRequest];
        int permissionsRequestIndex = 0;
        if (requestMicrophonePermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.RECORD_AUDIO;
            permissionsRequestIndex++;
        }
        if (requestStoragePermission) {
            permissionsToRequest[permissionsRequestIndex] = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            permissionsRequestIndex++;
        }
        ((FmRecordActivity)context).requestPermissions(permissionsToRequest, FM_PERMISSIONS_REQUEST_CODE);
        return false;
    }

    public static boolean hasStoragePermission(Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }
}
