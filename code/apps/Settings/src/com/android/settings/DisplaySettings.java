/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DropDownPreference.Callback;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.DropDownPreference;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
/* SPRD:add for press-brightness. @{ */
import java.io.File;
import android.widget.Toast;
/* @} */
//==========lovelyfonts add ===========
import android.app.ActivityManagerNative;
import android.os.RemoteException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ComponentName;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
//==========lovelyfonts end ===========
/*SUN:jicong.wang add for SUN_INCALL_FLASH start {@*/
import android.preference.CheckBoxPreference;
import com.sprd.android.config.OptConfig;
/*SUN:jicong.wang add for SUN_INCALL_FLASH end @}*/
public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "DisplaySettings";
//==========lovelyfonts add ===========
    private static final String KEY_FONT_SETTING = "font_setting";
    private Preference mFontSettingPreference;
//==========lovelyfonts end ===========

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE = "doze";
    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_WIFI_DISPLAY = "wifi_display";
    // SPRD: add for bug 510399
    private static final String KEY_TOUCH_LIGHT = "touch_light_timeout";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private WarnedListPreference mFontSizePref;

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mScreenTimeoutPreference;
    private ListPreference mNightModePreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mDozePreference;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private Preference mWifiDisplayPreference;
    /* SPRD:add for press-brightness. @{ */
    private ListPreference mTouchLightTimeoutPreference;
    private static final int VALUE_KEYLIGHT_1500 = 1500;
    private static final int VALUE_KEYLIGHT_6000 = 6000;
    private static final int VALUE_KEYLIGHT_1 = -1;
    private static final int VALUE_KEYLIGHT_2 = -2;
    private static final String BUTTON_TOUCH_LIGHT_TIMEOUT = "touch_light_timeout";
    private static final int FALLBACK_TOUCH_LIGHT_TIMEOUT_VALUE = 1500;
    /* @} */
    /*SUN:jicong.wang add for SUN_INCALL_FLASH start {@*/
    private static final String KEY_INCALL_FLASH = "incall_flash";
    private CheckBoxPreference mIncallFlash;
	/*SUN:jicong.wang add for SUN_INCALL_FLASH end @}*/


    // SPRD: add for bug 510399
    private static boolean mHasTouchLight = false;
    private static final boolean WCN_DISABLED = SystemProperties.get("ro.wcn").equals("disabled");
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    /* SPRD: add for unregisterRotationPolicyListener @{ */
    private DropDownPreference mRotatePreference;
    private final RotationPolicyListener mRotationPolicyListener = new RotationPolicyListener() {
        @Override
        public void onChange() {
            // SPRD 524619: can not use mRotatePreference before it init
            if (getActivity() != null && mRotatePreference != null) {
                mRotatePreference.setSelectedItem(RotationPolicy.isRotationLocked(getActivity()) ?
                        1 : 0);
            }
        }
    };
    /* @} */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);
        
		/*SUN:jicong.wang add for SUN_INCALL_FLASH start {@*/        
        mIncallFlash = (CheckBoxPreference) findPreference(KEY_INCALL_FLASH);
        mIncallFlash.setPersistent(false);
		if(!OptConfig.SUN_INCALL_FLASH){
	        getPreferenceScreen().removePreference(mIncallFlash);
		}
		/*SUN:jicong.wang add for SUN_INCALL_FLASH end @}*/

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        /* SPRD:add for press-brightness. @{ */
        mTouchLightTimeoutPreference = (ListPreference) findPreference(BUTTON_TOUCH_LIGHT_TIMEOUT);
        if (!fileIsExists()) {
            // SPRD: add for bug 510399
            mHasTouchLight = false;
            Log.d(TAG, "fileIsExists true--removePreference(mTouchLightTimeoutPreference)");
            getPreferenceScreen().removePreference(mTouchLightTimeoutPreference);
        } else {
            // SPRD: add for bug 510399
            mHasTouchLight = true;
            /* Sprd: for bug 501208 @{ */
            final int touchlightcurrentTimeout = Settings.System.getIntForUser(resolver, Settings.System.BUTTON_LIGHT_OFF_TIMEOUT,
                    FALLBACK_TOUCH_LIGHT_TIMEOUT_VALUE, UserHandle.USER_OWNER);
            /* @} */
            mTouchLightTimeoutPreference.setValue(String.valueOf(touchlightcurrentTimeout));
            updateTouchLightPreferenceSummary(touchlightcurrentTimeout);
            mTouchLightTimeoutPreference.setOnPreferenceChangeListener(this);
        }
        /* @} */
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mFontSizePref = (WarnedListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        if (isAutomaticBrightnessAvailable(getResources())) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_BRIGHTNESS);
        }

        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }

        if (isDozeAvailable(activity)) {
            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            mDozePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE);
        }

        if (isTapToWakeAvailable(getResources())) {
            mTapToWakePreference = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
            mTapToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_TAP_TO_WAKE);
        }

        mWifiDisplayPreference = (Preference)findPreference(KEY_WIFI_DISPLAY);
        if (WCN_DISABLED || !activity.getResources().getBoolean(
                com.android.internal.R.bool.config_enableWifiDisplay)) {
            getPreferenceScreen().removePreference(mWifiDisplayPreference);
            mWifiDisplayPreference = null;
        }

        if (RotationPolicy.isRotationLockToggleVisible(activity)) {
            mRotatePreference =
                    (DropDownPreference) findPreference(KEY_AUTO_ROTATE);
            mRotatePreference.addItem(activity.getString(R.string.display_auto_rotate_rotate),
                    false);
            int rotateLockedResourceId;
            // The following block sets the string used when rotation is locked.
            // If the device locks specifically to portrait or landscape (rather than current
            // rotation), then we use a different string to include this information.
            if (allowAllRotations(activity)) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
            } else {
                if (RotationPolicy.getRotationLockOrientation(activity)
                        == Configuration.ORIENTATION_PORTRAIT) {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_portrait;
                } else {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_landscape;
                }
            }
            mRotatePreference.addItem(activity.getString(rotateLockedResourceId), true);
            mRotatePreference.setSelectedItem(RotationPolicy.isRotationLocked(activity) ?
                    1 : 0);
            mRotatePreference.setCallback(new Callback() {
                @Override
                public boolean onItemSelected(int pos, Object value) {
                    final boolean locked = (Boolean) value;
                    MetricsLogger.action(getActivity(), MetricsLogger.ACTION_ROTATION_LOCK,
                            locked);
                    RotationPolicy.setRotationLock(activity, locked);
                    return true;
                }
            });
        } else {
            removePreference(KEY_AUTO_ROTATE);
        }
        
        /*SUN:jicong.wang add for touch light timeout start {@*/
        if(!isTouchLightTimeoutAvailable(getResources())){
            removePreference(BUTTON_TOUCH_LIGHT_TIMEOUT);
        }
        /*SUN:jicong.wang add for touch light timeout end @}*/
        
        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                    Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
        }
		//==========lovelyfonts add ===========
        mFontSettingPreference = (Preference)findPreference(KEY_FONT_SETTING);
		if(!SystemProperties.get("ro.lovelyfonts_support").equals("1")){
            getPreferenceScreen().removePreference(mFontSettingPreference);
		}
		//==========lovelyfonts end ===========
    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }
    
    /*SUN:jicong.wang touch light timeout check start {@*/
    private static boolean isTouchLightTimeoutAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_touch_light_timeout_available);
    }
    /*SUN:jicong.wang touch light light timeout end @}*/
    
    /* SPRD:add for press-brightness. @{ */
    private void writeTouchLightPreference(Object objValue) {
        int value = Integer.parseInt(objValue.toString());
        /* Sprd: for bug 501208 @{ */
        Settings.System.putIntForUser(getContentResolver(),
                Settings.System.BUTTON_LIGHT_OFF_TIMEOUT, value, UserHandle.USER_OWNER);
        /* @} */
        updateTouchLightPreferenceSummary(value);
    }

    private void updateTouchLightPreferenceSummary(int value) {
        if (value == VALUE_KEYLIGHT_1500) {
            mTouchLightTimeoutPreference
                    .setSummary(mTouchLightTimeoutPreference.getEntries()[0]);
        } else if (value == VALUE_KEYLIGHT_6000) {
            mTouchLightTimeoutPreference
                    .setSummary(mTouchLightTimeoutPreference.getEntries()[1]);
        } else if (value == VALUE_KEYLIGHT_1) {
            mTouchLightTimeoutPreference
                    .setSummary(mTouchLightTimeoutPreference.getEntries()[2]);
        } else if (value == VALUE_KEYLIGHT_2) {
            mTouchLightTimeoutPreference
                    .setSummary(mTouchLightTimeoutPreference.getEntries()[3]);
        }
    }
    /* @}*/
    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }

    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();
        /* SPRD: add for unregisterRotationPolicyListener @{ */
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }
        /* @} */
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
		/*SUN:jicong.wang add for SUN_INCALL_FLASH start {@*/
        updateIncallFlashCheckbox();
        /*SUN;jicong.wang add for SUN_INCALL_FLASH end @}*/
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();

        // Update auto brightness if it is available.
        if (mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            mAutoBrightnessPreference.setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }

        // Update tap to wake if it is available.
        if (mTapToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, 0);
            mTapToWakePreference.setChecked(value != 0);
        }
        /* SPRD:add for 495179 press-brightness and screen timeout. @{ */
        // update screen timeout
        if (mScreenTimeoutPreference != null) {
            final long screenTimeout = Settings.System.getLong(getContentResolver(), SCREEN_OFF_TIMEOUT,
                    FALLBACK_SCREEN_TIMEOUT_VALUE);
            mScreenTimeoutPreference.setValue(String.valueOf(screenTimeout));
            updateTimeoutPreferenceDescription(screenTimeout);
        }
        // update touch light timeout
        if (mTouchLightTimeoutPreference != null) {
            /* Sprd: for bug 501208 @{ */
            final int touchlightcurrentTimeout = Settings.System.getIntForUser(getContentResolver(), Settings.System.BUTTON_LIGHT_OFF_TIMEOUT,
                    FALLBACK_TOUCH_LIGHT_TIMEOUT_VALUE, UserHandle.USER_OWNER);
            /* @} */
            mTouchLightTimeoutPreference.setValue(String.valueOf(touchlightcurrentTimeout));
            updateTouchLightPreferenceSummary(touchlightcurrentTimeout);
        }
        /* @}*/
        /* SPRD: for bug 544461 @{ */
        if (RotationPolicy.isRotationLockToggleVisible(getActivity())) {
            if (mRotatePreference != null) {
                mRotatePreference.setSelectedItem(RotationPolicy.isRotationLocked(getActivity()) ?
                        1 : 0);
            }
        }
        /* @}*/
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }
    /*SUN:jicong.wang add for SUN_INCALL_FLASH start {@*/
    private void updateIncallFlashCheckbox() {
        if (getActivity() == null) return;
        mIncallFlash.setChecked(SystemProperties.getBoolean("persist.sys.incallflash",false));
        Log.v(TAG,"dlj persist.sys.incallflash update"+SystemProperties.getBoolean("persist.sys.incallflash",false));
    }
	/*SUN:jicong.wang add for SUN_INCALL_FLASH end @}*/

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /* SPRD:add for press-brightness. @{ */
        if (preference == mTouchLightTimeoutPreference) {
            try {
                int britnessmode = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE);
                if (britnessmode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Toast.makeText(getActivity(),
                            getString(R.string.close_screen_bright_automode),
                            Toast.LENGTH_SHORT).show();
                    mTouchLightTimeoutPreference.getDialog().cancel();
                }
            } catch (Exception e) {
            }
		/*SUN:jicong.wang add for SUN_INCALL_FLASH start {@*/
		}else if (preference == mIncallFlash) {//dlj
            boolean old_state = SystemProperties.getBoolean("persist.sys.incallflash",false);
						String  new_state = String.valueOf(!old_state);
        		SystemProperties.set("persist.sys.incallflash",new_state);
        		Log.v(TAG,"dlj persist.sys.incallflash"+old_state);			
		/*SUN:jicong.wang add for SUN_INCALL_FLASH end @}*/          
        }
        /* @}*/
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        }
        if (preference == mTapToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, value ? 1 : 0);
        }
        if (preference == mNightModePreference) {
            try {
                final int value = Integer.parseInt((String) objValue);
                final UiModeManager uiManager = (UiModeManager) getSystemService(
                        Context.UI_MODE_SERVICE);
                uiManager.setNightMode(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }
        }
        /* SPRD:add for press-brightness. @{ */
        if (BUTTON_TOUCH_LIGHT_TIMEOUT.equals(key)) {
            writeTouchLightPreference(objValue);
        }
        /* @}*/
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!isAutomaticBrightnessAvailable(context.getResources())) {
                        result.add(KEY_AUTO_BRIGHTNESS);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE);
                    }
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    if (!isTapToWakeAvailable(context.getResources())) {
                        result.add(KEY_TAP_TO_WAKE);
                    }
                    /* Sprd: for bug 510399 @{ */
                    if (!mHasTouchLight) {
                        result.add(KEY_TOUCH_LIGHT);
                    }
                    /* @} */
                    return result;
                }
            };

            /* SPRD:add for press-brightness. @{ */
            private boolean fileIsExists(){
                try{
                    File file = new File("/sys/class/leds/keyboard-backlight/brightness");
                    Log.d(TAG, " fileIsExists");
                    if(!file.exists()){
                        Log.d(TAG, "fileIsExists false");
                        return false;
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    return false;
                }
                Log.d(TAG, "fileIsExists true");
                return true;
            }
            /* @} */
    /* SPRD: add for unregisterRotationPolicyListener @{ */
    @Override
    public void onPause() {
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }
        super.onPause();
    }
    /* @} */

}
