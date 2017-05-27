/** Created by Spreadst */
package com.android.settings;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.util.Log;
import android.net.wifi.WifiManager;

public class NetworkManagementSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "NetworkManagementSettings";

    private static final String KEY_NETWORK_TYPE_SELECT = "network_type_select";

    private ConnectivityManager mConnectivity;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.network_management_settings);
        mConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initNetworkTpyPreference();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_NETWORK_TYPE_SELECT.equals(key)) {
            int value = Integer.parseInt(((String) newValue));
            if (value != Settings.Secure.getInt(getContentResolver(),
                    Settings.Global.NETWORK_PREFERENCE, 1)) {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Global.NETWORK_PREFERENCE, value);
            }
        }
        return true;
    }

    private void initNetworkTpyPreference() {
        ListPreference pref = (ListPreference) findPreference(KEY_NETWORK_TYPE_SELECT);
        pref.setOnPreferenceChangeListener(this);
        int mNerworkPreferenceType = Settings.Secure.getInt(getContentResolver(),
                Settings.Global.NETWORK_PREFERENCE, 1); // wifi value is 1, and mobile data value is
        pref.setValueIndex(mNerworkPreferenceType);
    }
}