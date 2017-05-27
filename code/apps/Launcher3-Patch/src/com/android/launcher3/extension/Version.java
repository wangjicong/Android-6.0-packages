package com.android.launcher3.extension;

import android.os.Build;

import com.android.launcher3.bridge.BridgeVersion;

/**
 * Created by lewa on 16-9-21.
 */
public class Version {

    public static final int Android_L = 500;

    public static final int Android_L_a = 501;

    public static final int Android_L_c = 502;

    public static final int Android_M = 600;

    public static final int Android_M_a = 601;

    public static final int Android_M_b = 601;

    public static final int Android_N = 700;

    public static int sVersion = Android_M;

    public static int getLauncherVersion(){
        return sVersion;
    }

    public static int getBridgeVersion(){
        return BridgeVersion.getBridgeVersion();
    }
}
