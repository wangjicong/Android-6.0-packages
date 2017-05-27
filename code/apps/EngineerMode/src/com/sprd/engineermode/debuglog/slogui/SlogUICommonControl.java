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

import static com.sprd.engineermode.debuglog.slogui.SlogService.NOTIFICATION_SNAP;
import static com.sprd.engineermode.debuglog.slogui.SlogService.NOTIFICATION_LOW_STORAGE;
import static com.sprd.engineermode.debuglog.slogui.SlogService.SERVICE_SNAP_KEY;
import static com.sprd.engineermode.debuglog.slogui.SlogService.SERVICES_SETTINGS_KEY;

import java.io.File;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.shapes.Shape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.android.internal.app.IMediaContainerService;
import com.sprd.engineermode.debuglog.slogui.SlogUIAlert.AlertCallBack;
import com.sprd.engineermode.R;
//import com.spreadst.android.eng.engconstents;
import com.sprd.engineermode.debuglog.slogui.SlogAction;
import com.sprd.engineermode.debuglog.slogui.SlogUIAlert.AlertCallBack;
import com.sprd.engineermode.utils.SocketUtils;
import java.io.OutputStream;
import java.io.File;

public class SlogUICommonControl extends AbsSlogUIActivity {
    private static final String EXTRA_SHOW_MODE_POPUP = "popup";
    private static final int MODE_QUERY_ALL = 0;
    private static final int MODE_QUERY_SINGLE = 1;
    private static final int MODE_FIRST_QUERY = 2;
    private static final int MODE_QUERY_MODES = 4;

    ToggleButton mGeneral;
    CheckBox mAndroid;
    CheckBox mModem;
    TextView mAPLogPath;
    TextView mCPLogPath;
    ProgressBar mStorageUsage;
    TextView mStorageUsed;
    TextView mStorageFree;
    Button mDump;
    Button mClear;

    private LocalSocket mClient;
    private OutputStream mOut;

