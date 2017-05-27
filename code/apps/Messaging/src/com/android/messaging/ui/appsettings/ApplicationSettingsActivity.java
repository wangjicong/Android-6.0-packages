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
 */

package com.android.messaging.ui.appsettings;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import com.android.messaging.sms.SystemProperties;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.TwoStatePreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.text.Editable;
import android.text.Selection;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import java.util.List;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.SmsManager;
import android.content.Context;
import android.util.Log;

import com.android.messaging.R;
import com.android.messaging.sms.MmsConfig;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.LicenseActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.DebugUtils;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.sprd.messaging.sms.commonphrase.ui.PharserActivity;

public class ApplicationSettingsActivity extends BugleActionBarActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final boolean topLevel = getIntent().getBooleanExtra(
                UIIntents.UI_INTENT_EXTRA_TOP_LEVEL_SETTINGS, false);
        if (topLevel) {
            getSupportActionBar().setTitle(getString(R.string.settings_activity_title));
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(android.R.id.content, new ApplicationSettingsFragment());
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        case R.id.action_license:
            final Intent intent = new Intent(this, LicenseActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ApplicationSettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private String mNotificationsEnabledPreferenceKey;
        private TwoStatePreference mNotificationsEnabledPreference;
        private String mRingtonePreferenceKey;
        private RingtonePreference mRingtonePreference;
        private Preference mVibratePreference;
        private String mSmsDisabledPrefKey;
        private Preference mSmsDisabledPreference;
        private String mSmsEnabledPrefKey;
        private Preference mSmsEnabledPreference;
        /*Add by SPRD for Bug:533513  2016.03.10 Start */
        private String mSignatureEenablePreferenceKey;
        private TwoStatePreference mSignatureEnabledPreference;
        private boolean isSignatureEnable;
        /*Add by SPRD for Bug:533513  2016.03.10 End */
        /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
        private String mSignaturePrefKey;
        private EditTextPreference mSignaturePreference;
        /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */
        private boolean mIsSmsPreferenceClicked;

        /*Add by SPRD for Bug:562203 Encode Type feature  Start */
        private String mEncodeTypeSettingPreKey;
        private Preference mEncodeTypeSettingPreference;
        private String mEncodeTypePreKey;
        private ListPreference mEncodeTypePreference;
        public static final String SMS_ENCODE_TYPE = "persist.radio.sms_encode_type";
        /*Add by SPRD for Bug:562203 Encode Type feature  End */

        // sprd add for common message begin
        private Preference mCommonPhrasePreference;
        private String mCommonPhrasePrefKey;
        // sprd add for common message end
        private PreferenceScreen advancedScreen;
        
        public ApplicationSettingsFragment() {
            // Required empty constructor
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            getPreferenceManager().setSharedPreferencesName(BuglePrefs.SHARED_PREFERENCES_NAME);
            addPreferencesFromResource(R.xml.preferences_application);

            mNotificationsEnabledPreferenceKey =
                    getString(R.string.notifications_enabled_pref_key);
            mNotificationsEnabledPreference = (TwoStatePreference) findPreference(
                    mNotificationsEnabledPreferenceKey);
            mRingtonePreferenceKey = getString(R.string.notification_sound_pref_key);
            mRingtonePreference = (RingtonePreference) findPreference(mRingtonePreferenceKey);
            mVibratePreference = findPreference(
                    getString(R.string.notification_vibration_pref_key));
            /**
             *SPRD: Add for bug 509830 begin
             * hide the ringtone and vibrate setting when the config is not origin
             */
            if (!MmsConfig.getKeepOrgSoundVibrate()) {
                getPreferenceScreen().removePreference(mRingtonePreference);
                getPreferenceScreen().removePreference(mVibratePreference);
            }
            /* SPRD: Add for bug 509830 end */
            mSmsDisabledPrefKey = getString(R.string.sms_disabled_pref_key);
            mSmsDisabledPreference = findPreference(mSmsDisabledPrefKey);
            mSmsEnabledPrefKey = getString(R.string.sms_enabled_pref_key);
            mSmsEnabledPreference = findPreference(mSmsEnabledPrefKey);
            /*Add by SPRD for Bug:533513  2016.03.10 Start */
            mSignatureEenablePreferenceKey = getString(R.string.pref_key_signature_enable);
            mSignatureEnabledPreference = (TwoStatePreference)findPreference(mSignatureEenablePreferenceKey);
            /*Add by SPRD for Bug:533513  2016.03.10 End */
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
            mSignaturePrefKey = getString(R.string.signature_pref_key);
            mSignaturePreference = (EditTextPreference) findPreference(mSignaturePrefKey);
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */
            mIsSmsPreferenceClicked = false;
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
            /*Add by SPRD for Bug:562203 Encode Type feature  Start */
            mEncodeTypeSettingPreKey = getString(R.string.pref_key_encode_type_setting);
            mEncodeTypeSettingPreference = findPreference(mEncodeTypeSettingPreKey);
            mEncodeTypePreKey = getString(R.string.pref_key_encode_type);
            mEncodeTypePreference = (ListPreference)findPreference(mEncodeTypePreKey);
            String encodeType = mEncodeTypePreference.getValue();
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.setProperty(SMS_ENCODE_TYPE, encodeType);
            Log.d("ApplicationSettingsFragment"+"-encodeTypeLog", "ApplicationSettingsActivity encodeType:"+encodeType);
            /*Add by SPRD for Bug:562203 Encode Type feature  End */
            // sprd add for common message begin
            mCommonPhrasePrefKey = getString(R.string.phrase_pref_key);
            mCommonPhrasePreference = findPreference(mCommonPhrasePrefKey);
            // sprd add for common message end
            
             /*Modify by SPRD for Bug:533513  2016.03.10 Start */
            //if (!MmsConfig.getSignatureEnabled()) {//if signature disabled, hide the signature preference
            //    getPreferenceScreen().removePreference(mSignaturePreference);
            //} else {
            //    mSignaturePreference.getEditText()
            //                        .setFilters(new InputFilter[] {
            //                                    new InputFilter.LengthFilter(50)
            //                                    });
            //}
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */

            updateSignatureOnOrOff();
            mSignaturePreference.getEditText().setFilters(new InputFilter[] {
                    new InputFilter.LengthFilter(50)
                        });
            updateSignatureSammary();
            /*Modify by SPRD for Bug:533513  2016.03.10 End */

            final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            /* SPRD: Modify for bug 509830 begin */
            if (MmsConfig.getKeepOrgSoundVibrate()) {
                updateSoundSummary(prefs);
            }
            /* SPRD: Modify for bug 509830 end */

            if (!DebugUtils.isDebugEnabled()) {
                final Preference debugCategory = findPreference(getString(
                        R.string.debug_pref_key));
                getPreferenceScreen().removePreference(debugCategory);
            }
            advancedScreen = (PreferenceScreen) findPreference(getString(R.string.advanced_pref_key));
            if(!OsUtil.hasPhonePermission()){
                Log.d("ApplicationSettingsFragment",
                        "=======zhongjihao===onCreate===0====");
                OsUtil.requestMissingPermission(getActivity());
            }else{
                Log.d("ApplicationSettingsFragment",
                        "=======zhongjihao===onCreate===1====");
                List<SubscriptionInfo> sublist = SmsManager.getDefault()
                        .getActiveSubInfoList();

                if (sublist == null || sublist.size() == 0) {
                    Log.d("ApplicationSettingsFragment",
                            "=======zhongjihao===onCreate=======sim: "
                                    + ((sublist == null) ? -1 : sublist.size()));
                    advancedScreen.setEnabled(false);
                    advancedScreen.setShouldDisableView(true);
                }
            }

            final boolean topLevel = getActivity().getIntent().getBooleanExtra(
                    UIIntents.UI_INTENT_EXTRA_TOP_LEVEL_SETTINGS, false);
            if (topLevel) {
                advancedScreen.setIntent(UIIntents.get()
                        .getAdvancedSettingsIntent(getPreferenceScreen().getContext()));
            } else {
                // Hide the Advanced settings screen if this is not top-level; these are shown at
                // the parent SettingsActivity.
                getPreferenceScreen().removePreference(advancedScreen);
            }
        }

        @Override
        public boolean onPreferenceTreeClick (PreferenceScreen preferenceScreen,
                Preference preference) {
            if (preference.getKey() ==  mSmsDisabledPrefKey ||
                    preference.getKey() == mSmsEnabledPrefKey) {
                mIsSmsPreferenceClicked = true;
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
            } else if (MmsConfig.getSignatureEnabled() && preference.getKey() == mSignaturePrefKey) {
                EditText signEdit = mSignaturePreference.getEditText();
                Editable sEditAble = signEdit.getText();
                Selection.setSelection(sEditAble, sEditAble.length());
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */
            }
            // sprd add for common message begin
            else if (preference.getKey() == mCommonPhrasePrefKey) {
                Intent intent = new Intent(getActivity(), PharserActivity.class);
                startActivity(intent);
            }
            // sprd add for common message end
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        private void updateSoundSummary(final SharedPreferences sharedPreferences) {
            // The silent ringtone just returns an empty string
            String ringtoneName = mRingtonePreference.getContext().getString(
                    R.string.silent_ringtone);

            String ringtoneString = sharedPreferences.getString(mRingtonePreferenceKey, null);

            // Bootstrap the default setting in the preferences so that we have a valid selection
            // in the dialog the first time that the user opens it.
            if (ringtoneString == null) {
                ringtoneString = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(mRingtonePreferenceKey, ringtoneString);
                editor.apply();
            }

            if (!TextUtils.isEmpty(ringtoneString)) {
                final Uri ringtoneUri = Uri.parse(ringtoneString);
                final Ringtone tone = RingtoneManager.getRingtone(mRingtonePreference.getContext(),
                        ringtoneUri);

                if (tone != null) {
                    ringtoneName = tone.getTitle(mRingtonePreference.getContext());
                }
            }

            mRingtonePreference.setSummary(ringtoneName);
        }

        private void updateSmsEnabledPreferences() {
            if (!OsUtil.isAtLeastKLP()) {
                getPreferenceScreen().removePreference(mSmsDisabledPreference);
                getPreferenceScreen().removePreference(mSmsEnabledPreference);
            } else {
                final String defaultSmsAppLabel = getString(R.string.default_sms_app,
                        PhoneUtils.getDefault().getDefaultSmsAppLabel());
                boolean isSmsEnabledBeforeState;
                boolean isSmsEnabledCurrentState;
                if (PhoneUtils.getDefault().isDefaultSmsApp()) {
                    if (getPreferenceScreen().findPreference(mSmsEnabledPrefKey) == null) {
                        getPreferenceScreen().addPreference(mSmsEnabledPreference);
                        isSmsEnabledBeforeState = false;
                    } else {
                        isSmsEnabledBeforeState = true;
                    }
                    isSmsEnabledCurrentState = true;
                    getPreferenceScreen().removePreference(mSmsDisabledPreference);
                    mSmsEnabledPreference.setSummary(defaultSmsAppLabel);
                } else {
                    if (getPreferenceScreen().findPreference(mSmsDisabledPrefKey) == null) {
                        getPreferenceScreen().addPreference(mSmsDisabledPreference);
                        isSmsEnabledBeforeState = true;
                    } else {
                        isSmsEnabledBeforeState = false;
                    }
                    isSmsEnabledCurrentState = false;
                    getPreferenceScreen().removePreference(mSmsEnabledPreference);
                    mSmsDisabledPreference.setSummary(defaultSmsAppLabel);
                }
                updateNotificationsPreferences();
            }
            mIsSmsPreferenceClicked = false;
        }

        private void updateNotificationsPreferences() {
            final boolean canNotify = !OsUtil.isAtLeastKLP()
                    || PhoneUtils.getDefault().isDefaultSmsApp();
            mNotificationsEnabledPreference.setEnabled(canNotify);
        }

        /*Add by SPRD for Bug:533513  2016.03.10 Start */
        private void updateSignatureOnOrOff() {
            isSignatureEnable = mSignatureEnabledPreference.isChecked();
            mSignatureEnabledPreference.setChecked(isSignatureEnable);
            MmsConfig.setSignatureEnable(isSignatureEnable);
        }
        /*Add by SPRD for Bug:533513  2016.03.10 End */

        /*Modify by SPRD for Bug:533513  2016.03.10 Start */
        /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
        private void updateSignatureSammary() {
            //mSignaturePreference.setSummary(mSignaturePreference.getText());
            String defaultSummary = getString(R.string.pref_signature_input_summary);
            if("".equals(mSignaturePreference.getText()) || "" == mSignaturePreference.getText()) {
                mSignaturePreference.setSummary(defaultSummary);
            } else {
                mSignaturePreference.setSummary(mSignaturePreference.getText());
            }
        }
        /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */
        /*Modify by SPRD for Bug:533513  2016.03.10 End */

        /*Add by SPRD for Bug:562203 Encode Type feature  Start */
        private void updateEncodeTypePreference() {
            SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(this.getActivity());
            if (mEncodeTypePreference != null) {
                String encodeType = sharedPref.getString(mEncodeTypePreKey, "0");
                mEncodeTypePreference.setValue(encodeType);
                mEncodeTypePreference.setSummary(mEncodeTypePreference.getEntry());
                mEncodeTypePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        // TODO Auto-generated method stub
                        SharedPreferences.Editor prefEditor = PreferenceManager
                                .getDefaultSharedPreferences(mEncodeTypePreference.getContext()).edit();
                        prefEditor.putString(mEncodeTypePreference.getContext().
                                getString(R.string.pref_key_encode_type), newValue.toString());
                        Log.d("ApplicationSettingsFragment"+"-encodeTypeLog","updateEncodeTypePreference  encodeType:"+newValue.toString());
                        mEncodeTypePreference.setValue(newValue.toString());
                        mEncodeTypePreference.setSummary(mEncodeTypePreference.getEntry());
                        MmsConfig.setEncodeType(newValue.toString());
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.setProperty(SMS_ENCODE_TYPE, newValue.toString());
                        prefEditor.apply();
                        return true;
                    }
               });
            }
        }
        /*Add by SPRD for Bug:562203 Encode Type feature  End */

        @Override
        public void onStart() {
            super.onStart();
            // We do this on start rather than on resume because the sound picker is in a
            // separate activity.
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            updateSmsEnabledPreferences();
            updateNotificationsPreferences();
            updateEncodeTypePreference();//Add by SPRD for Bug:562203 Encode Type feature
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
            if (MmsConfig.getSignatureEnabled()) {
                updateSignatureSammary();
            }
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */
            if(!OsUtil.hasPhonePermission()){
                Log.d("ApplicationSettingsFragment",
                        "=======zhongjihao===onResume===0====");
                OsUtil.requestMissingPermission(getActivity());
            }else{
                Log.d("ApplicationSettingsFragment",
                        "=======zhongjihao===onResume===1====");
                List<SubscriptionInfo> sublist = SmsManager.getDefault()
                        .getActiveSubInfoList();
                if (sublist == null || sublist.size() == 0) {
                    Log.d("ApplicationSettingsFragment",
                            "=======zhongjihao===onResume=======sim: "
                                    + ((sublist == null) ? -1 : sublist.size()));
                    advancedScreen.setEnabled(false);
                    advancedScreen.setShouldDisableView(true);
                }else{
                    advancedScreen.setEnabled(true);
                    advancedScreen.setShouldDisableView(true);
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
                final String key) {
            /*Add by SPRD for Bug:533513  2016.03.10 Start */
            if(key.equals(mSignatureEenablePreferenceKey)){
                updateSignatureOnOrOff();
            }
            /*Add by SPRD for Bug:533513  2016.03.10 End */

            /*Add by SPRD for Bug:562203 Encode Type feature  Start */
            if (key.equals(mEncodeTypePreference)) {
                updateEncodeTypePreference();
                Log.d("ApplicationSettingsFragment"+"-encodeTypeLog", "ApplicationSettingsActivity onSharedPreferenceChanged and encodeType has been changed!");
            }
            /*Add by SPRD for Bug:562203 Encode Type feature  End */
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. begin */
            if(MmsConfig.getSignatureEnabled()&& key.equals(mSignaturePrefKey)) {
                updateSignatureSammary();
            /*SPRD: add for Bug 489231--new feature,enable signature text append to a message. end */
            /* SPRD: Modify for bug 509830 begin */
            }else if (key.equals(mNotificationsEnabledPreferenceKey)) {
                updateNotificationsPreferences();
            } else if (MmsConfig.getKeepOrgSoundVibrate() && key.equals(mRingtonePreferenceKey)) {
                updateSoundSummary(sharedPreferences);
            /* SPRD: Modify for bug 509830 end */
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}
