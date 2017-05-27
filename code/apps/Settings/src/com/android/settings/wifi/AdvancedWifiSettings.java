/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.wifi;

import java.util.Calendar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppManager;
import android.net.NetworkScorerAppManager.NetworkScorerAppData;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import com.android.settings.PasspointPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.security.Credentials;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;
import android.widget.TimePicker;
import android.text.format.DateFormat;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.AppListSwitchPreference;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.Collection;

public class AdvancedWifiSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, TimePickerDialog.OnTimeSetListener {

    private static final String TAG = "AdvancedWifiSettings";
    private static final String KEY_MAC_ADDRESS = "mac_address";
    private static final String KEY_CURRENT_IP_ADDRESS = "current_ip_address";
    private static final String KEY_FREQUENCY_BAND = "frequency_band";
    private static final String KEY_NOTIFY_OPEN_NETWORKS = "notify_open_networks";
    private static final String KEY_SLEEP_POLICY = "sleep_policy";
    private static final String KEY_INSTALL_CREDENTIALS = "install_credentials";
    private static final String KEY_WIFI_ASSISTANT = "wifi_assistant";
    private static final String KEY_WIFI_DIRECT = "wifi_direct";
    private static final String KEY_WPS_PUSH = "wps_push_button";
    private static final String KEY_WPS_PIN = "wps_pin_entry";
    //SPRD Passpoint R1 Feature -->
    private static final String KEY_WIFI_PASSPOINT = "passpoint";
    private static final boolean KEY_WIFI_PASSPOINT_OPEN = SystemProperties.getBoolean("persist.sys.wifi.passpoint",false);
    private static final String WIFI_PASSPOINT_SWITCH = "wifi_passpoint_switch";
    //<--
    /* SPRD: Add auto roam function @{ */
    private static final String KEK_WIFI_AUTO_ROAM = "wifi_auto_roam";
    private static final boolean SUPPORT_AUTO_ROAM = SystemProperties.get("ro.support.auto.roam", "enabled").equals("enabled");
    /* @} */

    /* SPRD: add for cmcc wifi feature @{ */
    private SprdWifiSettingsAddonStub mAddonStub;
    private boolean supportCMCC = false;

    private static final String KEY_WIFI_NETMASK = "wifi_netmask";
    private static final String KEY_WIFI_GATEWAY = "wifi_gateway";

    private static final String KEY_MOBILE_TO_WLAN_PREFERENCE_CATEGORY = "mobile_to_wlan_preference_category";
    private static final String KEY_MOBILE_TO_WLAN_POLICY = "mobile_to_wlan_policy";
    private String[] mMoblieToWlanPolicys;
    private static final String KEY_DIALOG_CONNECT_TO_CMCC = "show_dialog_connect_to_cmcc";
    private static final int DEFAULT_CHECKED_VALUE = 1;
    private static final String KEY_RESET_WIFI_POLICY_DIALOG_FLAG = "reset_wifi_policy_dialog_flag";

    private static final String KEY_WIFI_ALARM_CATEGORY = "wifi_alarm_category";
    private static final String KEY_WIFI_CONNECT_ALARM_SWITCH = "wifi_connect_alarm_switch";
    private static final String KEY_WIFI_CONNECT_ALARM_TIME = "wifi_connect_alarm_time";
    private static final String KEY_WIFI_DISCONNECT_ALARM_SWITCH = "wifi_disconnect_alarm_switch";
    private static final String KEY_WIFI_DISCONNECT_ALARM_TIME = "wifi_disconnect_alarm_time";
    private static final int DIALOG_WIFI_CONNECT_TIMEPICKER = 0;
    private static final int DIALOG_WIFI_DISCONNECT_TIMEPICKER = 1;
    private SwitchPreference mConnectSwitch;
    private Preference mConnectTimePref;
    private SwitchPreference mDisconnectSwitch;
    private Preference mDisconnectTimePref;

