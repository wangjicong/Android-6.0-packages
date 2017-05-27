/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.stk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.android.internal.telephony.cat.CatLog;

/* SPRD: airplane mode change install/uninstall  support @{*/
import android.telephony.TelephonyManager;
import android.provider.Settings;
import android.provider.Settings.System;
import com.android.internal.telephony.TelephonyIntents;
/* @}*/
/* SPRD: SIM standby changed install/uninstall  support @{*/
import com.android.internal.telephony.PhoneConstants;
/* @}*/




/**
 * Boot completed receiver. used to reset the app install state every time the
 * device boots.
 *
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = new Object(){}.getClass().getEnclosingClass().getName();
    /* SPRD: SIM standby changed install/uninstall  support @{*/
    private StkAppService appService = StkAppService.getInstance();
    /* @}*/

    /* SPRD: airplane mode change install/uninstall  support @{*/
    private boolean isCardReady(Context context) {
        int simCount = TelephonyManager.from(context).getSimCount();
        TelephonyManager tm = TelephonyManager.from(context);
        CatLog.d(LOG_TAG, "simCount: " + simCount);
        for (int i = 0; i < simCount; i++) {
            //Check if the card is inserted.
            /* SPRD: SIM standby changed install/uninstall  support @{*/
            boolean isStandby = Settings.Global.getInt(
                        context.getContentResolver(), Settings.Global.SIM_STANDBY + i, 1) == 1;
            CatLog.d(LOG_TAG, "SIM " + i + " isStandby="+isStandby);
            /* @}*/
            if (tm.hasIccCard(i)) {
                CatLog.d(LOG_TAG, "SIM " + i + " is inserted.");
                /* SPRD: SIM standby changed install/uninstall  support @{*/
                CatLog.d(LOG_TAG, "mTm.hasIccCard(i) =" +tm.hasIccCard(i));
                CatLog.d(LOG_TAG, "appservice =" +appService);
                //if(tm.getSimState(i) == TelephonyManager.SIM_STATE_READY){
                if(tm.getSimState(i) == TelephonyManager.SIM_STATE_READY&&appService != null&&appService.getStkContext(i) != null
                    && appService.getStkContext(i).mMainCmd != null&&isStandby){
                    CatLog.d(LOG_TAG, "SIM " + i + " is ready.");
                    return true;
                }
                /* @}*/
            } else {
                CatLog.d(LOG_TAG, "SIM " + i + " is not inserted.");
            }
        }
        return false;
    }
    /* @}*/


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // make sure the app icon is removed every time the device boots.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Bundle args = new Bundle();
            args.putInt(StkAppService.OPCODE, StkAppService.OP_BOOT_COMPLETED);
            context.startService(new Intent(context, StkAppService.class)
                    .putExtras(args));
            CatLog.d(LOG_TAG, "[ACTION_BOOT_COMPLETED]");
        } else if (Intent.ACTION_USER_INITIALIZE.equals(action)) {
            if (!android.os.Process.myUserHandle().isOwner()) {
                //Disable package for all secondary users. Package is only required for device
                //owner.
                context.getPackageManager().setApplicationEnabledSetting(context.getPackageName(),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
                return;
            }
        /* SPRD: airplane mode change install/uninstall  support @{*/
        } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)
                /* SPRD: SIM standby changed install/uninstall  support @{*/
                //|| TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action) {
                || TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)
                || TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {

            CatLog.d(LOG_TAG, "action:"+action);
            if(TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)){
                int slotID = intent.getIntExtra(PhoneConstants.SLOT_KEY, 0);
                CatLog.d(LOG_TAG, "slotID:"+slotID);
            }
            /* @}*/
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            boolean isAirPlaneModeOn = Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
            if (!isAirPlaneModeOn && isCardReady(context)) {
                StkAppInstaller.install(context);
            } else {
                StkAppInstaller.unInstall(context);
            }

        }
        /* @}*/
    }
}
