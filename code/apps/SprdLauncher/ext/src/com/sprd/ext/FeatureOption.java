package com.sprd.ext;

import android.os.SystemProperties;

/**
 * Created by SPRD on 2016/9/27.
 */
public class FeatureOption {
    public static final String TAG = "FeatureOption";

    public static final boolean SPRD_SETTINGS_ACTIVITY_SUPPORT = true;

    //SPRD add for SPRD_SINGLE_LAYER_SUPPORT start {
    public static final boolean SPRD_SINGLE_LAYER_SUPPORT = getProp("ro.launcher.singlelayer", true);
    //end }

    //SPRD Add for finger gestures
    public static final boolean SPRD_FINGER_GESTURE_SUPPORT = getProp("ro.launcher.multigesture", true);

    //SPRD Add for two-finger scale reduction into the OverView Mode
    public static final boolean SPRD_FINGER_SCALE_TO_EDIT_SUPPORT =
            SPRD_FINGER_GESTURE_SUPPORT && getProp("ro.launcher.zoomoutedit", true);

    //SPRD add for SPRD_FOLDER_MODEL_SUPPORT
    public static final boolean SPRD_FOLDER_MODEL_SUPPORT = getProp("ro.launcher.foldermodel", true);

    //SPRD add for SPRD_SET_DEFAULT_HOME
    public static final boolean SPRD_SET_DEFAULT_HOME = getProp("ro.launcher.defaulthome", true);

    //SPRD add for SPRD_UNREAD_INFO_SUPPORT
    public static final boolean SPRD_UNREAD_INFO_SUPPORT = getProp("ro.launcher.unreadinfo", true);

    //SPRD add for SPRD_DYNAMIC_ICON_SUPPORT
    public static final boolean SPRD_DYNAMIC_ICON_SUPPORT = getProp("ro.launcher.dynamicicon", false);
    public static final boolean SPRD_DYNAMIC_CLOCK_SUPPORT =
            SPRD_DYNAMIC_ICON_SUPPORT && getProp("ro.launcher.dynamicclock", true);
    public static final boolean SPRD_DYNAMIC_CALENDAR_SUPPORT =
            SPRD_DYNAMIC_ICON_SUPPORT && getProp("ro.launcher.dynamiccalendar", true);

    //SPRD add for SPRD_CIRCLE_SLIDE_SUPPORT start {
    public static final boolean SPRD_CIRCLE_SLIDE_SUPPORT = getProp("ro.launcher.circleslide", true);
    //SPRD: SPRD_CIRCLE_SLIDE_SUPPORT end }

    //SPRD Add for customize app feature
    public static final boolean SPRD_CUSTOMIZEAPPSORT_SUPPORT = getProp("ro.launcher.customizeappsort", false);

    //SPRD Add for workspace animation SPRD_SWITCH_EFFECTS_SUPPORT
    public static final boolean SPRD_SWITCH_EFFECTS_SUPPORT = getProp("ro.launcher.switcheffects", true);

    //SPRD Add for lock screen wallpaper
    public static final boolean SPRD_LOCK_WALLPAPER_SUPPORT = getProp("ro.feature.lockwallpaper", false);

    //SPRD add for SPRD_ADAPTIVE_WALLPAPER_SUPPORT
    public static final boolean SPRD_ADAPTIVE_WALLPAPER_SUPPORT = getProp("ro.launcher.adapterwallpaper", false);

    //SPRD add for SPRD_DEFAULT_FOLDER_NAME
    public static final boolean SPRD_DEFAULT_FOLDER_NAME_SUPPORT = getProp("ro.launcher.defaultfoldername", true);

    private static boolean getProp(String prop, boolean devalues) {
        boolean ret = false;

        try {
            ret = SystemProperties.getBoolean(prop, devalues);
        } catch (Exception e) {
            LogUtils.e(TAG, "getProp:" + prop + " error." + e);
        }

        return ret;
    }
}
