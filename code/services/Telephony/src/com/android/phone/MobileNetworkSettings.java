/*
 * Copyright (C) 2006 The Android Open Source Project
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

import com.android.ims.ImsManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.policy.RadioTaskManager;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.RadioFeatures;
// SPRD: National Data Roaming.
import android.telephony.TelephonyManager.DataRoamType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabHost;
import android.widget.TextView;
import com.android.ims.ImsManager;
import com.sprd.phone.TeleServicePluginsHelper;
import com.sprd.android.config.OptConfig;
import com.android.internal.telephony.RILConstants;//Kalyy

/**
 * "Mobile network settings" screen.  This preference screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this PreferenceActivity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
public class MobileNetworkSettings extends PreferenceActivity
        implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener{

    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    // Number of active Subscriptions to show tabs
    private static final int TAB_THRESHOLD = 2;

    //String keys for preference lookup
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    // SPRD: National Data Roaming.
    private static final String BUTTON_PREFERRED_DATA_ROAMING = "preferred_data_roaming_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
    private static final String BUTTON_ENABLED_NETWORKS_KEY = "enabled_networks_key";
    private static final String BUTTON_4G_LTE_KEY = "enhanced_4g_lte";
    private static final String BUTTON_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key";
    private static final String BUTTON_OPERATOR_SELECTION_EXPAND_KEY = "button_carrier_sel_key";
    private static final String BUTTON_CARRIER_SETTINGS_KEY = "carrier_settings_key";
    private static final String BUTTON_CDMA_SYSTEM_SELECT_KEY = "cdma_system_select_key";
    private static final String BUTTON_LTE_PREFERED_NETWORK_MODE = "lte_preferred_network_mode_key";

    private static final String PREFERRED_NETWORK_MODE_4G_3G_2G = "0";
    private static final String PREFERRED_NETWORK_MODE_3G_2G = "1";
    private static final String PREFERRED_NETWORK_MODE_4G_ONLY="2";
    private static final String PREFERRED_NETWORK_MODE_3G_ONLY="3";
    private static final String PREFERRED_NETWORK_MODE_2G_ONLY="4";

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    //Information about logical "up" Activity
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String UP_ACTIVITY_CLASS =
            "com.android.settings.Settings$WirelessSettingsActivity";

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    //UI objects
    private ListPreference mButtonPreferredNetworkMode;
    private ListPreference mButtonEnabledNetworks;
    private SwitchPreference mButtonDataRoam;
    // SPRD: National Data Roaming.
    private ListPreference mButtonPreferredDataRoam;
    private SwitchPreference mButton4glte;
    private Preference mLteDataServicePref;
    private ListPreference mButtonLtePreferredNetworkMode;
    private PorgressDialogFragment mProgressDialogFragment;
    private boolean mIsForeground = false;
    private static final String iface = "rmnet0"; //TODO: this will go away
    private List<SubscriptionInfo> mActiveSubInfos;
    //SPRD:add for VoLTE
    private Preference mVoltePref;
    private boolean isVolteEnable;

    private UserManager mUm;
    private Phone mPhone;
    private MyHandler mHandler;
    private boolean mOkClicked;

    // We assume the the value returned by mTabHost.getCurrentTab() == slotId
    private TabHost mTabHost;

    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;
    private boolean mShow4GForLTE;
    private boolean mIsGlobalCdma;
    private boolean mUnavailable;
    private boolean mIsSupportWcdmaOnly;

    // SPRD: modify for bug508651
    private AlertDialog mDataRoamDialog = null;

    // SPRD: National Data Roaming.
    private boolean mShowNationalDataRoam;

    private RadioTaskManager mRadioTaskManger;

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /*
         * Enable/disable the 'Enhanced 4G LTE Mode' when in/out of a call
         * and depending on TTY mode and TTY support over VoLTE.
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);
            boolean enabled = (state == TelephonyManager.CALL_STATE_IDLE) &&
                    ImsManager.isNonTtyOrTtyOnVolteEnabled(getApplicationContext());
            Preference pref = getPreferenceScreen().findPreference(BUTTON_4G_LTE_KEY);
            if (pref != null) pref.setEnabled(enabled && hasActiveSubscriptions());
        }
    };

    private final BroadcastReceiver mPhoneChangeReceiver = new PhoneChangeReceiver();

    private class PhoneChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DBG) log("onReceive:");
            // When the radio changes (ex: CDMA->GSM), refresh all options.
            /* SPRD: modify for bug508651 @{ */
            String action = intent.getAction();
            if(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED.equals(action)){
                // When the radio changes (ex: CDMA->GSM), refresh all options.
                mGsmUmtsOptions = null;
                mCdmaOptions = null;
                updateBody();
            } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                Log.d(LOG_TAG, "handler ACTION_SIM_STATE_CHANGED");
                String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                Log.d(LOG_TAG, "SIM state: " + stateExtra);
                if (stateExtra != null && IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
                    dismissDialog();
                }
            }
            /* @} */
        }
    }

    //This is a method implemented for DialogInterface.OnClickListener.
    //  Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mPhone.setDataRoamingEnabled(true);
            mOkClicked = true;
        } else {
            // Reset the toggle
            mButtonDataRoam.setChecked(false);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        mButtonDataRoam.setChecked(mOkClicked);
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /** TODO: Refactor and get rid of the if's using subclasses */
        final int phoneSubId = mPhone.getSubId();
        if (preference.getKey().equals(BUTTON_4G_LTE_KEY)) {
            return true;
        } else if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
            return true;
        } else if (mCdmaOptions != null &&
                   mCdmaOptions.preferenceTreeClick(preference) == true) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mLteDataServicePref) {
            String tmpl = android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL);
            if (!TextUtils.isEmpty(tmpl)) {
                TelephonyManager tm = (TelephonyManager) getSystemService(
                        Context.TELEPHONY_SERVICE);
                String imsi = tm.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.isEmpty(tmpl) ? null
                        : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            }
            return true;
        }   else if (preference == mButtonLtePreferredNetworkMode) {
            return true;
        } else if (preference == mButtonEnabledNetworks) {
            int settingsNetworkMode = android.provider.Settings.Global.getInt(mPhone.getContext().
                            getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mButtonDataRoam) {
            // Do not disable the preference screen if the user clicks Data roaming.
            return true;
            /*SPRD: Bug 474686 Add for Uplmn @{*/
        } else if(preference == mUplmnPref){
            Log.d(LOG_TAG, "onPreferenceTreeClick: preference == mUplmnPref");
            int phoneId = SubscriptionManager.getPhoneId(phoneSubId);
            Intent intent = preference.getIntent();
            if (intent != null) {
                intent.putExtra("sub_id", phoneSubId);
                intent.putExtra("is_usim",isUsimCard(phoneId));
            }
            return false;
        }
        /* @} */
        /* SPRD: add for VoLTE @{ */
        else if(preference == mVoltePref){
            if (preference != null) {
                Intent intent = preference.getIntent();
                if (intent != null) {
                    intent.putExtra("sub_id", phoneSubId);
                }
            }
            return false;
        }
        /* @} */
        /* SPRD: National Data Roaming. {@ */
        else if (preference == mButtonPreferredDataRoam) {
            int roamType = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(),
                    android.provider.Settings.Global.DATA_ROAMING + phoneSubId,
                    DataRoamType.DISABLE.ordinal());
            //mButtonPreferredDataRoam.setValue(Integer.toString(roamType));
            updatePreferredDataRoamValueAndSummary(roamType);
            return false;
        }
        /* @} */
        else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged:");

            /* SPRD: add for bug522204 @{ */
            //initializeSubscriptions();
            final AsyncTask<Void, Void, List<SubscriptionInfo>> task =
                    new AsyncTask<Void, Void, List<SubscriptionInfo>>() {
                @Override
                protected List<SubscriptionInfo> doInBackground(Void... params) {
                    return mSubscriptionManager.getActiveSubscriptionInfoList();
                }

                @Override
                protected void onPostExecute(List<SubscriptionInfo> result) {
                    initializeSubscriptions(result);
                }
            };
            task.execute();
            /* @} */
            updatePhoneStateListener();
        }
    };

    // SPRD: add for bug522204
    private void initializeSubscriptions(List<SubscriptionInfo> result) {
        int currentTab = 0;
        if (DBG) log("initializeSubscriptions:+");

        // Before updating the the active subscription list check
        // if tab updating is needed as the list is changing.
        //List<SubscriptionInfo> sil = mSubscriptionManager.getActiveSubscriptionInfoList();
        List<SubscriptionInfo> sil = result;
        TabState state = isUpdateTabsNeeded(sil);

        // Update to the active subscription list
        mActiveSubInfos.clear();
        if (sil != null) {
            mActiveSubInfos.addAll(sil);
            // If there is only 1 sim then currenTab should represent slot no. of the sim.
            if (sil.size() == 1) {
                currentTab = sil.get(0).getSimSlotIndex();
            }
        }

        switch (state) {
            case UPDATE: {
                if (DBG) log("initializeSubscriptions: UPDATE");
                currentTab = mTabHost != null ? mTabHost.getCurrentTab() : 0;

                setContentView(com.android.internal.R.layout.common_tab_settings);

                mTabHost = (TabHost) findViewById(android.R.id.tabhost);
                mTabHost.setup();

                // Update the tabName. Since the mActiveSubInfos are in slot order
                // we can iterate though the tabs and subscription info in one loop. But
                // we need to handle the case where a slot may be empty.

                Iterator<SubscriptionInfo> siIterator = mActiveSubInfos.listIterator();
                SubscriptionInfo si = siIterator.hasNext() ? siIterator.next() : null;
                for (int simSlotIndex = 0; simSlotIndex  < mActiveSubInfos.size(); simSlotIndex++) {
                    String tabName;
                    if (si != null && si.getSimSlotIndex() == simSlotIndex) {
                        // Slot is not empty and we match
                        // SPRD: modify for display "SIM1" or "SIM2" if diaplay name is empty
                        tabName = String.valueOf(TextUtils.isEmpty(si.getDisplayName()) ?
                                "SIM" + (simSlotIndex + 1) : si.getDisplayName());
                        si = siIterator.hasNext() ? siIterator.next() : null;
                    } else {
                        // Slot is empty, set name to unknown
                        tabName = getResources().getString(R.string.unknown);
                    }
                    if (DBG) {
                        log("initializeSubscriptions: tab=" + simSlotIndex + " name=" + tabName);
                    }

                    mTabHost.addTab(buildTabSpec(String.valueOf(simSlotIndex), tabName));
                }

                mTabHost.setOnTabChangedListener(mTabListener);
                mTabHost.setCurrentTab(currentTab);
                break;
            }
            case NO_TABS: {
                if (DBG) log("initializeSubscriptions: NO_TABS");

                if (mTabHost != null) {
                    mTabHost.clearAllTabs();
                    mTabHost = null;
                }
                setContentView(com.android.internal.R.layout.common_tab_settings);
                break;
            }
            case DO_NOTHING: {
                if (DBG) log("initializeSubscriptions: DO_NOTHING");
                if (mTabHost != null) {
                    currentTab = mTabHost.getCurrentTab();
                }
                break;
            }
        }
        updatePhone(currentTab);
        updateBody();
        if (DBG) log("initializeSubscriptions:-");
    }

    private enum TabState {
        NO_TABS, UPDATE, DO_NOTHING
    }
    private TabState isUpdateTabsNeeded(List<SubscriptionInfo> newSil) {
        TabState state = TabState.DO_NOTHING;
        if (newSil == null) {
            if (mActiveSubInfos.size() >= TAB_THRESHOLD) {
                if (DBG) log("isUpdateTabsNeeded: NO_TABS, size unknown and was tabbed");
                state = TabState.NO_TABS;
            }
        } else if (newSil.size() < TAB_THRESHOLD && mActiveSubInfos.size() >= TAB_THRESHOLD) {
            if (DBG) log("isUpdateTabsNeeded: NO_TABS, size went to small");
            state = TabState.NO_TABS;
        } else if (newSil.size() >= TAB_THRESHOLD && mActiveSubInfos.size() < TAB_THRESHOLD) {
            if (DBG) log("isUpdateTabsNeeded: UPDATE, size changed");
            state = TabState.UPDATE;
        } else if (newSil.size() >= TAB_THRESHOLD) {
            Iterator<SubscriptionInfo> siIterator = mActiveSubInfos.iterator();
            for(SubscriptionInfo newSi : newSil) {
                SubscriptionInfo curSi = siIterator.next();
                if (!newSi.getDisplayName().equals(curSi.getDisplayName())) {
                    if (DBG) log("isUpdateTabsNeeded: UPDATE, new name=" + newSi.getDisplayName());
                    state = TabState.UPDATE;
                    break;
                }
            }
        }
        if (DBG) {
            log("isUpdateTabsNeeded:- " + state
                + " newSil.size()=" + ((newSil != null) ? newSil.size() : 0)
                + " mActiveSubInfos.size()=" + mActiveSubInfos.size());
        }
        return state;
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            if (DBG) log("onTabChanged:");
            // The User has changed tab; update the body.
            updatePhone(Integer.parseInt(tabId));
            updateBody();
        }
    };

    private void updatePhone(int slotId) {
        final SubscriptionInfo sir = mSubscriptionManager
                .getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (sir != null) {
            mPhone = PhoneFactory.getPhone(
                    SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
        }
        if (mPhone == null) {
            // Do the best we can
            mPhone = PhoneGlobals.getPhone();
        }

        if (DBG) log("updatePhone:- slotId=" + slotId + " sir=" + sir);
    }

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    private TabSpec buildTabSpec(String tag, String title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (DBG) log("onCreate:+");
        setTheme(R.style.Theme_Material_Settings);
        super.onCreate(icicle);
        //SPRD:add for volte
        isVolteEnable = ImsManager.isVolteEnabledByPlatform(this);
        mHandler = new MyHandler();
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(this);
        mTelephonyManager = (TelephonyManager)TelephonyManager.from(this);
        mRadioTaskManger = RadioTaskManager.getDefault();

        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            mUnavailable = true;
            setContentView(R.layout.telephony_disallowed_preference_screen);
            return;
        }

        if(isVolteEnable){
        	addPreferencesFromResource(R.xml.network_setting_volte);
        	mVoltePref = getPreferenceScreen().findPreference("ims_call_availability_settings");
        }else{
            addPreferencesFromResource(R.xml.network_setting);
        }

        mButton4glte = (SwitchPreference)findPreference(BUTTON_4G_LTE_KEY);
        mButton4glte.setOnPreferenceChangeListener(this);

        try {
            Context con = createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            mShow4GForLTE = con.getResources().getBoolean(id);
        } catch (NameNotFoundException e) {
            loge("NameNotFoundException for show4GFotLTE");
            mShow4GForLTE = false;
        }

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        mButtonDataRoam = (SwitchPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        /* SPRD: National Data Roaming. {@ */
        mButtonPreferredDataRoam = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERRED_DATA_ROAMING);
        mButtonPreferredDataRoam.setOnPreferenceChangeListener(this);
        /* @} */
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonEnabledNetworks = (ListPreference) prefSet.findPreference(
                BUTTON_ENABLED_NETWORKS_KEY);
        mButtonLtePreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_LTE_PREFERED_NETWORK_MODE);

        mButtonDataRoam.setOnPreferenceChangeListener(this);

        mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);


        /* SPRD: Bug 474686 Add for Uplmn @{ */
        mUplmnPref = (Preference) prefSet.findPreference(BUTTON_UPLMN_KEY);
        /* @} */
        // Initialize mActiveSubInfo
        int max = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        mActiveSubInfos = new ArrayList<SubscriptionInfo>(max);

        // SPRD: add for bug522204
        initializeSubscriptions(mSubscriptionManager.getActiveSubscriptionInfoList());

        IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        // SPRD: modify for bug508651
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mPhoneChangeReceiver, intentFilter);
        if (DBG) log("onCreate:-");
    }

    /* SPRD: modify for bug508651 @{ */
    private void dismissDialog(){
        if(mDataRoamDialog != null && mDataRoamDialog.isShowing() && mIsForeground){
            Log.d(LOG_TAG, "DataRoamDialog dismiss");
            mDataRoamDialog.dismiss();
        }
        if(mButtonEnabledNetworks.getDialog() != null && mButtonEnabledNetworks.getDialog().isShowing() && mIsForeground){
            Log.d(LOG_TAG, "NetworksNetworks dismiss");
            mButtonEnabledNetworks.getDialog().dismiss();
        }
        if(mButtonLtePreferredNetworkMode.getDialog() != null && mButtonLtePreferredNetworkMode.getDialog().isShowing() && mIsForeground){
            Log.d(LOG_TAG, "NetworksNetworks LTE dismiss");
            mButtonLtePreferredNetworkMode.getDialog().dismiss();
        }
    }
    /* @} */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPhoneChangeReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DBG) log("MESSAGE_SET_PREFERRED_NETWORK_TYPEonResume:+");

        if (mUnavailable) {
            if (DBG) log("onResume:- ignore mUnavailable == false");
            return;
        }

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        getPreferenceScreen().setEnabled(true);
        /* SPRD: add by fix bug494651 @{ */
        int phoneId = mPhone.getPhoneId();
        boolean isSimReady = mTelephonyManager
                .getSimState(phoneId) == TelephonyManager.SIM_STATE_READY;
        getPreferenceScreen().setEnabled(mTelephonyManager.isSimStandby(phoneId) && isSimReady);
        /* @} */

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null
                || getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null)  {
            updatePreferredNetworkUIFromDb();
        }

        if (ImsManager.isVolteEnabledByPlatform(this)
                && ImsManager.isVolteProvisionedOnDevice(this)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
        boolean enh4glteMode = ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this)
                && ImsManager.isNonTtyOrTtyOnVolteEnabled(this);
        mButton4glte.setChecked(enh4glteMode);

        registerPhoneStateListener();

        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        mIsForeground = true;

        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.RADIO_OPERATION), true,
                mRadioBusyObserver);
        if (!mTelephonyManager.isRadioBusy()) {
            dismissProgressingDialog();
        }

        if (DBG) log("onResume:-");

    }

    private boolean hasActiveSubscriptions() {
        return mActiveSubInfos.size() > 0;
    }

    private void updateBody() {
        final Context context = getApplicationContext();
        PreferenceScreen prefSet = getPreferenceScreen();
        /* SPRD: add by fix bug494651 @{ */
        final int phoneSubId = mPhone.getSubId();
        int phoneId = mTelephonyManager.getPhoneId(phoneSubId);
        boolean isSimReady = mTelephonyManager
                .getSimState(phoneId) == TelephonyManager.SIM_STATE_READY;
        // SPRD: modify for bug508651
        prefSet.setEnabled(mTelephonyManager.isSimStandby(phoneId) && isSimReady && !mTelephonyManager.isAirplaneModeOn());
        /* @} */

        boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;

        // SPRD: modify for bug523282
        int primaryCard = mTelephonyManager.getPrimaryCard();
        if (DBG) {
            log("updateBody : isLteOnCdma = " + isLteOnCdma + " phoneSubId = "
                + phoneSubId + " getPrimaryCard = " + primaryCard);
        }

        if (prefSet != null) {
            prefSet.removeAll();
            prefSet.addPreference(mButtonDataRoam);
            prefSet.addPreference(mButtonPreferredNetworkMode);
            prefSet.addPreference(mButtonEnabledNetworks);
            /* SPRD: add for bug 524480 @{ */
            if (mButton4glte != null && primaryCard == phoneId) {
                prefSet.addPreference(mButton4glte);
            }
            /* @} */
            /* SPRD:Uplmn won't show if not supported,see BUG 531918. @{ */
            if (TeleServicePluginsHelper.getInstance(this).isSupportPlmn()) {
                Log.d(LOG_TAG, "add preference of ulpmn settings");
                /* SPRD: Bug 474686 Add for Uplmn @{ */
                if(!OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){//wangxing add 20160630
                    prefSet.addPreference(mUplmnPref);  
                }
                /* SPRD: modify by BUG 492870 @{ */
                mUplmnPref.setEnabled(!(mTelephonyManager.getSimState(phoneId)
                        == TelephonyManager.SIM_STATE_PIN_REQUIRED));
                /* @} */
                /* @} */
            } else {
                Log.d(LOG_TAG, "remove preference of ulpmn settings due to CMCC or Reliance");
            }
            /* @} */
            /* SPRD: add for VoLTE @{ */
            if (isVolteEnable && mVoltePref != null && primaryCard == phoneId) {
               prefSet.addPreference(mVoltePref);
            }
            /* @} */
        }

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                preferredNetworkMode);

        PersistableBundle carrierConfig =
                PhoneGlobals.getInstance().getCarrierConfigForSubId(mPhone.getSubId());
        mIsGlobalCdma = isLteOnCdma
                && carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
        mIsSupportWcdmaOnly = carrierConfig
                .getBoolean(CarrierConfigManager.KEY_NETWORK_SUPPORT_WCDMA_ONLY);
        /* SPRD:Add for Bug 490243 @{ */
        boolean isSupport3GOnly2GOnly = carrierConfig
                .getBoolean(CarrierConfigManager.KEY_NETWORK_SUPPORT_3G_ONLY_AND_2G_ONLY_BOOL);
        TeleServicePluginsHelper.getInstance(context).set3GOnly2GOnly(
                isSupport3GOnly2GOnly);
        /* @} */
        if (carrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)) {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            prefSet.removePreference(mButtonEnabledNetworks);
            prefSet.removePreference(mLteDataServicePref);
        } else if (carrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL) == true) {
            prefSet.removePreference(mButtonEnabledNetworks);
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
        } else {
            prefSet.removePreference(mButtonPreferredNetworkMode);
            final int phoneType = mPhone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                int lteForced = android.provider.Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.LTE_SERVICE_FORCED + mPhone.getSubId(),
                        0);

                if (isLteOnCdma) {
                    if (lteForced == 0) {
                        mButtonEnabledNetworks.setEntries(
                                R.array.enabled_networks_cdma_choices);
                        mButtonEnabledNetworks.setEntryValues(
                                R.array.enabled_networks_cdma_values);
                    } else {
                        switch (settingsNetworkMode) {
                            case Phone.NT_MODE_CDMA:
                            case Phone.NT_MODE_CDMA_NO_EVDO:
                            case Phone.NT_MODE_EVDO_NO_CDMA:
                                mButtonEnabledNetworks.setEntries(
                                        R.array.enabled_networks_cdma_no_lte_choices);
                                mButtonEnabledNetworks.setEntryValues(
                                        R.array.enabled_networks_cdma_no_lte_values);
                                break;
                            case Phone.NT_MODE_GLOBAL:
                            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                            case Phone.NT_MODE_LTE_ONLY:
                                mButtonEnabledNetworks.setEntries(
                                        R.array.enabled_networks_cdma_only_lte_choices);
                                mButtonEnabledNetworks.setEntryValues(
                                        R.array.enabled_networks_cdma_only_lte_values);
                                break;
                            default:
                                mButtonEnabledNetworks.setEntries(
                                        R.array.enabled_networks_cdma_choices);
                                mButtonEnabledNetworks.setEntryValues(
                                        R.array.enabled_networks_cdma_values);
                                break;
                        }
                    }
                }
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);

                // In World mode force a refresh of GSM Options.
                if (isWorldMode()) {
                    mGsmUmtsOptions = null;
                }
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                if (TelephonyManager.isDeviceSupportLte()) {

                    prefSet.addPreference(mButtonLtePreferredNetworkMode);
                    TeleServicePluginsHelper.getInstance(this).setVolteEnable(isVolteEnable);
                    mButtonLtePreferredNetworkMode = TeleServicePluginsHelper.getInstance(this)
                            .setLtePreferenceValues(mButtonLtePreferredNetworkMode);
                    mButtonLtePreferredNetworkMode.setOnPreferenceChangeListener(this);
                    /**
                     * CMCC new case : Not allow user to set network type to 3g2g ;
                     * When insert SIM card,remove MobileNetworkSetting preference
                     * see bug522182
                     */
                    if (TeleServicePluginsHelper.getInstance(context)
                            .isMainSlotInsertSIMCard(primaryCard)) {
                        prefSet.removePreference(mButtonLtePreferredNetworkMode);
                    }
                    /** @} */
                     prefSet.removePreference(mButtonEnabledNetworks);
                     if (primaryCard != SubscriptionManager.getPhoneId(phoneSubId)) {
                        prefSet.removePreference(mButtonLtePreferredNetworkMode);
                     }
                }
                if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                        && !getResources().getBoolean(R.bool.config_enabled_lte) && !OptConfig.SUN_2G3G4G_SUPPORT) {//Kalyy
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_except_gsm_lte_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_gsm_lte_values);
                } else if (!carrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
                    int select = (mShow4GForLTE == true) ?
                            R.array.enabled_networks_except_gsm_4g_choices
                            : R.array.enabled_networks_except_gsm_choices;
                    mButtonEnabledNetworks.setEntries(select);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_except_gsm_values);
                } else if (!getResources().getBoolean(R.bool.config_enabled_lte) && !OptConfig.SUN_2G3G4G_SUPPORT) {//Kalyy
                    if (mPhone.getPhoneId() == primaryCard) {
                        /* SPRD: add network mode for WCDMA only *@{ */
                        if (mIsSupportWcdmaOnly) {
                            mButtonEnabledNetworks
                                    .setEntries(R.array.preferred_network_mode_choices_wcdma);
                            mButtonEnabledNetworks
                                    .setEntryValues(R.array.preferred_network_mode_values_wcdma);
                        } else {
                            mButtonEnabledNetworks
                                    .setEntries(R.array.enabled_networks_except_lte_choices);
                            mButtonEnabledNetworks
                                    .setEntryValues(R.array.enabled_networks_except_lte_values);
                        }
                        /*}@*/
                    } else {
                       prefSet.removePreference(mButtonEnabledNetworks);
                    }
                } else if (mIsGlobalCdma) {
                    mButtonEnabledNetworks.setEntries(
                            R.array.enabled_networks_cdma_choices);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_cdma_values);
                } else {
                    int select = (mShow4GForLTE == true) ? R.array.enabled_networks_4g_choices
                            : R.array.enabled_networks_choices;
                    mButtonEnabledNetworks.setEntries(select);
                    mButtonEnabledNetworks.setEntryValues(
                            R.array.enabled_networks_values);
                }
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            if (isWorldMode()) {
                mButtonEnabledNetworks.setEntries(
                        R.array.preferred_network_mode_choices_world_mode);
                mButtonEnabledNetworks.setEntryValues(
                        R.array.preferred_network_mode_values_world_mode);
            }
            mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
            if (DBG) log("settingsNetworkMode: " + settingsNetworkMode);
        }

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Global.getString(getContentResolver(),
                        android.provider.Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(mLteDataServicePref);
        } else {
            android.util.Log.d(LOG_TAG, "keep ltePref");
        }

        // Enable enhanced 4G LTE mode settings depending on whether exists on platform
        if (!(ImsManager.isVolteEnabledByPlatform(this)
                && ImsManager.isVolteProvisionedOnDevice(this))) {
            Preference pref = prefSet.findPreference(BUTTON_4G_LTE_KEY);
            if (pref != null) {
                prefSet.removePreference(pref);
            }
        }

        /* SPRD: National Data Roaming. {@ */
        mShowNationalDataRoam = carrierConfig
                .getBoolean(CarrierConfigManager.KEY_NATIONAL_DATA_ROAMING_BOOL);
        if (mShowNationalDataRoam) {
            prefSet.removePreference(mButtonDataRoam);
            prefSet.addPreference(mButtonPreferredDataRoam);
        }
        /* @} */

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final boolean isSecondaryUser = UserHandle.myUserId() != UserHandle.USER_OWNER;
        // Enable link to CMAS app settings depending on the value in config.xml.
        final boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        if (isSecondaryUser || !isCellBroadcastAppLinkEnabled
                || mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
            if (ps != null) {
                root.removePreference(ps);
            }
        }

        // Get the networkMode from Settings.System and displays it
        /* SPRD: National Data Roaming. @{ */
        if (mShowNationalDataRoam) {
            int roamType = android.provider.Settings.Global.getInt(mPhone.getContext().
                    getContentResolver(),
                    android.provider.Settings.Global.DATA_ROAMING + phoneSubId,
                    DataRoamType.DISABLE.ordinal());
            //updatePreferredDataRoamSummary(roamType);
            updatePreferredDataRoamValueAndSummary(roamType);
        } else {
            mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());
        }
        /* @} */
        mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
        mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);

        updateLtePreferredNetworkMode();

        // Display preferred network type based on what modem returns b/18676277
        mPhone.setPreferredNetworkType(settingsNetworkMode, mHandler
                .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));

        /**
         * Enable/disable depending upon if there are any active subscriptions.
         *
         * I've decided to put this enable/disable code at the bottom as the
         * code above works even when there are no active subscriptions, thus
         * putting it afterwards is a smaller change. This can be refined later,
         * but you do need to remember that this all needs to work when subscriptions
         * change dynamically such as when hot swapping sims.
         */
        boolean hasActiveSubscriptions = hasActiveSubscriptions();
        TelephonyManager tm = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        boolean canChange4glte = (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) &&
                ImsManager.isNonTtyOrTtyOnVolteEnabled(getApplicationContext());
        mButtonDataRoam.setEnabled(hasActiveSubscriptions);
        mButtonPreferredNetworkMode.setEnabled(hasActiveSubscriptions);
        // SPRD: [Bug505872] Enable/disable switching network type button when in/out of a call.
        mButtonEnabledNetworks.setEnabled(hasActiveSubscriptions && !isPhoneInCall());
        mButton4glte.setEnabled(hasActiveSubscriptions && canChange4glte);
        mLteDataServicePref.setEnabled(hasActiveSubscriptions);
        Preference ps;
        PreferenceScreen root = getPreferenceScreen();
        ps = findPreference(BUTTON_CELL_BROADCAST_SETTINGS);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_APN_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        ps = findPreference(BUTTON_CDMA_SYSTEM_SELECT_KEY);
        if (ps != null) {
            ps.setEnabled(hasActiveSubscriptions);
        }
        updateOptionsState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DBG) log("onPause:+");

        if (ImsManager.isVolteEnabledByPlatform(this)
                && ImsManager.isVolteProvisionedOnDevice(this)) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        unRegisterPhoneStateListener();

        mSubscriptionManager
            .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        getContentResolver().unregisterContentObserver(mRadioBusyObserver);
        mIsForeground = false;

        if (DBG) log("onPause:-");
    }

    private ContentObserver mRadioBusyObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (!mTelephonyManager.isRadioBusy()) {
                dismissProgressingDialog();
                updateOptionsState();
                updateLtePreferredNetworkMode();
            /* SPRD: modify for bug508651 @{ */
            } else if (mTelephonyManager.isAirplaneModeOn()) {
                Log.d(LOG_TAG, "AirplaneMode is true");
                dismissDialog();
                updateBody();
            }
            /* @} */
        }
    };

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final int phoneSubId = mPhone.getSubId();
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case Phone.NT_MODE_WCDMA_PREF:
                    case Phone.NT_MODE_GSM_ONLY:
                    case Phone.NT_MODE_WCDMA_ONLY:
                    case Phone.NT_MODE_GSM_UMTS:
                    case Phone.NT_MODE_CDMA:
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                    case Phone.NT_MODE_GLOBAL:
                    case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    case Phone.NT_MODE_LTE_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_ONLY:
                    case Phone.NT_MODE_LTE_WCDMA:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode + ") chosen. Ignore.");
                        return true;
                }

                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        buttonNetworkMode );
                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        } else if (preference == mButtonEnabledNetworks) {
            mButtonEnabledNetworks.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            if (DBG) log("buttonNetworkMode: " + buttonNetworkMode);
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                    preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode;
                // if new mode is invalid ignore it
                switch (buttonNetworkMode) {
                    case Phone.NT_MODE_WCDMA_PREF:
                    //SPRD: add network mode for WCDMA only
                    case Phone.NT_MODE_WCDMA_ONLY:
                    case Phone.NT_MODE_GSM_ONLY:
                    case Phone.NT_MODE_LTE_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    case Phone.NT_MODE_CDMA:
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                    case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                        // This is one of the modes we recognize
                        modemNetworkMode = buttonNetworkMode;
                        break;
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode + ") chosen. Ignore.");
                        return true;
                }

                UpdateEnabledNetworksValueAndSummary(buttonNetworkMode);

                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                        buttonNetworkMode );
                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
            //Kalyy add for 2G3G4G
            if(OptConfig.SUN_2G3G4G_SUPPORT){
                Intent intent = new Intent("com.android.system.network");
                getApplicationContext().sendBroadcast(intent);
            }
            //Kalyy add for 2G3G4G
        } else if (preference == mButton4glte) {
            SwitchPreference enhanced4gModePref = (SwitchPreference) preference;
            boolean enhanced4gMode = !enhanced4gModePref.isChecked();
            enhanced4gModePref.setChecked(enhanced4gMode);
            ImsManager.setEnhanced4gLteModeSetting(this, enhanced4gModePref.isChecked());
        } else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (!mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                /* SPRD: modify for bug508651 @{ */
                Builder builder = new AlertDialog.Builder(this).setMessage(
                        getResources().getString(R.string.roaming_warning))
                        .setTitle(R.string.roaming_alert_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this)
                        .setOnDismissListener(this);
                mDataRoamDialog = builder.show();
                /* @} */
            } else {
                mPhone.setDataRoamingEnabled(false);
            }
            return true;
        }
        /* SPRD: National Data Roaming. @{ */
        else if (preference == mButtonPreferredDataRoam) {
            if(DBG) log("onPreferenceChange: preference == mButtonPreferredDataRoam.");
            int buttonDataRoam;
            buttonDataRoam = Integer.valueOf((String) objValue).intValue();
            if (DBG) log("buttonDataRoam: " + buttonDataRoam);

            updatePreferredDataRoamValueAndSummary(buttonDataRoam);
            android.provider.Settings.Global.putInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.DATA_ROAMING + phoneSubId,
                    buttonDataRoam);
        }
        /* @} */
        else if (preference == mButtonLtePreferredNetworkMode) {
            if(DBG)log("onPreferenceChange: preference == mPreferredNetworkModeSelect.");
            if(DBG)log("onPreferenceChange: getValue=" + mButtonLtePreferredNetworkMode.getValue());
            setPreferencedNetworkMode(objValue,mButtonLtePreferredNetworkMode);
        }

        updateBody();
        // always let the preference setting proceed.
        return true;
    }

    private void setPreferencedNetworkMode(Object objValue,ListPreference pref) {
        if (!objValue.equals(pref.getValue())) {
            int setPreferenceNetworkMode = Integer.valueOf((String) objValue).intValue();
            int buttonNetworkMode = getPreferenceNetworkMode();
            if (buttonNetworkMode != setPreferenceNetworkMode) {
                updateLtePreferredNetworkModeSummary(String.valueOf(setPreferenceNetworkMode));
                showProgressingDialog();
                if(DBG)log("[setPreferenceNetworkMode] setPreferenceNetworkMode = " + setPreferenceNetworkMode);
                Message msg = new Message();
                msg.arg1 = setPreferenceNetworkMode;
                msg.obj = pref;
                msg.what = mHandler.MESSAGE_SET_PREFERENCE_NETWORK_TYPE;
                mHandler.sendMessage(msg);
            }
        }
    }

    private void showProgressingDialog() {
        if(DBG)log("show progressing dialog...");
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        mProgressDialogFragment = new PorgressDialogFragment();
        mProgressDialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        mProgressDialogFragment.setCancelable(false);
        mProgressDialogFragment.show(
                transaction, "progress_dialog");
    }

    private void dismissProgressingDialog() {
        if (mProgressDialogFragment != null && mProgressDialogFragment.isVisible() && mIsForeground) {
            if(DBG)log("dismiss progressing dialog...");
            mProgressDialogFragment.dismiss();
        }
    }

    private class MyHandler extends Handler {

        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;
        static final int MESSAGE_GET_PREFERENCE_NETWORK_TYPE = 3;
        static final int MESSAGE_SET_PREFERENCE_NETWORK_TYPE = 4;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
                case MESSAGE_SET_PREFERENCE_NETWORK_TYPE:
                    if (msg.obj != null) {
                        handleSetPreferenceNetworkMode(msg,(ListPreference) msg.obj);
                    }
                    break;
                case MESSAGE_GET_PREFERENCE_NETWORK_TYPE:
                    if (msg.obj != null) {
                        handleGetPreferenceNetworkMode(msg,(ListPreference) msg.obj);
                    }
                    break;
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            final int phoneSubId = mPhone.getSubId();

            if (ar.exception == null) {
                int networkMode;
                if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
                    networkMode =  Integer.valueOf(
                            mButtonPreferredNetworkMode.getValue()).intValue();
                    android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                            networkMode );
                }
                if (getPreferenceScreen().findPreference(BUTTON_ENABLED_NETWORKS_KEY) != null)  {
                    networkMode = Integer.valueOf(
                            mButtonEnabledNetworks.getValue()).intValue();
                    android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                            networkMode );
                }
            } else {
                if (DBG) {
                    log("handleSetPreferredNetworkTypeResponse: exception in setting network mode.");
                }
                updatePreferredNetworkUIFromDb();
            }
        }
    }
    private void updateLtePreferredNetworkModeSummary(String preferredNetworkMode) {
        if (TeleServicePluginsHelper.getInstance(this).updateLtePreferenceSummary(
                preferredNetworkMode) != null) {
            mButtonLtePreferredNetworkMode = TeleServicePluginsHelper.getInstance(this)
                    .updateLtePreferenceSummary(preferredNetworkMode);
        }
    }

    private void updateOptionsState() {
        int primaryCard = mTelephonyManager.getPrimaryCard();
        if(DBG) log("primaryCard = " + primaryCard);

        boolean isPrimaryCardStandby = mTelephonyManager.isSimStandby(primaryCard);
        boolean isPrimaryCardUsim = TeleServicePluginsHelper.getInstance(getApplicationContext()).
                needSetByPrimaryCardUsim(primaryCard);
        if(DBG) log("isPrimaryCardStandby = " + isPrimaryCardStandby
                    + " isPrimaryCardUsim = " + isPrimaryCardUsim);

        // SPRD: [Bug542092] Enable/disable switching network type button when in/out of a call.
        mButtonLtePreferredNetworkMode.setEnabled(SubscriptionManager.isValidPhoneId(primaryCard)
                    && isPrimaryCardStandby && isPrimaryCardUsim && !isPhoneInCall());
        boolean isLteEnabled = PREFERRED_NETWORK_MODE_4G_3G_2G
                .equals(mButtonLtePreferredNetworkMode.getValue());
        if (DBG)
            log("isLteEnabled = " + isLteEnabled);

    }

    public void updateLtePreferredNetworkMode() {
        if (!TelephonyManager.isDeviceSupportLte()) {
            return;
        }

        final int primaryCard = mTelephonyManager.getPrimaryCard();
        // SPRD: modify for bug523063
        boolean isPrimaryCardUsim = TeleServicePluginsHelper.getInstance(getApplicationContext()).
                needSetByPrimaryCardUsim(primaryCard);
        if (DBG) log("updatePreferredNetworkMode : isPrimaryCardUsim = " + isPrimaryCardUsim);
        if (SubscriptionManager.isValidPhoneId(primaryCard) && isPrimaryCardUsim) {
            if (mButtonLtePreferredNetworkMode!=null) {
                Message msg = new Message();
                msg.obj = mButtonLtePreferredNetworkMode;
                msg.what = mHandler.MESSAGE_GET_PREFERENCE_NETWORK_TYPE;
                mHandler.sendMessage(msg);
            }
        } else {
            updateLtePreferredNetworkModeSummary(PREFERRED_NETWORK_MODE_3G_2G);
        }
    }
    private void updatePreferredNetworkUIFromDb() {
        final int phoneSubId = mPhone.getSubId();

        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + phoneSubId,
                preferredNetworkMode);

        if (DBG) {
            log("updatePreferredNetworkUIFromDb: settingsNetworkMode = " +
                    settingsNetworkMode);
        }

        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        // changes the mButtonPreferredNetworkMode accordingly to settingsNetworkMode
        mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch(NetworkMode) {
            case Phone.NT_MODE_WCDMA_PREF:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_perf_summary);
                break;
            case Phone.NT_MODE_GSM_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_only_summary);
                break;
            case Phone.NT_MODE_WCDMA_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_only_summary);
                break;
            case Phone.NT_MODE_GSM_UMTS:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_CDMA:
                switch (mPhone.getLteOnCdmaMode()) {
                    case PhoneConstants.LTE_ON_CDMA_TRUE:
                        mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_summary);
                    break;
                    case PhoneConstants.LTE_ON_CDMA_FALSE:
                    default:
                        mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_evdo_summary);
                        break;
                }
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_only_summary);
                break;
            case Phone.NT_MODE_EVDO_NO_CDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_evdo_only_summary);
                break;
            case Phone.NT_MODE_LTE_ONLY:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_summary);
                break;
            case Phone.NT_MODE_LTE_GSM_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_cdma_evdo_summary);
                break;
            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                        mIsGlobalCdma ||
                        isWorldMode()) {
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_global_summary);
                } else {
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_summary);
                }
                break;
            case Phone.NT_MODE_GLOBAL:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                break;
            case Phone.NT_MODE_LTE_WCDMA:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_lte_wcdma_summary);
                break;
            default:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_global_summary);
        }
    }

    private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
        //Kalyy add for 2G3G4G
        if(OptConfig.SUN_2G3G4G_SUPPORT){
            int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                RILConstants.PREFERRED_NETWORK_MODE);
            if(settingsNetworkMode == 1){
                mButtonEnabledNetworks.setValue(Integer.toString(Phone.NT_MODE_GSM_ONLY));
                mButtonEnabledNetworks.setSummary(R.string.network_2G);
            } else if(settingsNetworkMode == 9){
                mButtonEnabledNetworks.setValue(Integer.toString(Phone.NT_MODE_LTE_GSM_WCDMA));
                mButtonEnabledNetworks.setSummary(R.string.network_4G);
            } else {
                mButtonEnabledNetworks.setValue(Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                mButtonEnabledNetworks.setSummary(R.string.network_3G);
            }
            return;
        }
        //Kalyy add for 2G3G4G
        switch (NetworkMode) {
            /* SPRD: add network mode for WCDMA only *@{ */
            case Phone.NT_MODE_WCDMA_ONLY:
                if (!mIsGlobalCdma && mIsSupportWcdmaOnly) {
                    mButtonEnabledNetworks.setValue(Integer
                            .toString(Phone.NT_MODE_WCDMA_ONLY));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                } else if (!mIsGlobalCdma && !mIsSupportWcdmaOnly) {
                    mButtonEnabledNetworks.setValue(Integer
                            .toString(Phone.NT_MODE_WCDMA_PREF));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G);
                } else {
                    mButtonEnabledNetworks.setValue(Integer
                            .toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
                /*@}*/
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_WCDMA_PREF:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_WCDMA_PREF));
                    mButtonEnabledNetworks.setSummary(R.string.network_3G_pref);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                /** wangxing add 20160630 Preferred Network type should showing Auto when Auto mode selected @{ */
                if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
                    mButtonEnabledNetworks.setSummary(mButtonEnabledNetworks.getEntry());
                }
                /** @} */
                
                break;
            case Phone.NT_MODE_GSM_ONLY:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_GSM_ONLY));
                    mButtonEnabledNetworks.setSummary(R.string.network_2G);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_LTE_GSM_WCDMA:
                if (isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(
                            R.string.preferred_network_mode_lte_gsm_umts_summary);
                    controlCdmaOptions(false);
                    controlGsmOptions(true);
                    break;
                }
            case Phone.NT_MODE_LTE_ONLY:
            case Phone.NT_MODE_LTE_WCDMA:
                if (!mIsGlobalCdma) {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                            ? R.string.network_4G : R.string.network_lte);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                }
                break;
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                if (isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(
                            R.string.preferred_network_mode_lte_cdma_summary);
                    controlCdmaOptions(true);
                    controlGsmOptions(false);
                } else {
                    mButtonEnabledNetworks.setValue(
                            Integer.toString(Phone.NT_MODE_LTE_CDMA_AND_EVDO));
                    mButtonEnabledNetworks.setSummary(R.string.network_lte);
                }
                break;
            case Phone.NT_MODE_CDMA:
            case Phone.NT_MODE_EVDO_NO_CDMA:
            case Phone.NT_MODE_GLOBAL:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_CDMA));
                mButtonEnabledNetworks.setSummary(R.string.network_3G);
                break;
            case Phone.NT_MODE_CDMA_NO_EVDO:
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_CDMA_NO_EVDO));
                mButtonEnabledNetworks.setSummary(R.string.network_1x);
                break;
            case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                if (isWorldMode()) {
                    controlCdmaOptions(true);
                    controlGsmOptions(false);
                }
                mButtonEnabledNetworks.setValue(
                        Integer.toString(Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
                if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                        mIsGlobalCdma ||
                        isWorldMode()) {
                    mButtonEnabledNetworks.setSummary(R.string.network_global);
                } else {
                    mButtonEnabledNetworks.setSummary((mShow4GForLTE == true)
                            ? R.string.network_4G : R.string.network_lte);
                }
                break;
            default:
                /* SPRD: modify for bug511452 @{ */
                String errMsg = this.getString(R.string.invalid_network_mode);
                loge(errMsg);
                mButtonEnabledNetworks.setSummary(errMsg);
                /* @} */
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the CDMA Options
                mCdmaOptions.showDialog(mClickedPreference);
            } else {
                // do nothing
            }
            break;

        default:
            break;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isWorldMode() {
        boolean worldModeOn = false;
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String configString = getResources().getString(R.string.config_world_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            // Check if we have World mode configuration set to True only or config is set to True
            // and SIM GID value is also set and matches to the current SIM GID.
            if (configArray != null &&
                   ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) ||
                       (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) &&
                           tm != null && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                               worldModeOn = true;
            }
        }

        if (DBG) {
            log("isWorldMode=" + worldModeOn);
        }

        return worldModeOn;
    }

    private void controlGsmOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }

        if (mGsmUmtsOptions == null) {
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, mPhone.getSubId());
        }
        PreferenceScreen apnExpand =
                (PreferenceScreen) prefSet.findPreference(BUTTON_APN_EXPAND_KEY);
        PreferenceScreen operatorSelectionExpand =
                (PreferenceScreen) prefSet.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        PreferenceScreen carrierSettings =
                (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SETTINGS_KEY);
        if (apnExpand != null) {
            apnExpand.setEnabled(isWorldMode() || enable);
        }
        if (operatorSelectionExpand != null) {
            operatorSelectionExpand.setEnabled(enable);
        }
        if (carrierSettings != null) {
            prefSet.removePreference(carrierSettings);
        }
    }

    private void controlCdmaOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet == null) {
            return;
        }
        if (enable && mCdmaOptions == null) {
            mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
        }
        CdmaSystemSelectListPreference systemSelect =
                (CdmaSystemSelectListPreference)prefSet.findPreference
                        (BUTTON_CDMA_SYSTEM_SELECT_KEY);
        if (systemSelect != null) {
            systemSelect.setEnabled(enable);
        }
    }

    private void handleSetPreferenceNetworkMode(Message msg,ListPreference pref) {
        int setPreferenceNetworkMode = msg.arg1;
        if (DBG) Log.d(LOG_TAG, "handleSetPreferenceNetworkMode setPreferenceNetworkMode ="
                + setPreferenceNetworkMode);
        int primaryCard = mTelephonyManager.getPrimaryCard();
        if (setPreferenceNetworkMode != -1) {
            setPreferenceNetworkMode(primaryCard,
                    setPreferenceNetworkMode);
        } else {
            Message message = new Message();
            message.obj = pref;
            message.what = mHandler.MESSAGE_GET_PREFERENCE_NETWORK_TYPE;
            mHandler.sendMessage(message);
        }
    }

    private void handleGetPreferenceNetworkMode(Message msg,ListPreference pref) {
        int preferenceNetworkMode = getPreferenceNetworkMode();
        if (DBG) Log.d(LOG_TAG, "handleGetPreferenceNetworkMode preferenceNetworkMode = "
                + preferenceNetworkMode);
        if (preferenceNetworkMode != -1) {
            updateLtePreferredNetworkModeSummary(String
                    .valueOf(preferenceNetworkMode));
            pref.setValue(String.valueOf(preferenceNetworkMode));
        } else {
            if (DBG) Log.d(LOG_TAG, "handleGetPreferenceNetworkMode: else: reset to default");
                resetPreferenceNetworkModeToDefault();
        }
    }

    private void resetPreferenceNetworkModeToDefault() {
        if (DBG) Log.d(LOG_TAG, "resetPreferenceNetworkModeToDefault");
        int primaryCard = mTelephonyManager.getPrimaryCard();
        boolean isPrimaryCardUsim = isUsimCard(primaryCard);
        boolean isPrimaryCardReady = mTelephonyManager.isSimStandby(primaryCard) &&
                mTelephonyManager.getSimState(primaryCard) == TelephonyManager.SIM_STATE_READY;
        boolean isLteAvailable = isPrimaryCardUsim && isPrimaryCardReady;
        if (primaryCard != -1 && isLteAvailable) {
            updateLtePreferredNetworkModeSummary(String.valueOf(PREFERRED_NETWORK_MODE_4G_3G_2G));
        } else if(primaryCard != -1 && !isLteAvailable) {
            updateLtePreferredNetworkModeSummary(String.valueOf(PREFERRED_NETWORK_MODE_3G_2G));
        } else {
            updateLtePreferredNetworkModeSummary(String.valueOf(PREFERRED_NETWORK_MODE_2G_ONLY));
        }
    }

    protected int getPreferenceNetworkMode() {
        return mRadioTaskManger.getPreferredNetworkModeForPhone(mTelephonyManager.getPrimaryCard());
    }

    private boolean isUsimCard(int phoneId) {
        UiccController uc = UiccController.getInstance();
        if (uc != null) {
            UiccCardApplication currentApp = uc.getUiccCardApplication(phoneId,
                    UiccController.APP_FAM_3GPP);
            if (currentApp != null) {
                return currentApp.getType() == AppType.APPTYPE_USIM;
            }
        }

        return false;
    }

    public void setPreferenceNetworkMode(int phoneId, int mode) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            mRadioTaskManger.setPreferredNetworkModeForPhone(phoneId, mode);
        }
    }

    public static class PorgressDialogFragment extends DialogFragment {
        View v;
        TextView mMessageView;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            //return super.onCreateView(inflater, container, savedInstanceState);
            v = inflater.inflate(R.layout.progress_dialog_fragment_ex, container, false);
            //ProgressBar mProgress = (ProgressBar) v.findViewById(R.id.progress);
            mMessageView = (TextView) v.findViewById(R.id.message);
            mMessageView.setText(getResources().getString(R.string.lte_service_switch));
            //setView(view);
            return v;
        }
    }

    /* SPRD: Bug 474686 Add for Uplmn @{ */
    private Preference mUplmnPref;
    private static final String BUTTON_UPLMN_KEY = "uplmn_setting_key";
    /*@}*/

    /* SPRD: [Bug505872] Enable/disable switching network type button when in/out of a call. @{ */
    private boolean mListening;
    private Map<Integer, PhoneStateListener> mPhoneStateListeners = new HashMap<Integer, PhoneStateListener>();

    private void updatePhoneStateListener() {
        boolean isNeedUpdate = false;
        // Create a new ArrayList to avoid ConcurrentModificationException during possible asynchronous updating.
        List<SubscriptionInfo> activeSubInfos = new ArrayList<SubscriptionInfo>(mActiveSubInfos);
        // Do confirm whether PhoneStateListeners need to be updated.
        if (activeSubInfos.size() != mPhoneStateListeners.size()) {
            isNeedUpdate = true;
        } else {
            for (SubscriptionInfo info : activeSubInfos) {
                if (!mPhoneStateListeners.containsKey(info.getSubscriptionId())) {
                    isNeedUpdate = true;
                }
            }
        }

        // Update PhoneStateListeners if need especially supporting for SIM hot plug. Unregister
        // outdated ones and register new created ones.
        if (isNeedUpdate) {
            HashMap<Integer, PhoneStateListener> cachedListeners = new HashMap<Integer, PhoneStateListener>(
                    mPhoneStateListeners);
            mPhoneStateListeners.clear();
            final int num = activeSubInfos.size();
            for (int i = 0; i < num; i++) {
                int subId = activeSubInfos.get(i).getSubscriptionId();
                if (cachedListeners.containsKey(subId)) {
                    mPhoneStateListeners.put(subId, cachedListeners.remove(subId));
                } else {
                    PhoneStateListener listener = new PhoneStateListener(subId) {
                        @Override
                        public void onCallStateChanged(int state, String incomingNumber) {
                            if (mButtonEnabledNetworks != null) {
                                mButtonEnabledNetworks.setEnabled(
                                        hasActiveSubscriptions() && !isPhoneInCall());
                            }

                            // SPRD: [Bug542092] Enable/disable switching network type button when in/out of a call.
                            updateOptionsState();
                        }
                    };
                    mPhoneStateListeners.put(subId, listener);

                    if (mListening) {
                        mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
                    }
                }
            }

            if (mListening) {
                for (Integer key : cachedListeners.keySet()) {
                    mTelephonyManager.listen(cachedListeners.get(key), PhoneStateListener.LISTEN_NONE);
                }
            }
        }
    }

    private void registerPhoneStateListener() {
        for (PhoneStateListener listener : mPhoneStateListeners.values()) {
            mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        mListening = true;
    }

    private void unRegisterPhoneStateListener() {
        for (PhoneStateListener listener : mPhoneStateListeners.values()) {
            mTelephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
        }
        mListening = false;
    }

    private boolean isPhoneInCall() {
        // Call SubscriptionController directly To avoid redundant RPC because switching network
        // type works in phone process.
        int[] activeSubIdList = SubscriptionController.getInstance().getActiveSubIdList();

        for (int subId : activeSubIdList) {
            if (mTelephonyManager.getCallState(subId) != TelephonyManager.CALL_STATE_IDLE) {
                return true;
            }
        }
        return false;
    }

    /* SPRD: National Data Roaming. {@ */
    void updatePreferredDataRoamSummary(int roamType) {
        if (DBG) log("updatePreferredDataRoamSummary, roamType: " + roamType
                + ": " + DataRoamType.fromInt(roamType));
        switch (DataRoamType.fromInt(roamType)) {
            case DISABLE:
                mButtonPreferredDataRoam.setSummary(R.string.preferred_data_roaming_disable);
                break;
            case ALL:
                mButtonPreferredDataRoam.setSummary(R.string.preferred_data_roaming_all_networks);
                break;
            default:    // NATIONAL
                mButtonPreferredDataRoam.setSummary(R.string.preferred_data_roaming_national);
        }
    }

    void updatePreferredDataRoamValueAndSummary(int roamType) {
        mButtonPreferredDataRoam.setValue(String.valueOf(roamType));
        updatePreferredDataRoamSummary(roamType);
    }
    /* @} */
}
