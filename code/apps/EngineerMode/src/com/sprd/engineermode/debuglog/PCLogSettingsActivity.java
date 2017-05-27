
package com.sprd.engineermode.debuglog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.PowerManager;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.telephony.TelephonyManager;
import com.sprd.engineermode.telephony.TelephonyManagerSprd;
import android.util.Log;
import android.widget.Toast;
import com.sprd.engineermode.debuglog.slogui.SlogAction;
import com.sprd.engineermode.debuglog.slogui.SlogUICommonControl;
import android.provider.Settings;

import com.sprd.engineermode.R;
import com.sprd.engineermode.engconstents;
import com.sprd.engineermode.utils.IATUtils;
import com.sprd.engineermode.utils.SocketUtils;
import com.sprd.engineermode.EngineerModeActivity;

public class PCLogSettingsActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String TAG = "PCLogSettingsActivity";

    private static final String SOCKET_NAME = "wcnd";
    private static final String CP_ASSERT_FLAG = "sys/module/smsg/parameters/assert_trigger";

    private static final String CP2_LOG_GET_CMD = "at+armlog?";
    private static final String CP2_LOG_SET_CMD = "at+armlog=";
    private static final String AT_OPEN_CP2 = "poweron";

    private static final String PROPERTY_LOGCAT = "persist.sys.logstate";
    private static final String PROPERTY_CP2_LOG = "persist.sys.cp2log";
    private static final String KEY_DSP_LOG = "dsplog_enable";
    private static final String KEY_ARM_LOG = "modemlog_enable";
    private static final String KEY_CP2_LOG = "cp2log_enable";
    private static final String KEY_AUDIO_LOG = "audiolog_enable";
    private static final String KEY_CAP_LOG = "caplog_enable";
    private static final String KEY_ENGPC_LOG = "engpc_service";
    private static final String KEY_WCDMA_IQ = "wcdma_iq";
    private static final String KEY_MEMORY_LEAK = "memory_leak";
    private static final String KEY_CP_ASSERT = "cp_assert_helper";
    private static final String KEY_WMS_DEBUG_LOG = "wms_log";
    private static final String KEY_INPUT_DEBUG_LOG = "input_log";
    private static final String KEY_ART_DEBUG_LOG = "art_log";
    private static final String KEY_SLEEP_LOG = "sleep_log";

    // Browser debug
    private static final String KEY_BROWSER_LOG_ENABELED = "log_enabled";
    private static final String KEY_SAVE_BROWSER_RECEIVE = "save_receive";
    private static final String KEY_DUMP_BROWSER_TREE = "dump_tree";

    private static final int OPEN_CP2 = 0;
    private static final int GET_ARM_LOG = 1;
    private static final int SET_ARM_LOG_OPEN = 2;
    private static final int SET_ARM_LOG_CLOSE = 3;
    private static final int GET_DSP_LOG = 4;
    private static final int SET_DSP_LOG = 5;
    private static final int GET_CAP_LOG = 6;
    private static final int SET_CAP_LOG_OPEN = 7;
    private static final int SET_CAP_LOG_CLOSE = 8;
    private static final int GET_AUDIO_LOG = 9;
    private static final int SET_AUDIO_LOG_OPEN = 10;
    private static final int SET_AUDIO_LOG_CLOSE = 11;
    private static final int GET_CP2_LOG = 12;
    private static final int SET_CP2_LOG_OPEN = 13;
    private static final int SET_CP2_LOG_CLOSE = 14;
    private static final int MEMORY_LEAK = 15;
    private static final int SET_SLEEP_LOG = 16;

    private TwoStatePreference mARMLogPref;
    private TwoStatePreference mCP2LogPref;
    private TwoStatePreference mAudioLogPref;
    private TwoStatePreference mCapLogPref;
    private TwoStatePreference mEngPcPref;
    private TwoStatePreference mCPAssertPref;
    private TwoStatePreference mWmsDebugLog;
    private TwoStatePreference mInputDebugLog;
    private TwoStatePreference mArtDebugLog;
    private ListPreference mDSPLogPref;
    private Preference mWCDMAIqPref;
    private Preference mMemoryLeakPref;
    private Preference mSleeplogPref;
    private TwoStatePreference mBrowserLogEnabled;
    private TwoStatePreference mSaveBrowserReceive;
    private TwoStatePreference mDumpBrowserTree;

    private boolean stateResponse;
    private String mLastDspPref = null;
    private SharedPreferences mSharePref;
    private LocalSocket mClient;
    private OutputStream mOut;
    private boolean mSlogModemChecked;
    // private boolean mSlogState;
    private String mEngPc;
    private String mConfirmKey;

    private Handler mUiThread = new Handler();
    private LogSettingHandler logSettingHandler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_pc_log_setting);

        mARMLogPref = (TwoStatePreference) findPreference(KEY_ARM_LOG);
        mARMLogPref.setOnPreferenceChangeListener(this);
        mCP2LogPref = (TwoStatePreference) findPreference(KEY_CP2_LOG);
        mCP2LogPref.setOnPreferenceChangeListener(this);
        if (SystemProperties.get("ro.modem.wcn.enable", "0").equals("0")) {
            mCP2LogPref.setChecked(false);
            mCP2LogPref.setSummary(R.string.feature_not_support);
            mCP2LogPref.setEnabled(false);
        }

        mAudioLogPref = (TwoStatePreference) findPreference(KEY_AUDIO_LOG);
        mAudioLogPref.setOnPreferenceChangeListener(this);
        mCapLogPref = (TwoStatePreference) findPreference(KEY_CAP_LOG);
        mCapLogPref.setOnPreferenceChangeListener(this);
        mDSPLogPref = (ListPreference) findPreference(KEY_DSP_LOG);
        mDSPLogPref.setOnPreferenceChangeListener(this);
        mEngPcPref = (TwoStatePreference) findPreference(KEY_ENGPC_LOG);
        mEngPcPref.setOnPreferenceChangeListener(this);
        mCPAssertPref = (TwoStatePreference) findPreference(KEY_CP_ASSERT);
        mCPAssertPref.setOnPreferenceChangeListener(this);
        mWCDMAIqPref = (Preference) findPreference(KEY_WCDMA_IQ);
        mMemoryLeakPref = (Preference) findPreference(KEY_MEMORY_LEAK);
        mMemoryLeakPref.setOnPreferenceClickListener(this);
        mWmsDebugLog = (TwoStatePreference) findPreference(KEY_WMS_DEBUG_LOG);
        mWmsDebugLog.setOnPreferenceChangeListener(this);
        mInputDebugLog = (TwoStatePreference) findPreference(KEY_INPUT_DEBUG_LOG);
        mInputDebugLog.setOnPreferenceChangeListener(this);
        mArtDebugLog = (TwoStatePreference) findPreference(KEY_ART_DEBUG_LOG);
        mArtDebugLog.setOnPreferenceChangeListener(this);
        mSleeplogPref = (Preference) findPreference(KEY_SLEEP_LOG);
        mSleeplogPref.setOnPreferenceClickListener(this);
        mBrowserLogEnabled = (TwoStatePreference) findPreference(KEY_BROWSER_LOG_ENABELED);
        mBrowserLogEnabled.setOnPreferenceChangeListener(this);
        mSaveBrowserReceive = (TwoStatePreference) findPreference(KEY_SAVE_BROWSER_RECEIVE);
        mSaveBrowserReceive.setOnPreferenceChangeListener(this);
        mDumpBrowserTree = (TwoStatePreference) findPreference(KEY_DUMP_BROWSER_TREE);
        mDumpBrowserTree.setOnPreferenceChangeListener(this);
        HandlerThread ht = new HandlerThread("LogSettingHandler");
        ht.start();
        logSettingHandler = new LogSettingHandler(ht.getLooper());

        // modify 335691 by sprd
        // WCDMA IQ function is support by WCDMA modem type
        if (TelephonyManagerSprd.getModemType() != TelephonyManagerSprd.MODEM_TYPE_WCDMA) {
            mWCDMAIqPref.setEnabled(false);
            mWCDMAIqPref.setSummary(R.string.feature_not_support);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mDSPLogPref != null) {
            mLastDspPref = (String) mDSPLogPref.getValue();
            mDSPLogPref.setSummary(mDSPLogPref.getEntry());
        }
        if (mARMLogPref != null) {
            Message armLog = logSettingHandler.obtainMessage(GET_ARM_LOG);
            logSettingHandler.sendMessage(armLog);
        }
        if (mCP2LogPref != null && mCP2LogPref.isEnabled()) {
            Message cp2Open = logSettingHandler.obtainMessage(OPEN_CP2);
            logSettingHandler.sendMessage(cp2Open);
        }
        if (mCapLogPref != null) {
            Message capLog = logSettingHandler.obtainMessage(GET_CAP_LOG);
            logSettingHandler.sendMessage(capLog);
        }
        if (mDSPLogPref != null) {
            Message dspLog = logSettingHandler.obtainMessage(GET_DSP_LOG);
            logSettingHandler.sendMessage(dspLog);
        }
        if (mEngPcPref != null) {
            mEngPc = SystemProperties.get("persist.sys.engpc.disable");
            if (mEngPc.equals("1")) {
                mEngPcPref.setChecked(false);
            } else {
                mEngPcPref.setChecked(true);
            }
        }
        if (mCPAssertPref != null) {
            if (readFile(CP_ASSERT_FLAG).contains("1")) {
                mCPAssertPref.setChecked(true);
            } else {
                mCPAssertPref.setChecked(false);
            }
        }
        if (mWmsDebugLog != null && mWmsDebugLog.isEnabled()) {
            mWmsDebugLog.setChecked(SystemProperties.getBoolean("persist.sys.wms.log",false));
        }
        if (mInputDebugLog != null && mInputDebugLog.isEnabled()) {
            mInputDebugLog.setChecked(SystemProperties.getBoolean("persist.sys.input.log",false));
        }
        if (mArtDebugLog != null && mArtDebugLog.isEnabled()) {
            mArtDebugLog.setChecked(SystemProperties.getBoolean("persist.sys.art.log.sprd_debug",false));
        }
        if (mBrowserLogEnabled != null) {
            boolean log_enabled = SystemProperties.getBoolean("persist.sys.br.log",false);
            mBrowserLogEnabled.setChecked(log_enabled);
        }
        if (mSaveBrowserReceive != null) {
            boolean save_receive = SystemProperties.getBoolean("persist.sys.br.save",false);
            mSaveBrowserReceive.setChecked(save_receive);
        }
        if (mDumpBrowserTree != null) {
            boolean dump_tree = SystemProperties.getBoolean("persist.sys.br.dump",false);
            mDumpBrowserTree.setChecked(dump_tree);
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        if (logSettingHandler != null) {
            Log.d(TAG, "HandlerThread has quit");
            logSettingHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    private void alertUnallowDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("modem log has been checked in slog");
        alert.setCancelable(false);
        alert.setNeutralButton("Confirm",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mEngPcPref.setChecked(false);
                    }
                });
        alert.create();
        alert.show();
    }

    public void sendMsgToModemd(String msg) {
        try {
            mClient = new LocalSocket();
            LocalSocketAddress serverAddress = new LocalSocketAddress("modemd_engpc",
                    LocalSocketAddress.Namespace.ABSTRACT);
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

    @Override
    public boolean onPreferenceClick(Preference pref) {
        Log.d(TAG, "EngineerModeActivity.mIsFirst = " + EngineerModeActivity.mIsFirst);
        if (pref == mMemoryLeakPref) {
            if (!EngineerModeActivity.mIsFirst) {
                AlertDialog.Builder alert = new AlertDialog.Builder(PCLogSettingsActivity.this);
                alert.setMessage(R.string.memory_leak_confirm);
                alert.setCancelable(false);
                alert.setPositiveButton(getString(R.string.alertdialog_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                EngineerModeActivity.mIsFirst = true;
                                Message m = logSettingHandler.obtainMessage(MEMORY_LEAK);
                                logSettingHandler.sendMessage(m);
                            }
                        });
                alert.setNegativeButton(getString(R.string.alertdialog_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        });
                alert.create();
                alert.show();
            } else {
                Toast.makeText(PCLogSettingsActivity.this,
                        R.string.memory_leak_tip, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
        } else if (pref == mSleeplogPref) {
            Message setSleeplog = logSettingHandler.obtainMessage(SET_SLEEP_LOG);
            logSettingHandler.sendMessage(setSleeplog);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        int logType = 0;
        if (pref == mARMLogPref) {
            if (mARMLogPref.isChecked()) {
                logType = SET_ARM_LOG_CLOSE;
            } else {
                logType = SET_ARM_LOG_OPEN;
            }
        } else if (pref == mCP2LogPref) {
            if (mCP2LogPref.isChecked()) {
                logType = SET_CP2_LOG_CLOSE;
            } else {
                logType = SET_CP2_LOG_OPEN;
            }
        } else if (pref == mAudioLogPref) {
            if (mAudioLogPref.isChecked()) {
                logType = SET_AUDIO_LOG_CLOSE;
            } else {
                logType = SET_AUDIO_LOG_OPEN;
            }
        } else if (pref == mCapLogPref) {
            if (mCapLogPref.isChecked()) {
                logType = SET_CAP_LOG_CLOSE;
            } else {
                logType = SET_CAP_LOG_OPEN;
            }
        } else if (pref == mEngPcPref) {
            if (!mEngPcPref.isChecked()) {
                if (SlogAction.CP0_ENABLE && SlogAction.getState(SlogAction.CP0KEY)
                        || SlogAction.CP1_ENABLE && SlogAction.getState(SlogAction.CP1KEY)
                        || SlogAction.CP2_ENABLE && SlogAction.getState(SlogAction.CP2KEY)
                        || SlogAction.CP3_ENABLE && SlogAction.getState(SlogAction.CP3KEY)
                        || SlogAction.CP4_ENABLE && SlogAction.getState(SlogAction.CP4KEY)
                        || SlogAction.CP5_ENABLE && SlogAction.getState(SlogAction.CP5KEY)) {
                    SlogAction.setState(SlogAction.CP0KEY, true);
                    SlogAction.setState(SlogAction.CP1KEY, true);
                    SlogAction.setState(SlogAction.CP2KEY, true);
                    SlogAction.setState(SlogAction.CP3KEY, true);
                    SlogAction.setState(SlogAction.CP4KEY, true);
                    SlogAction.setState(SlogAction.CP5KEY, true);
                    SlogAction.writeSlogConfig();
                    SystemProperties.set("persist.sys.engpc.disable", "1");
                    sendMsgToModemd("persist.engpc.disable");
                    alertUnallowDialog();
                } else {
                    SystemProperties.set("persist.sys.engpc.disable", "0");
                    sendMsgToModemd("persist.engpc.disable");
                }
            } else {
                SystemProperties.set("persist.sys.engpc.disable", "1");
                sendMsgToModemd("persist.engpc.disable");
            }
            return true;
        } else if (pref == mDSPLogPref) {
            String re = String.valueOf(objValue);
            Log.d(TAG, "mDSPLogPref value=" + re);
            Message setDSPLog = logSettingHandler.obtainMessage(SET_DSP_LOG, re);
            logSettingHandler.sendMessage(setDSPLog);
            return true;
        } else if (pref == mCPAssertPref) {
            if (!mCPAssertPref.isChecked()) {
                execShellStr("echo 1 > " + CP_ASSERT_FLAG);
            } else {
                execShellStr("echo 0 > " + CP_ASSERT_FLAG);
            }
            return true;
        } else if (pref == mWmsDebugLog) {
            confirmSetting(KEY_WMS_DEBUG_LOG);
            return true;
        } else if (pref == mInputDebugLog) {
            confirmSetting(KEY_INPUT_DEBUG_LOG);
            return true;
        } else if (pref == mArtDebugLog) {
            confirmSetting(KEY_ART_DEBUG_LOG);
            return true;
        } else if (pref == mBrowserLogEnabled) {
            if (!mBrowserLogEnabled.isChecked()) {
                SystemProperties.set("persist.sys.br.log", "true");
            } else {
                SystemProperties.set("persist.sys.br.log", "false");
            }
            return true;
        } else if (pref == mSaveBrowserReceive) {
            if (!mSaveBrowserReceive.isChecked()) {
                SystemProperties.set("persist.sys.br.save", "true");
            } else {
                SystemProperties.set("persist.sys.br.save", "false");
            }
            return true;
        } else if (pref == mDumpBrowserTree) {
            if (!mDumpBrowserTree.isChecked()) {
                SystemProperties.set("persist.sys.br.dump", "true");
            } else {
                SystemProperties.set("persist.sys.br.dump", "false");
            }
            return true;
        }
        Message m = logSettingHandler.obtainMessage(logType);
        logSettingHandler.sendMessage(m);
        return true;
    }

    private void confirmSetting(String key) {
        mConfirmKey = key;
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.restart_ue_to_effective_log);
        alert.setCancelable(false);
        alert.setPositiveButton(getString(R.string.alertdialog_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mConfirmKey.equals(KEY_WMS_DEBUG_LOG)) {
                            SystemProperties.set("persist.sys.wms.log",
                                    mWmsDebugLog.isChecked() ? "1" : "0");
                        } else if (mConfirmKey.equals(KEY_INPUT_DEBUG_LOG)) {
                            SystemProperties.set("persist.sys.input.log",
                                    mInputDebugLog.isChecked() ? "1" : "0");
                        } else if (mConfirmKey.equals(KEY_ART_DEBUG_LOG)) {
                            SystemProperties.set("persist.sys.art.log.sprd_debug",
                                    mArtDebugLog.isChecked() ? "1" : "0");
                        }
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        pm.reboot(null);
                    }
                });
        alert.setNegativeButton(getString(R.string.alertdialog_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mConfirmKey.equals(KEY_WMS_DEBUG_LOG)) {
                            mWmsDebugLog.setChecked(!mWmsDebugLog.isChecked());
                        } else if (mConfirmKey.equals(KEY_INPUT_DEBUG_LOG)) {
                            mInputDebugLog.setChecked(!mInputDebugLog.isChecked());
                        } else if (mConfirmKey.equals(KEY_ART_DEBUG_LOG)) {
                            mArtDebugLog.setChecked(!mArtDebugLog.isChecked());
                        }
                    }
                });
        alert.create();
        alert.show();
    }

    private String SendAt(String cmd, String serverName) {
        String strTmp = IATUtils.sendATCmd(cmd, serverName);
        return strTmp;
    }

    private String SendCP2At(String cmd) {
        Log.d(TAG, "SendCP2At is " + cmd);
        String strTmp = SocketUtils.sendCmdAndRecResult(SOCKET_NAME,
                LocalSocketAddress.Namespace.ABSTRACT, cmd);
        return strTmp;
    }

    private String analysisResponse(String response, int type) {
        Log.d(TAG, "analysisResponse response= " + response + "type = " + type);
        if (response != null && response.contains(IATUtils.AT_OK)) {
            if (type == GET_ARM_LOG
                    || type == GET_CAP_LOG
                    || type == GET_DSP_LOG) {
                String[] str = response.split("\n");
                String[] str1 = str[0].split(":");
                Log.d(TAG, type + "  " + str1[1]);
                return str1[1].trim();
            } else if (type == SET_ARM_LOG_CLOSE
                    || type == SET_ARM_LOG_OPEN
                    || type == SET_CAP_LOG_CLOSE
                    || type == SET_CAP_LOG_OPEN
                    || type == SET_AUDIO_LOG_CLOSE
                    || type == SET_AUDIO_LOG_OPEN
                    || type == SET_DSP_LOG) {
                return IATUtils.AT_OK;
            }
        }

        if (type == GET_CP2_LOG
                || type == SET_CP2_LOG_OPEN
                || type == SET_CP2_LOG_CLOSE) {
            if (response != null && !response.startsWith("Fail")) {
                if (type == GET_CP2_LOG) {
                    if (response.startsWith("FAIL")) {
                        return IATUtils.AT_FAIL;
                    } else {
                        String[] str1 = response.split(":");
                        Log.d(TAG, type + "  " + str1[1]);
                        return str1[1].trim();
                    }
                } else if (type == SET_CP2_LOG_OPEN
                        || type == SET_CP2_LOG_CLOSE) {
                    return IATUtils.AT_OK;
                }
            }
        }
        return IATUtils.AT_FAIL;

    }

    class LogSettingHandler extends Handler {

        public LogSettingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String atResponse;
            String responValue;
            switch (msg.what) {
                case OPEN_CP2: {
                    atResponse = SendCP2At("wcn " + AT_OPEN_CP2);
                    Log.d(TAG, "OPEN_CP2 Response is " + atResponse);
                    if (atResponse != null && atResponse.contains(SocketUtils.OK)) {
                        Message cp2Log = logSettingHandler.obtainMessage(GET_CP2_LOG);
                        logSettingHandler.sendMessage(cp2Log);
                        Log.d(TAG,"CP2 Open Success");
                    } else {
                        Log.d(TAG,"CP2 Open Fail");
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCP2LogPref.setEnabled(false);
                                mCP2LogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                    }
                }
                     break;
                case GET_ARM_LOG: {
                    atResponse = SendAt(engconstents.ENG_AT_GETARMLOG1,
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.GET_ARM_LOG);
                    if (responValue.equals(IATUtils.AT_FAIL)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "GET_ARM_LOG Fail");
                                mARMLogPref.setEnabled(false);
                                mARMLogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                    }
                    if (responValue.trim().equals("1")) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mARMLogPref.setChecked(true);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mARMLogPref.setChecked(false);
                            }
                        });
                    }
                }
                    break;
                case SET_ARM_LOG_CLOSE: {
                    atResponse = SendAt(engconstents.ENG_AT_SETARMLOG1 + "0",
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_ARM_LOG_CLOSE);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mARMLogPref.setChecked(false);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mARMLogPref.setChecked(true);
                            }
                        });
                    }
                }
                    break;
                case SET_ARM_LOG_OPEN: {
                    atResponse = SendAt(engconstents.ENG_AT_SETARMLOG1 + "1",
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_ARM_LOG_OPEN);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mARMLogPref.setChecked(true);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mARMLogPref.setChecked(false);
                            }
                        });
                    }
                }
                    break;
                case GET_CP2_LOG: {
                    atResponse = SendCP2At("wcn " + CP2_LOG_GET_CMD);
                    // modify 334340 by sprd start
                    if (atResponse == null) {
                        Log.d(TAG, "GET_CP2_LOG Response is null");
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCP2LogPref.setEnabled(false);
                                mCP2LogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                        break;
                    }
                    // modify 334340 by sprd end
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.GET_CP2_LOG);
                    if (responValue.equals(IATUtils.AT_FAIL)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "GET_CP2_LOG Fail");
                                mCP2LogPref.setEnabled(false);
                                mCP2LogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                    } else {
                        if (responValue.trim().equals("1")) {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mCP2LogPref.setChecked(true);
                                }
                            });
                        } else {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mCP2LogPref.setChecked(false);
                                }
                            });
                        }
                    }
                }
                    break;
                case SET_CP2_LOG_OPEN: {
                    atResponse = SendCP2At("wcn " + CP2_LOG_SET_CMD + "1");
                    // modify 334340 by sprd start
                    if (atResponse == null) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "SET_CP2_LOG_OPEN Response is null");
                                mCP2LogPref.setEnabled(false);
                                mCP2LogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                        break;
                    }
                    // modify 334340 by sprd end
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_CP2_LOG_OPEN);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCP2LogPref.setChecked(true);
                                SystemProperties.set(PROPERTY_CP2_LOG, "1");
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCP2LogPref.setChecked(false);
                            }
                        });
                    }
                }
                    break;
                case SET_CP2_LOG_CLOSE: {
                    atResponse = SendCP2At("wcn " + CP2_LOG_SET_CMD + "0");
                    // modify 334340 by sprd start
                    if (atResponse == null) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "SET_CP2_LOG_CLOSE Response is null");
                                mCP2LogPref.setEnabled(false);
                                mCP2LogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                        break;
                    }
                    // modify 334340 by sprd end
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_CP2_LOG_CLOSE);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCP2LogPref.setChecked(false);
                                SystemProperties.set(PROPERTY_CP2_LOG, "0");
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCP2LogPref.setChecked(true);
                            }
                        });
                    }
                }
                    break;
                case SET_AUDIO_LOG_CLOSE: {
                    atResponse = IATUtils.sendATCmd(
                            engconstents.ENG_AT_SETAUDIOLOG + "0", "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_AUDIO_LOG_CLOSE);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAudioLogPref.setChecked(false);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAudioLogPref.setChecked(true);
                            }
                        });
                    }
                }
                    break;
                case SET_AUDIO_LOG_OPEN: {
                    atResponse = IATUtils.sendATCmd(
                            engconstents.ENG_AT_SETAUDIOLOG + "4096",
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_AUDIO_LOG_OPEN);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAudioLogPref.setChecked(true);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAudioLogPref.setChecked(false);
                            }
                        });
                    }

                }
                    break;
                case GET_CAP_LOG: {
                    atResponse = SendAt(engconstents.ENG_AT_GETCAPLOG1,
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.GET_CAP_LOG);
                    if (responValue.equals(IATUtils.AT_FAIL)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "GET_CAP_LOG Fail");
                                mCapLogPref.setEnabled(false);
                                mCapLogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                    }
                    if (responValue.trim().equals("1")) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCapLogPref.setChecked(true);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCapLogPref.setChecked(false);
                            }
                        });
                    }
                }
                    break;
                case SET_CAP_LOG_CLOSE: {
                    atResponse = SendAt(engconstents.ENG_AT_SETCAPLOG1 + "0",
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_CAP_LOG_CLOSE);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCapLogPref.setChecked(false);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCapLogPref.setChecked(true);
                            }
                        });
                    }
                }
                    break;
                case SET_CAP_LOG_OPEN: {
                    atResponse = SendAt(engconstents.ENG_AT_SETCAPLOG1 + "1",
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_CAP_LOG_OPEN);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCapLogPref.setChecked(true);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mCapLogPref.setChecked(false);
                            }
                        });
                    }
                }
                    break;
                case GET_DSP_LOG: {
                    atResponse = SendAt(engconstents.ENG_AT_GETDSPLOG1, "atchannel0");
                    responValue = analysisResponse(atResponse, PCLogSettingsActivity.GET_DSP_LOG);
                    if (atResponse.contains(IATUtils.AT_OK)) {
                        if (responValue.trim().equals("1")) {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDSPLogPref.setValueIndex(1);
                                    mDSPLogPref.setSummary(mDSPLogPref.getEntry());
                                }
                            });
                        } else if (responValue.trim().equals("2")) {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDSPLogPref.setValueIndex(2);
                                    mDSPLogPref.setSummary(mDSPLogPref.getEntry());
                                }
                            });
                        } else {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDSPLogPref.setValueIndex(0);
                                    mDSPLogPref.setSummary(mDSPLogPref.getEntry());
                                }
                            });
                        }
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "GET_DSP_LOG Fail");
                                mDSPLogPref.setEnabled(false);
                                mDSPLogPref.setSummary(R.string.feature_abnormal);
                            }
                        });
                    }
                }
                    break;
                case SET_DSP_LOG: {
                    String valueStr = (String) msg.obj;
                    atResponse = SendAt(engconstents.ENG_AT_SETDSPLOG1 + valueStr,
                            "atchannel0");
                    responValue = analysisResponse(atResponse,
                            PCLogSettingsActivity.SET_DSP_LOG);
                    if (responValue.contains(IATUtils.AT_OK)) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mDSPLogPref.setSummary(mDSPLogPref.getEntry());
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mDSPLogPref.setValueIndex(Integer.valueOf(mLastDspPref));
                                Message dspLog = logSettingHandler.obtainMessage(GET_DSP_LOG);
                                logSettingHandler.sendMessage(dspLog);
                            }
                        });
                    }
                }
                    break;
                case MEMORY_LEAK: {
                    atResponse = SendAt("AT+SPENGMD=1,8,1,0", "atchannel0");
                    if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                        Log.d(TAG, "dump memory leak log sucess");
                    } else {
                        Log.d(TAG, "dump memory leak log fail");
                    }
                }
                    break;
                case SET_SLEEP_LOG: {
                    atResponse = SendAt(engconstents.ENG_SET_AT_SLEEP_LOG, "atchannel0");
                    if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                        Log.d(TAG, "sleeplog sucess");
                    } else {
                        Log.d(TAG, "sleeplog fail");
                    }
                }
                break;

            }
        }
    }

    // read file
    public String readFile(String path) {
        File file = new File(path);
        String str = new String("");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                str = str + line;
            }
        } catch (Exception e) {
            Log.d(TAG, "Read file error!!!");
            str = "readError";
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        Log.d(TAG, "read " + path + " value is " + str.trim());
        return str.trim();
    }

    // exec shell cmd
    private String execShellStr(String cmd) {
        String[] cmdStrings = new String[] {
                "sh", "-c", cmd
        };
        StringBuffer retString = new StringBuffer("");

        try {
            Process process = Runtime.getRuntime().exec(cmdStrings);
            BufferedReader stdout = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), "UTF-8"), 7777);
            BufferedReader stderr = new BufferedReader(new InputStreamReader(
                    process.getErrorStream(), "UTF-8"), 7777);

            String line = null;

            while ((null != (line = stdout.readLine()))
                    || (null != (line = stderr.readLine()))) {
                if ("" != line) {
                    retString = retString.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, cmd + ":" + retString.toString() + "");
        return retString.toString();
    }
}
