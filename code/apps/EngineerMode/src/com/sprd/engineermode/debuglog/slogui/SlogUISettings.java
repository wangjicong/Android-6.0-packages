/*
 * Copyright (C) 2013 Spreadtrum Communications Inc.
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

package com.sprd.engineermode.debuglog.slogui;

import static com.sprd.engineermode.debuglog.slogui.SlogService.NOTIFICATION_SLOG;
import static com.sprd.engineermode.debuglog.slogui.SlogService.NOTIFICATION_SNAP;
import static com.sprd.engineermode.debuglog.slogui.SlogService.SERVICES_SETTINGS_KEY;
import static com.sprd.engineermode.debuglog.slogui.SlogService.SERVICE_SLOG_KEY;
import static com.sprd.engineermode.debuglog.slogui.SlogService.SERVICE_SNAP_KEY;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.sprd.engineermode.utils.IATUtils;
import com.sprd.engineermode.engconstents;
import android.widget.Toast;
import android.os.SystemProperties;
import android.content.Intent;
import android.widget.LinearLayout;
import android.telephony.TelephonyManager;
import android.os.Environment;
import java.io.File;

import com.sprd.engineermode.R;

/**
 * <br>
 * <br>
 * Powered by <b>Spreadtrum</b>
 */
public class SlogUISettings extends AbsSlogUIActivity {
    private static final int WAITING_FOR_START_ACTIVITY = 2;

    private boolean isSupportLTE = SystemProperties.get("persist.radio.ssda.mode").equals("svlte")
            || SystemProperties.get("persist.radio.ssda.mode").equals("tdd-csfb")
            || SystemProperties.get("persist.radio.ssda.mode").equals("fdd-csfb")
            || SystemProperties.get("persist.radio.ssda.mode").equals("csfb");
    // Views
    CheckBox mGeneral;
    View mGeneralLayout;
    CheckBox mShowNotification;
    CheckBox mShowSnap;
    CheckBox mSaveAllOrHwWatchdog;
    /*RadioButton mStorageData;
    RadioButton mStorageExternal;*/

    CheckBox mAndroid;
    View mAndroidLayout;
    CheckBox mAndroidBranch;
    CheckBox mCap;
    CheckBox mBluetooth;
    
    CheckBox mSystem;
    CheckBox mMain;
    CheckBox mKernel;
    CheckBox mRadio;
    CheckBox mEvents;
    RadioButton mLoglevel2;
    RadioButton mLoglevel3;
    RadioButton mLoglevel5;
    Button mClear;
    Button mDump;
    
    CheckBox mModeLog;

    SharedPreferences mSettings;

    Runnable mCommitRunnable = new Runnable() {

        @Override
        public void run() {
            mSettings
                    .edit()
                    .putBoolean(SERVICE_SLOG_KEY, mShowNotification.isChecked())
                    .putBoolean(SERVICE_SNAP_KEY, mShowSnap.isChecked())
                    .commit();
            mCommitConfigRunnable.run();
        }
    };

    private SlogConfListener mListener = new SlogConfListener() {

        @Override
        public void onSlogConfigChanged() {
            // SlogAction.reloadCache(this);
        }

        @Override
        public void onLoadFinished() {
            mMainThreadHandler.post(new Runnable() {

                @Override
                public void run() {
                    syncState();

                }
            });

        }
    };