    AlarmManager mAlarmManager;
    private int whichTimepicker = -1;
    /* @} */

    private WifiManager mWifiManager;
    private NetworkScoreManager mNetworkScoreManager;
    private AppListSwitchPreference mWifiAssistantPreference;

    private IntentFilter mFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION) ||
                action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                refreshWifiInfo();
            }
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.WIFI_ADVANCED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAddonStub = SprdWifiSettingsAddonStub.getInstance(getActivity());
        supportCMCC = mAddonStub.isSupportCmcc();
        if (supportCMCC) {
            addPreferencesFromResource(R.xml.cmcc_wifi_advanced_settings);
        } else {
            addPreferencesFromResource(R.xml.wifi_advanced_settings);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mNetworkScoreManager =
                (NetworkScoreManager) getSystemService(Context.NETWORK_SCORE_SERVICE);
        /* SPRD: wifi plugin @{ */
        mAlarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        mAddonStub.initWifiConnectionPolicy();
        mMoblieToWlanPolicys = getResources().getStringArray(R.array.mobile_to_wlan);
        /* @} */
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
        if (supportCMCC) initCellularWLANPreference();
        getActivity().registerReceiver(mReceiver, mFilter);
        refreshWifiInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    private void initPreferences() {
        if (SUPPORT_AUTO_ROAM == false) {
            SwitchPreference autoRoamSwitch = (SwitchPreference) findPreference(KEK_WIFI_AUTO_ROAM);
            if(autoRoamSwitch != null) getPreferenceScreen().removePreference(autoRoamSwitch);
        }

        SwitchPreference notifyOpenNetworks =
            (SwitchPreference) findPreference(KEY_NOTIFY_OPEN_NETWORKS);
        notifyOpenNetworks.setChecked(Settings.Global.getInt(getContentResolver(),
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1);
        notifyOpenNetworks.setEnabled(mWifiManager.isWifiEnabled());

        Intent intent = new Intent(Credentials.INSTALL_AS_USER_ACTION);
        intent.setClassName("com.android.certinstaller",
                "com.android.certinstaller.CertInstallerMain");
        intent.putExtra(Credentials.EXTRA_INSTALL_AS_UID, android.os.Process.WIFI_UID);
        Preference pref = findPreference(KEY_INSTALL_CREDENTIALS);
        pref.setIntent(intent);

        final Context context = getActivity();
        mWifiAssistantPreference = (AppListSwitchPreference) findPreference(KEY_WIFI_ASSISTANT);
        Collection<NetworkScorerAppData> scorers =
                NetworkScorerAppManager.getAllValidScorers(context);
        if (UserHandle.myUserId() == UserHandle.USER_OWNER && !scorers.isEmpty()) {
            mWifiAssistantPreference.setOnPreferenceChangeListener(this);
            initWifiAssistantPreference(scorers);
        } else if (mWifiAssistantPreference != null) {
            getPreferenceScreen().removePreference(mWifiAssistantPreference);
        }

        Intent wifiDirectIntent = new Intent(context,
                com.android.settings.Settings.WifiP2pSettingsActivity.class);
        Preference wifiDirectPref = findPreference(KEY_WIFI_DIRECT);
        wifiDirectPref.setIntent(wifiDirectIntent);

        // WpsDialog: Create the dialog like WifiSettings does.
        Preference wpsPushPref = findPreference(KEY_WPS_PUSH);
        wpsPushPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    WpsFragment wpsFragment = new WpsFragment(WpsInfo.PBC);
                    wpsFragment.show(getFragmentManager(), KEY_WPS_PUSH);
                    return true;
                }
        });

        // WpsDialog: Create the dialog like WifiSettings does.
        Preference wpsPinPref = findPreference(KEY_WPS_PIN);
        wpsPinPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
                public boolean onPreferenceClick(Preference arg0) {
                    WpsFragment wpsFragment = new WpsFragment(WpsInfo.DISPLAY);
                    wpsFragment.show(getFragmentManager(), KEY_WPS_PIN);
                    return true;
                }
        });

        ListPreference frequencyPref = (ListPreference) findPreference(KEY_FREQUENCY_BAND);

        if (mWifiManager.isDualBandSupported()) {
            frequencyPref.setOnPreferenceChangeListener(this);
            int value = mWifiManager.getFrequencyBand();
            if (value != -1) {
                frequencyPref.setValue(String.valueOf(value));
                updateFrequencyBandSummary(frequencyPref, value);
            } else {
                Log.e(TAG, "Failed to fetch frequency band");
            }
        } else {
            if (frequencyPref != null) {
                // null if it has already been removed before resume
                getPreferenceScreen().removePreference(frequencyPref);
            }
        }

        ListPreference sleepPolicyPref = (ListPreference) findPreference(KEY_SLEEP_POLICY);
        if (sleepPolicyPref != null) {
            if (Utils.isWifiOnly(context)) {
                sleepPolicyPref.setEntries(R.array.wifi_sleep_policy_entries_wifi_only);
            }
            sleepPolicyPref.setOnPreferenceChangeListener(this);
            int value = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.WIFI_SLEEP_POLICY,
                    Settings.Global.WIFI_SLEEP_POLICY_NEVER);
            String stringValue = String.valueOf(value);
            sleepPolicyPref.setValue(stringValue);
            updateSleepPolicySummary(sleepPolicyPref, stringValue);
        }
        //SPRD Passpoint R1 Feature -->
        PasspointPreference passpointPref = (PasspointPreference) findPreference(KEY_WIFI_PASSPOINT);
        if (passpointPref != null) {
            if (KEY_WIFI_PASSPOINT_OPEN) {
                if (mWifiManager.isWifiEnabled()) {
                    passpointPref.setOnPreferenceChangeListener(this);
                    passpointPref.setChecked(Settings.Global.getInt(getContentResolver(),
                            WIFI_PASSPOINT_SWITCH, 0) == 1);
                    passpointPref.setSelectable(Settings.Global.getInt(getContentResolver(),
                            WIFI_PASSPOINT_SWITCH, 0) == 1);
                    passpointPref.setSummary((Settings.Global.getInt(getContentResolver(),
                            WIFI_PASSPOINT_SWITCH, 0) == 1)?R.string.passpoint_on:R.string.passpoint_off);
                } else {
                    passpointPref.setEnabled(false);
                    passpointPref.setSummary(R.string.open_wifi_first);
                }
            } else {
                getPreferenceScreen().removePreference(passpointPref);
            }
        }
        //<--
    }

    private void initWifiAssistantPreference(Collection<NetworkScorerAppData> scorers) {
        int count = scorers.size();
        String[] packageNames = new String[count];
        int i = 0;
        for (NetworkScorerAppData scorer : scorers) {
            packageNames[i] = scorer.mPackageName;
            i++;
        }
        mWifiAssistantPreference.setPackageNames(packageNames,
                mNetworkScoreManager.getActiveScorerPackage());
    }

    private void updateSleepPolicySummary(Preference sleepPolicyPref, String value) {
        if (value != null) {
            String[] values = getResources().getStringArray(R.array.wifi_sleep_policy_values);
            final int summaryArrayResId = Utils.isWifiOnly(getActivity()) ?
                    R.array.wifi_sleep_policy_entries_wifi_only : R.array.wifi_sleep_policy_entries;
            String[] summaries = getResources().getStringArray(summaryArrayResId);
            for (int i = 0; i < values.length; i++) {
                if (value.equals(values[i])) {
                    if (i < summaries.length) {
                        sleepPolicyPref.setSummary(summaries[i]);
                        return;
                    }
                }
            }
        }

        sleepPolicyPref.setSummary("");
        Log.e(TAG, "Invalid sleep policy value: " + value);
    }

    private void updateFrequencyBandSummary(Preference frequencyBandPref, int index) {
        String[] summaries = getResources().getStringArray(R.array.wifi_frequency_band_entries);
        frequencyBandPref.setSummary(summaries[index]);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        String key = preference.getKey();

        if (KEY_NOTIFY_OPEN_NETWORKS.equals(key)) {
            Global.putInt(getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                    ((SwitchPreference) preference).isChecked() ? 1 : 0);
        /* SPRD: wifi plugin @{ */
        } else if (KEY_DIALOG_CONNECT_TO_CMCC.equals(key)) {
            mAddonStub.setConnectCmccDialogFlag(((SwitchPreference) preference).isChecked());
        } else if (KEY_RESET_WIFI_POLICY_DIALOG_FLAG.equals(key)) {
            mAddonStub.resetWifiPolicyDialogFlag();
            Toast.makeText(getActivity(), R.string.reset_wifi_policy_dialog_flag_toast_message, Toast.LENGTH_SHORT).show();
        } else if (KEY_WIFI_CONNECT_ALARM_SWITCH.equals(key)) {
            boolean isChecked = ((SwitchPreference) preference).isChecked();
            Global.putInt(getContentResolver(), WifiManager.WIFI_CONNECT_ALARM_FLAG, isChecked ? 1 : 0);
            if (isChecked) {
                setConnectWifiAlarm();
            } else {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(
                        WifiManager.ALARM_FOR_CONNECT_WIFI_ACTION), 0);
                mAlarmManager.cancel(pendingIntent);
            }
        } else if (KEY_WIFI_CONNECT_ALARM_TIME.equals(key)) {
            removeDialog(DIALOG_WIFI_CONNECT_TIMEPICKER);
            showDialog(DIALOG_WIFI_CONNECT_TIMEPICKER);
        } else if (KEY_WIFI_DISCONNECT_ALARM_SWITCH.equals(key)) {
            boolean isChecked = ((SwitchPreference) preference).isChecked();
            Global.putInt(getContentResolver(), WifiManager.WIFI_DISCONNECT_ALARM_FLAG, isChecked ? 1 : 0);
            if (isChecked) {
                setDisonnectWifiAlarm();
            } else {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(
                        WifiManager.ALARM_FOR_DISCONNECT_WIFI_ACTION), 0);
                mAlarmManager.cancel(pendingIntent);
            }
        } else if (KEY_WIFI_DISCONNECT_ALARM_TIME.equals(key)) {
            removeDialog(DIALOG_WIFI_DISCONNECT_TIMEPICKER);
            showDialog(DIALOG_WIFI_DISCONNECT_TIMEPICKER);
        /* @} */
        } else if (KEK_WIFI_AUTO_ROAM.equals(key)) {
            boolean isChecked = ((SwitchPreference) preference).isChecked();
            if (isChecked) {
                Settings.Global.putInt(getContentResolver(), WifiManager.WIFI_AUTO_ROAM_SWITCH, 1);
            } else {
                Settings.Global.putInt(getContentResolver(), WifiManager.WIFI_AUTO_ROAM_SWITCH, 0);
            }
        }
        //SPRD Passpoint R1 Feature -->
        else if (KEY_WIFI_PASSPOINT.equals(key)) {
           if (getActivity() instanceof SettingsActivity) {
               ((SettingsActivity) getActivity()).startPreferencePanel(
                       PasspointSettings.class.getCanonicalName(), null,
                       R.string.passpoint_titlebar, null, this, 0);
           } else {
               startFragment(this, PasspointSettings.class.getCanonicalName(),
                       R.string.passpoint_titlebar, -1 /* Do not request a results */,
                       null);
           }
           //<--
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getActivity();
        String key = preference.getKey();

        if (KEY_FREQUENCY_BAND.equals(key)) {
            try {
                int value = Integer.parseInt((String) newValue);
                mWifiManager.setFrequencyBand(value, true);
                updateFrequencyBandSummary(preference, value);
            } catch (NumberFormatException e) {
                Toast.makeText(context, R.string.wifi_setting_frequency_band_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (KEY_WIFI_ASSISTANT.equals(key)) {
            NetworkScorerAppData wifiAssistant =
                    NetworkScorerAppManager.getScorer(context, (String) newValue);
            if (wifiAssistant == null) {
                mNetworkScoreManager.setActiveScorer(null);
                return true;
            }

            Intent intent = new Intent();
            if (wifiAssistant.mConfigurationActivityClassName != null) {
                // App has a custom configuration activity; launch that.
                // This custom activity will be responsible for launching the system
                // dialog.
                intent.setClassName(wifiAssistant.mPackageName,
                        wifiAssistant.mConfigurationActivityClassName);
            } else {
                // Fall back on the system dialog.
                intent.setAction(NetworkScoreManager.ACTION_CHANGE_ACTIVE);
                intent.putExtra(NetworkScoreManager.EXTRA_PACKAGE_NAME,
                        wifiAssistant.mPackageName);
            }

            startActivity(intent);
            // Don't update the preference widget state until the child activity returns.
            // It will be updated in onResume after the activity finishes.
            return false;
        }

        if (KEY_SLEEP_POLICY.equals(key)) {
            try {
                String stringValue = (String) newValue;
                Settings.Global.putInt(getContentResolver(), Settings.Global.WIFI_SLEEP_POLICY,
                        Integer.parseInt(stringValue));
                updateSleepPolicySummary(preference, stringValue);
            } catch (NumberFormatException e) {
                Toast.makeText(context, R.string.wifi_setting_sleep_policy_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        //SPRD Passpoint R1 Feature -->
        if (KEY_WIFI_PASSPOINT.equals(key)) {
           try {
               Settings.Global.putInt(getContentResolver(), WIFI_PASSPOINT_SWITCH,
                       (boolean) newValue?1:0);
               preference.setSelectable((boolean)newValue);
               mWifiManager.setPasspointEnable((boolean)newValue);
               if ((boolean)newValue) {
                   preference.setSummary(R.string.passpoint_on);
               } else {
                   preference.setSummary(R.string.passpoint_off);
               }
           } catch (NumberFormatException e) {
               Toast.makeText(context, "please open switch!",
                       Toast.LENGTH_SHORT).show();
               return false;
           }
        }
        //<--

        /* SPRD: wifi plugin @{ */
        if (KEY_MOBILE_TO_WLAN_POLICY.equals(key)) {
            mAddonStub.setMobileToWlanPolicy((String) newValue);

            int value = mAddonStub.getMobileToWlanPolicy();
            preference.setSummary(mMoblieToWlanPolicys[value]);
        }
        /* @} */

        return true;
    }

    private void refreshWifiInfo() {
        final Context context = getActivity();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_MAC_ADDRESS);
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : context.getString(R.string.status_unavailable));
        wifiMacAddressPref.setSelectable(false);

        Preference wifiIpAddressPref = findPreference(KEY_CURRENT_IP_ADDRESS);
        String ipAddress = Utils.getWifiIpAddresses(context);
        wifiIpAddressPref.setSummary(ipAddress == null ?
                context.getString(R.string.status_unavailable) : ipAddress);
        wifiIpAddressPref.setSelectable(false);

        //add mask & gateway
        if (supportCMCC) {
            refreshNetmaskAndGateway();
        }
    }

    /* Wrapper class for the WPS dialog to properly handle life cycle events like rotation. */
    public static class WpsFragment extends DialogFragment {
        private static int mWpsSetup;

        // Public default constructor is required for rotation.
        public WpsFragment() {
            super();
        }

        public WpsFragment(int wpsSetup) {
            super();
            mWpsSetup = wpsSetup;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new WpsDialog(getActivity(), mWpsSetup);
        }
    }

    /* SPRD: wifi plugin @{ */
    private void initCellularWLANPreference() {
        boolean wifiEnabled = mWifiManager.isWifiEnabled();
         ListPreference mMoblieToWlanPolicy = (ListPreference) findPreference(KEY_MOBILE_TO_WLAN_POLICY);
        mMoblieToWlanPolicy.setEnabled(wifiEnabled);
        mMoblieToWlanPolicy.setOnPreferenceChangeListener(this);
        int value = mAddonStub.getMobileToWlanPolicy();
        mMoblieToWlanPolicy.setValue(String.valueOf(value));
        mMoblieToWlanPolicy.setSummary(mMoblieToWlanPolicys[value]);

        SwitchPreference connectToCmccSwitch = (SwitchPreference) findPreference(KEY_DIALOG_CONNECT_TO_CMCC);
        setShowDialogSwitchStatus(connectToCmccSwitch, wifiEnabled);

        mConnectSwitch = (SwitchPreference) findPreference(KEY_WIFI_CONNECT_ALARM_SWITCH);
        mConnectTimePref = (Preference) findPreference(KEY_WIFI_CONNECT_ALARM_TIME);
        mDisconnectSwitch = (SwitchPreference) findPreference(KEY_WIFI_DISCONNECT_ALARM_SWITCH);
        mDisconnectTimePref = (Preference) findPreference(KEY_WIFI_DISCONNECT_ALARM_TIME);

        int hourOfDay = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_CONNECT_ALARM_HOUR, 0);
        int minute = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_CONNECT_ALARM_MINUTE, 0);
        Calendar calendar = getCalendar(hourOfDay, minute);
        updateTimeDisplay(mConnectTimePref, calendar);
        hourOfDay = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_DISCONNECT_ALARM_HOUR, 0);
        minute = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_DISCONNECT_ALARM_MINUTE, 0);
        calendar = getCalendar(hourOfDay, minute);
        updateTimeDisplay(mDisconnectTimePref, calendar);
    }

    private void setShowDialogSwitchStatus(SwitchPreference item, boolean wifiEnabled) {
        item.setChecked(mAddonStub.showDialogWhenConnectCMCC());
        item.setEnabled(wifiEnabled);
    }

    private void refreshNetmaskAndGateway() {
        Preference wifiNetmaskPref = findPreference(KEY_WIFI_NETMASK);
        Preference wifiGatewayPref = findPreference(KEY_WIFI_GATEWAY);

        wifiNetmaskPref.setSummary(R.string.status_unavailable);
        wifiGatewayPref.setSummary(R.string.status_unavailable);
        wifiNetmaskPref.setSelectable(false);
        wifiGatewayPref.setSelectable(false);

        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        if(dhcpInfo != null){
            int maskAddress = dhcpInfo.netmask;
            if (maskAddress != 0) {
                wifiNetmaskPref.setSummary(Formatter.formatIpAddress(maskAddress));
            }

            int gatewayAddress = dhcpInfo.gateway;
            if (gatewayAddress != 0) {
                wifiGatewayPref.setSummary(Formatter.formatIpAddress(gatewayAddress));
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        final Calendar calendar = Calendar.getInstance();
        switch (dialogId) {
            case DIALOG_WIFI_CONNECT_TIMEPICKER:
                whichTimepicker = DIALOG_WIFI_CONNECT_TIMEPICKER;
                return new TimePickerDialog(
                        getActivity(),
                        this,
                        //calendar.get(Calendar.HOUR_OF_DAY),
                        //calendar.get(Calendar.MINUTE),
                        Settings.Global.getInt(getContentResolver(),
                                WifiManager.WIFI_CONNECT_ALARM_HOUR, 0),
                        Settings.Global.getInt(getContentResolver(),
                                WifiManager.WIFI_CONNECT_ALARM_MINUTE, 0),
                        DateFormat.is24HourFormat(getActivity()));
            case DIALOG_WIFI_DISCONNECT_TIMEPICKER:
                whichTimepicker = DIALOG_WIFI_DISCONNECT_TIMEPICKER;
                return new TimePickerDialog(
                        getActivity(),
                        this,
                        //calendar.get(Calendar.HOUR_OF_DAY),
                        //calendar.get(Calendar.MINUTE),
                        Settings.Global.getInt(getContentResolver(),
                                WifiManager.WIFI_DISCONNECT_ALARM_HOUR, 0),
                        Settings.Global.getInt(getContentResolver(),
                                WifiManager.WIFI_DISCONNECT_ALARM_MINUTE, 0),
                        DateFormat.is24HourFormat(getActivity()));
            default:
                break;
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Log.d(TAG, "onTimeSet");

        Calendar calendar = getCalendar(hourOfDay, minute);
        if (whichTimepicker != -1) {
            switch (whichTimepicker) {
                case DIALOG_WIFI_CONNECT_TIMEPICKER:
                    Settings.Global.putInt(getContentResolver(), WifiManager.WIFI_CONNECT_ALARM_HOUR, hourOfDay);
                    Settings.Global.putInt(getContentResolver(), WifiManager.WIFI_CONNECT_ALARM_MINUTE, minute);
                    if (mConnectSwitch.isChecked()) {
                        setConnectWifiAlarm();
                    }
                    updateTimeDisplay(mConnectTimePref, calendar);
                    break;

                case DIALOG_WIFI_DISCONNECT_TIMEPICKER:
                    Settings.Global.putInt(getContentResolver(), WifiManager.WIFI_DISCONNECT_ALARM_HOUR, hourOfDay);
                    Settings.Global.putInt(getContentResolver(), WifiManager.WIFI_DISCONNECT_ALARM_MINUTE, minute);
                    if (mDisconnectSwitch.isChecked()) {
                        setDisonnectWifiAlarm();
                    }
                    updateTimeDisplay(mDisconnectTimePref, calendar);
                    break;
                default:
                    break;
            }
        }
    }

    private Calendar getCalendar(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 1);
        return calendar;
    }

    private void setConnectWifiAlarm() {
        Log.d(TAG, "setConnectWifiAlarm");
        int hourOfDay = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_CONNECT_ALARM_HOUR, 0);
        int minute = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_CONNECT_ALARM_MINUTE, 0);
        Calendar calendar = getCalendar(hourOfDay, minute);
        long inMillis = calendar.getTimeInMillis();
        if (isDismissCalendar(hourOfDay, minute)) {
            inMillis += WifiManager.INTERVAL_MILLIS;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(
                WifiManager.ALARM_FOR_CONNECT_WIFI_ACTION), 0);
        mAlarmManager.cancel(pendingIntent);
        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, inMillis, pendingIntent);
    }

    private void setDisonnectWifiAlarm() {
        Log.d(TAG, "setDisonnectWifiAlarm");
        int hourOfDay = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_DISCONNECT_ALARM_HOUR, 0);
        int minute = Settings.Global.getInt(getContentResolver(), WifiManager.WIFI_DISCONNECT_ALARM_MINUTE, 0);
        Calendar calendar = getCalendar(hourOfDay, minute);
        long inMillis = calendar.getTimeInMillis();
        if (isDismissCalendar(hourOfDay, minute)) {
            inMillis += WifiManager.INTERVAL_MILLIS;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(
                 WifiManager.ALARM_FOR_DISCONNECT_WIFI_ACTION), 0);
        mAlarmManager.cancel(pendingIntent);
        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, inMillis, pendingIntent);
    }

    private boolean isDismissCalendar(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) > hourOfDay) {
            return true;
        }else if (calendar.get(Calendar.HOUR_OF_DAY) == hourOfDay) {
            if (calendar.get(Calendar.MINUTE) >= minute) {
                return true;
            }
        }
        return false;
    }

    private void updateTimeDisplay(Preference preference, Calendar calendar) {
        preference.setSummary(DateFormat.getTimeFormat(getActivity()).format(
                calendar.getTime()));
    }
    /* @} */

}
