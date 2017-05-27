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

package com.android.messaging.ui.appsettings;

import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.provider.Settings;
/*Add for bug 526653 {@*/
import android.preference.ListPreference;
import android.preference.PreferenceManager;
/*@}*/
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.ParticipantRefresh;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.sms.ApnDatabase;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.sms.MmsUtils;
//Add for bug 526653
import com.android.messaging.sms.SystemProperties;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Assert;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.sprd.messaging.ui.appsettings.ShowSmscEditDialogActivity;
import com.sprd.messaging.ui.appsettings.SmscListActivity;
import com.sprd.messaging.ui.appsettings.SmscManager;
import com.android.sprd.telephony.RadioInteractor;

public class PerSubscriptionSettingsActivity extends BugleActionBarActivity {
    public static final String MAX_SEND_RETRIES = "3";
    public static final String MIN_SEND_RETRIES = "0";
    public static final String SMS_VALIDITY_SETTING = "persist.radio.smstime";
    public static final String MMS_VALIDITY_SETTING = "persist.radio.mmstime";

    /**
     * Declare the property
     *
     */
    public static final String SEND_RETRIE_TIME = "persist.radio.retry_control";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final String title = getIntent().getStringExtra(
                UIIntents.UI_INTENT_EXTRA_PER_SUBSCRIPTION_SETTING_TITLE);
        if (!TextUtils.isEmpty(title)) {
            getSupportActionBar().setTitle(title);
        } else {
            // This will fall back to the default title, i.e.
            // "Messaging settings," so No-op.
        }

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final PerSubscriptionSettingsFragment fragment = new PerSubscriptionSettingsFragment();
        ft.replace(android.R.id.content, fragment);
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PerSubscriptionSettingsFragment extends
            PreferenceFragment implements OnSharedPreferenceChangeListener,
            Preference.OnPreferenceChangeListener {
        private PhoneNumberPreference mPhoneNumberPreference;
        private Preference mGroupMmsPreference;
        private String mGroupMmsPrefKey;
        private String mPhoneNumberKey;
        private int mSubId;
        // sprd add for smsc begin
        private Preference mSmscPreference;
        private String mSmscPrefKey;
        private String mDisplayName;
        // sprd add for smsc end
        // Modify by SPRD for bug 526653
        private ListPreference mSmsValidityPref;
        private ListPreference mMmsValidityPref;
        // private ValidityPopWindow mValidityWin;

        // 489220 begin
        private Preference mMmsDeliveryReportsPreference;
        private Preference mMmsReadReportsPreference;
        /* Delete by SPRD for Bug531825  2016.03.17 Start*/
        //private Preference mEnableReturnMmsDeliveryReportsPreference;
        private Preference mEnableReturnMmsReadReportsPreference;
        private Preference mSmsRetryTimesPref;
        private String mMmsDeliveryReportPrefKey;
        private String mMmsReadReportsPrefKey;
        //private String mEnableReturnMmsDeliveryReportsPrefKey;
        /* Delete by SPRD for Bug531825  2016.03.17 End*/
        private String mEnableReturnMmsReadReportsPrefKey;

        // 489220 end
        // sprd #542214 start
        private ListPreference mSmsSaveSimPreference;
        private String mSmsSaveSimPrefKey;
        private RadioInteractor mRadioInteractor;
        private Context mContext;
        private int mPhoneId;
        // sprd #542214 end

        //add for bug 556256 begin
        private Preference mSimMessageCapacity;
        //add for bug 556256 end

        public PerSubscriptionSettingsFragment() {
            // Required empty constructor
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Get sub id from launch intent
            final Intent intent = getActivity().getIntent();
            Assert.notNull(intent);
            // Modify by SPRD for bug 526653
            // mValidityWin = new ValidityPopWindow(getContext());
            mSubId = (intent != null) ? intent.getIntExtra(
                    UIIntents.UI_INTENT_EXTRA_SUB_ID,
                    ParticipantData.DEFAULT_SELF_SUB_ID)
                    : ParticipantData.DEFAULT_SELF_SUB_ID;
            Log.d("PerSubscriptionSettingsActivity", " onCreate mSubId = " + mSubId);
            final BuglePrefs subPrefs = Factory.get().getSubscriptionPrefs(
                    mSubId);
            getPreferenceManager().setSharedPreferencesName(
                    subPrefs.getSharedPreferencesName());
            addPreferencesFromResource(R.xml.preferences_per_subscription);

            mPhoneNumberKey = getString(R.string.mms_phone_number_pref_key);
            mPhoneNumberPreference = (PhoneNumberPreference) findPreference(mPhoneNumberKey);
            final PreferenceCategory advancedCategory = (PreferenceCategory) findPreference(getString(R.string.advanced_category_pref_key));
            final PreferenceCategory mmsCategory = (PreferenceCategory) findPreference(getString(R.string.mms_messaging_category_pref_key));

            if (!OsUtil.hasPhonePermission()) {
                OsUtil.requestMissingPermission(getActivity());
            } else {
                mPhoneNumberPreference.setDefaultPhoneNumber(
                        PhoneUtils.get(mSubId)
                                .getCanonicalForSelf(false/* allowOverride */),
                        mSubId);
            }

            mGroupMmsPrefKey = getString(R.string.group_mms_pref_key);
            mGroupMmsPreference = findPreference(mGroupMmsPrefKey);
            if (!MmsConfig.get(mSubId).getGroupMmsEnabled()) {
                // Always show group messaging setting even if the SIM has no
                // number
                // If broadcast sms is selected, the SIM number is not needed
                // If group mms is selected, the phone number dialog will popup
                // when message
                // is being sent, making sure we will have a self number for
                // group mms.
                mmsCategory.removePreference(mGroupMmsPreference);
            } else {
                mGroupMmsPreference
                        .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference pref) {
                                GroupMmsSettingDialog.showDialog(getActivity(),
                                        mSubId);
                                return true;
                            }
                        });
                updateGroupMmsPrefSummary();
            }

            if (!MmsConfig.get(mSubId).getSMSDeliveryReportsEnabled()) {
                final Preference deliveryReportsPref = findPreference(getString(R.string.delivery_reports_pref_key));
                mmsCategory.removePreference(deliveryReportsPref);
            }

            mSmsRetryTimesPref = findPreference(getString(R.string.sms_retry_times_pref_key));
            if (!MmsConfig.get(mSubId).getSMSRetryTimesEnabled()) {
                advancedCategory.removePreference(mSmsRetryTimesPref);
            } else {
                mSmsRetryTimesPref.setOnPreferenceChangeListener(this);
            }

            //add for bug 556256 begin
            RadioInteractor radioInteractor = new RadioInteractor(Factory.get().getApplicationContext());
            String capacity = radioInteractor.getSimCapacity(
                    MmsUtils.tanslateSubIdToPhoneId(
                            Factory.get().getApplicationContext(), getRealSubId(mSubId)));
            Log.d("PerSubscriptionSettingsActivity", "the capacity is:"+capacity+" and the subId:"+getRealSubId(mSubId));
            mSimMessageCapacity = findPreference(getString(R.string.capacity_sim_message_key));
            if(capacity == null)
            {
                capacity = "";
            }
            else{
                capacity = capacity.replace(":","/");
            }
            String summary = getString(R.string.capacity_sim_message_summary);
            mSimMessageCapacity.setSummary(String.format(summary, capacity));
            //add for bug 556256 end

            // Access Point Names (APNs)
            final Preference apnsPref = findPreference(getString(R.string.sms_apns_key));

            if (MmsUtils.useSystemApnTable()
                    && !ApnDatabase.doesDatabaseExist()) {
                // Don't remove the ability to edit the local APN prefs if this
                // device lets us
                // access the system APN, but we can't find the MCC/MNC in the
                // APN table and we
                // created the local APN table in case the MCC/MNC was in there.
                // In other words,
                // if the local APN table exists, let the user edit it.
                advancedCategory.removePreference(apnsPref);
            } else {
                final PreferenceScreen apnsScreen = (PreferenceScreen) findPreference(getString(R.string.sms_apns_key));
                apnsScreen.setIntent(UIIntents.get().getApnSettingsIntent(
                        getPreferenceScreen().getContext(), mSubId));
            }

            // We want to disable preferences if we are not the default app, but
            // we do all of the
            // above first so that the user sees the correct information on the
            // screen
            if (!PhoneUtils.getDefault().isDefaultSmsApp()) {
                mGroupMmsPreference.setEnabled(false);
                final Preference autoRetrieveMmsPreference = findPreference(getString(R.string.auto_retrieve_mms_pref_key));
                autoRetrieveMmsPreference.setEnabled(false);
                final Preference deliveryReportsPreference = findPreference(getString(R.string.delivery_reports_pref_key));
                deliveryReportsPreference.setEnabled(false);
            }

            // 489220 begin
            mMmsDeliveryReportPrefKey = getString(R.string.mms_delivery_reports_pref_key);
            mMmsReadReportsPrefKey = getString(R.string.mms_read_reports_pref_key);
            /* Delete by SPRD for Bug531825  2016.03.17 Start*/
            //mEnableReturnMmsDeliveryReportsPrefKey = getString(R.string.enable_return_mms_delivery_reports_pref_key);
            mEnableReturnMmsReadReportsPrefKey = getString(R.string.enable_return_mms_read_reports_pref_key);

            mMmsDeliveryReportsPreference = findPreference(mMmsDeliveryReportPrefKey);
            mMmsReadReportsPreference = findPreference(mMmsReadReportsPrefKey);
            //mEnableReturnMmsDeliveryReportsPreference = findPreference(mEnableReturnMmsDeliveryReportsPrefKey);
            mEnableReturnMmsReadReportsPreference = findPreference(mEnableReturnMmsReadReportsPrefKey);

            if (!MmsConfig.get(mSubId).getmMmsReadReportsEnable()) {
                mMmsDeliveryReportsPreference.setEnabled(false);
                mMmsReadReportsPreference.setEnabled(false);
                //mEnableReturnMmsDeliveryReportsPreference.setEnabled(false);
                mEnableReturnMmsReadReportsPreference.setEnabled(false);
                mmsCategory.removePreference(mMmsDeliveryReportsPreference);
                mmsCategory.removePreference(mMmsReadReportsPreference);
                //mmsCategory
                        //.removePreference(mEnableReturnMmsDeliveryReportsPreference);
            /* Delete by SPRD for Bug531825  2016.03.17 End*/
                mmsCategory
                        .removePreference(mEnableReturnMmsReadReportsPreference);
            }
            // 489220 end

            /* Sprd add for sms and mms validity start */
            // sprd: fix fof bug 528758 start
            if (OsUtil.hasPhonePermission()) {
                /* Add by sprd for bug 526653 Start */
                String mmsValidityPrefKey = getString(R.string.mms_validity_pref_key);
                String smsValidityPrefKey = getString(R.string.sms_validity_pref_key);
                SmsManager smsManager = SmsManager.getDefault();
                SharedPreferences sharedPref = PreferenceManager
                        .getDefaultSharedPreferences(this.getActivity());
                /* Add by sprd for bug 526653 End */
                mMmsValidityPref = (ListPreference) findPreference(mmsValidityPrefKey);
                if (!MmsConfig.getValidityMmsEnabled()
                        || (PhoneUtils.getDefault()
                                .getActiveSubscriptionCount() == 0)) {
                    mmsCategory.removePreference(mMmsValidityPref);
                    /* Modify by sprd for bug 526653 Start */
                } else {
                    if (null != mMmsValidityPref) {
                        String curVal = sharedPref.getString(mmsValidityPrefKey
                                + mSubId, "604800");
                        smsManager.setProperty(
                                getRealValidityKey(MMS_VALIDITY_SETTING),
                                curVal);
                        mMmsValidityPref.setValue(curVal);
                        mMmsValidityPref
                                .setSummary(mMmsValidityPref.getEntry());
                        mMmsValidityPref.setOnPreferenceChangeListener(this);
                    }
                }

                mSmsValidityPref = (ListPreference) findPreference(smsValidityPrefKey);
                if (!MmsConfig.getValiditySmsEnabled()
                        || (PhoneUtils.getDefault()
                                .getActiveSubscriptionCount() == 0)) {
                    advancedCategory.removePreference(mSmsValidityPref);
                } else {
                    if (null != mSmsValidityPref) {
                        String curVal = sharedPref.getString(smsValidityPrefKey
                                + mSubId, "604800");
                        smsManager.setProperty(
                                getRealValidityKey(SMS_VALIDITY_SETTING),
                                curVal);
                        mSmsValidityPref.setValue(curVal);
                        mSmsValidityPref
                                .setSummary(mSmsValidityPref.getEntry());
                        mSmsValidityPref.setOnPreferenceChangeListener(this);
                    }
                }
                mmsValidityPrefKey = null;
                smsValidityPrefKey = null;
                smsManager = null;
                sharedPref = null;
                /* Modify by sprd for bug 526653 End */
            }
            // sprd: fix fof bug 528758 end
            /* Sprd add for sms and mms validity end */

            // sprd add for smsc begin
            mSmscPrefKey = getString(R.string.smsc_pref_key);
            mSmscPreference = findPreference(mSmscPrefKey);
            mDisplayName = PhoneUtils.get(getRealSubId(mSubId))
                    .getCarrierName();
            if (OsUtil.hasPhonePermission()) {
                if (PhoneUtils.getDefault().getActiveSubscriptionCount() == 0
                        || !MmsConfig.get(mSubId).getSmscShowEnabled()) {
                    Log.d("PerSubscriptionSettingsActivity",
                            "====[PerSubscriptionSettingsActivity]==ActiveSubscriptionCount:0");
                    advancedCategory.removePreference(mSmscPreference);
                } else {
                    mSmscPreference.setTitle(getString(
                            R.string.pref_title_manage_simx_smsc, " "
                                    + mDisplayName));
                    updateSmscSummary(getRealSubId(mSubId));
                    mSmscPreference
                            .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference pref) {
                                    Intent intent;
                                    if (!SmscManager.getInstance().isMultiSmsc(
                                            getRealSubId(mSubId))) {
                                        Log.d("PerSubscriptionSettingsActivity",
                                                "=======[persubscriptionSetting]===singleSmsc");
                                        intent = new Intent(
                                                getActivity(),
                                                ShowSmscEditDialogActivity.class);
                                    } else {
                                        Log.d("PerSubscriptionSettingsActivity",
                                                "====[persubscriptionSetting]===multiSmsc");
                                        intent = new Intent(getActivity(),
                                                SmscListActivity.class);
                                    }
                                    intent.putExtra("subId",
                                            getRealSubId(mSubId));
                                    startActivity(intent);
                                    return true;
                                }
                            });
                }
            }
            // sprd add for smsc end
            // sprd #542214 start
            mContext = Factory.get().getApplicationContext();
            mRadioInteractor = new RadioInteractor(mContext);
            mPhoneId = MmsUtils.tanslateSubIdToPhoneId(mContext,
                    getRealSubId(mSubId));
            mSmsSaveSimPrefKey = getString(R.string.sms_save_to_sim_pref_key);
            mSmsSaveSimPreference = (ListPreference) findPreference(mSmsSaveSimPrefKey);
            if (!MmsConfig.getIsCMCC()) {// get(mSubId).getSmsSaveSimEnabled())
                                         // {
                advancedCategory.removePreference(mSmsSaveSimPreference);
            } else {
                if (null != mSmsSaveSimPreference) {
                    SharedPreferences sharedPref = PreferenceManager
                            .getDefaultSharedPreferences(this.getActivity());
                    Log.d("PerSubscriptionSettingsActivity", "onCreate !!!!");
                    String curVal = sharedPref.getString(mSmsSaveSimPrefKey
                            + getRealSubId(mSubId), Boolean.toString(mContext
                            .getResources().getBoolean(
                                    R.bool.sms_save_to_sim_pref_default)));
                    mSmsSaveSimPreference.setValue(curVal);
                    mSmsSaveSimPreference.setSummary(mSmsSaveSimPreference
                            .getEntry());
                    mSmsSaveSimPreference.setOnPreferenceChangeListener(this);
                    Log.d("PerSubscriptionSettingsActivity",
                            "onCreate !!!! curVal = " + curVal
                                    + " getRealSubId(mSubId) = "
                                    + getRealSubId(mSubId) + " mPhoneId = "
                                    + mPhoneId);
                }
            }
            // sprd #542214 end
        }

        // sprd add for smsc begin
        private void updateSmscSummary(int subId) {
            // String summary = SmscManager.getSmscString(getActivity(), subId);
            // mSmscPreference.setSummary(summary);
        }

        // sprd add for smsc end

        private void updateGroupMmsPrefSummary() {
            final boolean groupMmsEnabled = getPreferenceScreen()
                    .getSharedPreferences().getBoolean(
                            mGroupMmsPrefKey,
                            getResources().getBoolean(
                                    R.bool.group_mms_pref_default));
            mGroupMmsPreference
                    .setSummary(groupMmsEnabled ? R.string.enable_group_mms
                            : R.string.disable_group_mms);
        }
        // sprd #542214 start
        private void updateSmsSaveSimPrefSummary() {
            SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(this.getActivity());
            String curVal = sharedPref.getString(
                    this.getActivity().getString(
                            R.string.sms_save_to_sim_pref_key)
                            + getRealSubId(mSubId),
                    Boolean.toString(mContext.getResources().getBoolean(
                            R.bool.sms_save_to_sim_pref_default)));
            mSmsSaveSimPreference.setValue(curVal);
            mSmsSaveSimPreference.setSummary(mSmsSaveSimPreference.getEntry());
            Log.d("PerSubscriptionSettingsActivity",
                    "updateSmsSaveSimPrefSummary = phoneId = " + mPhoneId
                            + " getRealSubId(mSubId) = " + getRealSubId(mSubId)
                            + " curVal = " + curVal);

        }

        // sprd #542214 end

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            updateSmscSummary(getRealSubId(mSubId));//sprd add for smsc
            // sprd #542214 start
            if (mSmsSaveSimPreference != null) {
                Log.d("PerSubscriptionSettingsActivity",
                        "onResume mSmsSaveSimPreference !!");
                updateSmsSaveSimPrefSummary();

            }
            // sprd #542214 end
        }

        @Override
        public void onSharedPreferenceChanged(
                final SharedPreferences sharedPreferences, final String key) {
            if (key.equals(mGroupMmsPrefKey)) {
                updateGroupMmsPrefSummary();
            } else if (key.equals(mPhoneNumberKey)) {
                // Save the changed phone number in preferences specific to the
                // sub id
                final String newPhoneNumber = mPhoneNumberPreference.getText();
                final BuglePrefs subPrefs = BuglePrefs
                        .getSubscriptionPrefs(mSubId);
                if (TextUtils.isEmpty(newPhoneNumber)) {
                    subPrefs.remove(mPhoneNumberKey);
                } else {
                    subPrefs.putString(
                            getString(R.string.mms_phone_number_pref_key),
                            newPhoneNumber);
                }
                // Update the self participants so the new phone number will be
                // reflected
                // everywhere in the UI.
                ParticipantRefresh.refreshSelfParticipants();
            }
            // sprd add for smsc begin
            else if (key.equals(mSmscPrefKey)) {
                updateSmscSummary(getRealSubId(mSubId));
            }
            // sprd add for smsc end
        }

        // sprd add for smsc begin
        private int getRealSubId(int subId) {
            if (OsUtil.hasPhonePermission()) {
                return PhoneUtils.getDefault().getEffectiveSubId(subId);
            } else {
                return -1;
            }
        }

        // sprd add for smsc end

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        /* Sprd add for sms and mms validity start */
        /* Delete by SPRD for bug 526653 Start */
        /*
         * public boolean onPreferenceClick(Preference preference) { if
         * (preference == mSmsValidityPref) {
         * mValidityWin.initPopWin(preference,
         * getRealValidityKey(SMS_VALIDITY_SETTING));
         * mValidityWin.showPopupWindow(); } else if (preference ==
         * mMmsValidityPref) { mValidityWin.initPopWin(preference,
         * getRealValidityKey(MMS_VALIDITY_SETTING));
         * mValidityWin.showPopupWindow(); } return true; }
         */
        /* Delete by SPRD for bug 526653 End */

        /* Add by sprd for bug 526653 Start */
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            SharedPreferences.Editor prefEditor = PreferenceManager
                    .getDefaultSharedPreferences(this.getActivity()).edit();
            SmsManager smsManager = SmsManager.getDefault();

            if (preference == mSmsRetryTimesPref) {
                if (((Boolean) newValue).booleanValue()) {
                    smsManager.setProperty(SEND_RETRIE_TIME, MAX_SEND_RETRIES);
                } else {
                    smsManager.setProperty(SEND_RETRIE_TIME, MIN_SEND_RETRIES);
                }
                return true;
            }

            if (preference == mSmsValidityPref) {
                smsManager.setProperty(
                        getRealValidityKey(SMS_VALIDITY_SETTING),
                        newValue.toString());
                prefEditor.putString(
                        this.getActivity().getString(
                                R.string.sms_validity_pref_key)
                                + mSubId, newValue.toString());
                mSmsValidityPref.setValue(newValue.toString());
                mSmsValidityPref.setSummary(mSmsValidityPref.getEntry());
                prefEditor.commit();
                return true;
            }

            if (preference == mMmsValidityPref) {
                smsManager.setProperty(
                        getRealValidityKey(MMS_VALIDITY_SETTING),
                        newValue.toString());
                prefEditor.putString(
                        this.getActivity().getString(
                                R.string.mms_validity_pref_key)
                                + mSubId, newValue.toString());
                mMmsValidityPref.setValue(newValue.toString());
                mMmsValidityPref.setSummary(mMmsValidityPref.getEntry());
                prefEditor.commit();
                return true;
            }
            if (preference == mSmsSaveSimPreference) {
                Log.d("PerSubscriptionSettingsActivity",
                        "onPreferenceChange preference = " + preference
                                + " newValue.toString() = "
                                + newValue.toString());
                boolean enable = Boolean.valueOf(newValue.toString());
                boolean retValue = mRadioInteractor.storeSmsToSim(enable,
                        mPhoneId);
                Log.d("PerSubscriptionSettingsActivity",
                        "onPreferenceChange preference = " + preference
                                + " retValue = " + retValue + " enable = "
                                + enable + " mPhoneId = " + mPhoneId + " getRealSubId(mSubId) = " + getRealSubId(mSubId));
                if (!retValue) {
                    return false;
                }
                prefEditor.putString(
                        this.getActivity().getString(
                                R.string.sms_save_to_sim_pref_key)
                                + getRealSubId(mSubId), newValue.toString());
                mSmsSaveSimPreference.setValue(newValue.toString());
                mSmsSaveSimPreference.setSummary(mSmsSaveSimPreference
                        .getEntry());
                prefEditor.commit();
                return true;
           }

            prefEditor = null;
            smsManager = null;

            return false;
        }

        /* Add by sprd for bug 526653 End */

        private String getRealValidityKey(String key) {
            // return key + mRealSubId;
            return key + getRealSubId(mSubId);
        }

        /* Delete by SPRD for bug 526653 Start */
        /*
         * private String getOldValidity(final String key) { String oldValidity
         * = mValidityWin.getKeyValidity(key); return oldValidity; }
         */
        /* Delete by SPRD for bug 526653 End */
        /* Sprd add for sms and mms validity end */
    }
}
