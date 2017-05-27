
package com.sprd.engineermode.connectivity.wifi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sprd.engineermode.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;

public class WifiTXActivity extends Activity implements OnClickListener, OnItemSelectedListener {

    private static final String TAG = "WifiTXActivity";

    // Cmd Message
    private static final int INIT_TEST_STATUS = 0;
    private static final int DEINIT_TEST_STATUS = 1;
    private static final int WIFI_TX_CW = 2;
    private static final int WIFI_TX_GO = 3;
    private static final int WIFI_TX_STOP = 4;

    // UI Message
    private static final int INIT_VIEW = 0;
    private static final int DEINIT_VIEW = 1;
    private static final int CW_ALERT = 2;
    private static final int GO_ALERT = 3;
    private static final int STOP_ALERT = 4;

    // setting pram key
    private static final String CHANNEL = "tx_channel";
    private static final String PKT_LENGTH = "tx_pktlength";
    private static final String PKT_CNT = "tx_pktcnt";
    private static final String POWER_LEVEL = "tx_powerlevel";
    private static final String RATE = "tx_rate";
    private static final String MODE = "tx_mode";
    private static final String PREAMBLE = "tx_preamble";
    private static final String BAND_WIDTH = "tx_bandwidth";
    private static final String GUARD_INTERVAL = "tx_guardinterval";

    private static final String INSMODE_RES = "insmode_result";

    private int mChannelPosition;
    private int mRatePosition;
    private int mModePosition;
    private int mPreamblePosition;
    private int mBandWidthPosition;
    private int mGuardIntervalPosition;

    // when WifiEUT Activity insmode success(mInsmodeSuccessByEUT is
    // true),WifiTXActivity does not insmode and rmmode
    // but when when WifiEUT Activity insmode fail,WifiTXActivity will insmode
    // and rmmode
    private boolean mInsmodeSuccessByEUT = false;
    private boolean mInsmodeSuccessByTX = false;

    private WifiTXHandler mWifiTXHandler;

    private Spinner mChannel;
    private Spinner mRate;
    private Spinner mMode;
    private Spinner mPreamble;
    private Spinner mBandWidth;
    private Spinner mGuardInterval;

    private EditText mPktlength;
    private EditText mPktcnt;
    private EditText mPowerLevel;

    private Button mGo;
    private Button mStop;
    private Button mCW;

    // private ProgressDialog mProgress;
    private AlertDialog mAlertDialog;
    private AlertDialog mCmdAlertDialog;
    private SharedPreferences mPref;

