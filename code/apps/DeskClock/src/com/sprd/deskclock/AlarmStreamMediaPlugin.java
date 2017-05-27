package com.sprd.deskclock;

import android.app.AddonManager;
import com.android.deskclock.R;
import android.content.Context;
import android.util.Log;

public class AlarmStreamMediaPlugin {
    static AlarmStreamMediaPlugin sInstance;
    private static final String TAG = "AlarmStreamMediaPlugin";
    private static Context mAddonContext;

    public static AlarmStreamMediaPlugin getInstance(Context context) {
        Log.d(TAG, "AlarmStreamMediaPlugin getInstance");
        AddonManager addonManager = new AddonManager(context);
        mAddonContext = context;
        sInstance = (AlarmStreamMediaPlugin) addonManager.getAddon(R.string.alarm_streammedia_plugin, AlarmStreamMediaPlugin.class);
        Log.d(TAG, "AlarmStreamMediaPlugin getInstance: plugin = " + context.getString(R.string.alarm_streammedia_plugin));
        return sInstance;
    }
    public AlarmStreamMediaPlugin() {
    }

    public boolean alarmStreamMediaInteract(int alarmState) {
        Log.d(TAG, "alarmStreamMediaInteract");
        return false;
    }

    public boolean isLiveMoveTopActivity(Context context) {
        Log.d(TAG, "AlarmStreamMediaInteract");
        return false ;
    }
}
