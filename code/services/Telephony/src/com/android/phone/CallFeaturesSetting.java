/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.phone;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.phone.common.util.SettingsUtil;
import com.android.phone.settings.AccountSelectionPreference;
import com.android.phone.settings.PhoneAccountSettingsFragment;
import com.android.phone.settings.VoicemailSettingsActivity;
import com.android.phone.settings.fdn.FdnSetting;
import com.android.services.telephony.sip.SipUtil;

import com.sprd.phone.IccUriUtils;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import com.sprd.android.config.OptConfig;

/**
 * Top level "Call settings" UI; see res/xml/call_feature_setting.xml
 *
 * This preference screen is the root of the "Call settings" hierarchy available from the Phone
 * app; the settings here let you control various features related to phone calls (including
 * voicemail settings, the "Respond via SMS" feature, and others.)  It's used only on
 * voice-capable phone devices.
 *
 * Note that this activity is part of the package com.android.phone, even
 * though you reach it from the "Phone" app (i.e. DialtactsActivity) which
 * is from the package com.android.contacts.
 *
 * For the "Mobile network settings" screen under the main Settings app,
 * See {@link MobileNetworkSettings}.
 *
 * @see com.android.phone.MobileNetworkSettings
 */
public class CallFeaturesSetting extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = "CallFeaturesSetting";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);
    // STOPSHIP if true. Flag to override behavior default behavior to hide VT setting.
    // SPRD: modify for bug523506
    private static final boolean ENABLE_VT_FLAG = false;

    // String keys for preference lookup
    // TODO: Naming these "BUTTON_*" is confusing since they're not actually buttons(!)
    // TODO: Consider moving these strings to strings.xml, so that they are not duplicated here and
    // in the layout files. These strings need to be treated carefully; if the setting is
    // persistent, they are used as the key to store shared preferences and the name should not be
    // changed unless the settings are also migrated.
    private static final String VOICEMAIL_SETTING_SCREEN_PREF_KEY = "button_voicemail_category_key";
    private static final String BUTTON_FDN_KEY   = "button_fdn_key";
    private static final String BUTTON_RETRY_KEY       = "button_auto_retry_key";
    private static final String BUTTON_GSM_UMTS_OPTIONS = "button_gsm_more_expand_key";
    private static final String BUTTON_CDMA_OPTIONS = "button_cdma_more_expand_key";

    private static final String PHONE_ACCOUNT_SETTINGS_KEY =
            "phone_account_settings_preference_screen";

    private static final String ENABLE_VIDEO_CALLING_KEY = "button_enable_video_calling";
    /* SPRD: bug#474338, LND/SDN FEATURE. @{ */
    private static final String SDN_LIST_KEY = "sdn_list_key";
    private static final String LND_LIST_KEY = "lnd_list_key";
    /* @} */
    /* SPRD: Add for feature of VOLTE @{ */
    private static final String CALL_FORWARDING_KEY = "call_forwarding_key";
    private static final String CALL_BARRING_KEY = "call_barring_key";
    private static final String ADDITIONAL_GSM_SETTINGS_KEY = "additional_gsm_call_settings_key";
    private static final int TOGGLE_ON_NETWORK = 605;
    /* @} */

    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private TelecomManager mTelecomManager;

    private CheckBoxPreference mButtonAutoRetry;
    private PreferenceScreen mVoicemailSettingsScreen;
    private CheckBoxPreference mEnableVideoCalling;

    /* SPRD: Add for feature of VOLTE @{ */
    private boolean mForeground;
    private boolean mIsSupportVolte;
    /* @} */

    /* SPRD: add for bug 521780 for sim hot plug @{ */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);

            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                Log.d(LOG_TAG, "action===" + action + ",state" + state);
                if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(state)) {
                    if (mPhone.getPhoneId() == phoneId) {
                        finish();
                    }
                }
            }
        }
    };
    /**/

    /*
     * Click Listeners, handle click based on objects attached to UI.
     */

    // Click listener for all toggle events
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // SPRD: Add for feature of VOLTE
        Log.d(LOG_TAG, "onPreferenceTreeClick preference.key:" + preference.getKey());
        if (preference == mButtonAutoRetry) {
            android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.CALL_AUTO_RETRY,
                    mButtonAutoRetry.isChecked() ? 1 : 0);
            return true;
        /* SPRD: bug#474338, LND/SDN FEATURE. @{ */
        } else if (preference != null && (SDN_LIST_KEY.equals(preference.getKey()) || LND_LIST_KEY.equals(preference.getKey()))) {
            Intent intent = preference.getIntent();
            if (intent != null) {
                String preferenceKey = preference.getKey();
                int sdnOrLnd = IccUriUtils.LND;
                if (SDN_LIST_KEY.equals(preferenceKey)) {
                    sdnOrLnd = IccUriUtils.SDN;
                }
            mSubscriptionInfoHelper.addExtrasToIntent(intent, sdnOrLnd);
            }
        /* @} */
        /* SPRD: Add for feature of VOLTE @{ */
        } else if (preference != null && (CALL_FORWARDING_KEY.equals(preference.getKey())
                || ADDITIONAL_GSM_SETTINGS_KEY.equals(preference.getKey())
                || CALL_BARRING_KEY.equals(preference.getKey()))) {
            if (getMobileDataEnabled() || !mIsSupportVolte) { // network is on dependency on
                                                              // mPhoneId
                log("network on");
                return false;
            } else { // network is off
                // Show dialog to let user toggle on network
                log("network off");
                // SPRD: add for bug556735
                if (mPhone != null && mPhone.isImsRegistered() && !getMobileDataEnabled()) {
                    showDialogIfForeground(TOGGLE_ON_NETWORK);
                    return true;
                }
            }
            /* @} */
        }
        return false;
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes.
     *
     * @param preference is the preference to be changed
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (DBG) log("onPreferenceChange: \"" + preference + "\" changed to \"" + objValue + "\"");

        if (preference == mEnableVideoCalling) {
            if (ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mPhone.getContext())) {
                PhoneGlobals.getInstance().phoneMgr.enableVideoCalling((boolean) objValue);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                DialogInterface.OnClickListener networkSettingsClickListener =
                        new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(mPhone.getContext(),
                                        com.android.phone.MobileNetworkSettings.class));
                            }
                        };
                builder.setMessage(getResources().getString(
                                R.string.enable_video_calling_dialog_msg))
                        .setNeutralButton(getResources().getString(
                                R.string.enable_video_calling_dialog_settings),
                                networkSettingsClickListener)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return false;
            }
        }

        // Always let the preference setting proceed.
        return true;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG) log("onCreate: Intent is " + getIntent());

        // Make sure we are running as the primary user.
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            Toast.makeText(this, R.string.call_settings_primary_user_only,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.call_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();
        mTelecomManager = TelecomManager.from(this);
        // SPRD: Add for feature of VOLTE
        mIsSupportVolte = ImsManager.isVolteEnabledByPlatform(this);
        /* SPRD: add for bug 521780 for sim hot plug @{ */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        this.registerReceiver(mReceiver, intentFilter);
        /* @} */
    }

    @Override
    protected void onResume() {
        super.onResume();

        /* SPRD: add for bug 521780 for sim hot plug @{ */
        if (SubscriptionManager.getSimStateForSlotIdx(mPhone.getPhoneId())
                == TelephonyManager.SIM_STATE_ABSENT) {
            finish();
        }
        /* @} */
        // SPRD: Add for feature of VOLTE
        mForeground = true;
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }

        addPreferencesFromResource(R.xml.call_feature_setting);

        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Preference phoneAccountSettingsPreference = findPreference(PHONE_ACCOUNT_SETTINGS_KEY);
        if (telephonyManager.isMultiSimEnabled() || !SipUtil.isVoipSupported(mPhone.getContext())) {
            getPreferenceScreen().removePreference(phoneAccountSettingsPreference);
        }

        PreferenceScreen prefSet = getPreferenceScreen();
        mVoicemailSettingsScreen =
                (PreferenceScreen) findPreference(VOICEMAIL_SETTING_SCREEN_PREF_KEY);
        mVoicemailSettingsScreen.setIntent(mSubscriptionInfoHelper.getIntent(
                VoicemailSettingsActivity.class));

        mButtonAutoRetry = (CheckBoxPreference) findPreference(BUTTON_RETRY_KEY);

        mEnableVideoCalling = (CheckBoxPreference) findPreference(ENABLE_VIDEO_CALLING_KEY);

        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());

        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_AUTO_RETRY_ENABLED_BOOL)) {
            mButtonAutoRetry.setOnPreferenceChangeListener(this);
            int autoretry = Settings.Global.getInt(
                    getContentResolver(), Settings.Global.CALL_AUTO_RETRY, 0);
            mButtonAutoRetry.setChecked(autoretry != 0);
        } else {
            prefSet.removePreference(mButtonAutoRetry);
            mButtonAutoRetry = null;
        }

        Preference cdmaOptions = prefSet.findPreference(BUTTON_CDMA_OPTIONS);
        Preference gsmOptions = prefSet.findPreference(BUTTON_GSM_UMTS_OPTIONS);
        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            cdmaOptions.setIntent(mSubscriptionInfoHelper.getIntent(CdmaCallOptions.class));
            gsmOptions.setIntent(mSubscriptionInfoHelper.getIntent(GsmUmtsCallOptions.class));
        } else {
            prefSet.removePreference(cdmaOptions);
            prefSet.removePreference(gsmOptions);

            int phoneType = mPhone.getPhoneType();
            Preference fdnButton = prefSet.findPreference(BUTTON_FDN_KEY);
            if (carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
                prefSet.removePreference(fdnButton);
            } else {
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    prefSet.removePreference(fdnButton);

                    if (!carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_VOICE_PRIVACY_DISABLE_UI_BOOL)) {
                        addPreferencesFromResource(R.xml.cdma_call_privacy);
                    }
                } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                    fdnButton.setIntent(mSubscriptionInfoHelper.getIntent(FdnSetting.class));

                    if (carrierConfig.getBoolean(
                            CarrierConfigManager.KEY_ADDITIONAL_CALL_SETTING_BOOL)) {
                        addPreferencesFromResource(R.xml.gsm_umts_call_options);
                        GsmUmtsCallOptions.init(prefSet, mSubscriptionInfoHelper);
                    }
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        }

        if (ImsManager.isVtEnabledByPlatform(mPhone.getContext()) && ENABLE_VT_FLAG) {
            boolean currentValue =
                    ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mPhone.getContext())
                    ? PhoneGlobals.getInstance().phoneMgr.isVideoCallingEnabled(
                            getOpPackageName()) : false;
            mEnableVideoCalling.setChecked(currentValue);
            mEnableVideoCalling.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mEnableVideoCalling);
        }

        if (ImsManager.isVolteEnabledByPlatform(this) &&
                !carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_VOLTE_TTY_SUPPORTED_BOOL)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            /* tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE); */
        }

        Preference wifiCallingSettings = findPreference(
                getResources().getString(R.string.wifi_calling_settings_key));

        final PhoneAccountHandle simCallManager = mTelecomManager.getSimCallManager();
        if (simCallManager != null) {
            Intent intent = PhoneAccountSettingsFragment.buildPhoneAccountConfigureIntent(
                    this, simCallManager);
            if (intent != null) {
                wifiCallingSettings.setTitle(R.string.wifi_calling);
                wifiCallingSettings.setSummary(null);
                wifiCallingSettings.setIntent(intent);
            } else {
                prefSet.removePreference(wifiCallingSettings);
            }
        } else if (!ImsManager.isWfcEnabledByPlatform(mPhone.getContext())) {
            prefSet.removePreference(wifiCallingSettings);
        } else {
            int resId = com.android.internal.R.string.wifi_calling_off_summary;
            if (ImsManager.isWfcEnabledByUser(mPhone.getContext())) {
                int wfcMode = ImsManager.getWfcMode(mPhone.getContext());
                switch (wfcMode) {
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                        resId = com.android.internal.R.string.wfc_mode_wifi_only_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_cellular_preferred_summary;
                        break;
                    case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                        resId = com.android.internal.R.string.wfc_mode_wifi_preferred_summary;
                        break;
                    default:
                        if (DBG) log("Unexpected WFC mode value: " + wfcMode);
                }
            }
            wifiCallingSettings.setSummary(resId);
        }
        /* SPRD: bug#474338, LND/SDN FEATURE. @{ */
        if(!OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
            addPreferencesFromResource(R.xml.lnd_sdn_list_ex);
        }
        /* @} */
    }

    /* SPRD: Add for feature of VOLTE @{ */
    @Override
    public void onPause() {
        super.onPause();
        mForeground = false;
    }

    /* SPRD: add for bug 521780 for sim hot plug @{ */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            this.unregisterReceiver(mReceiver);
        }
    }
    /* @} */

    @Override
    protected Dialog onCreateDialog(int dialogId) {
        if(dialogId == TOGGLE_ON_NETWORK && mIsSupportVolte) {
            final DialogInterface.OnClickListener butListener
                    = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Send Intent to toggle on network
                        Intent intent = new Intent(
                                "com.android.settings.sim.SIM_SUB_INFO_SETTINGS");
                        startActivity(intent);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                    default:
                        break;
                    }
                }
            };
            AlertDialog.Builder b = new AlertDialog.Builder(this)
                    .setPositiveButton(R.string.alert_dialog_yes, butListener)
                    .setNegativeButton(R.string.alert_dialog_no, butListener)
                    .setTitle(getText(R.string.error_updating_title).toString())
                    .setMessage(getText(R.string.network_not_on).toString());
            // b.setCancelable(false);
            AlertDialog dialog = b.create();

            // make the dialog more obvious by bluring the background.
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

            return dialog;
        }
        return new AlertDialog.Builder(this).create();
    }
    /* @} */

    @Override
    protected void onNewIntent(Intent newIntent) {
        setIntent(newIntent);

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.call_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Finish current Activity and go up to the top level Settings ({@link CallFeaturesSetting}).
     * This is useful for implementing "HomeAsUp" capability for second-level Settings.
     */
    public static void goUpToTopLevelSetting(
            Activity activity, SubscriptionInfoHelper subscriptionInfoHelper) {
        Intent intent = subscriptionInfoHelper.getIntent(CallFeaturesSetting.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * SPRD: add for bug 501695 @{
     */
    public static boolean isSimStandbyNow(SubscriptionInfoHelper subscriptionInfoHelper,
            Context context) {
        int phoneId = -1;
        boolean isSimStandby = false;
        int subId = subscriptionInfoHelper.getSubId();
        TelephonyManager telephonyManager = TelephonyManager.from(context);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            phoneId = telephonyManager.getPhoneId(subId);
            if (SubscriptionManager.isValidPhoneId(phoneId)) {
                isSimStandby = telephonyManager.isSimStandby(phoneId);
            }
        }
        return isSimStandby;
    }
    /** @} */

    /* SPRD: Add for feature of VOLTE @{ */
    private void showDialogIfForeground(int id) {
        if (mForeground) {
            showDialog(id);
        }
    }

    private boolean getMobileDataEnabled() {
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDataEnabled();
    }
    /* @} */
}
