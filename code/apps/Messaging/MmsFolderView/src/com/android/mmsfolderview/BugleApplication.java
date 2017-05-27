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

package com.android.mmsfolderview;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
//import android.support.v7.mms.CarrierConfigValuesLoader;
//import android.support.v7.mms.MmsManager;
import android.telephony.CarrierConfigManager;
import android.util.Log;

import com.android.mmsfolderview.ui.ConversationDrawables;
import com.android.mmsfolderview.util.OsUtil;
import com.android.mmsfolderview.util.PhoneUtils;
import com.google.common.annotations.VisibleForTesting;

import android.provider.ContactsContract.Data;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
// bug 478514: Add for MmsFolderView Feature -- Begin
//import com.android.pluginframework.PluginsManager;
// bug 478514: Add for MmsFolderView Feature -- End
/**
 * The application object
 */
public class BugleApplication extends Application implements UncaughtExceptionHandler {
    private static final String TAG = "BugleApplication";

    private UncaughtExceptionHandler sSystemUncaughtExceptionHandler;
    private static boolean sRunningTests = false;

    @VisibleForTesting
    protected static void setTestsRunning() {
        sRunningTests = true;
    }

    /**
     * @return true if we're running unit tests.
     */
    public static boolean isRunningTests() {
        return sRunningTests;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // spread: fixed for bug 519506 begin
        if(OsUtil.hasRequiredPermissions(getApplicationContext())){
            BugleApplication.this.getContentResolver().acquireContentProviderClient(Data.CONTENT_URI);
        }
        // spread: fixed for bug 519506 begin

        // Note onCreate is called in both test and real application environments
        if (!sRunningTests) {
            // Only create the factory if not running tests
            FactoryImpl.register(getApplicationContext(), this);
        } else {
            Log.e(TAG, "BugleApplication.onCreate: FactoryImpl.register skipped for test run");
        }
        // bug 478514: Add for MmsFolderView Feature -- Begin
//        try {
//            PluginsManager.setContext(this);
//            PluginsManager.initPluginsManager();
//		} catch (Exception e) {
//			LogUtil.e(TAG, "initPluginsManager Error", e);
//		}
        // bug 478514: Add for MmsFolderView Feature -- End
        sSystemUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update conversation drawables when changing writing systems
        // (Right-To-Left / Left-To-Right)
        //ConversationDrawables.get().updateDrawables();
    }

    // Called by the "real" factory from FactoryImpl.register() (i.e. not run in tests)
    public void initializeSync(final Factory factory) {
        final Context context = factory.getApplicationContext();
//        final BugleGservices bugleGservices = factory.getBugleGservices();
//        final BuglePrefs buglePrefs = factory.getApplicationPrefs();
//        final DataModel dataModel = factory.getDataModel();
//        final CarrierConfigValuesLoader carrierConfigValuesLoader =
//                factory.getCarrierConfigValuesLoader();


        BugleApplication.updateAppConfig(context);

        // Initialize MMS lib
//        initMmsLib(context, bugleGservices, carrierConfigValuesLoader);
        // Initialize APN database
        // Fixup messages in flight if we crashed and send any pending
//        dataModel.onApplicationCreated();
        // Register carrier config change receiver
        if (OsUtil.isAtLeastM()) {
            registerCarrierConfigChangeReceiver(context);
        }

    }

    private static void registerCarrierConfigChangeReceiver(final Context context) {
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Carrier config changed. Reloading MMS config.");
            }
        }, new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
    }

//    private static void initMmsLib(final Context context, final BugleGservices bugleGservices,
//            final CarrierConfigValuesLoader carrierConfigValuesLoader) {
//        MmsManager.setApnSettingsLoader(new BugleApnSettingsLoader(context));
//        MmsManager.setCarrierConfigValuesLoader(carrierConfigValuesLoader);
//        MmsManager.setUserAgentInfoLoader(new BugleUserAgentInfoLoader(context));
//        MmsManager.setUseWakeLock(true);
//        // If Gservices is configured not to use mms api, force MmsManager to always use
//        // legacy mms sending logic
//        MmsManager.setForceLegacyMms(!bugleGservices.getBoolean(
//                BugleGservicesKeys.USE_MMS_API_IF_PRESENT,
//                BugleGservicesKeys.USE_MMS_API_IF_PRESENT_DEFAULT));
//        bugleGservices.registerForChanges(new Runnable() {
//            @Override
//            public void run() {
//                MmsManager.setForceLegacyMms(!bugleGservices.getBoolean(
//                        BugleGservicesKeys.USE_MMS_API_IF_PRESENT,
//                        BugleGservicesKeys.USE_MMS_API_IF_PRESENT_DEFAULT));
//            }
//        });
//    }

    public static void updateAppConfig(final Context context) {
        // Make sure we set the correct state for the SMS/MMS receivers
//        SmsReceiver.updateSmsReceiveHandler(context);
    }

    // Called from thread started in FactoryImpl.register() (i.e. not run in tests)
    public void initializeAsync(final Factory factory) {
        // Handle shared prefs upgrade & Load MMS Configuration
//        Trace.beginSection("app.initializeAsync");
//        //maybeHandleSharedPrefsUpgrade(factory);
//        MmsConfig.load();
//        Trace.endSection();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        Log.d(TAG, "BugleApplication.onLowMemory");
        //Factory.get().reclaimMemory();
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        final boolean background = getMainLooper().getThread() != thread;
        if (background) {
            Log.e(TAG, "Uncaught exception in background thread " + thread, ex);

            final Handler handler = new Handler(getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {
                    sSystemUncaughtExceptionHandler.uncaughtException(thread, ex);
                }
            });
        } else {
            sSystemUncaughtExceptionHandler.uncaughtException(thread, ex);
        }
    }
}
