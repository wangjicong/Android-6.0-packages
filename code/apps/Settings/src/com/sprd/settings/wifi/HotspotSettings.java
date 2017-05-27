/** Created by Spreadst */

package com.sprd.settings.wifi;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import java.util.List;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.wifi.WifiApDialog;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import java.util.regex.Pattern;

public class HotspotSettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener,Preference.OnPreferenceChangeListener {

    private static final String TAG = "HotspotSettings";

    private static final String HOTSPOT_SSID_AND_SECURITY = "hotspot_ssid_and_security";
    private static final String HOTSPOT_CONNECTED_STATIONS = "hotspot_connected_stations";
    private static final String HOTSPOT_NO_CONNECTED_STATION = "hotspot_no_connected_station";
    private static final String HOTSPOT_BLOCKED_STATIONS = "hotspot_blocked_stations";
    private static final String HOTSPOT_NO_BLOCKED_STATION = "hotspot_no_blocked_station";
    private static final String HOTSPOT_WHITELIST_STATIONS = "hotspot_whitelist_stations";
    private static final String HOTSPOT_NO_WHITELIST_STATION = "hotspot_no_whitelist_station";
    private static final String HOTSPOT_WPS_MODE = "hotspot_wps_connect";
    private static final String HOTSPOT_MODE = "hotspot_mode";
    private static final String HOTSPOT_KEEP_WIFI_HOTSPOT_ON = "soft_ap_sleep_policy";
    private static final int HOTSPOT_NARMAL_MODE = 0;
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;
    private static final int DIALOG_AP_SETTINGS = 1;
    private static final int DIALOG_ADD_WHITELIST = 2;
    private static final int DIALOG_WPS_MODE = 3;
    public static final String STATIONS_STATE_CHANGED_ACTION = "com.sprd.settings.STATIONS_STATE_CHANGED";

    private String[] mSecurityType;
    private String mUserConnectTitle;
    private String mUserNoConnectTitle;
    private String mUserBlockTitle;
    private String mUserNoBlockTitle;
    private Preference mCreateNetwork;
    private PreferenceCategory mConnectedStationsCategory;
    private Preference mHotspotNoConnectedStation;
    private PreferenceCategory mBlockedStationsCategory;
    private Preference mHotspotNoBlockedStations;
    private PreferenceCategory mWhitelistStationsCategory;
    private Preference mHotspotNoWhitelistStations;
    private Preference mHotspotWpsMode;
    private ListPreference mHotspotMode;
    private ListPreference mHotspotKeepOn;
    private EditText mNameText;
    private EditText mMacText;
    private EditText mPinEdit;
    private TextView mPinText;
    private Spinner mModeSpinner;
    private WifiApDialog mDialog;
    private AlertDialog mAddDialog;
    private AlertDialog mWpsDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig;
    private StateReceiver mStateReceiver;

    private HotspotEnabler mHotspotEnabler;

    private boolean supportBtWifiSoftApCoexit = true;
    private boolean needPin = false;

