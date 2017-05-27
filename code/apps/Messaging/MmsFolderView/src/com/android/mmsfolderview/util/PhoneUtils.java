/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.mmsfolderview.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v4.util.ArrayMap;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.mmsfolderview.data.SortMsgDataCollector;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class abstracts away platform dependency of calling telephony related
 * platform APIs, mostly involving TelephonyManager, SubscriptionManager and a
 * bit of SmsManager. The class instance can only be obtained via the get(int
 * subId) method parameterized by a SIM subscription ID. On pre-L_MR1, the subId
 * is not used and it has to be the default subId (-1). A convenient
 * getDefault() method is provided for default subId (-1) on any platform
 */
public abstract class PhoneUtils {

    public final static String MESSAGING_PACKAGE_NAME = "com.android.messaging";

    private static final Object PHONEUTILS_INSTANCE_LOCK = new Object();
    private static final List<SubscriptionInfo> EMPTY_SUBSCRIPTION_LIST = new ArrayList<>();
    // Cached subId->instance for L_MR1 and beyond
    private static final ConcurrentHashMap<Integer, PhoneUtils> sPhoneUtilsInstanceCacheLMR1 =
            new ConcurrentHashMap<>();

    private static final String TAG = "PhoneUtils";
    private static PhoneUtils sPhoneUtilsInstancePreLMR1 = null; 

    // The canonical phone number cache
    // Each country gets its own cache. The following maps from ISO country code to
    // the country's cache. Each cache maps from original phone number to canonicalized phone
    private static final ArrayMap<String, ArrayMap<String, String>> sCanonicalPhoneNumberCache =
            new ArrayMap<>();

    protected final Context mContext;
    protected final TelephonyManager mTelephonyManager;
    protected final int mSubId;