    @Override
    public void syncState() {
        setViewVisibility(mGeneralLayout, mGeneral.isChecked());
        setViewVisibility(mAndroidLayout, mAndroid.isChecked());

        boolean general = SlogAction.getState(SlogAction.GENERALKEY);
        boolean isExternal = StorageUtil.getExternalStorageState();
        mModeLog.setEnabled(general);
        mAndroidBranch.setEnabled(general);
        setCheckBoxState(mBluetooth, general && isExternal,
                SlogAction.getState(SlogAction.BLUETOOTHKEY));
        setCheckBoxState(mCap, general && isExternal,
                SlogAction.getState(SlogAction.TCPKEY));
        mAndroidBranch.setChecked(SlogAction.getState(SlogAction.ANDROIDKEY));
        setAndroidChildCheckEnabled(general && mAndroidBranch.isChecked());
        setApLogLevelChildCheckEnabled(general);
        mSystem.setChecked(SlogAction.getState(SlogAction.SYSTEMKEY));
        mMain.setChecked(SlogAction.getState(SlogAction.MAINKEY));
        mKernel.setChecked(SlogAction.getState(SlogAction.KERNELKEY));
        mRadio.setChecked(SlogAction.getState(SlogAction.RADIOKEY));
        if (SlogAction.isKeyValid(SlogAction.EVENTKEY)) {
            mEvents.setVisibility(View.VISIBLE);
            mEvents.setChecked(SlogAction.getState(SlogAction.EVENTKEY));
        } else {
            mEvents.setVisibility(View.GONE);
        }
        if (SlogAction.isKeyValid(SlogAction.SAVEALLKEY)) {
            mSaveAllOrHwWatchdog.setChecked(SlogAction
                    .getState(SlogAction.SAVEALLKEY));
            setViewVisibility(mSaveAllOrHwWatchdog, false);
        } else {
            setViewVisibility(mSaveAllOrHwWatchdog, false);
        }

        mShowNotification.setChecked(mSettings.getBoolean(SERVICE_SLOG_KEY,
                false));
        mShowSnap.setChecked(mSettings.getBoolean(SERVICE_SNAP_KEY, false));

        Log.d(TAG, "boolean isExternal is " + isExternal);
        /*if (!StorageUtil.getExternalStorageState()) {
            mStorageExternal.setEnabled(false);
            mStorageData.setChecked(true);
        } else {
            mStorageData.setChecked(!isExternal);
            mStorageExternal.setChecked(isExternal);
        }*/

        mDump.setEnabled(general && isExternal && !SlogAction.isDumping());
        mClear.setEnabled(general && (!SlogAction.isClearing()));
        switch(SlogAction.getLevel(SlogAction.MAINKEY)){
            case 2:
                mLoglevel2.setChecked(true);
                break;
            case 3:
                mLoglevel3.setChecked(true);
                break;
            case 5:
                mLoglevel5.setChecked(true);
                break;
            default:
                break;
        }
    }

    OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            // Log.d(TAG, "View onClick");
            // Need reload, no need to reload if user click collector.
            boolean needReload = true;
            switch (v.getId()) {

                case R.id.settings_collector_general:
                    needReload = false;
                    setViewVisibility(mGeneralLayout, mGeneral.isChecked());
                    break;
                case R.id.settings_collection_general_show_notification:
                    needReload = false;
                    try {
                        mService.setNotification(NOTIFICATION_SLOG,
                                mShowNotification.isChecked());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.settings_collection_general_snap_service:
                    needReload = false;
                    try {
                        mService.setNotification(NOTIFICATION_SNAP,
                                mShowSnap.isChecked());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.settings_collection_general_save_all:
                    SlogAction.setState(SlogAction.SAVEALLKEY,
                            mSaveAllOrHwWatchdog.isChecked());
                    break;
                /*case R.id.settings_collection_general_storage_data:
                    SlogAction.setState(SlogAction.STORAGEKEY, false);
                    mDump.setEnabled(false);
                    break;
                case R.id.settings_collection_general_storage_external:
                    SlogAction.setState(SlogAction.STORAGEKEY, true);
                    mDump.setEnabled(true && !SlogAction
                            .getState(SlogAction.GENERALKEY));
                    break;
*/
                case R.id.settings_collector_android:
                    needReload = false;
                    setViewVisibility(mAndroidLayout, mAndroid.isChecked());
                    break;
                case R.id.settings_collection_android_branch:
                    setAndroidChildCheckBoxStates(mAndroidBranch.isChecked());
                    setAndroidChildCheckEnabled(mAndroidBranch.isChecked());
                    SlogAction.setState(SlogAction.ANDROIDKEY,
                            mAndroidBranch.isChecked());
                    break;
                case R.id.settings_collection_cap:
                    SlogAction.setState(SlogAction.TCPKEY,
                            mCap.isChecked(), false);
                    SlogAction.CAP_ENABLE = mCap.isChecked();
                    break;
                case R.id.settings_collection_bluetooth:
                    SlogAction.setState(SlogAction.BLUETOOTHKEY,
                            mBluetooth.isChecked(), false);
                    SlogAction.BLUETHOOTH_ENABLE = mBluetooth.isChecked();
                    break;
                case R.id.settings_collection_android_kernel:
                    SlogAction.setState(SlogAction.KERNELKEY, mKernel.isChecked(),
                            false);
                    break;
                case R.id.settings_collection_android_system:
                    SlogAction.setState(SlogAction.SYSTEMKEY, mSystem.isChecked(),
                            false);
                    break;
                case R.id.settings_collection_android_main:
                    SlogAction.setState(SlogAction.MAINKEY, mMain.isChecked(),
                            false);
                    break;
                case R.id.settings_collection_android_radio:
                    SlogAction.setState(SlogAction.RADIOKEY, mRadio.isChecked(),
                            false);
                    break;
                case R.id.settings_collection_android_events:
                    SlogAction.setState(SlogAction.EVENTKEY, mEvents.isChecked(),
                            false);
                    break;

                case R.id.settings_collection_loglevel_2:
                    SlogAction.setLevel(SlogAction.MAINKEY, 2);
                    break;
                case R.id.settings_collection_loglevel_3:
                    SlogAction.setLevel(SlogAction.MAINKEY, 3);
                    break;
                case R.id.settings_collection_loglevel_5:
                    SlogAction.setLevel(SlogAction.MAINKEY, 5);
                    break;
                case R.id.settings_collection_other_clear:
                    needReload = false;
                    clearLog();
                    break;
                case R.id.settings_collection_other_dump:
                    needReload = false;
                    dumpLog();
                    break;
                case R.id.logo:
                    finish();
                    break;
                case R.id.settings_collector_cp:
                    needReload = false;
                    mModeLog.setChecked(false);
                    Intent intent = new Intent(SlogUISettings.this,ModemLogSettings.class);
                    SlogUISettings.this.startActivity(intent);
                    break;
                default:
                    break;
            }
            if (needReload) {
                mHandler.removeCallbacks(mCommitRunnable);
                mHandler.postDelayed(mCommitRunnable, 1000);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindSlogService();
        setSlogConfigChangedListener(mListener);
        setContentView(R.layout.activity_slog_settings);
        // find views
        mGeneral = (CheckBox) findViewById(R.id.settings_collector_general);
        mGeneralLayout = findViewById(R.id.settings_collection_general_layout);
        mShowNotification = (CheckBox) findViewById(R.id.settings_collection_general_show_notification);
        mShowSnap = (CheckBox) findViewById(R.id.settings_collection_general_snap_service);
        mSaveAllOrHwWatchdog = (CheckBox) findViewById(R.id.settings_collection_general_save_all);
        /*mStorageData = (RadioButton) findViewById(R.id.settings_collection_general_storage_data);
        mStorageExternal = (RadioButton) findViewById(R.id.settings_collection_general_storage_external);*/

        mAndroid = (CheckBox) findViewById(R.id.settings_collector_android);
        mAndroidLayout = findViewById(R.id.settings_collection_android_layout);
        mAndroidBranch = (CheckBox) findViewById(R.id.settings_collection_android_branch);
        mCap = (CheckBox) findViewById(R.id.settings_collection_cap);
        mBluetooth = (CheckBox) findViewById(R.id.settings_collection_bluetooth);

        mSystem = (CheckBox) findViewById(R.id.settings_collection_android_system);
        mMain = (CheckBox) findViewById(R.id.settings_collection_android_main);
        mKernel = (CheckBox) findViewById(R.id.settings_collection_android_kernel);
        mRadio = (CheckBox) findViewById(R.id.settings_collection_android_radio);
        mEvents = (CheckBox) findViewById(R.id.settings_collection_android_events);
        mLoglevel2 = (RadioButton) findViewById(R.id.settings_collection_loglevel_2);
        mLoglevel3 = (RadioButton) findViewById(R.id.settings_collection_loglevel_3);
        mLoglevel5 = (RadioButton) findViewById(R.id.settings_collection_loglevel_5);
        mClear = (Button) findViewById(R.id.settings_collection_other_clear);
        mDump = (Button) findViewById(R.id.settings_collection_other_dump);

        mModeLog = (CheckBox) findViewById(R.id.settings_collector_cp);

        mGeneral.setOnClickListener(mClickListener);
        mShowNotification.setOnClickListener(mClickListener);
        mShowSnap.setOnClickListener(mClickListener);
        mSaveAllOrHwWatchdog.setOnClickListener(mClickListener);
        /*mStorageData.setOnClickListener(mClickListener);
        mStorageExternal.setOnClickListener(mClickListener);*/

        mAndroid.setOnClickListener(mClickListener);
        mAndroidBranch.setOnClickListener(mClickListener);
        mCap.setOnClickListener(mClickListener);
        mBluetooth.setOnClickListener(mClickListener);
        mSystem.setOnClickListener(mClickListener);
        mMain.setOnClickListener(mClickListener);
        mKernel.setOnClickListener(mClickListener);
        mRadio.setOnClickListener(mClickListener);
        mEvents.setOnClickListener(mClickListener);

        mLoglevel2.setOnClickListener(mClickListener);
        mLoglevel3.setOnClickListener(mClickListener);
        mLoglevel5.setOnClickListener(mClickListener);

        mClear.setOnClickListener(mClickListener);
        mDump.setOnClickListener(mClickListener);

        mModeLog.setOnClickListener(mClickListener);
        findViewById(R.id.logo).setOnClickListener(mClickListener);

        mSettings = getApplicationContext().getSharedPreferences(
                SERVICES_SETTINGS_KEY, MODE_PRIVATE);
    }

    private void setAndroidChildCheckBoxStates(boolean enable) {
        mKernel.setChecked(enable);
        mMain.setChecked(enable);
        mEvents.setChecked(enable);
        mRadio.setChecked(enable);
        mSystem.setChecked(enable);
    }

    private void setAndroidChildCheckEnabled(boolean enable) {
        mKernel.setEnabled(enable);
        mMain.setEnabled(enable);
        mEvents.setEnabled(enable);
        mRadio.setEnabled(enable);
        mSystem.setEnabled(enable);
    }

    private void setApLogLevelChildCheckEnabled(boolean enable) {
        mLoglevel2.setEnabled(enable);
        mLoglevel3.setEnabled(enable);
        mLoglevel5.setEnabled(enable);
    }
 
    @Override
    protected void onResume() {
        Log.d(TAG,"onResume...");
        syncState();
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mHandler.removeCallbacks(mCommitConfigRunnable);
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        mMainThreadHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "removeMessages");
                mHandler.removeMessages(WAITING_FOR_START_ACTIVITY);
            }
        }, 500);
        mHandler.post(mCommitRunnable);
    }

    @Override
    void onSlogServiceConnected() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mService != null) {
                    try {
                        boolean general = SlogAction.getState(SlogAction.GENERALKEY);
                        mShowSnap.setEnabled(general);
                        mService.setNotification(NOTIFICATION_SNAP, general
                                && mSettings.getBoolean(SERVICE_SNAP_KEY, false));
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    @Override
    void onSlogServiceDisconnected() {

    }

    @Override
    protected void onDestroy() {

        unbindSlogUIService();
        super.onDestroy();
    }

    @Override
    protected void onDumpLogStarted(boolean enabled) {
        mDump.setEnabled(enabled);
        super.onDumpLogStarted(enabled);
    }

    @Override
    protected void onClearLogStarted(boolean enabled) {
        mClear.setEnabled(enabled);
        super.onClearLogStarted(enabled);
    }

    @Override
    protected void onDumpLogEnded() {
        if (mDump != null) {
            mDump.setEnabled(SlogAction.getState(SlogAction.STORAGEKEY));
        }
        super.onDumpLogEnded();
    }

    @Override
    protected void onClearLogEnded() {
        if (mClear != null) {
            mClear.setEnabled(true);
        }
        super.onClearLogEnded();
    }

    /*
     * void requestDumpLog() {
     * SlogUIAlert.prepareAlert().setTitle(R.string.alert_request_dump_title)
     * new AlertDialog.Builder(this)
     * .setIcon(android.R.drawable.ic_dialog_alert)
     * .setTitle(R.string.alert_request_dump_title)
     * .setMessage(R.string.alert_request_dump_prompt)
     * .setPositiveButton(R.string.alert_dump_dialog_ok, new
     * DialogInterface.OnClickListener() { public void onClick(DialogInterface
     * dialog, int whichButton) { if (mStorageExternal.isChecked()) { dumpLog();
     * } } }) .setNegativeButton(R.string.alert_dump_dialog_cancel, null)
     * .create().show(); }
     */

    @Override
    void commit() {
        // TODO Auto-generated method stub

    }
}