    public void HotspotSettings() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.hotspot_settings);

        mConnectedStationsCategory = (PreferenceCategory) findPreference(HOTSPOT_CONNECTED_STATIONS);
        mBlockedStationsCategory = (PreferenceCategory) findPreference(HOTSPOT_BLOCKED_STATIONS);
        mWhitelistStationsCategory = (PreferenceCategory) findPreference(HOTSPOT_WHITELIST_STATIONS);
        mHotspotNoConnectedStation = (Preference) findPreference(HOTSPOT_NO_CONNECTED_STATION);
        mHotspotNoBlockedStations = (Preference) findPreference(HOTSPOT_NO_BLOCKED_STATION);
        mHotspotNoWhitelistStations = (Preference) findPreference(HOTSPOT_NO_WHITELIST_STATION);
        mHotspotWpsMode = (Preference) findPreference(HOTSPOT_WPS_MODE);
        if (mHotspotWpsMode != null && getResources().getBoolean(
                com.android.internal.R.bool.config_enableSoftApWPS) == false) {
            getPreferenceScreen().removePreference(mHotspotWpsMode);
        }
        mHotspotMode = (ListPreference) findPreference(HOTSPOT_MODE);
        mHotspotKeepOn = (ListPreference) findPreference(HOTSPOT_KEEP_WIFI_HOTSPOT_ON);

        if (SystemProperties.get("ro.btwifisoftap.coexist", "true").equals("false")) {
            supportBtWifiSoftApCoexit = false;
        }

        if (SystemProperties.get("ro.softap.whitelist", "true").equals("false")) {
            getPreferenceScreen().removePreference(mWhitelistStationsCategory);
            getPreferenceScreen().removePreference(mHotspotNoWhitelistStations);
            getPreferenceScreen().removePreference(mHotspotMode);
        }

        initWifiTethering();
        if (mHotspotMode != null) {
            mHotspotMode.setOnPreferenceChangeListener(this);
            int value = Settings.Global.getInt(getContentResolver(),
                    HOTSPOT_MODE,HOTSPOT_NARMAL_MODE);
            String stringValue = String.valueOf(value);
            mHotspotMode.setValue(stringValue);
            updateControlModeSummary(mHotspotMode, stringValue);
        }

        //add for keep wifi hotspot on
        WifiSoftApSleepPolicy.init(getActivity());
        mHotspotKeepOn.setOnPreferenceChangeListener(this);
        int value = Settings.System.getInt(getActivity().getContentResolver(),WifiSoftApSleepPolicy.WIFI_SOFT_AP_SLEEP_POLICY,
                    WifiSoftApSleepPolicy.WIFI_SOFT_AP_SLEEP_POLICY_NEVER);
        String stringValue = String.valueOf(value);
        mHotspotKeepOn.setValue(stringValue);
        updateHotspotKeepOnSummary(mHotspotKeepOn, stringValue);
    }
    @Override
    public void onStart() {
        super.onStart();

        // On/off switch is hidden for Setup Wizard (returns null)
        mHotspotEnabler = createhotspotEnabler();
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (mHotspotEnabler != null) {
            mHotspotEnabler.teardownSwitchBar();
        }
    };

    /**
     * @return new HotspotEnabler or null (as overridden by WifiSettingsForSetupWizard)
     */
    /* package */ HotspotEnabler createhotspotEnabler() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        return new HotspotEnabler(activity, activity.getSwitchBar());
    }
    @Override
    public void onResume() {
        super.onResume();
        mHotspotEnabler.resume();
        updateStations();

        mStateReceiver = new StateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_AP_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiManager.SOFTAP_BLOCKLIST_AVAILABLE_ACTION);
        filter.addAction(WifiManager.WIFI_AP_CLIENT_DETAILINFO_AVAILABLE_ACTION);
        filter.addAction(STATIONS_STATE_CHANGED_ACTION);
        getActivity().registerReceiver(mStateReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHotspotEnabler.pause();
        getActivity().unregisterReceiver(mStateReceiver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen,
            Preference preference) {
        if (preference == mCreateNetwork) {
            showDialog(DIALOG_AP_SETTINGS);
        } else if (preference == mHotspotNoWhitelistStations) {
            showDialog(DIALOG_ADD_WHITELIST);
        } else if (preference == mHotspotWpsMode) {
            showDialog(DIALOG_WPS_MODE);
        }
        System.out.println("fuzy onpreferenceclick");
        return super.onPreferenceTreeClick(screen, preference);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            return mDialog;
        } else if (id == DIALOG_ADD_WHITELIST) {
            LayoutInflater inflater = getLayoutInflater(null);
            View layout = inflater.inflate(R.layout.hotspot_add_whitelist,null);
            mNameText = (EditText)layout.findViewById(R.id.nameText);
            mMacText = (EditText)layout.findViewById(R.id.macText);
            mAddDialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.hotspot_whitelist).setView(layout)
                    .setPositiveButton(R.string.hotspot_whitelist_add, addClickListener)
                    .setNegativeButton(R.string.hotspot_whitelist_cancel, null).show();
            return mAddDialog;
        } else if (id == DIALOG_WPS_MODE) {
            LayoutInflater inflater = getLayoutInflater(null);
            View layout = inflater.inflate(R.layout.hotspot_wps_mode,null);
            mPinEdit = (EditText)layout.findViewById(R.id.pin_number);
            mPinText = (TextView)layout.findViewById(R.id.hotspot_wps_pin);
            mModeSpinner = (Spinner)layout.findViewById(R.id.hotspot_wps_mode);
            mModeSpinner.setOnItemSelectedListener(wpsSelectedListener);
            mWpsDialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.hotspot_wps_connect).setView(layout)
                    .setPositiveButton(R.string.hotspot_connect, null)
                    .setNegativeButton(R.string.hotspot_whitelist_cancel, null).show();
            mWpsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(connectClickListener);
            return mWpsDialog;
        }

        return null;
    }

    OnItemSelectedListener wpsSelectedListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            if (position == 0) {
                mPinEdit.setVisibility(View.GONE);
                mPinText.setVisibility(View.GONE);
                needPin = false;
            } else {
                mPinEdit.setVisibility(View.VISIBLE);
                mPinText.setVisibility(View.VISIBLE);
                needPin = true;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    View.OnClickListener connectClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (needPin) {
                if (mWifiManager.softApWpsCheckPin(mPinEdit.getText().toString().trim())) {
                    WpsInfo config = new WpsInfo();
                    config.pin = mPinEdit.getText().toString().trim();
                    config.setup = WpsInfo.KEYPAD;
                    Log.d(TAG,"hotspot wps config: "+config.toString());
                    mWpsDialog.dismiss();
                    mWifiManager.softApStartWps(config,null);
                } else {
                    Toast.makeText(getActivity(), R.string.hotspot_pin_error,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                WpsInfo config = new WpsInfo();
                config.setup = WpsInfo.PBC;
                Log.d(TAG,"hotspot wps config: "+config.toString());
                mWpsDialog.dismiss();
                mWifiManager.softApStartWps(config,null);
            }
        }
    };

    OnClickListener addClickListener = new OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // TODO Auto-generated method stub
            if (which == DialogInterface.BUTTON_POSITIVE) {
                List<String> mWhitelistStations = mWifiManager.softApGetClientWhiteList();
                if (mWhitelistStations.size() < 8) {
                    if (checkMac(mMacText.getText().toString().trim())) {
                        mWifiManager.softApAddClientToWhiteList(mMacText.getText().toString().trim(),mNameText.getText().toString().trim());
                        addWhitelistStations();
                    } else {
                        Toast.makeText(getActivity(), R.string.wifi_add_whitelist_error,
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.wifi_add_whitelist_limit,
                           Toast.LENGTH_SHORT).show();
               }
            }
        }
    };

    private void initWifiTethering() {
        final Activity activity = getActivity();
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);

        mCreateNetwork = findPreference(HOTSPOT_SSID_AND_SECURITY);
        mUserConnectTitle = activity.getString(R.string.wifi_tether_connect_title);
        mUserBlockTitle = activity.getString(R.string.wifi_tether_block_title);
        mUserNoConnectTitle = activity.getString(R.string.hotspot_connected_stations);
        mUserNoBlockTitle = activity.getString(R.string.hotspot_blocked_stations);
        if (mWifiConfig == null) {
            /*SUN:jicong.wang modify for custom phone model start {@*/
            //final String s = activity
            //        .getString(com.android.internal.R.string.wifi_tether_configure_ssid_default);
            final String s = SystemProperties.get("ro.product.model");
            /*SUN:jicong.wang modify for custom phone model end @}*/                    
            mCreateNetwork.setSummary(String.format(
                    activity.getString(CONFIG_SUBTEXT), s,
                    mSecurityType[WifiApDialog.OPEN_INDEX]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            mCreateNetwork.setSummary(String.format(
                    activity.getString(CONFIG_SUBTEXT), mWifiConfig.SSID,
                    mSecurityType[index]));
        }

        if (mWifiManager.getWifiApState() != WifiManager.WIFI_AP_STATE_ENABLED || mWifiConfig == null) {
            mHotspotWpsMode.setEnabled(false);
        } else if (mWifiConfig.getAuthType() == KeyMgmt.NONE) {
            mHotspotWpsMode.setEnabled(false);
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is stopped, bring up else restart with new config
                 * TODO: update config on a running access point when framework
                 * support is added
                 */
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    if (!supportBtWifiSoftApCoexit) {
                        Settings.Global.putInt(getContentResolver(),
                                Settings.Global.SOFTAP_REENABLING,
                                1);
                    }
                    mWifiManager.setWifiApEnabled(null, false);
                    mWifiManager.setWifiApEnabled(mWifiConfig, true);
                } else {
                    mWifiManager.setWifiApConfiguration(mWifiConfig);
                }
                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                mCreateNetwork.setSummary(String.format(getActivity()
                        .getString(CONFIG_SUBTEXT), mWifiConfig.SSID,
                        mSecurityType[index]));
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED && mWifiConfig.getAuthType() != KeyMgmt.NONE) {
                    mHotspotWpsMode.setEnabled(true);
                } else {
                    mHotspotWpsMode.setEnabled(false);
                }
            } else {
                    mHotspotWpsMode.setEnabled(false);
            }
        }
    }

    private void updateStations() {
        addConnectedStations();
        addBlockedStations();
        addWhitelistStations();
    }

    private void addConnectedStations() {
        List<String> mConnectedStationsDetail = mWifiManager.softApGetConnectedStationsDetail();
        mConnectedStationsCategory.removeAll();
        if (mConnectedStationsDetail == null || mConnectedStationsDetail.isEmpty()) {
            mConnectedStationsCategory.addPreference(mHotspotNoConnectedStation);
            mConnectedStationsCategory.setTitle(mUserNoConnectTitle);
            return;
        }
        mConnectedStationsCategory.setTitle(mConnectedStationsDetail.size() + mUserConnectTitle);
        for (String mConnectedStationsStr:mConnectedStationsDetail) {
            String[] mConnectedStations = mConnectedStationsStr.split(" ");
            if (mConnectedStations.length == 3) {
                mConnectedStationsCategory.addPreference(new Station(getActivity(), mConnectedStations[2], mConnectedStations[0], mConnectedStations[1], true, false));
            } else {
                mConnectedStationsCategory.addPreference(new Station(getActivity(), null, mConnectedStations[0], null, true, false));
            }
        }
    }

    private void addBlockedStations() {
        List<String> mBlockedStationsDetail = mWifiManager.softApGetBlockedStationsDetail();

        mBlockedStationsCategory.removeAll();
        if (mBlockedStationsDetail == null || mBlockedStationsDetail.isEmpty()) {
            mBlockedStationsCategory.addPreference(mHotspotNoBlockedStations);
            mBlockedStationsCategory.setTitle(mUserNoBlockTitle);
            return;
        }
        mBlockedStationsCategory.setTitle(mBlockedStationsDetail.size() + mUserBlockTitle);
        for (String mBlockedStationsStr:mBlockedStationsDetail) {
            String[] mBlockedStations = mBlockedStationsStr.split(" ");

            if (mBlockedStations.length == 3) {
                mBlockedStationsCategory.addPreference(new Station(getActivity(), mBlockedStations[2], mBlockedStations[0], null, false, false));
            } else {
                mBlockedStationsCategory.addPreference(new Station(getActivity(), null, mBlockedStations[0], null, false, false));
            }
        }
    }

    private void addWhitelistStations() {
         List<String> mWhitelistStationsDetail = mWifiManager.softApGetClientWhiteList();

         mWhitelistStationsCategory.removeAll();
         if (mWhitelistStationsDetail == null || mWhitelistStationsDetail.isEmpty()) {
             return;
         }
         for (String mWhitelistStationsStr:mWhitelistStationsDetail) {
             String[] mWhitelistStations = mWhitelistStationsStr.split(" ");

             if (mWhitelistStations.length == 2) {
                 mWhitelistStationsCategory.addPreference(new Station(getActivity(), mWhitelistStations[1], mWhitelistStations[0], null, false, true));
             } else {
                 mWhitelistStationsCategory.addPreference(new Station(getActivity(), null, mWhitelistStations[0], null, false, true));
             }
         }
     }

    private class StateReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.WIFI_AP_CONNECTION_CHANGED_ACTION)
                    || action.equals(WifiManager.SOFTAP_BLOCKLIST_AVAILABLE_ACTION)
                    || action.equals(STATIONS_STATE_CHANGED_ACTION)
                    || action.equals(WifiManager.WIFI_AP_CLIENT_DETAILINFO_AVAILABLE_ACTION)) {
                updateStations();
            } else if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                int hotspotState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE,
                        WifiManager.WIFI_AP_STATE_FAILED);
                if (hotspotState != WifiManager.WIFI_AP_STATE_ENABLED) {
                    mConnectedStationsCategory.removeAll();
                    mConnectedStationsCategory.addPreference(mHotspotNoConnectedStation);
                    mConnectedStationsCategory.setTitle(mUserNoConnectTitle);
                    mBlockedStationsCategory.removeAll();
                    mBlockedStationsCategory.addPreference(mHotspotNoBlockedStations);
                    mBlockedStationsCategory.setTitle(mUserNoBlockTitle);
                    mHotspotWpsMode.setEnabled(false);
                } else {
                    if (mWifiManager.getWifiApConfiguration() != null && mWifiManager.getWifiApConfiguration().getAuthType() != KeyMgmt.NONE) {
                        mHotspotWpsMode.setEnabled(true);
                    } else {
                        mHotspotWpsMode.setEnabled(false);
                    }
                    updateStations();
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO Auto-generated method stub
        final Context context = getActivity();
        String key = preference.getKey();
        if (HOTSPOT_MODE.equals(key)) {
            try {
                String stringValue = (String) newValue;
                Settings.Global.putInt(getContentResolver(), HOTSPOT_MODE,
                        Integer.parseInt(stringValue));
                updateControlModeSummary(preference, stringValue);
            } catch (NumberFormatException e) {
                Toast.makeText(context, R.string.wifi_setting_sleep_policy_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (HOTSPOT_KEEP_WIFI_HOTSPOT_ON.equals(key)) {
            try {
                int value = Integer.parseInt((String) newValue);
                Settings.System.putInt(getActivity().getContentResolver(),WifiSoftApSleepPolicy.WIFI_SOFT_AP_SLEEP_POLICY,value);
                updateHotspotKeepOnSummary(preference, (String) newValue);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }

    private void updateControlModeSummary(Preference modePref, String value) {
        if (value != null) {
            String[] values = getResources().getStringArray(R.array.hotspot_mode_values);
            String[] summaries = getResources().getStringArray(R.array.hotspot_mode);
            for (int i = 0; i < values.length; i++) {
                if (value.equals(values[i])) {
                    if (i < summaries.length) {
                        modePref.setSummary(summaries[i]);
                        mWifiManager.softApSetClientWhiteListEnabled((i==1));
                        updateModePref((i==1));
                        return;
                    }
                }
            }
        }

        modePref.setSummary("");
        Log.e(TAG, "Invalid controlMode value: " + value);
    }

    private boolean checkMac(String str) {
        String patternStr = "^[A-Fa-f0-9]{2}(:[A-Fa-f0-9]{2}){5}$";
        return Pattern.matches(patternStr, str);
    }

    private void updateModePref(boolean mode) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (mode) {
            if (mBlockedStationsCategory != null) preferenceScreen.removePreference(mBlockedStationsCategory);
        } else {
            preferenceScreen.addPreference(mBlockedStationsCategory);
        }
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsLogger.WIFI_HOTSPOT_CONTROL;
    }
    private void updateHotspotKeepOnSummary(Preference modePref, String value) {
        if (value !=null) {
            String[] values = getResources().getStringArray(R.array.soft_ap_sleep_policy_entryvalues);
            String[] summaries = getResources().getStringArray(R.array.soft_ap_sleep_policy_entries);
            for (int i = 0; i < values.length; i++) {
                if (value.equals(values[i])) {
                    if (i < summaries.length) {
                        modePref.setSummary(summaries[i]);
                        return;
                    }
                }
            }
        }

        modePref.setSummary("");
        Log.e(TAG, "Invalid  value: " + value);
    }
}