    public PhoneUtils(int subId, Context context) {
        mSubId = subId;
        mContext = context;
        mTelephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public PhoneUtils(Context context) {
        mSubId = SortMsgDataCollector.DEFAULT_SELF_SUB_ID;
        mContext = context;
        mTelephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Get the SIM's country code
     *
     * @return the country code on the SIM
     */
    public abstract String getSimCountry();

    /**
     * Get number of SIM slots
     *
     * @return the SIM slot count
     */
    public abstract int getSimSlotCount();

    /**
     * Get SIM's carrier name
     *
     * @return the carrier name of the SIM
     */
    public abstract String getCarrierName();

    /**
     * Check if there is SIM inserted on the device
     *
     * @return true if there is SIM inserted, false otherwise
     */
    public abstract boolean hasSim();

    /**
     * Check if the SIM is roaming
     *
     * @return true if the SIM is in romaing state, false otherwise
     */
    public abstract boolean isRoaming();

    /**
     * Get the MCC and MNC in integer of the SIM's provider
     *
     * @return an array of two ints, [0] is the MCC code and [1] is the MNC code
     */
    public abstract int[] getMccMnc();

    /**
     * Get the mcc/mnc string
     *
     * @return the text of mccmnc string
     */
    public abstract String getSimOperatorNumeric();

    /**
     * Returns the number of active subscriptions in the device.
     */
    public abstract int getActiveSubscriptionCount();

    /**
     * Get {@link SmsManager} instance
     *
     * @return the relevant SmsManager instance based on OS version and subId
     */
    public abstract SmsManager getSmsManager();

    /**
     * Check if data roaming is enabled
     *
     * @return true if data roaming is enabled, false otherwise
     */
    public abstract boolean isDataRoamingEnabled();

    /**
     * Check if mobile data is enabled
     *
     * @return true if mobile data is enabled, false otherwise
     */
    public abstract boolean isMobileDataEnabled();

    /**
     * This interface packages methods should only compile on L_MR1.
     * This is needed to make unit tests happy when mockito tries to
     * mock these methods. Calling on these methods on L_MR1 requires
     * an extra invocation of toMr1().
     */
    public interface LMr1 {
        /**
         * Get this SIM's information. Only applies to L_MR1 above
         *
         * @return the subscription info of the SIM
         */
        public abstract SubscriptionInfo getActiveSubscriptionInfo();

        /**
         * Get the list of active SIMs in system. Only applies to L_MR1 above
         *
         * @return the list of subscription info for all inserted SIMs
         */
        public abstract List<SubscriptionInfo> getActiveSubscriptionInfoList();

        /**
         * Register subscription change listener. Only applies to L_MR1 above
         *
         * @param listener The listener to register
         */
        public abstract void registerOnSubscriptionsChangedListener(
                SubscriptionManager.OnSubscriptionsChangedListener listener);
    }

    public static boolean isMessagingDefaultSmsApp(Context context) {
        if (OsUtil.isAtLeastKLP()) {
            final String configuredApplication = Telephony.Sms.getDefaultSmsPackage(context);
            Log.d("tim_V6_df", "configuredApplication="+configuredApplication);
            return MESSAGING_PACKAGE_NAME.equals(configuredApplication);
        }
        return true;
    }

    public static Intent getChangeDefaultSmsAppIntent(final Activity activity) {
        final Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
        return intent;
    }

    public static Intent getChangeDefaultSmsAppIntent(String packageName) {
        final Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
        return intent;
    }

    public PhoneUtils getPhoneUtils(int subId, Context context) {
        if (OsUtil.isAtLeastL_MR1()) {
            if (subId == SortMsgDataCollector.DEFAULT_SELF_SUB_ID) {
                subId = SmsManager.getDefaultSmsSubscriptionId();
            }
            if (subId < 0) {
                subId = SortMsgDataCollector.DEFAULT_SELF_SUB_ID;
            }
            PhoneUtils instance = sPhoneUtilsInstanceCacheLMR1.get(subId);
            if (instance == null) {
                instance = new PhoneUtils.PhoneUtilsLMR1(subId, context);
                sPhoneUtilsInstanceCacheLMR1.putIfAbsent(subId, instance);
            }
            return instance;
        } else {
            if (sPhoneUtilsInstancePreLMR1 == null) {
                synchronized (PHONEUTILS_INSTANCE_LOCK) {
                    if (sPhoneUtilsInstancePreLMR1 == null) {
                        sPhoneUtilsInstancePreLMR1 = new PhoneUtils.PhoneUtilsPreLMR1(context);
                    }
                }
            }
            return sPhoneUtilsInstancePreLMR1;
        }
    }
    public LMr1 toLMr1() {
        if (OsUtil.isAtLeastL_MR1()) {
            return (LMr1) this;
        } else {
            Log.e(TAG, "PhoneUtils.toLMr1(): invalid OS version");
            return null;
        }
    }
    /**
     * The PhoneUtils class for L_MR1
     */
    public static class PhoneUtilsLMR1 extends PhoneUtils implements LMr1 {
        private final SubscriptionManager mSubscriptionManager;

        public PhoneUtilsLMR1(final int subId, Context context) {
            super(subId, context);
            mSubscriptionManager = SubscriptionManager.from(context);
        }

        public PhoneUtilsLMR1(Context context) {
            super(context);
            mSubscriptionManager = SubscriptionManager.from(context);
        }

        @Override
        public String getSimCountry() {
            final SubscriptionInfo subInfo = getActiveSubscriptionInfo();
            if (subInfo != null) {
                final String country = subInfo.getCountryIso();
                if (TextUtils.isEmpty(country)) {
                    return null;
                }
                return country.toUpperCase();
            }
            return null;
        }

        @Override
        public int getSimSlotCount() {
            return mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        }

        @Override
        public String getCarrierName() {
            final SubscriptionInfo subInfo = getActiveSubscriptionInfo();
            if (subInfo != null) {
                final CharSequence displayName = subInfo.getDisplayName();
                if (!TextUtils.isEmpty(displayName)) {
                    return displayName.toString();
                }
                final CharSequence carrierName = subInfo.getCarrierName();
                if (carrierName != null) {
                    return carrierName.toString();
                }
            }
            return null;
        }

        @Override
        public boolean hasSim() {
            return mSubscriptionManager.getActiveSubscriptionInfoCount() > 0;
        }

        @Override
        public boolean isRoaming() {
            return mSubscriptionManager.isNetworkRoaming(mSubId);
        }

        @Override
        public int[] getMccMnc() {
            int mcc = 0;
            int mnc = 0;
            final SubscriptionInfo subInfo = getActiveSubscriptionInfo();
            if (subInfo != null) {
                mcc = subInfo.getMcc();
                mnc = subInfo.getMnc();
            }
            return new int[]{mcc, mnc};
        }

        @Override
        public String getSimOperatorNumeric() {
            // For L_MR1 we return the canonicalized (xxxxxx) string
            return getMccMncString(getMccMnc());
        }

        @Override
        public SubscriptionInfo getActiveSubscriptionInfo() {
            try {
                final SubscriptionInfo subInfo =
                        mSubscriptionManager.getActiveSubscriptionInfo(mSubId);
                if (subInfo == null) {
                        // This is possible if the sub id is no longer available.
                        Log.d(TAG, "PhoneUtils.getActiveSubscriptionInfo(): empty sub info for "
                                + mSubId);
                }
                return subInfo;
            } catch (Exception e) {
                Log.e(TAG, "PhoneUtils.getActiveSubscriptionInfo: system exception for "
                        + mSubId, e);
            }
            return null;
        }

        @Override
        public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
            final List<SubscriptionInfo> subscriptionInfos =
                    mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptionInfos != null) {
                return subscriptionInfos;
            }
            return EMPTY_SUBSCRIPTION_LIST;
        }

        @Override
        public void registerOnSubscriptionsChangedListener(
                SubscriptionManager.OnSubscriptionsChangedListener listener) {
            mSubscriptionManager.addOnSubscriptionsChangedListener(listener);
        }

        @Override
        public SmsManager getSmsManager() {
            return SmsManager.getSmsManagerForSubscriptionId(mSubId);
        }

        @Override
        public int getActiveSubscriptionCount() {
            return mSubscriptionManager.getActiveSubscriptionInfoCount();
        }

        @Override
        public boolean isDataRoamingEnabled() {
            final SubscriptionInfo subInfo = getActiveSubscriptionInfo();
            if (subInfo == null) {
                // There is nothing we can do if system give us empty sub info
                Log.e(TAG, "PhoneUtils.isDataRoamingEnabled: system return empty sub info for "
                        + mSubId);
                return false;
            }
            return subInfo.getDataRoaming() != SubscriptionManager.DATA_ROAMING_DISABLE;
        }

        @Override
        public boolean isMobileDataEnabled() {
            boolean mobileDataEnabled = false;
            try {
                final Class cmClass = mTelephonyManager.getClass();
                final Method method = cmClass.getDeclaredMethod("getDataEnabled", Integer.TYPE);
                method.setAccessible(true); // Make the method callable
                // get the setting for "mobile data"
                mobileDataEnabled = (Boolean) method.invoke(
                        mTelephonyManager, Integer.valueOf(mSubId));
            } catch (final Exception e) {
                Log.e(TAG, "PhoneUtil.isMobileDataEnabled: system api not found", e);
            }
            return mobileDataEnabled;

        }

    }
    /**
     * The PhoneUtils class for pre L_MR1
     */
    public static class PhoneUtilsPreLMR1 extends PhoneUtils {
        private final ConnectivityManager mConnectivityManager;

