package com.android.settings;

import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;

public class SmartWakeSetting extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener{

	private static final String TAG = "SmartWakeSetting";
	private CheckBoxPreference smartWakeSwitch;
	private ListPreference preferenceChangeMusic;
	private ListPreference preferenceDoubleClick;
	private ListPreference preferenceUp;
	private ListPreference preferenceDown;
	private ListPreference preferenceC;
	private ListPreference preferenceE;
	private ListPreference preferenceM;
	private ListPreference preferenceO;
	private ListPreference preferenceS;
	private ListPreference preferenceV;
	private ListPreference preferenceW;
	private ListPreference preferenceZ;
	private boolean smartWakeEnable = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.smartwake_setting);

		smartWakeSwitch = (CheckBoxPreference) findPreference("gesture_smartwake");
		String smartValue = Settings.Global.getString(getContentResolver(), Settings.Global.GESTUREACTION_SWITCH);
		if(smartValue.equals("off")){
			 smartWakeEnable = false;
		}
		smartWakeSwitch.setChecked(smartWakeEnable);
		preferenceChangeMusic = initListPreference("smartwake_changemusic",Settings.Global.GESTUREACTION_CHANGE_MUSIC);
		preferenceDoubleClick = initListPreference("smartwake_doubleclick",Settings.Global.GESTUREACTION_DOUBLECLICK);
		preferenceUp = initListPreference("smartwake_up",Settings.Global.GESTUREACTION_UP);
		preferenceDown = initListPreference("smartwake_down",Settings.Global.GESTUREACTION_DOWN);
		preferenceC = initListPreference("smartwake_c",Settings.Global.GESTUREACTION_C);
		preferenceE = initListPreference("smartwake_e",Settings.Global.GESTUREACTION_E);
		preferenceM = initListPreference("smartwake_m",Settings.Global.GESTUREACTION_M);
		preferenceO = initListPreference("smartwake_o",Settings.Global.GESTUREACTION_O);
		preferenceS = initListPreference("smartwake_s",Settings.Global.GESTUREACTION_S);
		preferenceV = initListPreference("smartwake_v",Settings.Global.GESTUREACTION_V);
		preferenceW = initListPreference("smartwake_w",Settings.Global.GESTUREACTION_W);
		preferenceZ = initListPreference("smartwake_z",Settings.Global.GESTUREACTION_Z);
		setListPreferenceEnable(smartWakeEnable);
	}
	
	 @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SMART_WAKE;
    }

	private ListPreference initListPreference(String keyPreference, String globalValue){
		ListPreference listPreference = (ListPreference) findPreference(keyPreference);
		String smartValue = Settings.Global.getString(getContentResolver(), globalValue);

		if(smartValue.equals("ignore")){//hide
			getPreferenceScreen().removePreference(listPreference);
		}else{
			listPreference.setValue(smartValue);
			listPreference.setSummary(listPreference.getEntry());
			listPreference.setOnPreferenceChangeListener(this);
		}

		return listPreference;
	}

	private void setListPreferenceEnable(boolean enable)
	{
		preferenceChangeMusic.setEnabled(enable);
		preferenceDoubleClick.setEnabled(enable);
		preferenceUp.setEnabled(enable);
		preferenceDown.setEnabled(enable);
		preferenceC.setEnabled(enable);
		preferenceE.setEnabled(enable);
		preferenceM.setEnabled(enable);
		preferenceO.setEnabled(enable);
		preferenceS.setEnabled(enable);
		preferenceV.setEnabled(enable);
		preferenceW.setEnabled(enable);
		preferenceZ.setEnabled(enable);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object objValue) {
		final String valueString = objValue.toString();
		ContentResolver cr = getContentResolver();
		if(preference instanceof ListPreference){
			ListPreference listPreference=(ListPreference)preference;
			CharSequence[] entries = listPreference.getEntries();
			int index = listPreference.findIndexOfValue(valueString);
			listPreference.setSummary(entries[index]);
		}

		switch(preference.getKey()){
			case "smartwake_changemusic": {
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_CHANGE_MUSIC,valueString);
			}break;

			case "smartwake_doubleclick":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_DOUBLECLICK,valueString);
			}break;

			case "smartwake_up":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_UP,valueString);
			}break;

			case "smartwake_down":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_DOWN,valueString);
			}break;

			case "smartwake_c":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_C,valueString);
			}break;

			case "smartwake_e":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_E,valueString);
			}break;

			case "smartwake_m":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_M,valueString);
			}break;

			case "smartwake_o":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_O,valueString);
			}break;

			case "smartwake_s":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_S,valueString);
			}break;

			case "smartwake_v":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_V,valueString);
			}break;

			case "smartwake_w":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_W,valueString);
			}break;

			case "smartwake_z":{
				Settings.Global.putString(cr,Settings.Global.GESTUREACTION_Z,valueString);
			}break;

			default:
				return false;
		}
		return true;
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference == (Preference)smartWakeSwitch){
			smartWakeEnable = smartWakeSwitch.isChecked();
			String smartWakeString = smartWakeEnable?"on":"off";
			Settings.Global.putString(getContentResolver(),Settings.Global.GESTUREACTION_SWITCH,smartWakeString);
			setListPreferenceEnable(smartWakeEnable);
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}
