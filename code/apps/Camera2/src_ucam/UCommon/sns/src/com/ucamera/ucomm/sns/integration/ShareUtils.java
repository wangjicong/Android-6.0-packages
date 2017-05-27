/*
 * Copyright (C) 2011,2013 Thundersoft Corporation
 * All rights Reserved
 */

package com.ucamera.ucomm.sns.integration;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.ucamera.ucomm.downloadcenter.DownloadTabActivity;
import com.ucamera.ucomm.downloadcenter.UiUtils;
import com.ucamera.ucomm.sns.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShareUtils {
    private static final String[] OUTER_SNSES = new String[] { "facebook",
            "twitter", "line", "weibo", "bluetooth", "mail", "tencent",
            "twisohu", "renren", "qzone", "weibo", "tumblr", "kaixin001",
            "flickr", "netease", "com.android.mms"};
    private static final String[] DOPOD_OUTER_SNSES = new String[] {
            "com.android.mms", "bluetooth" };
    public static boolean OUTER_SNS = false;
    private static SharedPreferences mSharedPref;

    public static void setOuterSns(boolean outer) {
        OUTER_SNS = outer;
    }

    public static boolean SNS_SHARE_IS_ON = false;

    public static void setSnsShareOn(boolean on) {
        SNS_SHARE_IS_ON = on;
    }
    public static void shareIDPhotoImage(Context context, Uri uri) {
        snsShare(context, uri, true);
    }
    public static void shareImage(Context context, Uri uri) {
        if(mSharedPref == null) {
            mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        }

        if(DownloadTabActivity.isNeedNetworkPermission() &&
                (!mSharedPref.contains(UiUtils.KEY_CONFIRM_NETWORK_PERMISSION) ||
                        mSharedPref.getString(UiUtils.KEY_CONFIRM_NETWORK_PERMISSION, "").equals("off"))) {
            UiUtils.confirmNetworkPermission(context, new String[]{context.getString(R.string.sns_title_share)}, null, null, 2, uri.toString());
        }else {
            if (OUTER_SNS || !Build.SNS_SHARE_ON) {
                otherShare(context, uri, null, Bitmap.CompressFormat.JPEG);
            } else {
                snsShare(context, uri, false);
            }
        }
    }

    public static void shareVideo(Context context, Intent intent) {
        try {
            List<ShareInfoItem> lists = getSupportVideosSns(intent, context);
            showDialog(lists, context, intent);
        } catch (Exception e) {
            Toast.makeText(context, R.string.edit_operation_failure_tip,
                    Toast.LENGTH_LONG).show();
        }
    }
    private static void snsShare(Context context, Uri uri, boolean isIDPhotoModule) {
        Intent intent = new Intent();
        intent.setClassName(context, "com.ucamera.ucomm.sns.ShareActivity");
        intent.putExtra("isIDPhotoModule", isIDPhotoModule);
        intent.setAction("android.intent.action.UGALLERY_SHARE");
        intent.setDataAndType(uri, "image/*");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.text_activity_is_not_found,
                    Toast.LENGTH_LONG).show();
        }
    }

    public static void otherShare(Context context, Uri uri, Bitmap bitmap,
            Bitmap.CompressFormat outputFormat) {
        try {
            /*
             * filter the activity GifPlayActivity and GIFEditActivity from the
             * application list
             */
            String[] filterClasses = new String[3];
            filterClasses[0] = "com.ucamera.ucam.modules.ugif.GifPlayActivity";
            filterClasses[1] = "com.ucamera.ucam.modules.ugif.edit.GIFEditActivity";
            filterClasses[2] = "com.ucamera.uphoto.ImageEditControlActivity";
            // showShareDialog(uri, filterClasses, context);
            showOtherShare(uri, filterClasses, context);
        } catch (Exception e) {
            Toast.makeText(context, R.string.edit_operation_failure_tip,
                    Toast.LENGTH_LONG).show();
        }
    }

    private static AlertDialog mShareDialog = null; // the dialog to show the
                                                    // application list

    public static void showOtherShare(Uri uri, String[] classNameString,
            final Context context) {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        List<ShareInfoItem> lists = getSupportSns(intent, classNameString,
                context);
        showDialog(lists, context, intent);
    }

    private static List<ShareInfoItem> getSupportVideosSns(Intent intent,final Context context) {
        final List<ShareInfoItem> lists = new ArrayList<ShareInfoItem>();
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(
                intent, 0);
        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(
                packageManager));
        int numActivities = activities.size();
        for (int i = 0; i != numActivities; ++i) {
            final ResolveInfo info = activities.get(i);
            ActivityInfo ai = info.activityInfo;
            String className = ai.name;
            boolean bContinue = false;
            if (!isVideoSns(ai.packageName)) {
                bContinue = true;
            }
            if (bContinue) {
                continue;
            }
            Drawable iconId = info.loadIcon(packageManager);
            if (iconId == null) {
                iconId = ai.applicationInfo.loadIcon(packageManager);
            }
            String textId = info.loadLabel(packageManager).toString();

            ShareInfoItem shareInfoItem = new ShareInfoItem();
            shareInfoItem.iconId = iconId;
            shareInfoItem.textId = textId;
            shareInfoItem.activityInfo = ai;

            lists.add(shareInfoItem);
        }
        return lists;
    }
    private static List<ShareInfoItem> getSupportSns(Intent intent,
            String[] classNameString, final Context context) {
        final List<ShareInfoItem> lists = new ArrayList<ShareInfoItem>();
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(
                intent, 0);
        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(
                packageManager));
        int numActivities = activities.size();
        for (int i = 0; i != numActivities; ++i) {
            final ResolveInfo info = activities.get(i);
            ActivityInfo ai = info.activityInfo;
            String className = ai.name;
            boolean bContinue = false;
            if (OUTER_SNS) {
                if (!isSNS(ai.packageName)) {
                    bContinue = true;
                }
            } else if(!Build.SNS_SHARE_ON){
                int numOfClasses = classNameString.length;
                for (int j = 0; j < numOfClasses; j++) {
                    if (className != null
                            && className.equals(classNameString[j])) {
                        bContinue = true;
                    }
                }
            }
            if (bContinue) {
                continue;
            }
            Drawable iconId = info.loadIcon(packageManager);
            if (iconId == null) {
                iconId = ai.applicationInfo.loadIcon(packageManager);
            }
            String textId = info.loadLabel(packageManager).toString();

            ShareInfoItem shareInfoItem = new ShareInfoItem();
            shareInfoItem.iconId = iconId;
            shareInfoItem.textId = textId;
            shareInfoItem.activityInfo = ai;

            lists.add(shareInfoItem);
        }
        return lists;
    }

    private static void showDialog(final List<ShareInfoItem> lists,
            final Context context, final Intent intent) {
        ShareBaseAdapter shareBaseAdapter = new ShareBaseAdapter(context,
                R.layout.resolve_list_item, lists);

        if (mShareDialog != null && mShareDialog.isShowing()) {
            mShareDialog.dismiss();
            mShareDialog = null;
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.text_edit_share_title);
            builder.setAdapter(shareBaseAdapter,
                    new android.content.DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            final Intent resolvedIntent = new Intent(intent);
                            try{
                                ShareInfoItem shareItem = lists.get(which);
                                ActivityInfo info = shareItem.activityInfo;
                                resolvedIntent
                                .setComponent(new ComponentName(
                                        info.applicationInfo.packageName,
                                        info.name));
                            }catch(NullPointerException e) {
                                /*
                                 * FIX BUG: 6122
                                 * BUG COMMENT: avoid null pointer exception
                                 * DATE: 2014-03-26
                                 */
                                if (mShareDialog != null && mShareDialog.isShowing()) {
                                    mShareDialog.dismiss();
                                    mShareDialog = null;
                                }
                                return;
                            }
                            try {
                                context.startActivity(resolvedIntent);
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(context,
                                        R.string.edit_operation_failure_tip,
                                        Toast.LENGTH_SHORT).show();
                            } finally {
                                if (mShareDialog != null && mShareDialog.isShowing()) {
                                    mShareDialog.dismiss();
                                    mShareDialog = null;
                                }
                            }
                        }
                    });
            mShareDialog = builder.create();
            mShareDialog.show();
        }
    }

    private static boolean isSNS(String packageName) {
        if (SNS_SHARE_IS_ON) {
            for (int i = 0; i < DOPOD_OUTER_SNSES.length; i++) {
                if (packageName.contains(DOPOD_OUTER_SNSES[i])) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < OUTER_SNSES.length; i++) {
                if (packageName.contains(OUTER_SNSES[i])) {
                    return true;
                }
            }
        }
        return false;
    }
    private static boolean isVideoSns(String packageName) {
        for (int i = 0; i < DOPOD_OUTER_SNSES.length; i++) {
            if (packageName.contains(DOPOD_OUTER_SNSES[i])) {
                return true;
            }
        }
        return false;
    }
}
