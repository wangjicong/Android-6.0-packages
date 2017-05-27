
package com.sprd.engineermode.debuglog;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.sprd.engineermode.R;

public class AndroidUtilsActivity extends PreferenceActivity {

    public static final String KEY_SLIDE_SETTINGS = "key_slide_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_slide_settings);
        Preference slideSettingsPreference = (Preference) findPreference(KEY_SLIDE_SETTINGS);
    }

}
