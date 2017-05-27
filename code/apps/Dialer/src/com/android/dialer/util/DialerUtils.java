/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.dialer.util;

import android.Manifest.permission;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.interactions.TouchPointManager;
import com.android.dialer.DialerApplication;
import com.android.dialer.R;
import com.android.dialer.widget.EmptyContentView;
import com.android.incallui.CallCardFragment;
import com.android.incallui.Log;

import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import android.telephony.VoLteServiceState;
import com.android.incallui.CallList;
import com.android.incallui.InCallPresenter;
import android.telephony.PhoneNumberUtils;
/**
 * General purpose utility methods for the Dialer.
 */
public class DialerUtils {

    // SPRD: add for bug510314
    private static HashMap<Integer, SubscriptionInfo> sSubInfoMap =
            new HashMap<Integer, SubscriptionInfo>();

    /**
     * Attempts to start an activity and displays a toast with the default error message if the
     * activity is not found, instead of throwing an exception.
     *
     * @param context to start the activity with.
     * @param intent to start the activity with.
     */
    public static void startActivityWithErrorToast(Context context, Intent intent) {
        startActivityWithErrorToast(context, intent, R.string.activity_not_available);
    }

    /**
     * Attempts to start an activity and displays a toast with a provided error message if the
     * activity is not found, instead of throwing an exception.
     *
     * @param context to start the activity with.
     * @param intent to start the activity with.
     * @param msgId Resource ID of the string to display in an error message if the activity is
     *              not found.
     */
    public static void startActivityWithErrorToast(Context context, Intent intent, int msgId) {
        try {
            if ((IntentUtil.CALL_ACTION.equals(intent.getAction())
                            && context instanceof Activity)) {
                /* SPRD: add for bug534275,534535 @{ */
                if (intent.getData() != null) {
                    DialerUtils.showCallNotSwitchToast(context,
                            intent.getData().getSchemeSpecificPart());
                }
                /* @} */
                // All dialer-initiated calls should pass the touch point to the InCallUI
                Point touchPoint = TouchPointManager.getInstance().getPoint();
                if (touchPoint.x != 0 || touchPoint.y != 0) {
                    Bundle extras = new Bundle();
                    extras.putParcelable(TouchPointManager.TOUCH_POINT, touchPoint);
                    intent.putExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
                }
                final TelecomManager tm =
                        (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                tm.placeCall(intent.getData(), intent.getExtras());
            } else {
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns the component name to use in order to send an SMS using the default SMS application,
     * or null if none exists.
     */
    public static ComponentName getSmsComponent(Context context) {
        String smsPackage = Telephony.Sms.getDefaultSmsPackage(context);
        if (smsPackage != null) {
            final PackageManager packageManager = context.getPackageManager();
            final Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts(ContactsUtils.SCHEME_SMSTO, "", null));
            final List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo resolveInfo : resolveInfos) {
                if (smsPackage.equals(resolveInfo.activityInfo.packageName)) {
                    return new ComponentName(smsPackage, resolveInfo.activityInfo.name);
                }
            }
        }
        return null;
    }

    /**
     * Closes an {@link AutoCloseable}, silently ignoring any checked exceptions. Does nothing if
     * null.
     *
     * @param closeable to close.
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Joins a list of {@link CharSequence} into a single {@link CharSequence} seperated by a
     * localized delimiter such as ", ".
     *
     * @param resources Resources used to get list delimiter.
     * @param list List of char sequences to join.
     * @return Joined char sequences.
     */
    public static CharSequence join(Resources resources, Iterable<CharSequence> list) {
        StringBuilder sb = new StringBuilder();
        final BidiFormatter formatter = BidiFormatter.getInstance();
        final CharSequence separator = resources.getString(R.string.list_delimeter);

        Iterator<CharSequence> itr = list.iterator();
        boolean firstTime = true;
        while (itr.hasNext()) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(separator);
            }
            // Unicode wrap the elements of the list to respect RTL for individual strings.
            sb.append(formatter.unicodeWrap(
                    itr.next().toString(), TextDirectionHeuristics.FIRSTSTRONG_LTR));
        }

        // Unicode wrap the joined value, to respect locale's RTL ordering for the whole list.
        return formatter.unicodeWrap(sb.toString());
    }

    /**
     * @return True if the application is currently in RTL mode.
     */
    public static boolean isRtl() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
            View.LAYOUT_DIRECTION_RTL;
    }

    public static void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    public static void hideInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // SPRD: add for bug516743
    private static boolean sPermissionFlag = false;

    /* SPRD: fix bug bug510314 @{ */
    public static synchronized SubscriptionInfo getActiveSubscriptionInfo(
            Context context, int slotId, boolean forceReload) {

        Log.i("DialerUtils", "getActiveSubscriptionInfo: forceReload = "
            + forceReload + " sPermissionFlag = " + DialerUtils.sPermissionFlag);

        // SPRD: modify for bug513834
        if ((forceReload || !DialerUtils.sPermissionFlag)
                && context.checkSelfPermission(permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED) {

            // SPRD: add for bug516743
            DialerUtils.sPermissionFlag = true;
            DialerUtils.sSubInfoMap.clear();
            final SubscriptionManager subScriptionManager = SubscriptionManager.from(context);
            List<SubscriptionInfo> subInfos = subScriptionManager.getActiveSubscriptionInfoList();
            if (subInfos != null) {
                for (SubscriptionInfo subInfo : subInfos) {
                    int phoneId = subInfo.getSimSlotIndex();
                    // SPRD: add for bug514290
                    DialerUtils.sSubInfoMap.put(phoneId, subInfo);
                }
            }
        }

        return DialerUtils.sSubInfoMap.get(slotId);
    }
    /* @} */

    /* SPRD: add for bug519889 @{ */
    public static boolean canIntentBeHandled(Context context, Intent intent) {
        if (context == null) return false;
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.size() > 0;
    }
    /* @} */

    /*SPRD: add for Bug#523966 @{ */
    public static boolean is4GPhone() {
        return TelephonyManager.getRadioCapbility() != TelephonyManager.RadioCapbility.NONE;
    }
    /* @} */

    /* SPRD: add for bug534275,534535 @{ */
    public static void showCallNotSwitchToast(Context context, String number) {
        if (context == null || TextUtils.isEmpty(number)) return;

        CallList callList = InCallPresenter.getInstance().getCallList();
        if (callList != null) {
            if (callList.getActiveCall() != null && callList.getBackgroundCall() != null
                    && !PhoneNumberUtils.isEmergencyNumber(number)) {
                Toast.makeText(context, R.string.incall_error_supp_service_switch,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    /* @} */

    /*SPRD: add for Bug#533902 @{ */
    public static boolean isInVolteService(Context context) {
        boolean volteServiceEnable = false;
        if (context != null) {
            Context mContext = context.getApplicationContext();
            DialerApplication dialerApplication = (DialerApplication) mContext;
            volteServiceEnable = dialerApplication.isInVolteService();
        }
        return volteServiceEnable;
    }
    /* @} */
}
