package com.sprd.engineermode.connectivity;

import com.sprd.engineermode.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import android.os.PowerManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import com.sprd.engineermode.utils.SocketUtils;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;

public class SoftapWifiChannelActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener {

    private static final String TAG = "SoftapWifiChannelActivity";

    private static final String SOCKET_NAME = "wcnd_eng";
    private static final String KEY_AUTO_SWITCH = "auto_switch";
    private static final String KEY_WIFI_CHANNEL = "wifi_channel";

    private static final String WIFI_CHANNEL_STATUS_GET_CMD = "eng wifi softap setchan mode";
    private static final String AUTO_SET_OPEN_CMD = "eng wifi softap setchan default";
    private static final String WIFI_CHANNEL_SET_CMD = "eng wifi softap setchan fix ";

    private static final int GET_AUTO_STATUS = 1;
    private static final int SET_AUTO_OPEN = 2;
    private static final int SET_AUTO_CLOSE = 3;
    private static final int SET_WIFI_CHANNEL = 4;

    private String wifiChnaValue;
    private String autoChnaValue;

    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig;
    private TwoStatePreference mAuto;
    private ListPreference mWifiChannel;

    private SoftapWifiChannelHandler mSoftapWifiChannelHandler;
    private Handler uiThread = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.pref_softap_wifi_channel);

        mAuto = (TwoStatePreference) this.findPreference(KEY_AUTO_SWITCH);
        mWifiChannel = (ListPreference) this.findPreference(KEY_WIFI_CHANNEL);
        mAuto.setOnPreferenceChangeListener(this);
        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mSoftapWifiChannelHandler = new SoftapWifiChannelHandler(ht.getLooper());
        SharedPreferences sharPref = PreferenceManager
                .getDefaultSharedPreferences(this);
        sharPref.registerOnSharedPreferenceChangeListener(this);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiConfig = mWifiManager.getWifiApConfiguration();
    }

    @Override
    protected void onDestroy() {
        if (mSoftapWifiChannelHandler != null) {
            mSoftapWifiChannelHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuto != null && mWifiChannel != null) {
            Message getAutoStatus = mSoftapWifiChannelHandler
                    .obtainMessage(GET_AUTO_STATUS);
            mSoftapWifiChannelHandler.sendMessage(getAutoStatus);
        }
    }

    private String SendCP2At(String cmd) {
        Log.d(TAG, "SendCP2At is " + cmd);
        String strTmp = SocketUtils.sendCmdAndRecResult(SOCKET_NAME,
                LocalSocketAddress.Namespace.ABSTRACT, cmd);
        return strTmp;
    }

    class SoftapWifiChannelHandler extends Handler {
        public SoftapWifiChannelHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String atResponse;
            String cmd;
            boolean needreload = false;
            switch (msg.what) {
            case GET_AUTO_STATUS:
                needreload = false;
                atResponse = SendCP2At(WIFI_CHANNEL_STATUS_GET_CMD);
                Log.d(TAG, "Auto status is: " + atResponse);
                if (atResponse != null && atResponse.startsWith("OK")) {
                    if (atResponse.contains("auto")) {
                        autoChnaValue = analysisResponse(atResponse);
                        uiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAuto.setChecked(true);
                                mWifiChannel.setEnabled(false);
                            }
                        });
                    } else if (atResponse.contains("fixed")) {
                        wifiChnaValue = analysisResponse(atResponse);
                        uiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAuto.setChecked(false);
                                mWifiChannel.setEnabled(true);
                                mWifiChannel.setValue(wifiChnaValue);
                                mWifiChannel.setSummary(mWifiChannel.getEntry());
                            }
                        });
                    }
                } else {
                    uiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mAuto.setEnabled(false);
                            mAuto.setSummary(R.string.feature_abnormal);
                            mWifiChannel.setEnabled(false);
                            mWifiChannel.setSummary(R.string.feature_abnormal);
                        }
                    });
                }
                break;
            case SET_AUTO_OPEN:
                needreload = false;
                atResponse = SendCP2At(AUTO_SET_OPEN_CMD);
                Log.d(TAG, "auto open " + atResponse);
                if (atResponse != null && atResponse.startsWith("OK")) {
                    uiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mAuto.setChecked(true);
                            mWifiChannel.setEnabled(false);
                            mWifiChannel.setSummary(null);
                            Toast.makeText(SoftapWifiChannelActivity.this,
                                    "Auto open Success", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                } else {
                    uiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mAuto.setChecked(false);
                            Toast.makeText(SoftapWifiChannelActivity.this,
                                    "Auto open Fail", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
                break;
            case SET_AUTO_CLOSE:
                atResponse = SendCP2At(WIFI_CHANNEL_STATUS_GET_CMD);
                Log.d(TAG, "Auto status is: " + atResponse);
                if (atResponse != null && atResponse.startsWith("OK")) {
                    autoChnaValue = analysisResponse(atResponse);
                    atResponse = null;
                    cmd = WIFI_CHANNEL_SET_CMD + autoChnaValue;
                    atResponse = SendCP2At(cmd);
                    Log.d(TAG, cmd + "is: " + atResponse);
                    if (atResponse != null && atResponse.startsWith("OK")) {
                        needreload = true;
                        wifiChnaValue = autoChnaValue;
                        uiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAuto.setChecked(false);
                                mWifiChannel.setEnabled(true);
                                mWifiChannel.setValue(wifiChnaValue);
                                mWifiChannel.setSummary(mWifiChannel.getEntry());
                                Toast.makeText(SoftapWifiChannelActivity.this,
                                        "Auto close OK", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    } else {
                        needreload = false;
                        uiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mAuto.setChecked(true);
                                mWifiChannel.setEnabled(false);
                                Toast.makeText(SoftapWifiChannelActivity.this,
                                        "Auto close Fail", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                } else {
                    needreload = false;
                    uiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mAuto.setChecked(true);
                            mWifiChannel.setEnabled(false);
                            Toast.makeText(SoftapWifiChannelActivity.this,
                                    "Auto close Fail", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
                break;
            case SET_WIFI_CHANNEL:
                String ftValue = (String) msg.obj;
                cmd = WIFI_CHANNEL_SET_CMD + ftValue;
                atResponse = SendCP2At(cmd);
                Log.d(TAG, cmd + "is: " + atResponse);
                if (atResponse != null && atResponse.startsWith("OK")) {
                    needreload = true;
                    wifiChnaValue = ftValue;
                    uiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mWifiChannel.setSummary(mWifiChannel.getEntry());
                            Toast.makeText(SoftapWifiChannelActivity.this,
                                    "Success", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    needreload = false;
                    uiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mWifiChannel.setValue(wifiChnaValue);
                            mWifiChannel.setSummary(mWifiChannel.getEntry());
                            Toast.makeText(SoftapWifiChannelActivity.this,
                                    "Fail", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            if (needreload) {
                if (mWifiConfig != null) {
                    mWifiManager.setWifiApEnabled(null, false);
                    mWifiManager.setWifiApEnabled(mWifiConfig, true);
                }
            }
        }
    }

    private String analysisResponse(String response) {
        Log.d(TAG, response + "");
        String[] str = response.split("\n");
        String[] strs = str[0].split("=");
        Log.d(TAG, response + ": " + strs[2]);
        return strs[2].trim();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        // TODO Auto-generated method stub
        if (preference == mAuto) {
            if (mAuto.isChecked()) {
                AlertDialog alertDialog = new AlertDialog.Builder(
                        SoftapWifiChannelActivity.this)
                        .setTitle(getString(R.string.softap_auto_close_warn))
                        .setMessage(getString(R.string.auto_close_warnings))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.alertdialog_ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        Message message = mSoftapWifiChannelHandler
                                                .obtainMessage(SET_AUTO_CLOSE);
                                        mSoftapWifiChannelHandler
                                                .sendMessage(message);
                                    }
                                })
                        .setNegativeButton(
                                getString(R.string.alertdialog_cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int which) {
                                        Message message = mSoftapWifiChannelHandler
                                                .obtainMessage(GET_AUTO_STATUS);
                                        mSoftapWifiChannelHandler
                                                .sendMessage(message);
                                    }
                                }).create();
                alertDialog.show();
            } else {
                Message message = mSoftapWifiChannelHandler
                        .obtainMessage(SET_AUTO_OPEN);
                mSoftapWifiChannelHandler.sendMessage(message);
            }
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(KEY_WIFI_CHANNEL)) {
            String str = sharedPreferences.getString(key, "");
            Log.d(TAG, "onSharedPreferenceChange,key=" + key + ",value=" + str);
            Message setWifiChannel = mSoftapWifiChannelHandler.obtainMessage(
                    SET_WIFI_CHANNEL, str);
            mSoftapWifiChannelHandler.sendMessage(setWifiChannel);
        }
    }
}
