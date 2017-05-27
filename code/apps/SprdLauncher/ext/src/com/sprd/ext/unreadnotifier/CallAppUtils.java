package com.sprd.ext.unreadnotifier;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by SPRD on 10/16/16.
 */
public class CallAppUtils {
    public static class CallApplicationData {
        /**
         * Name of this SMS app for display.
         */
        public String mApplicationName;

        /**
         * Package name for this SMS app.
         */
        public String mPackageName;

        /**
         * Activity class name for this SMS app.
         */
        public String callClassName;

        public CallApplicationData(String applicationName, String packageName) {
            mApplicationName = applicationName;
            mPackageName = packageName;
        }
    }

    /**
     * Returns a list of installed and available dialer applications.
     **/
    @TargetApi(Build.VERSION_CODES.M)
    public static Collection<CallApplicationData> getInstalledDialerApplications(Context context) {
        PackageManager packageManager = context.getPackageManager();

        // Get the list of apps registered for the DIAL intent with empty scheme
        Intent intent = new Intent(Intent.ACTION_DIAL);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);

        List<String> callPackageNames = new ArrayList<>();

        for (ResolveInfo resolveInfo : resolveInfoList) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo != null && !callPackageNames.contains(activityInfo.packageName)) {
                callPackageNames.add(activityInfo.packageName);
            }
        }

        Intent intentAll = new Intent(Intent.ACTION_MAIN);
        intentAll.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allResolveInfoList = packageManager.queryIntentActivities(intentAll, 0);

        HashMap<String, CallApplicationData> callActivityInfo = new HashMap<>();

        // Add one entry to the map for every sms Application
        for (ResolveInfo resolveInfo : allResolveInfoList) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null) {
                continue;
            }
            final String packageName = activityInfo.packageName;

            if(callPackageNames.contains(packageName)) {

                if (!callActivityInfo.containsKey(packageName)) {
                    final String applicationName = resolveInfo.loadLabel(packageManager).toString();
                    final CallApplicationData callApplicationData = new CallApplicationData(
                            applicationName, packageName);
                    callApplicationData.callClassName = activityInfo.name;
                    callActivityInfo.put(packageName, callApplicationData);
                }
            }
        }

        return callActivityInfo.values();
    }

}
