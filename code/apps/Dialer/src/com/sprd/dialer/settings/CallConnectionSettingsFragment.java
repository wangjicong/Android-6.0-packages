package com.sprd.dialer.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.android.dialer.R;
import android.provider.Settings;//SUN:jicong.wang
import android.preference.CheckBoxPreference;//SUN:jicong.wang
import android.preference.PreferenceScreen;//SUN:jicong.wang
import android.preference.Preference;//SUN:jicong.wang

public class CallConnectionSettingsFragment extends PreferenceFragment {
    /*SUN:jicong.wang add for vibrate start {@*/
    private CheckBoxPreference mVibrateCallConnection;
    /*SUN:jicong.wang add for vibrate end @}*/
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.call_connection_prompt_settings_ex);
        /*SUN:jicong.wang add for vibrate start {@*/
        mVibrateCallConnection = (CheckBoxPreference) findPreference("call_connection_prompt_key");
        mVibrateCallConnection.setChecked(Settings.System.getInt(getActivity().getContentResolver(),Settings.System.VIBRATE_WHEN_CALL_CONNECTION, 0)==1?true:false);        
        /*SUN:jicong.wang add for vibrate end @}*/
    }

    /*SUN:jicong.wang add for vibrate start {@*/
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mVibrateCallConnection) {
            Settings.System.putInt(getActivity().getContentResolver(),Settings.System.VIBRATE_WHEN_CALL_CONNECTION, mVibrateCallConnection.isChecked()?1:0);
            return true;
        }
        return true;
    }  
    /*SUN:jicong.wang add for vibrate end @}*/
}
