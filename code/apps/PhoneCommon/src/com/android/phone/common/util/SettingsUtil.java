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
 * limitations under the License
 */

package com.android.phone.common.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.phone.common.R;

import java.lang.CharSequence;
import java.lang.String;
import java.io.File;

public class SettingsUtil {
    private static final String DEFAULT_NOTIFICATION_URI_STRING =
            Settings.System.DEFAULT_NOTIFICATION_URI.toString();
    private static final String TAG = "SettingsUtil";
    private static final boolean DBG = true;

    /**
     * Queries for a ringtone name, and sets the name using a handler.
     * This is a method was originally copied from com.android.settings.SoundSettings.
     *
     * @param context The application context.
     * @param handler The handler, which takes the name of the ringtone as a String as a parameter.
     * @param type The type of sound.
     * @param key The key to the shared preferences entry being updated.
     * @param msg An integer identifying the message sent to the handler.
     */
    public static void updateRingtoneName(
            Context context, Handler handler, int type, String key, int msg) {
        final Uri ringtoneUri;
        boolean defaultRingtone = false;
        if (type == RingtoneManager.TYPE_RINGTONE) {
            // For ringtones, we can just lookup the system default because changing the settings
            // in Call Settings changes the system default.
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        } else {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            // For voicemail notifications, we use the value saved in Phone's shared preferences.
            String uriString = prefs.getString(key, DEFAULT_NOTIFICATION_URI_STRING);
            if (TextUtils.isEmpty(uriString)) {
                // silent ringtone
                ringtoneUri = null;
            } else {
                if (uriString.equals(DEFAULT_NOTIFICATION_URI_STRING)) {
                    // If it turns out that the voicemail notification is set to the system
                    // default notification, we retrieve the actual URI to prevent it from showing
                    // up as "Unknown Ringtone".
                    defaultRingtone = true;
                    ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
                } else {
                    ringtoneUri = Uri.parse(uriString);
                }
            }
        }
        CharSequence summary = context.getString(R.string.ringtone_unknown);
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(R.string.ringtone_silent);
        } else {
            // Fetch the ringtone title from the media provider
            final Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
            if (ringtone != null) {
                try {
                    final String title = ringtone.getTitle(context);
                    if (!TextUtils.isEmpty(title)) {
                        summary = title;
                    }
                } catch (SQLiteException sqle) {
                    // Unknown title for the ringtone
                }
            }
        }
        if (defaultRingtone) {
            summary = context.getString(R.string.default_notification_description, summary);
        }
        handler.sendMessage(handler.obtainMessage(msg, summary));
    }

    /**
     * SPRD: add for bug 544002 to set default ringtone when file is deleted
     * Queries for a ringtone name, and sets the name using a handler.
     * This is a method was originally copied from com.android.settings.SoundSettings.
     *
     * @param context The application context.
     * @param handler The handler, which takes the name of the ringtone as a String as a parameter.
     * @param type The type of sound.
     * @param key The key to the shared preferences entry being updated.
     * @param msg An integer identifying the message sent to the handler.
     */
    public static void updateRingtoneNameEx(
            Context context, Handler handler, int type, String key, int msg) {
        Uri ringtoneUri;
        boolean defaultRingtone = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (type == RingtoneManager.TYPE_RINGTONE) {
            // For ringtones, we can just lookup the system default because changing the settings
            // in Call Settings changes the system default.
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
        } else {
            // For voicemail notifications, we use the value saved in Phone's shared preferences.
            String uriString = prefs.getString(key, DEFAULT_NOTIFICATION_URI_STRING);
            if (TextUtils.isEmpty(uriString)) {
                // silent ringtone
                ringtoneUri = null;
            } else {
                if (uriString.equals(DEFAULT_NOTIFICATION_URI_STRING)) {
                    // If it turns out that the voicemail notification is set to the system
                    // default notification, we retrieve the actual URI to prevent it from showing
                    // up as "Unknown Ringtone".
                    defaultRingtone = true;
                    ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
                } else {
                    ringtoneUri = Uri.parse(uriString);
                }
            }
        }
        CharSequence summary = context.getString(R.string.ringtone_unknown);
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(R.string.ringtone_silent);
        } else {
            // Fetch the ringtone title from the media provider
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(
                        ringtoneUri,
                        new String[] {MediaStore.Audio.Media.DATA},
                        null,
                        null,
                        null);
                if (cursor != null && cursor.getCount() > 0) {
                    if (cursor.moveToFirst()) {
                        File filePath = new File(cursor.getString(0));
                        if (!filePath.exists()) { // exist in db but the
                            // file is deleted
                            if (DBG) Log.d(TAG, "filePath is not exists");
                            defaultRingtone = true;
                            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(
                                    context, type);
                        }
                    }
                } else {
                    if (DBG) Log.d(TAG, "cursor is null");
                    defaultRingtone = true;
                    ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
                }
            } catch (SQLiteException sqle) {
                if (DBG) Log.d(TAG, sqle.toString());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            final Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
            if (ringtone != null) {
                try {
                    final String title = ringtone.getTitle(context);
                    if (DBG) Log.d(TAG, "titled = " + title);
                    if (!TextUtils.isEmpty(title)) {
                        summary = title;
                    }
                } catch (SQLiteException sqle) {
                    // Unknown title for the ringtone
                }
            }
        }
        if (defaultRingtone) {
            summary = context.getString(R.string.default_notification_description, summary);
        }
        handler.sendMessage(handler.obtainMessage(msg, summary));
    }
}