    private WifiEUTHelper mWifiHelper;
    private WifiEUTHelper.WifiTX mWifiTX;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_tx);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        mInsmodeSuccessByEUT = this.getIntent().getBooleanExtra(INSMODE_RES, false);
        mContext = this;
        initUI();

        // disabledView();

        mPref = PreferenceManager.getDefaultSharedPreferences(this);

        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.alert_wifi))
                .setMessage(null)
                .setPositiveButton(
                        getString(R.string.alertdialog_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).create();

        mCmdAlertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.alert_wifi))
                .setMessage(null)
                .setPositiveButton(
                        getString(R.string.alertdialog_ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).create();

        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mWifiTXHandler = new WifiTXHandler(ht.getLooper());
        mWifiTX = new WifiEUTHelper.WifiTX();
        // enabledView();

        /*
         * mProgress = ProgressDialog.show(this, "Wifi Initing...",
         * "Please wait...", true, true); Message initStatus =
         * mWifiTXHandler.obtainMessage(INIT_TEST_STATUS);
         * mWifiTXHandler.sendMessage(initStatus);
         */
    }

    @Override
    protected void onStart() {
        refreshUI();
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        /*
         * if (mProgress != null){ mProgress.dismiss(); mProgress = null;
         * finish(); } else { Message deInitStatus =
         * mWifiTXHandler.obtainMessage(DEINIT_TEST_STATUS);
         * mWifiTXHandler.sendMessage(deInitStatus); }
         */
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mWifiTX != null) {
            Editor editor = mPref.edit();
            editor.putInt(CHANNEL, mChannelPosition);
            editor.putString(PKT_LENGTH, mWifiTX.pktlength);
            editor.putString(PKT_CNT, mWifiTX.pktcnt);
            editor.putString(POWER_LEVEL, mWifiTX.powerlevel);
            editor.putInt(RATE, mRatePosition);
            editor.putInt(MODE, mModePosition);
            editor.putInt(PREAMBLE, mPreamblePosition);
            editor.putInt(BAND_WIDTH, mBandWidthPosition);
            editor.putInt(GUARD_INTERVAL, mGuardIntervalPosition);
            editor.commit();
        }

        if (mWifiTXHandler != null) {
            Log.d(TAG, "HandlerThread has quit");
            mWifiTXHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Message doMessage = null;
        // get the setting param
        getSettingParam();
        if (v == mCW) {
            doMessage = mWifiTXHandler.obtainMessage(WIFI_TX_CW);
        } else if (v == mGo) {
            if (mWifiTX.pktlength.length() == 0
                    || Integer.parseInt(mWifiTX.pktlength) < 64
                    || Integer.parseInt(mWifiTX.pktlength) > 4095) {
                Toast.makeText(WifiTXActivity.this, "Pkt length: number between 64 and 4095",
                        Toast.LENGTH_SHORT).show();
                return;
            } else if (mWifiTX.pktcnt.toString().length() == 0
                    || Integer.parseInt(mWifiTX.pktcnt.toString()) < 0
                    || Integer.parseInt(mWifiTX.pktcnt.toString()) > 65535) {
                Toast.makeText(WifiTXActivity.this, "Pkt cnt: number between 0 and 65535",
                        Toast.LENGTH_SHORT).show();
                return;
            } else if (mWifiTX.powerlevel.toString().length() == 0
                    || Integer.parseInt(mWifiTX.powerlevel.toString()) < 0
                    || Integer.parseInt(mWifiTX.powerlevel.toString()) > 127) {
                Toast.makeText(WifiTXActivity.this, "Power Level: number between 0 and 127",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            doMessage = mWifiTXHandler.obtainMessage(WIFI_TX_GO);
        } else if (v == mStop) {
            doMessage = mWifiTXHandler.obtainMessage(WIFI_TX_STOP);
        }
        mWifiTXHandler.sendMessage(doMessage);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        if (mWifiTX != null) {
            if (parent == mChannel) {
                mWifiTX.channel = this.getResources().getStringArray(R.array.channel_arr)[position];
                mChannelPosition = position;
            } else if (parent == mRate) {
                mWifiTX.rate = this.getResources().getStringArray(R.array.rate_int_arr)[position];
                mRatePosition = position;
            } else if (parent == mMode) {
                mWifiTX.mode = this.getResources().getStringArray(R.array.mode_str_arr)[position];
                mModePosition = position;
            } else if (parent == mPreamble) {
                mWifiTX.preamble = this.getResources().getStringArray(R.array.preamble_int_arr)[position];
                mPreamblePosition = position;
            } else if (parent == mBandWidth) {
                mWifiTX.bandwidth = this.getResources().getStringArray(R.array.bandwidth_int_arr)[position];
                mBandWidthPosition = position;
            } else if (parent == mGuardInterval) {
                mWifiTX.guardinterval = this.getResources().getStringArray(
                        R.array.guardinterval_int_arr)[position];
                mGuardIntervalPosition = position;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * refresh UI Handler
     */
    public Handler mHandler = new Handler() {

        public void handleMessage(android.os.Message msg) {
            if (msg == null) {
                Log.d(TAG, "UI Message is null");
                return;
            }

            /*
             * if(mProgress != null) { mProgress.dismiss(); mProgress = null; }
             */

            switch (msg.what) {
                case INIT_VIEW:
                    if (msg.arg1 == 1) {
                        mWifiTX = new WifiEUTHelper.WifiTX();
                        enabledView();
                        refreshUI();
                    } else {
                        mAlertDialog.setMessage("Wifi Init Fail");
                        if (!isFinishing()) {
                            mAlertDialog.show();
                        }
                    }
                    break;
                case DEINIT_VIEW:
                    mAlertDialog.setMessage("Wifi Deinit Fail");
                    if (!isFinishing()) {
                        mAlertDialog.show();
                    }
                    break;
                case CW_ALERT:
                    if (msg.arg1 == 1) {
                        mCmdAlertDialog.setMessage("Wifi EUT CW Success");
                    } else {
                        mCmdAlertDialog.setMessage("Wifi EUT CW Fail");
                    }
                    if (!isFinishing()) {
                        mCmdAlertDialog.show();
                    }
                    break;
                case GO_ALERT:
                    if (msg.arg1 == 1) {
                        mCmdAlertDialog.setMessage("Wifi EUT TX Go Success");
                    } else {
                        mCmdAlertDialog.setMessage("Wifi EUT TX Go Fail");
                    }
                    if (!isFinishing()) {
                        mCmdAlertDialog.show();
                    }
                    break;
                case STOP_ALERT:
                    if (msg.arg1 == 1) {
                        mCmdAlertDialog.setMessage("Wifi EUT TX Stop Success");
                    } else {
                        mCmdAlertDialog.setMessage("Wifi EUT TX Stop Fail");
                    }
                    if (!isFinishing()) {
                        mCmdAlertDialog.show();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * send cmd Handler
     *
     * @author SPREADTRUM\qianqian.tian
     */
    class WifiTXHandler extends Handler {
        public WifiTXHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Message uiMessage = null;
            switch (msg.what) {
                case INIT_TEST_STATUS:
                    if (!mInsmodeSuccessByEUT) {
                        if (WifiEUTHelper.insmodeWifi()) {
                            mInsmodeSuccessByTX = true;
                            mWifiHelper = new WifiEUTHelper();
                            uiMessage = mHandler.obtainMessage(INIT_VIEW, 1, 0);
                            mHandler.sendMessage(uiMessage);
                            // initWifi();
                        } else {
                            uiMessage = mHandler.obtainMessage(INIT_VIEW, 0, 0);
                            mHandler.sendMessage(uiMessage);
                        }
                    } else {
                        mWifiHelper = new WifiEUTHelper();
                        uiMessage = mHandler.obtainMessage(INIT_VIEW, 1, 0);
                        mHandler.sendMessage(uiMessage);
                        // initWifi();
                    }
                    break;
                case DEINIT_TEST_STATUS:
                    /*
                     * if (!mWifiHelper.wifiStop() || !mWifiHelper.wifiDown()) {
                     * uiMessage = mHandler.obtainMessage(DEINIT_VIEW);
                     * mHandler.sendMessage(uiMessage); }
                     */
                    if (mInsmodeSuccessByTX && !WifiEUTHelper.remodeWifi()) {
                        uiMessage = mHandler.obtainMessage(DEINIT_VIEW);
                        mHandler.sendMessage(uiMessage);
                    } else {
                        mInsmodeSuccessByTX = false;
                        finish();
                    }
                    break;
                case WIFI_TX_CW:
                    if (WifiEUTHelper.wifiTXCw(mWifiTX)) {
                        uiMessage = mHandler.obtainMessage(CW_ALERT, 1, 0);
                    } else {
                        uiMessage = mHandler.obtainMessage(CW_ALERT, 0, 0);
                    }
                    mHandler.sendMessage(uiMessage);
                    break;
                case WIFI_TX_GO:
                    if (WifiEUTHelper.wifiTXGo(mWifiTX)) {
                        uiMessage = mHandler.obtainMessage(GO_ALERT, 1, 0);
                    } else {
                        uiMessage = mHandler.obtainMessage(GO_ALERT, 0, 0);
                    }
                    mHandler.sendMessage(uiMessage);
                    break;
                case WIFI_TX_STOP:
                    if (WifiEUTHelper.wifiTXStop()) {
                        uiMessage = mHandler.obtainMessage(STOP_ALERT, 1, 0);
                    } else {
                        uiMessage = mHandler.obtainMessage(STOP_ALERT, 0, 0);
                    }
                    mHandler.sendMessage(uiMessage);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * init WifiTX View
     */
    private void initUI() {
        mChannel = (Spinner) findViewById(R.id.wifi_eut_channel);
        ArrayAdapter<String> channelAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                        .getStringArray(R.array.channel_arr));
        channelAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChannel.setAdapter(channelAdapter);
        mChannel.setOnItemSelectedListener(this);

        mPktlength = (EditText) findViewById(R.id.wifi_eut_pkt_length);
        mPktlength.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mPktlength.getText().toString().length() == 0
                        || Integer.parseInt(mPktlength.getText().toString()) < 64
                        || Integer.parseInt(mPktlength.getText().toString()) > 4095) {
                    Toast.makeText(WifiTXActivity.this, "number between 64 and 4095",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mWifiTX != null) {
                    mWifiTX.pktlength = mPktlength.getText().toString();
                }
            }
        });

        mPktcnt = (EditText) findViewById(R.id.wifi_eut_pkt_cnt);
        mPktcnt.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mPktcnt.getText().toString().length() == 0
                        || Integer.parseInt(mPktcnt.getText().toString()) < 0
                        || Integer.parseInt(mPktcnt.getText().toString()) > 65535) {
                    Toast.makeText(WifiTXActivity.this, "number between 0 and 65535",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mWifiTX != null) {
                    mWifiTX.pktcnt = mPktcnt.getText().toString();
                }
            }
        });

        mPowerLevel = (EditText) findViewById(R.id.wifi_eut_powerLevel);
        mPowerLevel.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mPowerLevel.getText().toString().length() == 0
                        || Integer.parseInt(mPowerLevel.getText().toString()) < 0
                        || Integer.parseInt(mPowerLevel.getText().toString()) > 127) {
                    Toast.makeText(WifiTXActivity.this, "number between 0 and 127",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mWifiTX != null) {
                    mWifiTX.powerlevel = mPowerLevel.getText().toString();
                }
            }
        });

        mRate = (Spinner) findViewById(R.id.wifi_eut_rate);
        ArrayAdapter<String> rateAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                        .getStringArray(R.array.rate_str_arr));
        rateAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRate.setAdapter(rateAdapter);
        mRate.setOnItemSelectedListener(this);

        mMode = (Spinner) findViewById(R.id.wifi_eut_mode);
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                        .getStringArray(R.array.mode_str_arr));
        modeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMode.setAdapter(modeAdapter);
        mMode.setOnItemSelectedListener(this);

        mPreamble = (Spinner) findViewById(R.id.wifi_eut_preamble);
        ArrayAdapter<String> preadmbleAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                        .getStringArray(R.array.preamble_str_arr));
        preadmbleAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPreamble.setAdapter(preadmbleAdapter);
        mPreamble.setOnItemSelectedListener(this);

        mBandWidth = (Spinner) findViewById(R.id.wifi_eut_band_width);
        ArrayAdapter<String> bandWidthAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                        .getStringArray(R.array.bandwidth_str_arr));
        bandWidthAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBandWidth.setAdapter(bandWidthAdapter);
        mBandWidth.setOnItemSelectedListener(this);

        mGuardInterval = (Spinner) findViewById(R.id.wifi_eut_guard_interval);
        ArrayAdapter<String> guardintervalAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                        .getStringArray(R.array.guardinterval_str_arr));
        guardintervalAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mGuardInterval.setAdapter(guardintervalAdapter);
        mGuardInterval.setOnItemSelectedListener(this);

        // Button
        mCW = (Button) findViewById(R.id.wifi_tx_cw);
        mGo = (Button) findViewById(R.id.wifi_tx_go);
        mStop = (Button) findViewById(R.id.wifi_tx_stop);

        mCW.setOnClickListener(this);
        mGo.setOnClickListener(this);
        mStop.setOnClickListener(this);
    }

    /**
     * display the setting param which setting in the last time
     */
    private void refreshUI() {
        mChannelPosition = mPref.getInt(CHANNEL, 0);
        mRatePosition = mPref.getInt(RATE, 0);
        mModePosition = mPref.getInt(MODE, 0);
        mPreamblePosition = mPref.getInt(PREAMBLE, 0);
        mBandWidthPosition = mPref.getInt(BAND_WIDTH, 0);
        mGuardIntervalPosition = mPref.getInt(GUARD_INTERVAL, 0);

        mChannel.setSelection(mChannelPosition);
        mRate.setSelection(mRatePosition);
        mMode.setSelection(mModePosition);
        mPreamble.setSelection(mPreamblePosition);
        mBandWidth.setSelection(mBandWidthPosition);
        mGuardInterval.setSelection(mGuardIntervalPosition);

        mPktlength.setText(mPref.getString(PKT_LENGTH, "0"));
        mPktcnt.setText(mPref.getString(PKT_CNT, "0"));
        mPowerLevel.setText(mPref.getString(POWER_LEVEL, "0"));
    }

    /**
     * when insmodeWifi fail or others,all view shoule disable
     */
    private void disabledView() {
        mChannel.setEnabled(false);
        mPktlength.setEnabled(false);
        mPktcnt.setEnabled(false);
        mPowerLevel.setEnabled(false);
        mRate.setEnabled(false);
        mMode.setEnabled(false);
        mPreamble.setEnabled(false);
        mBandWidth.setEnabled(false);
        mGuardInterval.setEnabled(false);
        mCW.setEnabled(false);
        mGo.setEnabled(false);
        mStop.setEnabled(false);
    }

    /**
     * when insmodeWifi success or others,all view shoule enable
     */

    private void enabledView() {
        mChannel.setEnabled(true);
        mPktlength.setEnabled(true);
        mPktcnt.setEnabled(true);
        mPowerLevel.setEnabled(true);
        mRate.setEnabled(true);
        mMode.setEnabled(true);
        mPreamble.setEnabled(true);
        mBandWidth.setEnabled(true);
        mGuardInterval.setEnabled(true);
        mCW.setEnabled(true);
        mGo.setEnabled(true);
        mStop.setEnabled(true);
    }

    /**
     * get setting param when click button cw/start/go
     */
    private void getSettingParam() {
        mWifiTX.channel = this.getResources().getStringArray(R.array.channel_arr)[mChannelPosition];
        mWifiTX.rate = this.getResources().getStringArray(R.array.rate_int_arr)[mRatePosition];
        mWifiTX.mode = this.getResources().getStringArray(R.array.mode_str_arr)[mModePosition];
        mWifiTX.preamble = this.getResources().getStringArray(R.array.preamble_int_arr)[mPreamblePosition];
        mWifiTX.bandwidth = this.getResources().getStringArray(R.array.bandwidth_int_arr)[mBandWidthPosition];
        mWifiTX.guardinterval = this.getResources().getStringArray(R.array.guardinterval_int_arr)[mGuardIntervalPosition];
        mWifiTX.pktlength = mPktlength.getText().toString();
        /* SPRD: Fixbug454047 ,some vales is null.{@ */
        if (TextUtils.isEmpty(mWifiTX.pktlength)) {
            mWifiTX.pktlength = "0";
        }
        mWifiTX.pktcnt = mPktcnt.getText().toString();
        if (TextUtils.isEmpty(mWifiTX.pktcnt)) {
            mWifiTX.pktcnt = "0";
        }
        mWifiTX.powerlevel = mPowerLevel.getText().toString();
        if (TextUtils.isEmpty(mWifiTX.powerlevel)) {
            mWifiTX.powerlevel = "0";
        }
        /* @} */
        Log.d(TAG, "Now testing TX in WifiEUT, the setting param is: \n channel " + mWifiTX.channel
                + "\n rate is " + mWifiTX.rate + "\n mode is " + mWifiTX.mode
                + "\n preamble is " + mWifiTX.preamble + "\n bandwidth is " + mWifiTX.bandwidth
                + "\n guardinterval is " + mWifiTX.guardinterval + "\n pktlength is "
                + mWifiTX.pktlength
                + "\n pktcnt is " + mWifiTX.pktcnt + "\n powerlevel is " + mWifiTX.powerlevel);
    }

    private void initWifi() {
        Message uiMessage;
        mWifiHelper = new WifiEUTHelper();
        if (mWifiHelper.wifiUp() && mWifiHelper.wifiStart()) {
            uiMessage = mHandler.obtainMessage(INIT_VIEW, 1, 0);
        } else {
            uiMessage = mHandler.obtainMessage(INIT_VIEW, 0, 0);
        }
        mHandler.sendMessage(uiMessage);
    }

}
