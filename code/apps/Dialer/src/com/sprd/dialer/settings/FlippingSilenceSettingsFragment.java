package com.sprd.dialer.settings;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.dialer.R;

public class FlippingSilenceSettingsFragment extends PreferenceFragment {

    private static final String BUTTON_FLIPPINGL_SILENCE = "incomingcall_flipping_silence_key";
    private CheckBoxPreference mFlippingToSilence;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.incomingcall_flipping_silence_settings_ex);

        Context context = getActivity();

        mFlippingToSilence = (CheckBoxPreference) findPreference(BUTTON_FLIPPINGL_SILENCE);
        mFlippingToSilence.setChecked(Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.FLIPPING_SILENCE_DATA, 0) != 0);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mFlippingToSilence) {
            Settings.Global.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.Global.FLIPPING_SILENCE_DATA, mFlippingToSilence.isChecked() ? 1 : 0);
            return true;
        }
        return true;
    }
}
