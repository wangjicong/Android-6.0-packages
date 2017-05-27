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

package com.android.messaging.receiver;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.android.messaging.BugleApplication;
import com.android.messaging.Factory;
import com.android.messaging.datamodel.action.UpdateMessageNotificationAction;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.BuglePrefsKeys;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.OsUtil;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.R;
import com.android.sprd.telephony.RadioInteractor;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.PhoneConstants;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Receives notification of boot completion and package replacement
 */
public class BootAndPackageReplacedReceiver extends BroadcastReceiver {
    //sprd #542214 start
	private static int simReadyStatus = 0;
	private static int mPhoneId = -1;
	private static final int BOOT_STATUS_BIT_0 = 0x00000001;
	private static final int BOOT_STATUS_BIT_1 = 0x00000010;
	private static final int SIM_STATUS_BIT_2 = 0x00000100;
    private static final int SIM_STATUS_BIT_3 = 0x00001000;
	//sprd #542214 end
    @Override
    public void onReceive(final Context context, final Intent intent) {
        LogUtil.i("PerSubscriptionSettingsActivity",
                "onReceive intent.getAction() = " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            // Repost unseen notifications
            Factory.get().getApplicationPrefs().putLong(
                    BuglePrefsKeys.LATEST_NOTIFICATION_MESSAGE_TIMESTAMP, Long.MIN_VALUE);
            UpdateMessageNotificationAction.updateMessageNotification();
            BugleApplication.updateAppConfig(context);
            // sprd #542214 start
            simReadyStatus = simReadyStatus | BOOT_STATUS_BIT_0
                    | BOOT_STATUS_BIT_1;
            if ((simReadyStatus & SIM_STATUS_BIT_2) == SIM_STATUS_BIT_2) {
                simReadyStatus = simReadyStatus & (~SIM_STATUS_BIT_2);
                setSimDefaultSatus(context, 0);
                LogUtil.i("PerSubscriptionSettingsActivity",
                        "ACTION_BOOT_COMPLETED mPhoneId = " + mPhoneId
                                + " setSimDefaultSatus(context, 0); ");
            }
            if ((simReadyStatus & SIM_STATUS_BIT_3) == SIM_STATUS_BIT_3) {
                simReadyStatus = simReadyStatus & (~SIM_STATUS_BIT_3);
                setSimDefaultSatus(context, 1);
                LogUtil.i("PerSubscriptionSettingsActivity",
                        "ACTION_BOOT_COMPLETED mPhoneId = " + mPhoneId
                                + " setSimDefaultSatus(context, 1); ");
            }
        } else if (IccCardProxy.ACTION_INTERNAL_SIM_STATE_CHANGED.equals(intent
                .getAction())) {
            String simStatus = intent
                    .getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simStatus)) {
                mPhoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                if (mPhoneId == 0) {
                    if ((simReadyStatus & BOOT_STATUS_BIT_0) == BOOT_STATUS_BIT_0) {
                        //simReadyStatus = simReadyStatus & (~BOOT_STATUS_BIT_0);
                        setSimDefaultSatus(context, 0);
                        LogUtil.i("PerSubscriptionSettingsActivity",
                                "ACTION_INTERNAL_SIM_STATE_CHANGED mPhoneId = "
                                        + mPhoneId
                                        + " setSimDefaultSatus(context, 0); ");
                    }
                    simReadyStatus = simReadyStatus | SIM_STATUS_BIT_2;
                }
                if (mPhoneId == 1) {
                    if ((simReadyStatus & BOOT_STATUS_BIT_1) == BOOT_STATUS_BIT_1) {
                        //simReadyStatus = simReadyStatus & (~BOOT_STATUS_BIT_1);
                        setSimDefaultSatus(context, 1);
                        LogUtil.i("PerSubscriptionSettingsActivity",
                                "ACTION_INTERNAL_SIM_STATE_CHANGED mPhoneId = "
                                        + mPhoneId
                                        + " setSimDefaultSatus(context, 1); ");
                    }
                    simReadyStatus = simReadyStatus | SIM_STATUS_BIT_3;
                }
            }
            // sprd #542214 end
        } else {
            LogUtil.i(LogUtil.BUGLE_TAG, "BootAndPackageReplacedReceiver got unexpected action: "
                    + intent.getAction());
        }
    }

    // sprd 542214 start
    private void setSimDefaultSatus(final Context context, int phoneId) {
        RadioInteractor mRadioInteractor = new RadioInteractor(Factory.get()
                .getApplicationContext());
        final int subId = MmsUtils.tanslatePhoneIdToSubId(context, phoneId);
        final Resources res = context.getResources();
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context);
        String[] entryValues = res
                .getStringArray(R.array.sms_save_to_sim_pref_entry_values);
        String value = sharedPref.getString(
                res.getString(R.string.sms_save_to_sim_pref_key) + subId,
                Boolean.toString(res
                        .getBoolean(R.bool.sms_save_to_sim_pref_default)));
        boolean enable = Boolean.valueOf(value);
        //String mSim = mRadioInteractor.querySmsStorageMode(phoneId);
        //if (enable && "ME".equals(mSim)) {
            boolean retValue = mRadioInteractor.storeSmsToSim(enable, phoneId);
            LogUtil.i("PerSubscriptionSettingsActivity",
                    "BootAndPackageReplacedReceiver setSimDefaultSatus  phoneId = "
                            + phoneId + " enable = " + enable + " retValue = "
                            + retValue + " subId = " + subId);
        //}
    }
    // sprd 542214 end
}

