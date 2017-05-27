package com.sprd.ext.circular;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.sprdlauncher3.LauncherFiles;
import com.android.sprdlauncher3.R;
import com.sprd.ext.UtilitiesExt;


/**
 * Created by sprd on 2016/9/18.
 * You can search the key word "circular" to look all modifiy about this.
 */
public class CircleSlideUtils {

    public static final String PREF_CIRCLE_SLIDE_KEY = "pref_allowCircularSliding";
    public static final String HAS_CUSTOMCONTENT_KEY = "has_customcontent";

    public static boolean isCircleSlideEnabled(Context context) {
        return UtilitiesExt.getLauncherSettingsBoolean(context, PREF_CIRCLE_SLIDE_KEY, context.getResources().getBoolean(R.bool.allow_circle_slide));
    }

    public static void setHasCustomContent(Context context,boolean bool){
        SharedPreferences.Editor editor = getLauncherSharedPref(context).edit();

        editor.putBoolean(HAS_CUSTOMCONTENT_KEY, bool);
        editor.apply();
    }

    //return Launcher ishas CustomContent
    public static boolean hasCustomContent(Context context){
        return getLauncherSharedPref(context).getBoolean(HAS_CUSTOMCONTENT_KEY,false);
    }

    private static SharedPreferences getLauncherSharedPref (Context context) {
        return context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

}