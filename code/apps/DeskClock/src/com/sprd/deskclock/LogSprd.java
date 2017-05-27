package com.sprd.deskclock;

public class LogSprd {
    public final static String LOGTAG = "AlarmClock";

    /** This must be false for production.  If true, turns on logging,
     test code, etc. */
    public static final boolean LOGV = false;

    public static void d(String logMe) {
        android.util.Log.d(LOGTAG, logMe);
    }

    public static void v(String logMe) {
        android.util.Log.v(LOGTAG, logMe);
    }

    public static void i(String logMe) {
        android.util.Log.i(LOGTAG, logMe);
    }

    public static void e(String logMe) {
        android.util.Log.e(LOGTAG, logMe);
    }

    public static void e(String logMe, Exception ex) {
        android.util.Log.e(LOGTAG, logMe, ex);
    }

    public static void w(String logMe) {
        android.util.Log.w(LOGTAG, logMe);
    }

    public static void wtf(String logMe) {
        android.util.Log.wtf(LOGTAG, logMe);
    }
}
