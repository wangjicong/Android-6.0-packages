package com.sprd.ext;

import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

/**
 * Created by SPRD on 2016/9/18.
 */
public final class LogUtils {

    public static final String MODULE_NAME = "SPRDLauncher3";

    private static LogUtils INSTANCE;

    public static boolean DEBUG_ALL = false;
    public static boolean DEBUG = false;
    public static boolean DEBUG_SL = false;
    public static boolean DEBUG_LOADER = false;
    public static boolean DEBUG_WIDGET = false;
    public static boolean DEBUG_RECEIVER = false;
    public static boolean DEBUG_RESUME_TIME = false;
    public static boolean DEBUG_DUMP_LOG = false;
    public static boolean DEBUG_DEFAULT_PAGE = false;
    public static boolean DEBUG_UNREAD = false;
    public static boolean DEBUG_DYNAMIC_ICON = false;
    public static boolean DEBUG_DYNAMIC_ICON_ALL = false;
    public static boolean DEBUG_PERFORMANCE = false;

    /** use android properties to control debug on/off. */
    // define system properties
    private static final String PROP_DEBUG_ALL = "persist.sys.launcher.all";
    private static final String PROP_DEBUG = "persist.sys.launcher.debug";
    private static final String PROP_DEBUG_SL = "persist.sys.launcher.sl";
    private static final String PROP_DEBUG_LOADER = "persist.sys.launcher.loader";
    private static final String PROP_DEBUG_WIDGET = "persist.sys.launcher.widget";
    private static final String PROP_DEBUG_RECEIVER = "persist.sys.launcher.receiver";
    private static final String PROP_DEBUG_RESUME_TIME = "persist.sys.launcher.resume";
    private static final String PROP_DEBUG_DUMP_LOG = "persist.sys.launcher.dumplog";
    private static final String PROP_DEBUG_DEFAULT_PAGE = "persist.sys.launcher.dftpage";
    private static final String PROP_DEBUG_UNREAD = "persist.sys.launcher.unread";
    private static final String PROP_DEBUG_DYNAMIC_ICON = "persist.sys.launcher.dyicon";
    private static final String PROP_DEBUG_DYNAMIC_ICON_ALL = "persist.sys.launcher.dy.all";
    private static final String PROP_DEBUG_PERFORMANCE = "persist.sys.launcher.perform";
    /** end */

    static {
        DEBUG_ALL = SystemProperties.getBoolean(PROP_DEBUG_ALL, false);
        if (DEBUG_ALL) {
            DEBUG = true;
            DEBUG_SL = true;
            DEBUG_LOADER = true;
            DEBUG_WIDGET = true;
            DEBUG_RECEIVER = true;
            DEBUG_RESUME_TIME = true;
            DEBUG_DUMP_LOG = true;
            DEBUG_DEFAULT_PAGE = true;
            DEBUG_UNREAD = true;
            DEBUG_DYNAMIC_ICON = true;
            DEBUG_DYNAMIC_ICON_ALL= true;
            DEBUG_PERFORMANCE = true;
        } else {
            DEBUG = SystemProperties.getBoolean(PROP_DEBUG, !Build.TYPE.equals("user"));
            DEBUG_SL = SystemProperties.getBoolean(PROP_DEBUG_SL, false);
            DEBUG_LOADER = SystemProperties.getBoolean(PROP_DEBUG_LOADER, DEBUG);
            DEBUG_WIDGET = SystemProperties.getBoolean(PROP_DEBUG_WIDGET, false);
            DEBUG_RECEIVER = SystemProperties.getBoolean(PROP_DEBUG_RECEIVER, DEBUG);
            DEBUG_RESUME_TIME = SystemProperties.getBoolean(PROP_DEBUG_RESUME_TIME, false);
            DEBUG_DUMP_LOG = SystemProperties.getBoolean(PROP_DEBUG_DUMP_LOG, false);
            DEBUG_DEFAULT_PAGE = SystemProperties.getBoolean(PROP_DEBUG_DEFAULT_PAGE, false);
            DEBUG_UNREAD = SystemProperties.getBoolean(PROP_DEBUG_UNREAD, false);
            DEBUG_DYNAMIC_ICON = SystemProperties.getBoolean(PROP_DEBUG_DYNAMIC_ICON, false);
            DEBUG_DYNAMIC_ICON_ALL = DEBUG_DYNAMIC_ICON &&
                SystemProperties.getBoolean(PROP_DEBUG_DYNAMIC_ICON_ALL, false);
            DEBUG_PERFORMANCE = SystemProperties.getBoolean(PROP_DEBUG_PERFORMANCE, false);
        }
    }
    /**
     * private constructor here, It is a singleton class.
     */
    private LogUtils() {
    }


    public static LogUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LogUtils();
        }
        return INSTANCE;
    }

    /**
     * The method prints the log, level error.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void e(String tag, String msg) {
        Log.e(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level error.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void e(String tag, String msg, Throwable t) {
        Log.e(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level warning.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void w(String tag, String msg) {
        Log.w(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level warning.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void w(String tag, String msg, Throwable t) {
        Log.w(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void i(String tag, String msg) {
        Log.i(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t an exception to log.
     */
    public static void i(String tag, String msg, Throwable t) {
        Log.i(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void d(String tag, String msg) {
        Log.d(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t An exception to log.
     */
    public static void d(String tag, String msg, Throwable t) {
        Log.d(MODULE_NAME, tag + ", " + msg, t);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     */
    public static void v(String tag, String msg) {
        Log.v(MODULE_NAME, tag + ", " + msg);
    }

    /**
     * The method prints the log, level debug.
     *
     * @param tag the tag of the class.
     * @param msg the message to print.
     * @param t An exception to log.
     */
    public static void v(String tag, String msg, Throwable t) {
        Log.v(MODULE_NAME, tag + ", " + msg, t);
    }

}
