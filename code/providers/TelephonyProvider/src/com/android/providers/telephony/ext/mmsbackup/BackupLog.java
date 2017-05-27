
package com.android.providers.telephony.ext.mmsbackup;

import android.util.Log;

public class BackupLog {
    final static String TAG = "MmsBackup";
    final static boolean DEBUG_E = true;
    final static boolean DEBUG_I = true;

    public static void log(String tag, String log) {
        if (DEBUG_I) {
            Log.i(TAG, tag + "  " + log);
        }
    }

    public static void log(String tag, String log, Throwable err) {
        if (DEBUG_E) {
            Log.e(TAG, tag + "  " + log, err);
        }
    }

    public static void log(String log) {
        if (DEBUG_I) {
            Log.i(TAG, log);
        }
    }

    public static void logE(String log) {
        Log.e(TAG, log);
    }

    public static void logE(String tag, String log) {
        Log.e(TAG, tag + "  " + log);
    }
}
