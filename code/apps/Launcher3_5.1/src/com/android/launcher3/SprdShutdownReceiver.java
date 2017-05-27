package com.android.launcher3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// SPRD: bug381889 2014-12-24 Bugfix don't remove package after received shutdown broadcast.
public class SprdShutdownReceiver extends BroadcastReceiver {

    public static boolean SHUTDOWN_IS_GONING_ON = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        SHUTDOWN_IS_GONING_ON = true;
    }

}
