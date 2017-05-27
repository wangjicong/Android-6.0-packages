/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.cellbroadcastreceiver;

import android.app.ActionBar;
import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;

import java.lang.Thread;
import java.lang.Runnable;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;

import com.sprd.cellbroadcastreceiver.provider.ChannelTableDefine;
import com.sprd.cellbroadcastreceiver.provider.CommonSettingTableDefine;
import com.sprd.cellbroadcastreceiver.provider.LangMapTableDefine;
import com.sprd.cellbroadcastreceiver.util.Utils;

/**
 * Settings activity for the cell broadcast receiver.
 */
public class CellBroadcastSettings extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

    // Preference category for custom channel ringtone settings.
    public static final String KEY_CATEGORY_ENABLE_CELLBROADCAST = "category_enable_cellbroadcast";

    //set the notification ringtone
    public static final String KEY_ENABLE_CELLBROADCAST = "enable_cellbroadcast";

    // Preference key for whether to enable emergency notifications (default enabled).
    public static final String KEY_ENABLE_EMERGENCY_ALERTS = "enable_emergency_alerts";

    // Duration of alert sound (in seconds).
    public static final String KEY_ALERT_SOUND_DURATION = "alert_sound_duration";

    // Default alert duration (in seconds).
    public static final String ALERT_SOUND_DEFAULT_DURATION = "4";

    // Preference category for custom channel ringtone settings.
    public static final String KEY_CATEGORY_CUSTOM_SETTINGS = "category_custom_settings";

    //set the notification ringtone
    public static final String KEY_SOUND_NOTIFICATION = "sound_notification";

    // Enable vibration on alert (unless master volume is silent).
    public static final String KEY_ENABLE_ALERT_VIBRATE = "enable_alert_vibrate";

    // Speak contents of alert after playing the alert sound.
    public static final String KEY_ENABLE_ALERT_SPEECH = "enable_alert_speech";

    // Preference category for emergency alert and CMAS settings.
    public static final String KEY_CATEGORY_ALERT_SETTINGS = "category_alert_settings";

    // Preference category for ETWS related settings.
    public static final String KEY_CATEGORY_ETWS_SETTINGS = "category_etws_settings";

    // Whether to display CMAS extreme threat notifications (default is enabled).
    public static final String KEY_ENABLE_CMAS_EXTREME_THREAT_ALERTS =
            "enable_cmas_extreme_threat_alerts";

    // Whether to display CMAS severe threat notifications (default is enabled).
    public static final String KEY_ENABLE_CMAS_SEVERE_THREAT_ALERTS =
            "enable_cmas_severe_threat_alerts";

    // Whether to display CMAS amber alert messages (default is enabled).
    public static final String KEY_ENABLE_CMAS_AMBER_ALERTS = "enable_cmas_amber_alerts";

    // Preference category for development settings (enabled by settings developer options toggle).
    public static final String KEY_CATEGORY_DEV_SETTINGS = "category_dev_settings";

    // Whether to display ETWS test messages (default is disabled).
    public static final String KEY_ENABLE_ETWS_TEST_ALERTS = "enable_etws_test_alerts";

    // Whether to display CMAS monthly test messages (default is disabled).
    public static final String KEY_ENABLE_CMAS_TEST_ALERTS = "enable_cmas_test_alerts";

    // Preference category for Brazil specific settings.
    public static final String KEY_CATEGORY_BRAZIL_SETTINGS = "category_brazil_settings";

    // Preference key for whether to enable channel 50 notifications
    // Enabled by default for phones sold in Brazil, otherwise this setting may be hidden.
    public static final String KEY_ENABLE_CHANNEL_50_ALERTS = "enable_channel_50_alerts";

    // Preference key for initial opt-in/opt-out dialog.
    public static final String KEY_SHOW_CMAS_OPT_OUT_DIALOG = "show_cmas_opt_out_dialog";

    // Alert reminder interval ("once" = single 2 minute reminder).
    public static final String KEY_ALERT_REMINDER_INTERVAL = "alert_reminder_interval";

    // Default reminder interval.
    public static final String ALERT_REMINDER_INTERVAL = "0";

    private final static String TAG = "CellBroadcastSettings";

    private TelephonyManager mTelephonyManager;
    private SubscriptionInfo mSir;
    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private List<SubscriptionInfo> mSelectableSubInfos;

    private CheckBoxPreference mExtremeCheckBox;
    private CheckBoxPreference mSevereCheckBox;
    private CheckBoxPreference mAmberCheckBox;
    private CheckBoxPreference mEmergencyCheckBox;
    private ListPreference mAlertDuration;
    private ListPreference mReminderInterval;
    private RingtonePreference mRingtonePreference;
    private CheckBoxPreference mVibrateCheckBox;
    private CheckBoxPreference mEnableCheckBox;
    private CheckBoxPreference mSpeechCheckBox;
    private CheckBoxPreference mEtwsTestCheckBox;
    private CheckBoxPreference mChannel50CheckBox;
    private CheckBoxPreference mCmasCheckBox;
    private CheckBoxPreference mOptOutCheckBox;
    private PreferenceCategory mAlertCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SPRD:Bug#527751
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        if (userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS)) {
            setContentView(R.layout.cell_broadcast_disallowed_preference_screen);
            return;
        }

        mTelephonyManager = (TelephonyManager) getSystemService(
                Context.TELEPHONY_SERVICE);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        for (int i = 0; i < mTelephonyManager.getSimCount(); i++) {
            final SubscriptionInfo sir =
                    findRecordBySlotId(getApplicationContext(), i);
            if (sir != null) {
                mSelectableSubInfos.add(sir);
            }
        }

        addPreferencesFromResource(R.xml.preferences);
        mSir = mSelectableSubInfos.size() > 0 ? mSelectableSubInfos.get(0) : null;
        if (mSelectableSubInfos.size() > 1) {
            setContentView(com.android.internal.R.layout.common_tab_settings);

            mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            mTabHost.setup();
            mTabHost.setOnTabChangedListener(mTabListener);
            mTabHost.clearAllTabs();

            for (int i = 0; i < mSelectableSubInfos.size(); i++) {
                mTabHost.addTab(buildTabSpec(String.valueOf(i),
                        String.valueOf(mSelectableSubInfos.get(i).getDisplayName())));
            }
        }
        updatePreferences();

        final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        String ringtoneString = getRingtoneUri();
        updateSoundSummary(prefs, ringtoneString);
        updateEnabledCategory();
    }

    private void updatePreferences() {

        PreferenceScreen prefScreen = getPreferenceScreen();

        if (prefScreen != null) {
            prefScreen.removeAll();
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            mExtremeCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_CMAS_EXTREME_THREAT_ALERTS);
            mSevereCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_CMAS_SEVERE_THREAT_ALERTS);
            mAmberCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_CMAS_AMBER_ALERTS);
            mEmergencyCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_EMERGENCY_ALERTS);
            mAlertDuration = (ListPreference)
                    findPreference(KEY_ALERT_SOUND_DURATION);
            mReminderInterval = (ListPreference)
                    findPreference(KEY_ALERT_REMINDER_INTERVAL);
            //add for ringtone
            mRingtonePreference = (RingtonePreference)
            		findPreference(KEY_SOUND_NOTIFICATION);
            mEnableCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_CELLBROADCAST);

            mVibrateCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_ALERT_VIBRATE);
            mSpeechCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_ALERT_SPEECH);
            mEtwsTestCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_ETWS_TEST_ALERTS);
            mChannel50CheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_CHANNEL_50_ALERTS);
            mCmasCheckBox = (CheckBoxPreference)
                    findPreference(KEY_ENABLE_CMAS_TEST_ALERTS);
            mOptOutCheckBox = (CheckBoxPreference)
                    findPreference(KEY_SHOW_CMAS_OPT_OUT_DIALOG);
            mAlertCategory = (PreferenceCategory)
                    findPreference(KEY_CATEGORY_ALERT_SETTINGS);

            if (!Utils.DEPEND_ON_SLOT) {
                Log.d(TAG, "remove the uri setting in Setting.");
                prefScreen.removePreference(findPreference(KEY_CATEGORY_CUSTOM_SETTINGS));
            }

            if(mSir == null) {
                mExtremeCheckBox.setEnabled(false);
                mSevereCheckBox.setEnabled(false);
                mAmberCheckBox.setEnabled(false);
                mEmergencyCheckBox.setEnabled(false);
                mReminderInterval.setEnabled(false);
                mAlertDuration.setEnabled(false);
                mVibrateCheckBox.setEnabled(false);
                mSpeechCheckBox.setEnabled(false);
                mEtwsTestCheckBox.setEnabled(false);
                mChannel50CheckBox.setEnabled(false);
                mCmasCheckBox.setEnabled(false);
                mOptOutCheckBox.setEnabled(false);
                mEnableCheckBox.setEnabled(false);
                mRingtonePreference.setEnabled(false);
                return;
            }

            // Handler for settings that require us to reconfigure enabled channels in radio
            Preference.OnPreferenceChangeListener startConfigServiceListener =
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference pref, Object newValue) {
                            int newVal = (((Boolean) newValue).booleanValue()) ? 1 : 0;

                            switch (pref.getKey()) {
                                case KEY_ENABLE_EMERGENCY_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_EMERGENCY_ALERT,
                                                    newVal + "");
                                    break;
                                case KEY_ENABLE_CHANNEL_50_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_CHANNEL_50_ALERT,
                                                    newVal + "");
                                    break;
                                case KEY_ENABLE_ETWS_TEST_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_ETWS_TEST_ALERT,
                                                    newVal + "");
                                    break;
                                case KEY_ENABLE_CMAS_EXTREME_THREAT_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_EXTREME_THREAT_ALERT,
                                                    newVal + "");
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_SEVERE_THREAT_ALERT,
                                                    "0");

                                    boolean isExtremeAlertChecked =
                                            ((Boolean) newValue).booleanValue();

                                    if (mSevereCheckBox != null) {
                                        mSevereCheckBox.setEnabled(isExtremeAlertChecked);
                                        mSevereCheckBox.setChecked(false);
                                    }
                                    break;
                                case KEY_ENABLE_CMAS_SEVERE_THREAT_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_SEVERE_THREAT_ALERT,
                                                    newVal + "");
                                    break;
                                case KEY_ENABLE_CMAS_AMBER_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_AMBER_ALERT,
                                                    newVal + "");
                                    break;
                                case KEY_ENABLE_CMAS_TEST_ALERTS:
                                    SubscriptionManager
                                            .setSubscriptionProperty(mSir.getSubscriptionId(),
                                                    SubscriptionManager.CB_CMAS_TEST_ALERT,
                                                    newVal + "");
                                    break;

                                default:
                                    Log.d(TAG, "Invalid preference changed");

                            }

                            CellBroadcastReceiver.startConfigService(pref.getContext(),
                                    mSir.getSubscriptionId());
                            return true;
                        }
                    };

            // Show extra settings when developer options is enabled in settings.
            boolean enableDevSettings = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;

            boolean showEtwsSettings = SubscriptionManager.getResourcesForSubId(
                    getApplicationContext(), mSir.getSubscriptionId())
                    .getBoolean(R.bool.show_etws_settings);

            String queryReturnVal;
            // alert reminder interval
            queryReturnVal = SubscriptionManager.getIntegerSubscriptionProperty(
                    mSir.getSubscriptionId(), SubscriptionManager.CB_ALERT_REMINDER_INTERVAL,
                    Integer.parseInt(ALERT_REMINDER_INTERVAL), this) + "";

            mReminderInterval.setValue(queryReturnVal);
            mReminderInterval.setSummary(mReminderInterval
                    .getEntries()[mReminderInterval.findIndexOfValue(queryReturnVal)]);

            mReminderInterval.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference pref, Object newValue) {
                            final ListPreference listPref = (ListPreference) pref;
                            final int idx = listPref.findIndexOfValue((String) newValue);
                            listPref.setSummary(listPref.getEntries()[idx]);
                            SubscriptionManager.setSubscriptionProperty(mSir.getSubscriptionId(),
                                    SubscriptionManager.CB_ALERT_REMINDER_INTERVAL,
                                    (String) newValue);
                            return true;
                        }
                    });

            boolean forceDisableEtwsCmasTest =
                    isEtwsCmasTestMessageForcedDisabled(this, mSir.getSubscriptionId());

            // Show alert settings and ETWS categories for ETWS builds and developer mode.
            if (enableDevSettings || showEtwsSettings) {
                // enable/disable all alerts
                if (mEmergencyCheckBox != null) {
                    if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                            SubscriptionManager.CB_EMERGENCY_ALERT, true, this)) {
                        mEmergencyCheckBox.setChecked(true);
                    } else {
                        mEmergencyCheckBox.setChecked(false);
                    }
                    mEmergencyCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
                }

                // alert sound duration
                queryReturnVal = SubscriptionManager.getIntegerSubscriptionProperty(
                        mSir.getSubscriptionId(), SubscriptionManager.CB_ALERT_SOUND_DURATION,
                        Integer.parseInt(ALERT_SOUND_DEFAULT_DURATION), this) + "";
                mAlertDuration.setValue(queryReturnVal);
                mAlertDuration.setSummary(mAlertDuration
                        .getEntries()[mAlertDuration.findIndexOfValue(queryReturnVal)]);
                mAlertDuration.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                final ListPreference listPref = (ListPreference) pref;
                                final int idx = listPref.findIndexOfValue((String) newValue);
                                listPref.setSummary(listPref.getEntries()[idx]);
                                SubscriptionManager.setSubscriptionProperty(
                                        mSir.getSubscriptionId(),
                                        SubscriptionManager.CB_ALERT_SOUND_DURATION,
                                        (String) newValue);
                                return true;
                            }
                        });
                if (forceDisableEtwsCmasTest) {
                    // Remove ETWS test preference.
                    prefScreen.removePreference(findPreference(KEY_CATEGORY_ETWS_SETTINGS));

                    PreferenceCategory devSettingCategory =
                            (PreferenceCategory) findPreference(KEY_CATEGORY_DEV_SETTINGS);

                    // Remove CMAS test preference.
                    if (devSettingCategory != null) {
                        devSettingCategory.removePreference(
                                findPreference(KEY_ENABLE_CMAS_TEST_ALERTS));
                    }
                }
            } else {
                // Remove general emergency alert preference items (not shown for CMAS builds).
                mAlertCategory.removePreference(findPreference(KEY_ENABLE_EMERGENCY_ALERTS));
                mAlertCategory.removePreference(findPreference(KEY_ALERT_SOUND_DURATION));
                mAlertCategory.removePreference(findPreference(KEY_ENABLE_ALERT_SPEECH));
                // Remove ETWS test preference category.
                prefScreen.removePreference(findPreference(KEY_CATEGORY_ETWS_SETTINGS));
            }

            if (!SubscriptionManager.getResourcesForSubId(getApplicationContext(),
                    mSir.getSubscriptionId()).getBoolean(R.bool.show_cmas_settings)) {
                // Remove CMAS preference items in emergency alert category.
                mAlertCategory.removePreference(
                        findPreference(KEY_ENABLE_CMAS_EXTREME_THREAT_ALERTS));
                mAlertCategory.removePreference(
                        findPreference(KEY_ENABLE_CMAS_SEVERE_THREAT_ALERTS));
                mAlertCategory.removePreference(findPreference(KEY_ENABLE_CMAS_AMBER_ALERTS));
            }

            boolean enableChannel50Support = SubscriptionManager.getResourcesForSubId(
                    getApplicationContext(), mSir.getSubscriptionId()).getBoolean(
                    R.bool.show_brazil_settings)
                    || "br".equals(mTelephonyManager.getSimCountryIso());

            if (!enableChannel50Support) {
                prefScreen.removePreference(findPreference(KEY_CATEGORY_BRAZIL_SETTINGS));
            }
            if (!enableDevSettings) {
                prefScreen.removePreference(findPreference(KEY_CATEGORY_DEV_SETTINGS));
            }

            if (mEnableCheckBox != null) {
                if (mEnableCheckBox.isChecked()) {
                    if (prefScreen.findPreference(KEY_CATEGORY_BRAZIL_SETTINGS) != null) {
                        prefScreen.findPreference(KEY_CATEGORY_BRAZIL_SETTINGS).setEnabled(true);
                    }
                    if (prefScreen.findPreference(KEY_CATEGORY_DEV_SETTINGS) != null) {
                        prefScreen.findPreference(KEY_CATEGORY_DEV_SETTINGS).setEnabled(true);
                    }
                    if (prefScreen.findPreference(KEY_CATEGORY_CUSTOM_SETTINGS) != null) {
                        prefScreen.findPreference(KEY_CATEGORY_CUSTOM_SETTINGS).setEnabled(true);
                    }
                    prefScreen.findPreference(KEY_CATEGORY_ALERT_SETTINGS).setEnabled(true);
                    prefScreen.findPreference(KEY_CATEGORY_ETWS_SETTINGS).setEnabled(true);
                } else {
                    if (prefScreen.findPreference(KEY_CATEGORY_BRAZIL_SETTINGS) != null) {
                        prefScreen.findPreference(KEY_CATEGORY_BRAZIL_SETTINGS).setEnabled(false);
                    }
                    if (prefScreen.findPreference(KEY_CATEGORY_DEV_SETTINGS) != null) {
                        prefScreen.findPreference(KEY_CATEGORY_DEV_SETTINGS).setEnabled(false);
                    }
                    if (prefScreen.findPreference(KEY_CATEGORY_CUSTOM_SETTINGS) != null) {
                        prefScreen.findPreference(KEY_CATEGORY_CUSTOM_SETTINGS).setEnabled(false);
                    }
                    prefScreen.findPreference(KEY_CATEGORY_ALERT_SETTINGS).setEnabled(false);
                    prefScreen.findPreference(KEY_CATEGORY_ETWS_SETTINGS).setEnabled(false);
                }
                mEnableCheckBox.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                final boolean newVal = (((Boolean) newValue).booleanValue());

                                enabledStateChanged(newVal);
                                CellBroadcastReceiver.startConfigService(getPreferenceScreen().getContext(),
                                        mSir.getSubscriptionId());
                                //modify for bug 542807 begin
                                final int subOrPhoneId = getSubIdOrPhoneId();
                                Thread t = new Thread(new Runnable(){

                                    @Override
                                    public void run(){
                                        sendATCommandToAll(subOrPhoneId, newVal);
                                        updateEnabledToDB(newVal);
                                    }
                                });
                                t.start();
                                //modify for bug 542807 end
                                return true;
                            }
                        });
            }

            if (mSpeechCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_ALERT_SPEECH, true, this)) {
                    mSpeechCheckBox.setChecked(true);
                } else {
                    mSpeechCheckBox.setChecked(false);
                }
                mSpeechCheckBox.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                int newVal = (((Boolean) newValue).booleanValue()) ? 1 : 0;
                                SubscriptionManager.setSubscriptionProperty(
                                        mSir.getSubscriptionId(),
                                        SubscriptionManager.CB_ALERT_SPEECH, newVal + "");
                                return true;
                            }
                        });
            }

            if (mVibrateCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_ALERT_VIBRATE, true, this)) {
                    mVibrateCheckBox.setChecked(true);
                } else {
                    mVibrateCheckBox.setChecked(false);
                }
                mVibrateCheckBox.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                int newVal = (((Boolean) newValue).booleanValue()) ? 1 : 0;
                                SubscriptionManager.setSubscriptionProperty(
                                        mSir.getSubscriptionId(),
                                        SubscriptionManager.CB_ALERT_VIBRATE, newVal + "");
                                return true;
                            }
                        });
            }

            if (mOptOutCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_OPT_OUT_DIALOG, true, this)) {
                    mOptOutCheckBox.setChecked(true);
                } else {
                    mOptOutCheckBox.setChecked(false);
                }
                mOptOutCheckBox.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference pref, Object newValue) {
                                int newVal = (((Boolean) newValue).booleanValue()) ? 1 : 0;
                                SubscriptionManager.setSubscriptionProperty(
                                        mSir.getSubscriptionId(),
                                        SubscriptionManager.CB_OPT_OUT_DIALOG, newVal + "");
                                return true;
                            }
                        });
            }

            if (mChannel50CheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_CHANNEL_50_ALERT, true, this)) {
                    mChannel50CheckBox.setChecked(true);
                } else {
                    mChannel50CheckBox.setChecked(false);
                }
                mChannel50CheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
            }

            if (mEtwsTestCheckBox != null) {
                if (!forceDisableEtwsCmasTest &&
                        SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_ETWS_TEST_ALERT, false, this)) {
                    mEtwsTestCheckBox.setChecked(true);
                } else {
                    mEtwsTestCheckBox.setChecked(false);
                }
                mEtwsTestCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
            }

            if (mExtremeCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_EXTREME_THREAT_ALERT, true, this)) {
                    mExtremeCheckBox.setChecked(true);
                } else {
                    mExtremeCheckBox.setChecked(false);
                }
                mExtremeCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
            }

            if (mSevereCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_SEVERE_THREAT_ALERT, true, this)) {
                    mSevereCheckBox.setChecked(true);
                } else {
                    mSevereCheckBox.setChecked(false);
                }
                mSevereCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
                if (mExtremeCheckBox != null) {
                    boolean isExtremeAlertChecked =
                            ((CheckBoxPreference) mExtremeCheckBox).isChecked();
                    mSevereCheckBox.setEnabled(isExtremeAlertChecked);
                }
            }

            if (mAmberCheckBox != null) {
                if (SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_AMBER_ALERT, true, this)) {
                    mAmberCheckBox.setChecked(true);
                } else {
                    mAmberCheckBox.setChecked(false);
                }
                mAmberCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
            }

            if (mCmasCheckBox != null) {
                if (!forceDisableEtwsCmasTest &&
                        SubscriptionManager.getBooleanSubscriptionProperty(mSir.getSubscriptionId(),
                        SubscriptionManager.CB_CMAS_TEST_ALERT, false, this)) {
                    mCmasCheckBox.setChecked(true);
                } else {
                    mCmasCheckBox.setChecked(false);
                }
                mCmasCheckBox.setOnPreferenceChangeListener(startConfigServiceListener);
            }
        }
    }

    // Check if ETWS/CMAS test message is forced disabled on the device.
    public static boolean isEtwsCmasTestMessageForcedDisabled(Context context, int subId) {

        if (context == null) {
            return false;
        }

        CarrierConfigManager configManager =
                (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);

        if (configManager != null) {
            PersistableBundle carrierConfig =
                    configManager.getConfigForSubId(subId);

            if (carrierConfig != null) {
                return carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_FORCE_DISABLE_ETWS_CMAS_TEST_BOOL);
            }
        }

        return false;
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            final int slotId = Integer.parseInt(tabId);
            mSir = mSelectableSubInfos.get(slotId);
            updatePreferences();

            final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            String ringtoneString = getRingtoneUri();
            updateSoundSummary(prefs, ringtoneString);
            updateEnabledCategory();
        }
    };

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

    public SubscriptionInfo findRecordBySlotId(Context context, final int slotId) {
        final List<SubscriptionInfo> subInfoList =
                SmsManager.getDefault().getActiveSubInfoList();
        if (subInfoList != null) {
            final int subInfoLength = subInfoList.size();

            for (int i = 0; i < subInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                if (sir.getSimSlotIndex() == slotId) {
                    return sir;
                }
            }
        }

        return null;
    }

    @Override
    public void onStart() {
        super.onStart();
        // We do this on start rather than on resume because the sound picker is in a
        // separate activity.
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
            final String key) {
        Log.d(TAG, "--onSharedPreferenceChanged--");
        if (key.equals(KEY_SOUND_NOTIFICATION)) {
            String ringtoneString = sharedPreferences.getString(KEY_SOUND_NOTIFICATION, null);
            updateSoundSummary(sharedPreferences, ringtoneString);
        }
    }
    
    private void updateSoundSummary(final SharedPreferences sharedPreferences, String ringtoneString){
        // The silent ringtone just returns an empty string
        String ringtoneName = mRingtonePreference.getContext().getString(
                R.string.silent_ringtone);

        // Bootstrap the default setting in the preferences so that we have a valid selection
        // in the dialog the first time that the user opens it.

        if (ringtoneString == null) {
            if (getRingtoneUri() == null) {
                ringtoneString = Utils.DEFAULT_RINGTONE;
                insertUriRecord(ringtoneString);
            } else {
                ringtoneString = getRingtoneUri();
            }
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SOUND_NOTIFICATION, ringtoneString);
            editor.apply();
        }

        if (!TextUtils.isEmpty(ringtoneString)) {
            //save the new ringtoneString to db
            if (ringtoneString != getRingtoneUri()) {
                updateUriToDB(ringtoneString);
            }
            final Uri ringtoneUri = Uri.parse(ringtoneString);
            final Ringtone tone = RingtoneManager.getRingtone(mRingtonePreference.getContext(),
                    ringtoneUri);

            if (tone != null) {
                ringtoneName = tone.getTitle(mRingtonePreference.getContext());
            }
        }

        Log.d(TAG, "update---the ringtonename is:"+ ringtoneName+" and the ringtoneString is:"+ringtoneString);
        mRingtonePreference.setSummary(ringtoneName);
    }

    private String getRingtoneUri() {
        String[] select_column = { CommonSettingTableDefine.RING_URL };
        Cursor cursor = getContentResolver().query(Utils.mCommonSettingUri,
                select_column,
                CommonSettingTableDefine.SUBID + "=" + getSubIdOrPhoneId(), null,
                null);
        Log.d(TAG, "---getRingtoneUri---subId="+getSubIdOrPhoneId()+" and cursor.getcount="+cursor.getCount());
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        cursor.moveToFirst();
        int index = cursor.getColumnIndex(CommonSettingTableDefine.RING_URL);
        String ringtoneName = cursor.getString(index);
        cursor.close();
        return ringtoneName;
    }

    private int getSubIdOrPhoneId() {
        if (mSir == null) {
            return -1;
        }
        Log.d(TAG, "---getSubIdOrPhoneId---the subId is:"+mSir.getSubscriptionId()
                +" and slotId is:"+mSir.getSimSlotIndex());
        if (Utils.USE_SUBID) {
            return mSir.getSubscriptionId();
        } else {
            return mSir.getSimSlotIndex();
        }
    }

    private void insertUriRecord(String ringtoneString){
        Log.d(TAG, "insert a new record,subId is:"+getSubIdOrPhoneId()+" and uri is:"+ringtoneString);
        if (getSubIdOrPhoneId() == -1) {
            return;
        }
        ContentValues cv = new ContentValues(2);
        cv.put(CommonSettingTableDefine.SUBID, getSubIdOrPhoneId());
        cv.put(CommonSettingTableDefine.RING_URL, ringtoneString);
        
        getContentResolver().insert(Utils.mCommonSettingUri, cv);
    }

    private void updateUriToDB(String ringtoneString){
        Log.d(TAG, "update the ringtone Uir:"+ringtoneString);
        ContentValues cv = new ContentValues(1);
        cv.put(CommonSettingTableDefine.RING_URL, ringtoneString);
        
        getContentResolver().update(Utils.mCommonSettingUri,
                cv, CommonSettingTableDefine.SUBID+ "="+getSubIdOrPhoneId(),
                null);
    }

    private void updateEnabledCategory(){
        if (mSir == null) {
            return;
        }
        String[] select_column = { CommonSettingTableDefine.ENABLED_CELLBROADCAST };
        Cursor cursor = getContentResolver().query(Utils.mCommonSettingUri,
                select_column,
                CommonSettingTableDefine.SUBID + "=" + getSubIdOrPhoneId(), null,
                null);
        Log.d(TAG, "---updateEnabledCategory---subId="+getSubIdOrPhoneId()+" and cursor.getcount="+cursor.getCount());
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }
        cursor.moveToFirst();
        int index = cursor.getColumnIndex(CommonSettingTableDefine.ENABLED_CELLBROADCAST);
        int enabled = cursor.getInt(index);
        cursor.close();
        mEnableCheckBox.setChecked(enabled == 1);
        enabledStateChanged(enabled == 1);
    }

    private void enabledStateChanged(boolean newVal){
        Log.d(TAG, "Enable Cellbroadcast Preference Changed to:"+newVal);
        if (findPreference(KEY_CATEGORY_BRAZIL_SETTINGS) != null) {
            findPreference(KEY_CATEGORY_BRAZIL_SETTINGS).setEnabled(newVal);
        }
        if (findPreference(KEY_CATEGORY_DEV_SETTINGS) != null) {
            findPreference(KEY_CATEGORY_DEV_SETTINGS).setEnabled(newVal);
        }
        if (findPreference(KEY_CATEGORY_CUSTOM_SETTINGS) != null) {
            findPreference(KEY_CATEGORY_CUSTOM_SETTINGS).setEnabled(newVal);
        }
        findPreference(KEY_CATEGORY_ALERT_SETTINGS).setEnabled(newVal);
        findPreference(KEY_CATEGORY_ETWS_SETTINGS).setEnabled(newVal);
    }

    private void updateEnabledToDB(boolean enabled){
        Log.d(TAG, "update the EnabledCellbroadcast:"+enabled);
        ContentValues cv = new ContentValues(1);
        cv.put(CommonSettingTableDefine.ENABLED_CELLBROADCAST, enabled?1:0);
        
        getContentResolver().update(Utils.mCommonSettingUri,
                cv, CommonSettingTableDefine.SUBID+ "="+getSubIdOrPhoneId(),
                null);
    }

    private void sendATCommandToAll(int subId, boolean enabled){
        if (enabled) {
            openAllCustomChannels(subId, 1);
            openAllCustomLanguage(subId, 1);
        } else {
            Utils.sendATCmd(CellBroadcastSettings.this, 1000, 0, subId, Utils.SET_CHANNEL);
        }
    }

    private void openAllCustomChannels(int subId, int enable) {
        String[] query_colomn = {ChannelTableDefine.CHANNEL_ID };
        String sql_queryAll = ChannelTableDefine.SUB_ID + "=" + subId
                +" and "+ChannelTableDefine.ENABLE+"="+enable;
        Cursor cursor = getContentResolver().query(Utils.mChannelUri, query_colomn,
                sql_queryAll, null, null);
        Log.d("ran", "open All Custom Channels for subId:"+subId+" and the size is:"+cursor.getCount());
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }
        cursor.moveToFirst();
        do {
            int channel_id = cursor.getInt(cursor.getColumnIndex(ChannelTableDefine.CHANNEL_ID));
            Utils.sendATCmd(CellBroadcastSettings.this, channel_id, enable, subId, Utils.SET_CHANNEL);
        } while (cursor.moveToNext());
        cursor.close();
    }

    private void openAllCustomLanguage(int subId, int enable) {
        String[] query_colomn = {LangMapTableDefine.LANG_ID };
        String sql_queryAll = LangMapTableDefine.SUBID + "=" + subId
                +" and "+LangMapTableDefine.ENABLE+"="+enable;
        Cursor cursor = getContentResolver().query(Utils.mLangUri, query_colomn,
                sql_queryAll, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            return;
        }
        cursor.moveToFirst();
        do {
            int lang_id = cursor.getInt(cursor.getColumnIndex(LangMapTableDefine.LANG_ID));
            Utils.sendATCmd(CellBroadcastSettings.this, lang_id, enable, subId, Utils.SET_LANGUAGE);
        } while (cursor.moveToNext());
        cursor.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        //SPRD:Bug#527751
        case android.R.id.home:
            finish();
            break;
        }
        return false;
    }
}
