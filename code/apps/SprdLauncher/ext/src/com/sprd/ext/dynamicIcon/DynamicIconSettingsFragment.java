package com.sprd.ext.dynamicIcon;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.text.TextUtils;

import com.android.sprdlauncher3.LauncherSettings;
import com.android.sprdlauncher3.R;
import com.sprd.ext.FeatureOption;
import com.sprd.ext.UtilitiesExt;

/**
 * This fragment shows the dynamic icon setting preferences.
 */
public class DynamicIconSettingsFragment  extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String PRE_DYNAMIC_CALENDAR = "pref_calendar";
    public static final String PRE_DYNAMIC_CLOCK = "pref_clock";

    private DynamicIcon mDynamicCalendar;
    private DynamicIcon mDynamicDeskclock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dynamic_icon_settings);

        DynamicIconUtils utils = DynamicIconUtils.getInstance(getActivity());

        SwitchPreference calendarPre = (SwitchPreference) findPreference(PRE_DYNAMIC_CALENDAR);
        if (FeatureOption.SPRD_DYNAMIC_CALENDAR_SUPPORT) {
            mDynamicCalendar = utils.getDynamicIconByType(DynamicIconUtils.DYNAMIC_CALENDAR_TYPE);
            if (mDynamicCalendar != null) {
                boolean isValidCal = UtilitiesExt.isAppInstalled(getActivity(), mDynamicCalendar.getComponentName());
                if (isValidCal) {
                    calendarPre.setChecked(mDynamicCalendar.isCheckedState());
                    CharSequence calendarCh = mDynamicCalendar.getAppLabel();
                    if (!TextUtils.isEmpty(calendarCh)) {
                        calendarPre.setTitle(calendarCh);
                    }
                    calendarPre.setOnPreferenceChangeListener(this);
                } else {
                    calendarPre.setEnabled(false);
                    calendarPre.setSelectable(false);
                }
            } else {
                getPreferenceScreen().removePreference(calendarPre);
            }
        } else {
            getPreferenceScreen().removePreference(calendarPre);
        }

        SwitchPreference clockPre = (SwitchPreference) findPreference(PRE_DYNAMIC_CLOCK);
        if (FeatureOption.SPRD_DYNAMIC_CLOCK_SUPPORT) {
            mDynamicDeskclock = utils.getDynamicIconByType(DynamicIconUtils.DYNAMIC_CLOCK_TYPE);
            if (mDynamicDeskclock != null) {
                boolean isValidClock = UtilitiesExt.isAppInstalled(getActivity(), mDynamicDeskclock.getComponentName());
                if (isValidClock) {
                    clockPre.setChecked(mDynamicDeskclock.isCheckedState());
                    CharSequence clockCh = mDynamicDeskclock.getAppLabel();
                    if (!TextUtils.isEmpty(clockCh)) {
                        clockPre.setTitle(clockCh);
                    }
                clockPre.setOnPreferenceChangeListener(this);
            } else {
                clockPre.setEnabled(false);
                clockPre.setSelectable(false);
                }
            } else {
                getPreferenceScreen().removePreference(clockPre);
            }
        } else {
            getPreferenceScreen().removePreference(clockPre);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        Bundle extras = new Bundle();
        extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE, (Boolean) newValue);
        getActivity().getContentResolver().call(
                LauncherSettings.Settings.CONTENT_URI,
                LauncherSettings.Settings.METHOD_SET_BOOLEAN,
                preference.getKey(), extras);
        if (key.equals(PRE_DYNAMIC_CALENDAR)) {
                if (mDynamicCalendar != null) {
                    mDynamicCalendar.setCheckedState((Boolean) newValue);
                }
            } else if (key.equals(PRE_DYNAMIC_CLOCK)) {
                if (mDynamicDeskclock != null) {
                    mDynamicDeskclock.setCheckedState((Boolean) newValue);
                }
            }
        return true;
    }
}
