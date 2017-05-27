/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.dialer;

import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Trace;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;

import com.android.contacts.common.extensions.ExtensionsFactory;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.android.dialer.util.DialerUtils;
import com.android.ims.ImsManager;

public class DialerApplication extends Application {

    private static final String TAG = "DialerApplication";
    /* SPRD: used for identifying automatic recording started or not @{ */
    private boolean mIsAutomaticRecordingStart = false;
    /* @} */
    // SPRD: Add for shaking phone to start recording feature.
    private boolean mIsRecordingStart = false;

    @Override
    public void onCreate() {
        Trace.beginSection(TAG + " onCreate");
        super.onCreate();
        Trace.beginSection(TAG + " ExtensionsFactory initialization");
        ExtensionsFactory.init(getApplicationContext());
        Trace.endSection();
        Trace.beginSection(TAG + " Analytics initialization");
        AnalyticsUtil.initialize(this);
        Trace.endSection();
        Trace.endSection();
        // SPRD: add for bug533902
        startMonitor();
    }

    /* SPRD: AUTOMATIC RECORD FEATURE.@{ */
    public boolean getIsAutomaticRecordingStart() {
        return mIsAutomaticRecordingStart;
    }

    public void setIsAutomaticRecordingStart(boolean isAutomaticRecordingStart) {
        mIsAutomaticRecordingStart = isAutomaticRecordingStart;
    }
    /* @} */

    /* Add for shaking phone to start recording feature. @{ */
    public boolean getIsRecordingStart() {
        return mIsRecordingStart;
    }

    public void setIsRecordingStart(boolean isRecordingStart) {
        mIsRecordingStart = isRecordingStart;
    }
    /* @} */

    /* SPRD: add for bug533902 @{ */
    private boolean mVolteServiceEnable = false;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mLtePhoneStateListener;

    public boolean isInVolteService() {
        return mVolteServiceEnable;
    }

    public void startMonitor() {
        mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null
                && checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && ImsManager.isVolteEnabledByPlatform(this)
                && DialerUtils.is4GPhone()
                && mLtePhoneStateListener == null) {
            mLtePhoneStateListener = new PhoneStateListener() {
                @Override
                public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
                    mVolteServiceEnable = serviceState.getSrvccState()
                            == VoLteServiceState.IMS_REG_STATE_REGISTERED;
                }
            };
            mTelephonyManager.listen(mLtePhoneStateListener, PhoneStateListener.LISTEN_VOLTE_STATE);
        }
    }

    public void stopMonitor() {
        if (mTelephonyManager != null
                && checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                && ImsManager.isVolteEnabledByPlatform(this)
                && DialerUtils.is4GPhone()
                && mLtePhoneStateListener != null) {
            mTelephonyManager.listen(mLtePhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        mTelephonyManager = null;
        mLtePhoneStateListener = null;
    }

    @Override
    protected void finalize() throws Throwable {
        stopMonitor();
        super.finalize();
    }
    /* @} */
}
