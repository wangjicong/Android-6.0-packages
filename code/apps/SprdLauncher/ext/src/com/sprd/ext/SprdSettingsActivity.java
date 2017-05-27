/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * SPRD_SETTINGS_ACTIVITY_SUPPORT
 */

package com.sprd.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.android.sprdlauncher3.LauncherSettings;
import com.android.sprdlauncher3.R;
import com.sprd.ext.circular.CircleSlideUtils;
import com.sprd.ext.dynamicIcon.DynamicIconSettings;
import com.sprd.ext.folder.FolderIconController;
import com.sprd.ext.unreadnotifier.UnreadActivity;

/**
 * Created by SPRD on 11/8/16.
 *
 * SPRD Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SprdSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SprdLauncherSettingsFragment())
                .commit();
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class SprdLauncherSettingsFragment extends PreferenceFragment
            implements OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
        private static final String PRE_KEY_UNREAD = "pref_unreadNotifier";
        private static final String PRE_KEY_DYNAMICICON = "pref_dynamicIcon";
        ListPreference mFolderPref;
        Context mContext;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.sprdlauncher_settings_preferences);
            mContext = getActivity().getApplicationContext();

            //TODO add your Preference and listener
            //foldericon preference
            mFolderPref = (ListPreference) findPreference(FolderIconController.FOLDER_MODEL_KEY);
            if (FeatureOption.SPRD_FOLDER_MODEL_SUPPORT){
                String modelValue = UtilitiesExt.getLauncherSettingsString(getActivity(),
                        FolderIconController.FOLDER_MODEL_KEY,
                        mContext.getResources().getString(R.string.default_folder_model));
                mFolderPref.setValue(modelValue);
                setFolderPrefSummary(modelValue);
                mFolderPref.setOnPreferenceChangeListener(this);
            }else{
                getPreferenceScreen().removePreference(mFolderPref);
            }

            //SPRD: add for unreadNotifier function start
            Preference unreadPref = findPreference(PRE_KEY_UNREAD);
            unreadPref.setOnPreferenceClickListener(this);
            if(!FeatureOption.SPRD_UNREAD_INFO_SUPPORT) {
                getPreferenceScreen().removePreference(unreadPref);
            }
            //SPRD: add for unreadNotifier function end

            SwitchPreference cyclePref = (SwitchPreference) findPreference(
                    CircleSlideUtils.PREF_CIRCLE_SLIDE_KEY);

            if (FeatureOption.SPRD_CIRCLE_SLIDE_SUPPORT && !CircleSlideUtils.hasCustomContent(mContext)) {
                boolean cycleValue = CircleSlideUtils.isCircleSlideEnabled(mContext);
                cyclePref.setChecked(cycleValue);
                cyclePref.setOnPreferenceChangeListener(this);
            } else {
                // when CustomContent is enabled, Circle slide Feature need close
                getPreferenceScreen().removePreference(cyclePref);
            }

            //SPRD: add for dynamic icon feature start
            Preference dynamicIconPref = findPreference(PRE_KEY_DYNAMICICON);
            boolean hide = getActivity().getResources().getBoolean(R.bool.config_hide_dynamic_icon_settings);
            if (FeatureOption.SPRD_DYNAMIC_ICON_SUPPORT) {
                if (hide || !isAnyDynamicIconFeatureSupport()) {
                    getPreferenceScreen().removePreference(dynamicIconPref);
                }
            } else {
                getPreferenceScreen().removePreference(dynamicIconPref);
            }

            if (dynamicIconPref != null) {
                dynamicIconPref.setOnPreferenceClickListener(this);
            }
            //SPRD: add for dynamic icon feature end
        }

        private boolean isAnyDynamicIconFeatureSupport() {
            return FeatureOption.SPRD_DYNAMIC_CALENDAR_SUPPORT || FeatureOption.SPRD_DYNAMIC_CLOCK_SUPPORT;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            //You can add CheckboxPreference here if need,I think boolean and String type is enough!
            if(preference instanceof SwitchPreference) {
                Bundle extras = new Bundle();
                extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE, (Boolean) newValue);
                getActivity().getContentResolver().call(
                        LauncherSettings.Settings.CONTENT_URI,
                        LauncherSettings.Settings.METHOD_SET_BOOLEAN,
                        preference.getKey(), extras);
            }else if(preference instanceof ListPreference){
                Bundle extras = new Bundle();
                extras.putString(LauncherSettings.Settings.EXTRA_VALUE, (String) newValue);
                getActivity().getContentResolver().call(
                        LauncherSettings.Settings.CONTENT_URI,
                        LauncherSettings.Settings.METHOD_SET_STRING,
                        preference.getKey(), extras);
                if( FolderIconController.FOLDER_MODEL_KEY.equals(preference.getKey()) ){
                    setFolderPrefSummary((String) newValue);
                }
            }
            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            if(key.equals(PRE_KEY_UNREAD)) {
                // start UnreadActivity
                Intent unreadIntent = new Intent(getActivity(), UnreadActivity.class);
                startActivity(unreadIntent);
            } else if (PRE_KEY_DYNAMICICON.equals(key)) {
                Intent dynamicIconIntent = new Intent(getActivity(), DynamicIconSettings.class);
                startActivity(dynamicIconIntent);
            }
            return false;
        }

        // set summary of folder perfence
        private void setFolderPrefSummary(String value){
            CharSequence[] entries = mFolderPref.getEntries();
            int index = mFolderPref.findIndexOfValue(value);
            mFolderPref.setSummary(entries[index]);
        }
    }
}