        public PhoneUtilsPreLMR1(Context context) {
            super(SortMsgDataCollector.DEFAULT_SELF_SUB_ID, context);
            mConnectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        @Override
        public String getSimCountry() {
            final String country = mTelephonyManager.getSimCountryIso();
            if (TextUtils.isEmpty(country)) {
                return null;
            }
            return country.toUpperCase();
        }

        @Override
        public int getSimSlotCount() {
            // Don't support MSIM pre-L_MR1
            return 1;
        }

        @Override
        public String getCarrierName() {
            return mTelephonyManager.getNetworkOperatorName();
        }

        @Override
        public boolean hasSim() {
            return mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
        }

        @Override
        public boolean isRoaming() {
            return mTelephonyManager.isNetworkRoaming();
        }

        @Override
        public int[] getMccMnc() {
            final String mccmnc = mTelephonyManager.getSimOperator();
            int mcc = 0;
            int mnc = 0;
            try {
                mcc = Integer.parseInt(mccmnc.substring(0, 3));
                mnc = Integer.parseInt(mccmnc.substring(3));
            } catch (Exception e) {
                Log.w(TAG, "PhoneUtils.getMccMnc: invalid string " + mccmnc, e);
            }
            return new int[]{mcc, mnc};
        }

        @Override
        public String getSimOperatorNumeric() {
            return mTelephonyManager.getSimOperator();
        }

        @Override
        public SmsManager getSmsManager() {
            return SmsManager.getDefault();
        }

        @Override
        public int getActiveSubscriptionCount() {
            return hasSim() ? 1 : 0;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isDataRoamingEnabled() {
            boolean dataRoamingEnabled = false;
            final ContentResolver cr = mContext.getContentResolver();
            if (OsUtil.isAtLeastJB_MR1()) {
                dataRoamingEnabled =
                        (Settings.Global.getInt(cr, Settings.Global.DATA_ROAMING, 0) != 0);
            } else {
                dataRoamingEnabled =
                        (Settings.System.getInt(cr, Settings.System.DATA_ROAMING, 0) != 0);
            }
            return dataRoamingEnabled;
        }

        @Override
        public boolean isMobileDataEnabled() {
            boolean mobileDataEnabled = false;
            try {
                final Class cmClass = mConnectivityManager.getClass();
                final Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                method.setAccessible(true); // Make the method callable
                // get the setting for "mobile data"
                mobileDataEnabled = (Boolean) method.invoke(mConnectivityManager);
            } catch (final Exception e) {
                Log.e(TAG, "PhoneUtil.isMobileDataEnabled: system api not found", e);
            }
            return mobileDataEnabled;
        }

    }
    public static String getMccMncString(int[] mccmnc) {
        if (mccmnc == null || mccmnc.length != 2) {
            return "000000";
        }
        return String.format("%03d%03d", mccmnc[0], mccmnc[1]);
    }
}
