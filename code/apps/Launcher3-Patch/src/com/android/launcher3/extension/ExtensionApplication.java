package com.android.launcher3.extension;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.android.launcher3.bridge.BuildConfig;
import com.android.launcher3.bridge.ExtensionManager;

public class ExtensionApplication extends Application {

    public static boolean ENABLE_PLUGIN = true;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        checkEnable();
        if (ENABLE_PLUGIN) {
            ExtensionManager.setApplicationContext(base, true, true);
        }
    }

    public void checkEnable(){
        try {
            String enable = System.getProperties().getProperty("ro.feature.launcher_etc", "true");
            ENABLE_PLUGIN = Boolean.parseBoolean(enable);
            Log.e("ExtensionApplication", " enable = " + enable);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
