/*
 *spreadtrum Communication Inc.
 */
package com.sprd.settings;

import android.os.SystemProperties;
import android.app.Activity;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;

public class HeartBeatSyncSettingsHelper {

    private static String TAG = "HeartBeatSyncSettingsHelper";

    /* defaults */
    private static String sHeartBeatsPackage = "com.sprd.heartbeatsynchronization";
    private static String sHeartBeatsClass = "com.sprd.heartbeatsynchronization.MainActivity";

    private static Intent sIntent;

    static {
        sIntent = new Intent(Intent.ACTION_MAIN);
        ComponentName component = new ComponentName(sHeartBeatsPackage,
                sHeartBeatsClass);
        sIntent.setComponent(component);
    }

    private static HeartBeatSyncSettingsHelper sInstance;

    /**
     * Prepare the helper instance
     */
    public static void prepare(Context context) {
        if (sInstance != null) return;

        // Lazy instialize
        synchronized (HeartBeatSyncSettingsHelper.class) {
            if (sInstance != null) return;
            sInstance = new HeartBeatSyncSettingsHelper();
        }

        // To avoid memory leak, use this.
        sInstance.mContext = context.getApplicationContext();
        // For overlay or other implments, leave this interface
        sIntent = sInstance.rebuildIntentIfNeeded();
    }

    public static HeartBeatSyncSettingsHelper getInstance() {
        if (sInstance == null) {
            prepare(ActivityThread.currentApplication());
        }
        return sInstance;
    }

    private Context mContext;
    private boolean mAvailable = false;

    /**
    * Rebuild the launch intent if the class be overlayed,
     * currently no rules for it, only keep interface.
     */
    private Intent rebuildIntentIfNeeded() {
        // get overlayed resouces
        // assume if equals
        // update the intent component
        return sIntent;
    }

    /**
     * When the package modified, call this to update available state.
     */
    public void callPackageModified(String packageName, boolean enabled) {
        onPackageModified(packageName, enabled);
    }

    private void onPackageModified(String packageName, boolean enabled) {
        if (packageName.equals(sHeartBeatsPackage)) {
            setAvailable(enabled);
        }
    }

    /**
     * Set the heart beats application is avaiable
     */
    public void setAvailable(boolean available) {
        mAvailable = available;
    }

    /**
     * Wether the expected package installed
     */
    public boolean isAvailable() {
        // Currently use oneshot query if no more feature request
        // or performance sides.
        PackageManager pm = mContext.getPackageManager();
        List<?> results = pm.queryIntentActivities(sIntent, 0);
        boolean packageInstalled = results != null && !results.isEmpty();
        boolean powerGuruEnabled = (mContext
                .getSystemService(Context.POWERGURU_SERVICE) != null);
        boolean ifEnable;
        if(SystemProperties.getInt("persist.sys.heartbeat.enable", 1) == 0) {
            ifEnable = false;
        } else {
            ifEnable = true;
        }
        Log.v(TAG,"isAvailable : packageInstalled:"+packageInstalled+" powerGuruEnabled:"+powerGuruEnabled+" ifEnable:"+ifEnable);
        return packageInstalled && powerGuruEnabled && ifEnable;
    }

    /**
     * It is much more safety to use a activity to launch a activity
     */
    public void launchHeartBeatSyncActivity(Activity activity) {
        // Copy a new instance to avoid modfy by others.
        Intent intent = new Intent(sIntent);
        activity.startActivity(intent);
    }
}
