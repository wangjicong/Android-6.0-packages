package com.sprd.engineermode.debuglog.slogui;

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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;

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
import android.content.Intent;
import android.os.SystemClock;
import com.sprd.engineermode.debuglog.slogui.SlogAction;
import com.sprd.engineermode.debuglog.slogui.SlogUICommonControl;
import android.os.PowerManager;

import com.sprd.engineermode.R;
import com.sprd.engineermode.engconstents;
import com.sprd.engineermode.utils.IATUtils;
import com.sprd.engineermode.utils.SocketUtils;
import com.sprd.engineermode.EngineerModeActivity;

import java.util.Calendar;

public class ModemLogSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private static final String TAG = "ModemLogSettings";
    private static final String LOG_SCENARIOS_STATUS = "scenarios_status";
    private static final String LOG_SPECIAL_STATUS = "special_status";

    private static final String SOCKET_NAME = "wcnd";

    private static final String LOG_USER_CMD = "1,0,"
            + "\""
            + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
            + "\""
            + ","
            + "\""
            + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
            + "\"";
    private static final String LOG_DEBUG_CMD = "1,3,"
            + "\""
            + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
            + "\""
            + ","
            + "\""
            + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
            + "\"";
    private static final String LOG_FULL_CMD = "1,5,"
            + "\""
            + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
            + "\""
            + ","
            + "\""
            + "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
            + "\"";
    private String mSupportMode;
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
    private static final String KEY_SCENARIOS_LOG = "log_scenarios";
    private static final String KEY_LOG_OUTPUT_SETTINGS = "log_output_settings";
    private static final String KEY_LOG_SD_SETTINGS = "sd_settings";
    private static final String KEY_ART_DEBUG_LOG = "art_log";
    private static final String KEY_SAVE_SLEEPLOG = "save_sleeplog";
    private static final String KEY_SAVE_RINGBUF = "save_ringbuf";
    private static final String KEY_ENABLE_DUMP_MARLIN_MEM = "enable_dump_marlin_mem";
    private static final String KEY_DUMP_MARLIN_MEM = "dump_marlin_mem";
    private static final String KEY_GNSS_LOG = "gnss_enable";
    private static final String KEY_MD_STOR_POS = "mini_dump_patch_setting";

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
    private static final int SET_LOG_SCENARIOS_STATUS = 16;
    private static final int SET_LOG_OUTPUT_STYLE = 17;
    private static final int SET_SAVE_SLEEPLOG = 18;
    private static final int SET_SAVE_RINGBUF = 19;

    private static final int GET_ENABLE_DUMP_MARLIN = 20;
    private static final int SET_ENABLE_DUMP_MARLIN_OPEN = 21;
    private static final int SET_ENABLE_DUMP_MARLIN_CLOSE = 22;
    private static final int SET_DUMP_MARLIN_MEM = 23;
    private static final int SET_GNSS_LOG_OPEN = 24;
    private static final int SET_GNSS_LOG_CLOSE = 25;
    private static final int GET_GNSS_LOG_STATUS = 26;

    private static final int GET_MD_STOR_POS = 27;
    private static final int SET_MD_STOR_POS = 28;

    private static final int LOG_OUTPUT_STYLE_PC = 0;
    private static final int LOG_OUTPUT_STYLE_SD = 1;

    private TwoStatePreference mARMLogPref;
    private TwoStatePreference mCP2LogPref;
    private TwoStatePreference mCapLogPref;
    private ListPreference mDSPLogPref;
    private Preference mWCDMAIqPref;
    private Preference mMemoryLeakPref;
    private ListPreference mListPreferenceScenarios;
    private ListPreference mLogOutputSettings;
    private Preference mSdSettings;
    private TwoStatePreference mArtDebugLog;
    private Preference mSaveSleepLog;
    private Preference mSaveRingBuf;
    private Preference mDumpMarlinMem;
    private TwoStatePreference mEnableDumpMarlinMem;
    private TwoStatePreference mGnssLog;
    private ListPreference mMdStorPos;

    private boolean stateResponse;
    private String mLastDspPref = null;
    private SharedPreferences mSharePref;
    private LocalSocket mClient;
    private OutputStream mOut;
    private boolean mSlogModemChecked;
    private boolean isUser = false;
    private boolean isSupportW = false;
    // private boolean mSlogState;
    private String mEngPc;
    private String mConfirmKey;
    private int mScenariosStatus = 0;
    private static SharedPreferences pref;
    private Handler mUiThread = new Handler();
    private LogSettingHandler logSettingHandler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_modem_settings);
        HandlerThread ht = new HandlerThread("LogSettingHandler");
        ht.start();
        logSettingHandler = new LogSettingHandler(ht.getLooper());
        mARMLogPref = (TwoStatePreference) findPreference(KEY_ARM_LOG);
        mARMLogPref.setOnPreferenceChangeListener(this);
        mCP2LogPref = (TwoStatePreference) findPreference(KEY_CP2_LOG);
        mCP2LogPref.setOnPreferenceChangeListener(this);
        mCapLogPref = (TwoStatePreference) findPreference(KEY_CAP_LOG);
        mCapLogPref.setOnPreferenceChangeListener(this);
        mDSPLogPref = (ListPreference) findPreference(KEY_DSP_LOG);
        mDSPLogPref.setOnPreferenceChangeListener(this);
        mWCDMAIqPref = (Preference) findPreference(KEY_WCDMA_IQ);
        mMemoryLeakPref = (Preference) findPreference(KEY_MEMORY_LEAK);
        mMemoryLeakPref.setOnPreferenceClickListener(this);
        mListPreferenceScenarios = (ListPreference) findPreference(KEY_SCENARIOS_LOG);
        mListPreferenceScenarios.setOnPreferenceChangeListener(this);
        mLogOutputSettings = (ListPreference) findPreference(KEY_LOG_OUTPUT_SETTINGS);
        mLogOutputSettings.setOnPreferenceChangeListener(this);
        mSdSettings = (Preference) findPreference(KEY_LOG_SD_SETTINGS);
        mArtDebugLog = (TwoStatePreference) findPreference(KEY_ART_DEBUG_LOG);
        mArtDebugLog.setOnPreferenceChangeListener(this);
        mSaveSleepLog = (Preference) findPreference(KEY_SAVE_SLEEPLOG);
        mSaveSleepLog.setOnPreferenceClickListener(this);
        mSaveRingBuf = (Preference) findPreference(KEY_SAVE_RINGBUF);
        mSaveRingBuf.setOnPreferenceClickListener(this);
        mEnableDumpMarlinMem = (TwoStatePreference) findPreference(KEY_ENABLE_DUMP_MARLIN_MEM);
        mEnableDumpMarlinMem.setOnPreferenceChangeListener(this);
        mDumpMarlinMem = (Preference) findPreference(KEY_DUMP_MARLIN_MEM);
        mDumpMarlinMem.setOnPreferenceClickListener(this);
        mGnssLog = (TwoStatePreference) findPreference(KEY_GNSS_LOG);
        mGnssLog.setOnPreferenceChangeListener(this);
        mListPreferenceScenarios.setEnabled(false);
        mListPreferenceScenarios.setSummary(R.string.feature_not_support);

        if (SystemProperties.get("ro.modem.wcn.enable", "0").equals("0")) {
            mCP2LogPref.setChecked(false);
            mCP2LogPref.setSummary(R.string.feature_not_support);
            mCP2LogPref.setEnabled(false);
            mEnableDumpMarlinMem.setChecked(false);
            mEnableDumpMarlinMem.setSummary(R.string.feature_not_support);
            mEnableDumpMarlinMem.setEnabled(false);
            mDumpMarlinMem.setSummary(R.string.feature_not_support);
            mDumpMarlinMem.setEnabled(false);
        }
        mMdStorPos = (ListPreference) findPreference(KEY_MD_STOR_POS);
        mMdStorPos.setOnPreferenceChangeListener(this);

        if (SlogAction.CP0_ENABLE) {
            mSupportMode = "WCDMA";
        } else if (SlogAction.CP1_ENABLE) {
            mSupportMode = "TD";
        } else if (SlogAction.CP3_ENABLE) {
            mSupportMode = "5MODE";
        } else if (SlogAction.CP4_ENABLE) {
            mSupportMode = "TDD-LTE";
        } else if (SlogAction.CP5_ENABLE) {
            mSupportMode = "FDD-LTE";
        }
        if (mSupportMode == null) {
            mSaveSleepLog.setEnabled(false);
            mSaveRingBuf.setEnabled(false);
        }
        isSupportW = (TelephonyManager.getRadioCapbility() == TelephonyManager.RadioCapbility.FDD_CSFB
                || TelephonyManager.getRadioCapbility() == TelephonyManager.RadioCapbility.CSFB
                || TelephonyManagerSprd.getModemType() == TelephonyManagerSprd.MODEM_TYPE_WCDMA);
        if (isSupportW) {
            mListPreferenceScenarios
                    .setEntries(R.array.log_scenarios_w_entries);
            mListPreferenceScenarios
                    .setEntryValues(R.array.log_scenarios_w_values);
        } else {
            mListPreferenceScenarios.setEntries(R.array.log_scenarios_entries);
            mListPreferenceScenarios
                    .setEntryValues(R.array.log_scenarios_values);
            mWCDMAIqPref.setEnabled(false);
            mWCDMAIqPref.setSummary(R.string.feature_not_support);
        }
        isUser = SystemProperties.get("ro.build.type").equalsIgnoreCase("user");
        pref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLogOutputSettings != null) {
            if (SystemProperties.get("persist.sys.engpc.disable").equals("0")) {
                mLogOutputSettings.setValueIndex(0);
                mSdSettings.setEnabled(false);
            } else {
                mLogOutputSettings.setValueIndex(1);
                mSdSettings.setEnabled(true);
            }
            mLogOutputSettings.setSummary(mLogOutputSettings.getEntry());
        }
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
        if (mArtDebugLog != null && mArtDebugLog.isEnabled()) {
            mArtDebugLog.setChecked(SystemProperties.getBoolean("persist.sys.art.log.sprd_debug",false));
        }
        if (mDSPLogPref != null) {
            Message dspLog = logSettingHandler.obtainMessage(GET_DSP_LOG);
            logSettingHandler.sendMessage(dspLog);
        }
        if (mListPreferenceScenarios != null) {
            if (isUser) {
                mScenariosStatus = pref.getInt(LOG_SCENARIOS_STATUS, 0);
            } else {
                mScenariosStatus = pref.getInt(LOG_SCENARIOS_STATUS, 0);
            }
            if (isSupportW) {
                mListPreferenceScenarios.setValueIndex(mScenariosStatus);
            } else {
                if (mScenariosStatus == 3) {
                    mListPreferenceScenarios.setValueIndex(2);
                } else {
                    mListPreferenceScenarios.setValueIndex(mScenariosStatus);
                }
            }
            //mListPreferenceScenarios.setSummary(mListPreferenceScenarios.getEntry());
        }
        if (mEnableDumpMarlinMem != null && mEnableDumpMarlinMem.isEnabled()) {
            Message getEnableDumpMarlinState = logSettingHandler.obtainMessage(GET_ENABLE_DUMP_MARLIN);
            logSettingHandler.sendMessage(getEnableDumpMarlinState);
        }
        if (mGnssLog != null) {
            Message getGnssStatus = logSettingHandler.obtainMessage(GET_GNSS_LOG_STATUS);
            logSettingHandler.sendMessage(getGnssStatus);
        }
        if (mMdStorPos != null) {
            Message getMdStorPosStatus = logSettingHandler.obtainMessage(GET_MD_STOR_POS);
            logSettingHandler.sendMessage(getMdStorPosStatus);
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

    @Override
    public boolean onPreferenceClick(Preference pref) {
        Log.d(TAG, "EngineerModeActivity.mIsFirst = "
                + EngineerModeActivity.mIsFirst);
        if (pref == mMemoryLeakPref) {
            if (!EngineerModeActivity.mIsFirst) {
                AlertDialog.Builder alert = new AlertDialog.Builder(ModemLogSettings.this);
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
                Toast.makeText(ModemLogSettings.this,
                        R.string.memory_leak_tip, Toast.LENGTH_SHORT)
                        .show();
                return false;
            }
        }else if (pref == mSaveSleepLog) {
            mSaveSleepLog.setEnabled(false);
            Message m = logSettingHandler.obtainMessage(SET_SAVE_SLEEPLOG);
            logSettingHandler.sendMessage(m);
            return true;
        } else if (pref == mSaveRingBuf) {
            mSaveRingBuf.setEnabled(false);
            Message m = logSettingHandler.obtainMessage(SET_SAVE_RINGBUF);
            logSettingHandler.sendMessage(m);
            return true;
        } else if (pref == mDumpMarlinMem) {
            mDumpMarlinMem.setEnabled(false);
            Message m = logSettingHandler.obtainMessage(SET_DUMP_MARLIN_MEM);
            logSettingHandler.sendMessage(m);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        int logType = 0;
        final String re = String.valueOf(objValue);
        if (pref == mLogOutputSettings) {
            Message setLogOutput = logSettingHandler
                    .obtainMessage(SET_LOG_OUTPUT_STYLE, re);
            logSettingHandler.sendMessage(setLogOutput);
            return true;
        } else if (pref == mARMLogPref) {
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
        } else if (pref == mCapLogPref) {
            if (mCapLogPref.isChecked()) {
                logType = SET_CAP_LOG_CLOSE;
            } else {
                logType = SET_CAP_LOG_OPEN;
            }
        } else if (pref == mArtDebugLog) {
            confirmSetting(KEY_ART_DEBUG_LOG);
            return true;
        } else if (pref == mDSPLogPref) {
            Log.d(TAG, "mDSPLogPref value=" + re);
            Message setDSPLog = logSettingHandler
                    .obtainMessage(SET_DSP_LOG, re);
            logSettingHandler.sendMessage(setDSPLog);
            return true;
        } else if (pref == mListPreferenceScenarios) {
            Log.d(TAG, "mListPreferenceScenarios value=" + re);
            if (re.startsWith("2") && isSupportW) {
                Intent intentSpecial = new Intent("android.intent.action.SPECIAL");
                ModemLogSettings.this.startActivity(intentSpecial);
            } else if(re.startsWith("3") || (re.startsWith("2") && (!isSupportW))) {
                Intent intentCustomSet = new Intent("android.intent.action.CUSTOMSET");
                ModemLogSettings.this.startActivity(intentCustomSet);
            } else {
                Message setScenariosStatus = logSettingHandler.obtainMessage(SET_LOG_SCENARIOS_STATUS, re);
                logSettingHandler.sendMessage(setScenariosStatus);
                return true;
            }
            return true;
        } else if (pref == mEnableDumpMarlinMem) {
            if (mEnableDumpMarlinMem.isChecked()) {
                logType = SET_ENABLE_DUMP_MARLIN_CLOSE;
            } else {
                logType = SET_ENABLE_DUMP_MARLIN_OPEN;
            }
        } else if (pref == mGnssLog) {
            if (mGnssLog.isChecked()) {
                logType = SET_GNSS_LOG_CLOSE;
            } else {
                logType = SET_GNSS_LOG_OPEN;
            }
        } else if (pref == mMdStorPos) {
            Message setMdStorPos = logSettingHandler
                    .obtainMessage(SET_MD_STOR_POS, re);
            logSettingHandler.sendMessage(setMdStorPos);
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
                        if (mConfirmKey.equals(KEY_ART_DEBUG_LOG)) {
                            boolean lastStatus = SystemProperties.getBoolean(
                                    "persist.sys.art.log.sprd_debug", false);
                            SystemProperties.set(
                                    "persist.sys.art.log.sprd_debug",
                                    lastStatus ? "0" : "1");
                        }
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        pm.reboot(null);
                    }
                });
        alert.setNegativeButton(getString(R.string.alertdialog_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mConfirmKey.equals(KEY_ART_DEBUG_LOG)) {
                            mArtDebugLog.setChecked(SystemProperties
                                    .getBoolean(
                                            "persist.sys.art.log.sprd_debug",
                                            false));
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

    private String SendSlogModemAt(String cmd) {
        Log.d(TAG, "SendSlogModemAt " + cmd);
        String strTmp = SocketUtils.sendCmdAndRecResult("slogmodem",
                LocalSocketAddress.Namespace.ABSTRACT, cmd);
        return strTmp;
    }

    private String changeIndexToCmd(int value) {
        String cmd = null;
        switch (value) {
//        case 0:
//            cmd = engconstents.ENG_SET_LOG_LEVEL + LOG_USER_CMD;
//            break;
        case 0:
            cmd = engconstents.ENG_SET_LOG_LEVEL + LOG_DEBUG_CMD;
            break;
        case 1:
            cmd = engconstents.ENG_SET_LOG_LEVEL + LOG_FULL_CMD;
            break;
        default:
            break;
        }
        Log.d(TAG, "changeIndex is: " + value + "cmd is: " + cmd);
        return cmd;
    }

    private String analysisResponse(String response, int type) {
        Log.d(TAG, "analysisResponse response= " + response + "type = " + type);
        if (response != null && response.contains(IATUtils.AT_OK)) {
            if (type == GET_ARM_LOG || type == GET_CAP_LOG
                    || type == GET_DSP_LOG) {
                String[] str = response.split("\n");
                String[] str1 = str[0].split(":");
                Log.d(TAG, type + "  " + str1[1]);
                return str1[1].trim();
            } else if (type == SET_ARM_LOG_CLOSE || type == SET_ARM_LOG_OPEN
                    || type == SET_CAP_LOG_CLOSE || type == SET_CAP_LOG_OPEN
                    || type == SET_AUDIO_LOG_CLOSE
                    || type == SET_AUDIO_LOG_OPEN || type == SET_DSP_LOG) {
                return IATUtils.AT_OK;
            }
        }

        if (type == GET_CP2_LOG || type == SET_CP2_LOG_OPEN
                || type == SET_CP2_LOG_CLOSE) {
            if (response != null && !response.startsWith("Fail")) {
                if (type == GET_CP2_LOG) {
                    if (response.contains("FAIL")) {
                        return IATUtils.AT_FAIL;
                    } else {
                        String[] str1 = response.split(":");
                        if (str1.length < 2) {
							return IATUtils.AT_FAIL;
                        }
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
                    EngineerModeActivity.isCP2On = true;
                    Message cp2Log = logSettingHandler
                            .obtainMessage(GET_CP2_LOG);
                    logSettingHandler.sendMessage(cp2Log);
                    Log.d(TAG, "CP2 Open Success");
                } else {
                    Log.d(TAG, "CP2 Open Fail");
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
                        ModemLogSettings.GET_ARM_LOG);
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
                        ModemLogSettings.SET_ARM_LOG_CLOSE);
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
                        ModemLogSettings.SET_ARM_LOG_OPEN);
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
                        ModemLogSettings.GET_CP2_LOG);
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
                Calendar c = Calendar.getInstance();
                int month = c.get(Calendar.MONTH) + 1;
                int day = c.get(Calendar.DAY_OF_MONTH);
                int hour = c.get(Calendar.HOUR);
                int minute = c.get(Calendar.MINUTE);
                int second = c.get(Calendar.SECOND);
                int millisecond = c.get(Calendar.MILLISECOND);
                Log.d(TAG, "month: " + month + "day: " + day + "hour: " + hour
                        + "minute: " + minute + "second: " + second + "millisecond: "
                        + millisecond);
                String cmd = "wcn " + "at+aptime=" + month + "," + day + "," + hour + "," + minute + "," + second + "," + millisecond + "\r";
                String setTime = SendCP2At(cmd);
                Log.d(TAG,"the value of setTime is: " + setTime);
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
                        ModemLogSettings.SET_CP2_LOG_OPEN);
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
                        ModemLogSettings.SET_CP2_LOG_CLOSE);
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
            case GET_CAP_LOG: {
                atResponse = SendAt(engconstents.ENG_AT_GETCAPLOG1,
                        "atchannel0");
                responValue = analysisResponse(atResponse,
                        ModemLogSettings.GET_CAP_LOG);
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
                        ModemLogSettings.SET_CAP_LOG_CLOSE);
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
                        ModemLogSettings.SET_CAP_LOG_OPEN);
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
                atResponse = SendAt(engconstents.ENG_AT_GETDSPLOG1,
                        "atchannel0");
                responValue = analysisResponse(atResponse,
                        ModemLogSettings.GET_DSP_LOG);
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
                        ModemLogSettings.SET_DSP_LOG);
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
                            mDSPLogPref.setValueIndex(Integer
                                    .valueOf(mLastDspPref));
                            Message dspLog = logSettingHandler
                                    .obtainMessage(GET_DSP_LOG);
                            logSettingHandler.sendMessage(dspLog);
                        }
                    });
                }
            }
                break;
            case SET_LOG_SCENARIOS_STATUS:
                int value = Integer.parseInt((String) msg.obj);
                Log.d(TAG, "setValueIndex is: " + value);
                String atCmd = changeIndexToCmd(value);
                atResponse = IATUtils.sendATCmd(atCmd, "atchannel0");
                  Log.d(TAG, atCmd + ": " + atResponse);
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mScenariosStatus = value;
                    pref.edit()
                    .putInt(LOG_SCENARIOS_STATUS, value)
                    .putInt(LOG_SPECIAL_STATUS, 0)
                    .commit();
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Set log level success");
                            mListPreferenceScenarios
                                    .setValueIndex(mScenariosStatus);
                            mListPreferenceScenarios
                                    .setSummary(mListPreferenceScenarios
                                            .getEntry());

                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Set log level fail");
                            mListPreferenceScenarios
                                    .setValueIndex(mScenariosStatus);
                            mListPreferenceScenarios
                                    .setSummary(mListPreferenceScenarios
                                            .getEntry());
                        }
                    });
                }
                break;
            case SET_LOG_OUTPUT_STYLE:
                final int outputvalue = Integer.parseInt((String) msg.obj);
                Log.d(TAG, "setValueIndex is: " + outputvalue);
                if (outputvalue == 0) {
                    if (SlogAction.CP0_ENABLE) {
                        SendSlogModemAt("DISABLE_LOG WCDMA");
                        SlogAction.setState(SlogAction.CP0KEY, false);
                        }
                    if (SlogAction.CP1_ENABLE) {
                        SendSlogModemAt("DISABLE_LOG TD");
                        SlogAction.setState(SlogAction.CP1KEY, false);
                        }
                    if (SlogAction.CP2_ENABLE) {
                        SendSlogModemAt("DISABLE_LOG WCN");
                        SlogAction.setState(SlogAction.CP2KEY, false);
                        }
                    if (SlogAction.CP3_ENABLE) {
                        SendSlogModemAt("DISABLE_LOG TDD-LTE");
                        SlogAction.setState(SlogAction.CP3KEY, false);
                        }
                    if (SlogAction.CP4_ENABLE) {
                        SendSlogModemAt("DISABLE_LOG FDD-LTE");
                        SlogAction.setState(SlogAction.CP4KEY, false);
                        }
                    if (SlogAction.CP5_ENABLE) {
                        SendSlogModemAt("DISABLE_LOG 5MODE");
                        SlogAction.setState(SlogAction.CP5KEY, false);
                        }
                        SlogAction.writeSlogConfig();
                        SystemProperties.set("persist.sys.engpc.disable", "0");
                        sendMsgToModemd("persist.engpc.disable");
                        SendCP2At("wcn startengpc");
                } else if (outputvalue == 1) {
                    if (SlogAction.CP0_ENABLE) {
                        SendSlogModemAt("ENABLE_LOG WCDMA");
                        SlogAction.setState(SlogAction.CP0KEY, true);
                        }
                    if (SlogAction.CP1_ENABLE) {
                        SendSlogModemAt("ENABLE_LOG TD");
                        SlogAction.setState(SlogAction.CP1KEY, true);
                        }
                    if (SlogAction.CP2_ENABLE) {
                        SendSlogModemAt("ENABLE_LOG WCN");
                        SlogAction.setState(SlogAction.CP2KEY, true);
                        }
                    if (SlogAction.CP3_ENABLE) {
                        SendSlogModemAt("ENABLE_LOG TDD-LTE");
                        SlogAction.setState(SlogAction.CP3KEY, true);
                        }
                    if (SlogAction.CP4_ENABLE) {
                        SendSlogModemAt("ENABLE_LOG FDD-LTE");
                        SlogAction.setState(SlogAction.CP4KEY, true);
                        }
                    if (SlogAction.CP5_ENABLE) {
                        SendSlogModemAt("ENABLE_LOG 5MODE");
                        SlogAction.setState(SlogAction.CP5KEY, true);
                        }
                    SystemProperties.set("persist.sys.engpc.disable", "1");
                    sendMsgToModemd("persist.engpc.disable");
                    SendCP2At("wcn stopengpc");
                    SlogAction.writeSlogConfig();
                }
                mUiThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mLogOutputSettings.setValueIndex(outputvalue);
                        mLogOutputSettings.setSummary(mLogOutputSettings
                                .getEntry());
                        if ( outputvalue==1 ) {
                            mSdSettings.setEnabled(true);
                        } else {
                            mSdSettings.setEnabled(false);
                        }
                    }
                });
                break;
            case MEMORY_LEAK: {
                atResponse = SendAt(engconstents.ENG_AT_MEMORY_LEAK, "atchannel0");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    Log.d(TAG, "dump memory leak log sucess");
                } else {
                    Log.d(TAG, "dump memory leak log fail");
                }
            }
                break;
            case SET_SAVE_SLEEPLOG:
                atResponse = SendSlogModemAt("SAVE_SLEEP_LOG " + mSupportMode);
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mSaveSleepLog.setEnabled(true);
                            Toast.makeText(ModemLogSettings.this, "Save SleepLog Success", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mSaveSleepLog.setEnabled(true);
                            Toast.makeText(ModemLogSettings.this, "Save SleepLog Fail", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case SET_SAVE_RINGBUF:
                atResponse = SendSlogModemAt("SAVE_RINGBUF " + mSupportMode);
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mSaveRingBuf.setEnabled(true);
                            Toast.makeText(ModemLogSettings.this, "Save RingBuf Success", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mSaveRingBuf.setEnabled(true);
                            Toast.makeText(ModemLogSettings.this, "Save RingBuf Fail", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case GET_ENABLE_DUMP_MARLIN:
                atResponse = SendCP2At("wcn dump?");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    final boolean isChecked = atResponse.contains("1");
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnableDumpMarlinMem.setChecked(isChecked);
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnableDumpMarlinMem.setChecked(false);
                            mEnableDumpMarlinMem.setSummary(R.string.feature_abnormal);
                            mEnableDumpMarlinMem.setEnabled(false);
                        }
                    });
                }
                break;
            case SET_ENABLE_DUMP_MARLIN_OPEN:
                atResponse = SendCP2At("wcn dump_enable");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnableDumpMarlinMem.setChecked(true);
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnableDumpMarlinMem.setChecked(false);
                        }
                    });
                }
                break;
            case SET_ENABLE_DUMP_MARLIN_CLOSE:
                atResponse = SendCP2At("wcn dump_disable");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnableDumpMarlinMem.setChecked(false);
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnableDumpMarlinMem.setChecked(true);
                        }
                    });
                }
                break;
            case SET_DUMP_MARLIN_MEM:
                atResponse = SendCP2At("wcn dumpmem");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mDumpMarlinMem.setEnabled(true);
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mDumpMarlinMem.setEnabled(false);
                            mDumpMarlinMem.setSummary(R.string.feature_abnormal);
                        }
                    });
                }
                break;
            case GET_GNSS_LOG_STATUS:
                atResponse = SendSlogModemAt("GET_LOG_STATE GNSS");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    if (atResponse.contains("ON")) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mGnssLog.setChecked(true);
                            }
                        });
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mGnssLog.setChecked(false);
                            }
                        });
                    }
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mGnssLog.setEnabled(false);
                            mGnssLog.setSummary(R.string.feature_abnormal);
                        }
                    });
                }
                break;
            case SET_GNSS_LOG_OPEN:
                atResponse = SendSlogModemAt("ENABLE_LOG GNSS");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mGnssLog.setChecked(true);
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mGnssLog.setChecked(false);
                        }
                    });
                }
                break;
            case SET_GNSS_LOG_CLOSE:
                atResponse = SendSlogModemAt("DISABLE_LOG GNSS");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mGnssLog.setChecked(false);
                        }
                    });
                } else {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mGnssLog.setChecked(true);
                        }
                    });
                }
                break;
            case GET_MD_STOR_POS:
                atResponse = SendSlogModemAt("GET_MD_STOR_POS");
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    if (atResponse.contains("EXTERNAL")) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mMdStorPos.setValueIndex(0);
                                mMdStorPos.setSummary(mMdStorPos.getEntry());
                            }
                        });
                    } else if (atResponse.contains("INTERNAL")) {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mMdStorPos.setValueIndex(1);
                                mMdStorPos.setSummary(mMdStorPos.getEntry());
                            }
                        });
                }
            } else {
                mUiThread.post(new Runnable() {
                    @Override
                    public void run() {
                        mMdStorPos.setEnabled(false);
                        mMdStorPos.setSummary(R.string.feature_abnormal);
                    }
                });
                }
                break;
            case SET_MD_STOR_POS:
                int setValue = Integer.parseInt((String) msg.obj);
                String cmd = null;
                Log.d(TAG,"set md storage pos: " + setValue);
                if (setValue == 0) {
                    cmd = "SET_MD_STOR_POS EXTERNAL";
                } else {
                    cmd = "SET_MD_STOR_POS INTERNAL";
                }
                atResponse = SendSlogModemAt(cmd);
                if (atResponse != null && atResponse.contains(IATUtils.AT_OK)) {
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mMdStorPos.setSummary(mMdStorPos.getEntry());
                        }
                    });
                } else {
                    Log.d(TAG,"sned " + cmd + " fail");
                    Message getMdStorPosStatus = logSettingHandler.obtainMessage(GET_MD_STOR_POS);
                    logSettingHandler.sendMessage(getMdStorPosStatus);
                }
                break;
            }
        }
    }
}
