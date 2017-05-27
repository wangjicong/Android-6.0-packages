package com.sprd.ext.unreadnotifier;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by SPRD on 10/16/16.
 */

public class MMSAppUtils {
    public static class SmsApplicationData {
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
        public String smsClassName;

        public SmsApplicationData(String applicationName, String packageName) {
            mApplicationName = applicationName;
            mPackageName = packageName;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static Collection<SmsApplicationData> getApplicationCollectionInternal(
            Context context) {
        PackageManager packageManager = context.getPackageManager();

        // Get the list of apps registered for SMS
        Intent smsIntent = new Intent("android.provider.Telephony.SMS_DELIVER");
        List<ResolveInfo> smsReceivers = packageManager.queryBroadcastReceivers(smsIntent, 0);

        // Get package names for all SMS apps
        List<String> smsPackageNames = new ArrayList<>();

        for (ResolveInfo resolveInfo : smsReceivers) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null) {
                continue;
            }
            if (!Manifest.permission.BROADCAST_SMS.equals(activityInfo.permission)) {
                continue;
            }
            if (!smsPackageNames.contains(activityInfo.packageName)) {
                smsPackageNames.add(activityInfo.packageName);
            }
        }

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allResolveInfoList = packageManager.queryIntentActivities(intent, 0);

        HashMap<String, SmsApplicationData> smsActivityInfo = new HashMap<>();

        // Add one entry to the map for every sms Application
        for (ResolveInfo resolveInfo : allResolveInfoList) {
            final ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null) {
                continue;
            }
            final String packageName = activityInfo.packageName;

            if(smsPackageNames.contains(packageName)) {

                if (!smsActivityInfo.containsKey(packageName)) {
                    final String applicationName = resolveInfo.loadLabel(packageManager).toString();
                    final SmsApplicationData smsApplicationData = new SmsApplicationData(
                            applicationName, packageName);
                    smsApplicationData.smsClassName = activityInfo.name;
                    smsActivityInfo.put(packageName, smsApplicationData);
                }
            }

        }

        return smsActivityInfo.values();
    }

    /**
     * Returns the list of available SMS apps defined as apps that are registered for both the
     * SMS_RECEIVED_ACTION (SMS) and WAP_PUSH_RECEIVED_ACTION (MMS) broadcasts (and their broadcast
     * receivers are enabled)
     *
     */
    public static Collection<SmsApplicationData> getApplicationCollection(Context context) {
        final long token = Binder.clearCallingIdentity();
        try {
            return getApplicationCollectionInternal(context);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
