package com.android.settings;

import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.logging.MetricsLogger;

public class NavigationSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener{
    private static final String TAG = "NavigationSettings";
    private static final String KEY_NAVIGATION= "navigation_settings";
    private static final String START_HIDE_BAR_ACTION = "start.hide.bar.action";
    private static final String CLOSE_HIDE_BAR_ACTION = "close.hide.bar.action";
    private static final String HIDE_BAR_CLICKED = "hide_bar_clicked";
    private CheckBoxPreference mNavigationBar;
    private boolean ismNavigationBarSelected = true;
    
    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.navigation_settings);
        mNavigationBar = (CheckBoxPreference) findPreference(KEY_NAVIGATION);
		mNavigationBar.setChecked(Settings.System.getInt(getContentResolver(),Settings.System.NAVIGATION_SHOW_HIDEBUTTON, 0) != 0);
        mNavigationBar.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        ismNavigationBarSelected = mNavigationBar.isChecked();
        Log.d(TAG,"onPreferenceClick ismNavigationBarSelected: "+ismNavigationBarSelected);
        Settings.System.putInt(getContentResolver(),Settings.System.NAVIGATION_SHOW_HIDEBUTTON, ismNavigationBarSelected?1:0);
        if(ismNavigationBarSelected){
        Intent intent = new Intent(START_HIDE_BAR_ACTION);
        getActivity().sendBroadcast(intent);
        Log.d(TAG,"start");
        return true;
      	}else{
      	Intent intent = new Intent(CLOSE_HIDE_BAR_ACTION);
        getActivity().sendBroadcast(intent);
        Log.d(TAG,"close");
        return false;
      	}
    }
}
