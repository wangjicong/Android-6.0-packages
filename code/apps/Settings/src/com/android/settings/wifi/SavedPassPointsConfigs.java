/*
 * Copyright (C) 2015 SPRD Passpoint R1 Feature
 */

package com.android.settings.wifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import android.util.Log;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI to manage saved networks/access points.
 */
public class SavedPassPointsConfigs extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener, Indexable {
    private static final String TAG = "SavedPassPointsConfigs";

    private WifiManager mWifiManager;
    private Preference mSelectedPreference;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.wifi_display_saved_passpoints);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    private void initPreferences() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        final Context context = getActivity();

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final List<WifiConfiguration> passPoints = mWifiManager.getPasspointConfiguredCreds();
        preferenceScreen.removeAll();
        if (passPoints == null) {
            Log.w(TAG, "passPoints is NULL!");
            return;
        }
        final int passPointsSize = passPoints.size();
        for (int i = 0; i < passPointsSize; ++i){
            final Preference passPointspreference = new Preference(context);
            String showConfigInfo = passPoints.get(i).configKey() + "\n" + passPoints.get(i).enterpriseConfig.toString();
            passPointspreference.setTitle(passPoints.get(i).passpointCredIdentifier);
            passPointspreference.setSummary(showConfigInfo);
            preferenceScreen.addPreference(passPointspreference);
        }

        if(getPreferenceScreen().getPreferenceCount() < 1) {
            Log.w(TAG, "Saved networks activity loaded, but there are no saved networks!");
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == AlertDialog.BUTTON_POSITIVE && mSelectedPreference != null) {
            mWifiManager.removePasspointCred(mSelectedPreference.getTitle().toString());
            getPreferenceScreen().removePreference(mSelectedPreference);
            mSelectedPreference = null;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
            mSelectedPreference = preference;
            new AlertDialog.Builder(getActivity()).setTitle(R.string.wifi_saved_passpoint_config)
            .setMessage(mSelectedPreference.getTitle())
            .setPositiveButton(R.string.passpoint_config_forget, this)
            .setNegativeButton(R.string.passpoint_config_cancel,this).show();

            return true;
    }

    /**
     * For search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();
                final String title = res.getString(R.string.wifi_saved_passpoint_titlebar);

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = title;
                data.screenTitle = title;
                data.enabled = enabled;
                result.add(data);

                return result;
            }
        };
	@Override
	protected int getMetricsCategory() {
		// TODO Auto-generated method stub
		return MetricsLogger.WIFI_PASSPOINT_SAVE_CONFIG;
	}
}
