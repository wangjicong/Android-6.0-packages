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

package com.android.mmsfolderview.util;

import android.content.Context;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
//import com.android.ims.ImsManager;
import android.util.Log;

import com.android.mmsfolderview.R;

public class VoLteState {

    private final static String TAG = "VoLteState";
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mLtePhoneStateListener;
    private boolean mIsVoLteNetRegistered;
    private boolean mIsVoLteProduct = true;
    private Context mContext;

    public VoLteState(Context context) {
        this.mContext = context;
        initConfig();
    }

    private void initConfig() {
//        mIsVoLteProduct = ImsManager.isVolteEnabledByPlatform(mContext);
//        Log.d(TAG, " This Is VoLte Product: " + mIsVoLteProduct);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mLtePhoneStateListener = new PhoneStateListener() {
            @Override
            public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
                mIsVoLteNetRegistered = (serviceState.getSrvccState() == VoLteServiceState.IMS_REG_STATE_REGISTERED);
                Log.d(TAG, "onVoLteServiceStateChanged: " + serviceState
                        + " , mIsVoLteNetRegistered: " + mIsVoLteNetRegistered);
            }
        };
    }

    public void registerVoLteNetStateListener() {
        if (mTelephonyManager != null && mLtePhoneStateListener != null) {
            mTelephonyManager.listen(mLtePhoneStateListener, PhoneStateListener.LISTEN_VOLTE_STATE);
        }
    }

    public void unRegisterVoLteNetStateListener() {
        if (mTelephonyManager != null && mLtePhoneStateListener != null) {
            mTelephonyManager.listen(mLtePhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    public boolean getRegisteredState() {
        return mIsVoLteNetRegistered;
    }

    public boolean getIsVoLteProduct() {
        return mIsVoLteProduct;
    }
}
