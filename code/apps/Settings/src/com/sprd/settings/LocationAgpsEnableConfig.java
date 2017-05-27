/** Created by Spreadst */

package com.sprd.settings;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.location.RadioButtonPreference;

public class LocationAgpsEnableConfig extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ENABLE_AGPS_REGISTERED = "enable_agps_registered";
    private static final String KEY_ENABLE_AGPS_ALL = "enable_agps_all";
    private static final String KEY_ENABLE_AGPS_NONE = "enable_agps_none";
    private static final String KEY_DEFAULT_SET = "default_set_button";

    // SPRD: MODIFY change the CheckBox into RadioButton
    private RadioButtonPreference mEnableAgpsRegistered;
    private RadioButtonPreference mEnableAgpsAll;
    private RadioButtonPreference mEnableAgpsNone;
    private ContentResolver resolver;
    private Preference mDefaultSet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolver = getActivity().getContentResolver();
        addPreferencesFromResource(R.xml.agps_enable_config);
        // SPRD: MODIFY change the CheckBox into RadioButton
        mEnableAgpsRegistered = (RadioButtonPreference) findPreference(KEY_ENABLE_AGPS_REGISTERED);
        mEnableAgpsAll = (RadioButtonPreference) findPreference(KEY_ENABLE_AGPS_ALL);
        mEnableAgpsNone = (RadioButtonPreference) findPreference(KEY_ENABLE_AGPS_NONE);
        mDefaultSet = findPreference(KEY_DEFAULT_SET);
        mDefaultSet.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!mEnableAgpsRegistered.isChecked()) {
                    mEnableAgpsRegistered.setChecked(true);
                    mEnableAgpsAll.setChecked(false);
                    mEnableAgpsNone.setChecked(false);
                    Settings.Secure.putInt(resolver, "assisted_gps_enable_option", 0);
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int tmp = Settings.Secure.getInt(resolver, "assisted_gps_enable_option", 0);
        if (tmp == 0) {
            mEnableAgpsRegistered.setChecked(true);
            /* SPRD: Add for bug457629. @{ */
            if (mEnableAgpsAll != null && mEnableAgpsNone != null) {
                mEnableAgpsAll.setChecked(false);
                mEnableAgpsNone.setChecked(false);
            }
            /* @} */
        } else if (tmp == 1) {
            mEnableAgpsAll.setChecked(true);
            /* SPRD: Add for bug457629. @{ */
            if (mEnableAgpsRegistered != null && mEnableAgpsNone != null) {
                mEnableAgpsRegistered.setChecked(false);
                mEnableAgpsNone.setChecked(false);
            }
            /* @} */
        } else if (tmp == 2) {
            mEnableAgpsNone.setChecked(true);
            /* SPRD: Add for bug457629. @{ */
            if (mEnableAgpsAll != null && mEnableAgpsRegistered != null) {
                mEnableAgpsAll.setChecked(false);
                mEnableAgpsRegistered.setChecked(false);
            }
            /* @} */
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mEnableAgpsRegistered) {
            mEnableAgpsRegistered.setChecked(true);
            mEnableAgpsAll.setChecked(false);
            mEnableAgpsNone.setChecked(false);
            Settings.Secure.putInt(resolver, "assisted_gps_enable_option", 0);
        } else if (preference == mEnableAgpsAll) {
            mEnableAgpsAll.setChecked(true);
            mEnableAgpsRegistered.setChecked(false);
            mEnableAgpsNone.setChecked(false);
            Settings.Secure.putInt(resolver, "assisted_gps_enable_option", 1);
        } else if (preference == mEnableAgpsNone) {
            mEnableAgpsNone.setChecked(true);
            mEnableAgpsAll.setChecked(false);
            mEnableAgpsRegistered.setChecked(false);
            Settings.Secure.putInt(resolver, "assisted_gps_enable_option", 2);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.LOCATION;
    }

}