    protected static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            "com.android.defcontainer",
            "com.android.defcontainer.DefaultContainerService");
    IMediaContainerService mMediaContainerService;
    protected boolean mMCSEnable;
    private String mEngPc;
    private ServiceConnection mMCSConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMCSEnable = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMediaContainerService = IMediaContainerService.Stub
                    .asInterface(service);
            mMCSEnable = true;
            setUsage();
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

    private void alertUnallowDialog(int mes) {

        AlertCallBack callback = new AlertCallBack() {

            @Override
            public void onTextAccept(int which, String text) {
             // do nothing
            }

            @Override
            public void onClick(int which) {
                
                if (which == R.id.positive_button) {
                    if (SystemProperties.get("persist.sys.engpc.disable").equals(
                            "1")) {
                        setModeLog(true);
                    }
                    startActivity(new Intent(SlogUICommonControl.this,
                            ModemLogSettings.class));
                } else if (which == R.id.negative_button) {
                    mModem.setChecked(false);
                }
            }
        };

        Intent intent = SlogUIAlert.prepareIntent()
                .setTitle(R.string.modem_log_warn)
                .setMessage(mes,"")
                .setPositiveButton(R.string.alertdialog_ok, callback)
                .setNegativeButton(R.string.alertdialog_cancel, callback)
                .generateIntent();
        intent.setClass(this, SlogUIAlert.class);
        startActivity(intent);
    }

    private void setModeLog(boolean isOpen) {
        if (SlogAction.CP0_ENABLE) {
            SocketUtils.sendCmdAndRecResult("slogmodem",
                    LocalSocketAddress.Namespace.ABSTRACT, isOpen ?"ENABLE_LOG WCDMA": "DISABLE_LOG WCDMA");
            SlogAction.setState(SlogAction.CP0KEY, isOpen);
        }
        if (SlogAction.CP1_ENABLE) {
            SocketUtils.sendCmdAndRecResult("slogmodem",
                    LocalSocketAddress.Namespace.ABSTRACT, isOpen ?"ENABLE_LOG TD": "DISABLE_LOG TD");
            SlogAction.setState(SlogAction.CP1KEY, isOpen);
        }
        if (SlogAction.CP2_ENABLE) {
            SocketUtils.sendCmdAndRecResult("slogmodem",
                    LocalSocketAddress.Namespace.ABSTRACT, isOpen ?"ENABLE_LOG WCN": "DISABLE_LOG WCN");
            SlogAction.setState(SlogAction.CP2KEY, isOpen);
        }
        if (SlogAction.CP3_ENABLE) {
            SocketUtils.sendCmdAndRecResult("slogmodem",
                    LocalSocketAddress.Namespace.ABSTRACT, isOpen ?"ENABLE_LOG TDD-LTE": "DISABLE_LOG TDD-LTE");
            SlogAction.setState(SlogAction.CP3KEY, isOpen);
        }
        if (SlogAction.CP4_ENABLE) {
            SocketUtils.sendCmdAndRecResult("slogmodem",
                    LocalSocketAddress.Namespace.ABSTRACT, isOpen ?"ENABLE_LOG FDD-LTE": "DISABLE_LOG FDD-LTE");
            SlogAction.setState(SlogAction.CP4KEY, isOpen);
        }
        if (SlogAction.CP5_ENABLE) {
            SocketUtils.sendCmdAndRecResult("slogmodem",
                    LocalSocketAddress.Namespace.ABSTRACT, isOpen ?"ENABLE_LOG 5MODE": "DISABLE_LOG 5MODE");
            SlogAction.setState(SlogAction.CP5KEY, isOpen);
        }
        updateSlogConfig();
    }

    OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.general:
                if (!mGeneral.isChecked()
                        && SlogAction.getState(SlogAction.STORAGEKEY)
                        && SlogAction.getState(SlogAction.BLUETOOTHKEY)) {
                    new Thread(new Runnable() {
                        public void run() {
                            int result = SlogAction.runSlogCommand(
                                    SlogAction.Contracts.SLOG_BT_FASLE);
                            Log.d(TAG, "slog bt set false result is: " + result);
                            mMainThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    SlogAction.setState(SlogAction.GENERALKEY,
                                            mGeneral.isChecked(), true);
                                }
                            });
                        }
                    }).start();
                } else {
                    SlogAction.setState(SlogAction.GENERALKEY,
                            mGeneral.isChecked(), true);
                }
                setModeLog(mGeneral.isChecked());
                break;
            case R.id.general_android:
                SlogAction
                        .setState(SlogAction.ANDROIDKEY, mAndroid.isChecked());
                if (!mAndroid.isChecked()) {
                    SlogAction.setState(SlogAction.TCPKEY, false, false);
                    SlogAction.setState(SlogAction.BLUETOOTHKEY, false, false);
                }
                break;
            case R.id.general_modem:
                if (mModem.isChecked()) {
                    int mes;
                    if (SystemProperties.get("persist.sys.engpc.disable").equals(
                            "0")) {
                        mes = R.string.modem_log_enpc_warn;
                    } else {
                        mes = R.string.modem_log_no_enpc_warn;
                    }
                    alertUnallowDialog(mes);
                } else {
                    setModeLog(false);
                }
                new Thread(mModemCommitRunnable).start();
                break;
            case R.id.right:
                startActivity(new Intent(SlogUICommonControl.this,
                        SlogUISettings.class));
                break;
            case R.id.dumplog:
                dumpLog();
                return;
            case R.id.clearlog:
                clearLog();
                return;
            default:
                break;
            }
            updateSlogConfig();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setSlogConfigChangedListener(mListener);
        super.onCreate(savedInstanceState);
        bindSlogService();
        setContentView(R.layout.activity_slog_common_control);
        mGeneral = (ToggleButton) findViewById(R.id.general);
        mAndroid = (CheckBox) findViewById(R.id.general_android);
        mModem = (CheckBox) findViewById(R.id.general_modem);
        /*mBlueTooth = (CheckBox) findViewById(R.id.general_bluetooth);
        mTCP = (CheckBox) findViewById(R.id.general_cap);*/

        mStorageFree = (TextView) findViewById(R.id.storage_usage_free);
        mStorageUsed = (TextView) findViewById(R.id.storage_usage_used);

        mStorageUsage = (ProgressBar) findViewById(R.id.storage_usage);

        mAPLogPath = (TextView) findViewById(R.id.ap_logpath);
        mCPLogPath = (TextView) findViewById(R.id.cp_logpath);

        mDump = (Button) findViewById(R.id.dumplog);
        mClear = (Button) findViewById(R.id.clearlog);

        mGeneral.setOnClickListener(mClickListener);
        mAndroid.setOnClickListener(mClickListener);
        mModem.setOnClickListener(mClickListener);
        mDump.setOnClickListener(mClickListener);
        mClear.setOnClickListener(mClickListener);
        findViewById(R.id.right).setOnClickListener(mClickListener);
        bindService(new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT),
                mMCSConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void syncState() {
        Log.d(TAG, "syncState");
        boolean general = SlogAction.getState(SlogAction.GENERALKEY);
        boolean isExternal = StorageUtil.getExternalStorageState();
        Log.d(TAG, "General key loaded");
        mDump.setEnabled(general);
        mClear.setEnabled(general);
        mGeneral.setChecked(general);
        if(SlogAction.isCacheInvalid()){
            mGeneral.setEnabled(false);
        }else{
            mGeneral.setEnabled(true);
        }
        setCheckBoxState(mAndroid, general,
                SlogAction.getState(SlogAction.ANDROIDKEY)
                ||(isExternal&&SlogAction.getState(SlogAction.BLUETOOTHKEY))
                ||(isExternal&&SlogAction.getState(SlogAction.TCPKEY)));
        if (SystemProperties.get("persist.sys.engpc.disable").equals(
                "0")) {
            setCheckBoxState(mModem, general, false);
        } else {
            setCheckBoxState(mModem, general, SlogAction.getState(SlogAction.MODEMLOGKEY));
        }
        setUsage();
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Log.d("duke", "runOnUIThread");
                if (mService != null) {
                    try {
                        Log.d("duke", "mService is not null");
                        boolean general = SlogAction
                                .getState(SlogAction.GENERALKEY);
                        SharedPreferences settings = getApplicationContext()
                                .getSharedPreferences(SERVICES_SETTINGS_KEY,
                                        MODE_PRIVATE);
                        Log.d("duke",
                                "settings.getBoolean(SERVICE_SNAP_KEY, false) = "
                                        + settings.getBoolean(SERVICE_SNAP_KEY,
                                                false) + "general = " + general);
                        mService.setNotification(NOTIFICATION_SNAP, general
                                && settings.getBoolean(SERVICE_SNAP_KEY, false));
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    public void sendMsgToModemd(String msg) {
        try {
            mClient = new LocalSocket();
            LocalSocketAddress serverAddress = new LocalSocketAddress(
                    "modemd_engpc", LocalSocketAddress.Namespace.ABSTRACT);
            mClient.connect(serverAddress);
            mOut = mClient.getOutputStream();
            mOut.write(msg.getBytes());
            mOut.flush();
        } catch (Exception e) {
            Log.w(TAG, "client connet exception " + e.getMessage());
        }

        if (mClient.isConnected()) {
            try {
                mOut.close();
                mClient.close();
            } catch (Exception e) {
                Log.w(TAG, "close client exception " + e.getMessage());
            }
        }
    }

    private String SendCP2At(String cmd) {
        Log.d(TAG, "SendCP2At is " + cmd);
        String strTmp = SocketUtils.sendCmdAndRecResult("wcnd",
                LocalSocketAddress.Namespace.ABSTRACT, cmd);
        return strTmp;
    }

    private void setUsage() {
        if (mStorageUsed == null || mStorageUsage == null
                || mStorageFree == null || mMediaContainerService == null) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                final boolean isExternal = StorageUtil.getExternalStorageState();
                final long freespace = SlogAction.getFreeSpace(
                        mMediaContainerService, isExternal);
                final long total = SlogAction.getTotalSpace(
                        mMediaContainerService, isExternal);
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int progress = 0;
                        if (total != 0) {
                            progress = (int) ((total - freespace) * 100 / total);
                        }
                        mStorageFree.setText(Formatter.formatFileSize(
                                SlogUICommonControl.this, freespace)
                                + " "
                                + getText(R.string.storage_free));
                        mStorageUsed.setText(Formatter.formatFileSize(
                                SlogUICommonControl.this, total - freespace)
                                + " " + getText(R.string.storage_usage));
                        mStorageUsage.setProgress(progress);
                        mAPLogPath.setText("Path: "
                                + (isExternal ? StorageUtil.getExternalStorage().getAbsolutePath()
                                        + File.separator + "slog" : Environment.getDataDirectory()
                                        .getAbsolutePath()
                                        + File.separator + "slog"));
                        if (SystemProperties.get("persist.sys.engpc.disable")
                                .equals("0")) {
                            mCPLogPath.setText("Path: "
                                    + getResources().getString(
                                            R.string.modem_saved_pc));
                        } else {
                            mCPLogPath.setText("Path: "
                                    + (isExternal ? StorageUtil.getExternalStorage().getAbsolutePath()
                                            + File.separator + "modem_log"
                                            : Environment.getDataDirectory()
                                                    .getAbsolutePath()
                                                    + File.separator
                                                    + "modem_log"));
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        syncState();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mHandler.removeCallbacks(mCommitConfigRunnable);
        mHandler.post(mCommitConfigRunnable);
        new Thread(mCommitConfigRunnable).start();
        new Thread(mModemCommitRunnable).start();
        super.onStop();
    }

    @Override
    void onSlogServiceConnected() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Log.d("duke", "runOnUIThread");
                if (mService != null) {
                    try {
                        mService.setNotification(NOTIFICATION_LOW_STORAGE,
                                false);
                        // Log.d("duke", "mService is not null");
                        boolean general = SlogAction
                                .getState(SlogAction.GENERALKEY);
                        SharedPreferences settings = getApplicationContext()
                                .getSharedPreferences(SERVICES_SETTINGS_KEY,
                                        MODE_PRIVATE);
                        Log.d("duke",
                                "settings.getBoolean(SERVICE_SNAP_KEY, false) = "
                                        + settings.getBoolean(SERVICE_SNAP_KEY,
                                                false) + "general = " + general);
                        mService.setNotification(NOTIFICATION_SNAP, general
                                && settings.getBoolean(SERVICE_SNAP_KEY, false));
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

    private Runnable mModemCommitRunnable = new Runnable() {
        @Override
        public void run() {
            
            // SlogAction.sendATCommand(engconstents.ENG_AT_SETARMLOG,
            // mModem.isChecked());
        }
    };

    @Override
    public void onDestroy() {
        unbindSlogUIService();
        unbindService(mMCSConnection);
        super.onDestroy();
    }

    @Override
    void commit() {

    }

    @Override
    protected void onClearLogStarted(boolean enabled) {
        if (mClear != null) {
            mClear.setEnabled(enabled);
        }
        super.onClearLogStarted(enabled);
    }

    @Override
    protected void onClearLogEnded() {
        if (mClear != null) {
            mClear.setEnabled(true);
        }
        super.onClearLogEnded();
    }

    @Override
    protected void onDumpLogStarted(boolean enabled) {
        if (mDump != null) {
            mDump.setEnabled(enabled);
        }
        super.onDumpLogStarted(enabled);
    }

    @Override
    protected void onDumpLogEnded() {
        if (mDump != null) {
            mDump.setEnabled(SlogAction.getState(SlogAction.STORAGEKEY));
        }
        super.onDumpLogEnded();
    }
}
