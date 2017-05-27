/** Create by Spreadst */
package com.sprd.settings.timerpower;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Debug;

import android.util.Config;

class Log {
    public final static String LOGTAG = "TimerPower";
//    private static final boolean DEBUG = Debug.isDebug();
    private static final boolean DEBUG = true;

    static final boolean LOGV = AlarmClock.DEBUG ? Config.LOGD : Config.LOGV;

    static void v(String logMe) {
        if (DEBUG) android.util.Log.v(LOGTAG, logMe);
    }

    static void i(String logMe) {
        android.util.Log.i(LOGTAG, logMe);
    }

    static void e(String logMe) {
        android.util.Log.e(LOGTAG, logMe);
    }

    static void e(String logMe, Exception ex) {
        android.util.Log.e(LOGTAG, logMe, ex);
    }

    static void wtf(String logMe) {
        android.util.Log.wtf(LOGTAG, logMe);
    }

    static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss.SSS aaa").format(new Date(millis));
    }
}
