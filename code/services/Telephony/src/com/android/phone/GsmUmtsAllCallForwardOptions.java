/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import android.telephony.TelephonyManager;
import com.android.internal.telephony.PhoneConstants;

public class GsmUmtsAllCallForwardOptions extends PreferenceActivity {
    private static final String LOG_TAG = "GsmUmtsAllCallForwardOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String VIDEO_CALL_FORWARDING_KEY = "video_call_forwarding_key";
    private static final String AUDIO_CALL_FORWARDING_KEY = "audio_call_forwarding_key";
    // SPRD: modify for bug544979
    private SubscriptionInfoHelper mSubscriptionInfoHelper;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.gsm_umts_all_call_forward_options);

        /* SPRD: add for bug544979 @{ */
        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        // SPRD: modify for bug544089
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.labelCF);
        init(getPreferenceScreen(), mSubscriptionInfoHelper);

        if (mSubscriptionInfoHelper.getPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_GSM) {
            //disable the entire screen
            getPreferenceScreen().setEnabled(false);
        }
        /* @} */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // SPRD: modify for bug544979
            CallFeaturesSetting.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* SPRD: add for bug544979 @{ */
    @Override
    public void onBackPressed() {
        CallFeaturesSetting.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
    }
    /* @} */

    public void init(PreferenceScreen prefScreen, SubscriptionInfoHelper subInfoHelper) {
        Preference callForwardingPref = prefScreen.findPreference(AUDIO_CALL_FORWARDING_KEY);
        callForwardingPref.setIntent(subInfoHelper.getIntent(GsmUmtsCallForwardOptions.class));
        Preference videoCallForwardingPref = prefScreen.findPreference(VIDEO_CALL_FORWARDING_KEY);
        videoCallForwardingPref
                .setIntent(subInfoHelper.getIntent(GsmUmtsVideoCallForwardOptions.class));
        TelephonyManager telephonyManager = (TelephonyManager)
                this.getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager.getPrimaryCard() != subInfoHelper.getPhone().getPhoneId()) {
            videoCallForwardingPref.setEnabled(false);
        }
    }

    /* SPRD: add for bug544979 @{ */
    public static void goUpToTopLevelSetting(
            Activity activity, SubscriptionInfoHelper subscriptionInfoHelper) {
        Intent intent = subscriptionInfoHelper.getIntent(GsmUmtsAllCallForwardOptions.class);
        activity.startActivity(intent);
        activity.finish();
    }
    /* @} */
}
