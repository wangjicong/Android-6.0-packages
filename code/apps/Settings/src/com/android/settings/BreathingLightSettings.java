package com.android.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.sprd.android.config.OptConfig;

public class BreathingLightSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener{
    private static final String TAG = "BreathingLightSettings";
    private static final String KEY_NOTIFICATIONS = "breathing_light_notifications";
    private static final String KEY_LOW_BATTERY = "breathing_light_low_battery";
    private static final String KEY_CHARGING = "breathing_light_charging";
    //Kalyy 2015-02-04
    private static final String KEY_INCOMING_CALL = "breathing_light_incoming_call";
    private static final String KEY_MEDIA = "breathing_light_media";
    private static final String BREATHING_LIGHT_SERVICE_ACTION = "com.android.settings.service.breathinglight";
    private static final String BREATHING_NOTIFICATIONS_CLICKED = "breathing_light_notifications_clicked";
    private CheckBoxPreference mBreathingLightNotifications;
    private CheckBoxPreference mBreathingLightLowBattery;
    private CheckBoxPreference mBreathingLightCharging;
    //Kalyy 2015-02-04
    private CheckBoxPreference mBreathingLightIncomingCall;
    private CheckBoxPreference mBreathingLightMedia;
    private boolean isNotificationSelected = true;
    private boolean isLowBatterySelected = true;
    private boolean isChargingSelected = true;
    //Kalyy 2015-02-04
    private boolean isIncomingCallSelected = true;
    private boolean isMediaSelected = true;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.breathing_light_settings);
        mBreathingLightNotifications = (CheckBoxPreference) findPreference(KEY_NOTIFICATIONS);
        mBreathingLightLowBattery = (CheckBoxPreference) findPreference(KEY_LOW_BATTERY);
        mBreathingLightCharging = (CheckBoxPreference) findPreference(KEY_CHARGING);
        //Kalyy 2015-02-04
        mBreathingLightIncomingCall = (CheckBoxPreference) findPreference(KEY_INCOMING_CALL);
        mBreathingLightMedia = (CheckBoxPreference) findPreference(KEY_MEDIA);
        mBreathingLightNotifications.setOnPreferenceClickListener(this);
        mBreathingLightLowBattery.setOnPreferenceClickListener(this);
        mBreathingLightCharging.setOnPreferenceClickListener(this);
        //Kalyy 2015-02-04
        mBreathingLightIncomingCall.setOnPreferenceClickListener(this);
        mBreathingLightMedia.setOnPreferenceClickListener(this);
        /*SUN:jicong.wang add for rgb start{@*/  
        if (OptConfig.SUN_RGB_SINGLE_FOR_LEDS){
            getPreferenceScreen().removePreference(mBreathingLightNotifications);
            getPreferenceScreen().removePreference(mBreathingLightMedia);            
        }
        /*SUN:jicong.wang add for rgb end @}*/
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.d(TAG,"onPreferenceClick.");
        isNotificationSelected = mBreathingLightNotifications.isChecked();
        isLowBatterySelected = mBreathingLightLowBattery.isChecked();
        isChargingSelected = mBreathingLightCharging.isChecked();
        //Kalyy 2015-02-04
        isIncomingCallSelected = mBreathingLightIncomingCall.isChecked();
        isMediaSelected = mBreathingLightMedia.isChecked();
        Log.d(TAG,"onPreferenceClick isNotificationSelected: "+isNotificationSelected);
        Log.d(TAG,"onPreferenceClick isLowBatterySelected: "+isLowBatterySelected);
        Log.d(TAG,"onPreferenceClick isChargingSelected: "+isChargingSelected);
        Intent intent = new Intent(BREATHING_LIGHT_SERVICE_ACTION);
        intent.setPackage(getActivity().getPackageName());
							
        if (preference == mBreathingLightNotifications) {
            intent.putExtra(BREATHING_NOTIFICATIONS_CLICKED, true);
        } else {
            intent.putExtra(BREATHING_NOTIFICATIONS_CLICKED, false);
        }
        if (isNotificationSelected || isLowBatterySelected || isChargingSelected || isIncomingCallSelected || isMediaSelected) {
            getActivity().startService(intent);
        } else {
            getActivity().stopService(intent);
        }
        return false;
    }
    
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SUN_BREATHING_LIGHT;
    }
}
