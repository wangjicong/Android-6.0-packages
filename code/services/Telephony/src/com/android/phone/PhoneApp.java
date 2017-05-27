/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.UserHandle;
import com.android.internal.telephony.TelephonyIntents;

import com.android.services.telephony.TelephonyGlobals;
import com.sprd.phone.CallForwardHelper;
import com.sprd.phone.OtherGlobals;

/**
 * Top-level Application class for the Phone app.
 */
public class PhoneApp extends Application {
    PhoneGlobals mPhoneGlobals;
    TelephonyGlobals mTelephonyGlobals;
    OtherGlobals mOtherGlobals; // SPRD: Bring up OtherGlobals

    public PhoneApp() {
    }

    @Override
    public void onCreate() {
        if (UserHandle.myUserId() == 0) {
            // We are running as the primary user, so should bring up the
            // global phone state.
            mPhoneGlobals = new PhoneGlobals(this);
            mPhoneGlobals.onCreate();

            mTelephonyGlobals = new TelephonyGlobals(this);
            mTelephonyGlobals.onCreate();

            // SPRD: Bring up OtherGlobals.
            mOtherGlobals = new OtherGlobals(this);
            mOtherGlobals.onCreate();
            // SPRD: Add for auto query call forward for bug 504482
            CallForwardHelper.getInstance();
            /* SPRD: Add for bug 521132, bind ringer service when phone app created @{ */
            Intent intent = new Intent(TelephonyIntents.ACTION_BIND_RINGER_SERVICE);
            intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.setPackage("com.android.server.telecom");
            this.sendStickyBroadcast(intent);
        }
    }
}
