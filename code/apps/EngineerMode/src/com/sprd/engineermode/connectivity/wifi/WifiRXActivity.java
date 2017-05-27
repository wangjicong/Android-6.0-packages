
package com.sprd.engineermode.connectivity.wifi;

import com.sprd.engineermode.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import java.text.NumberFormat;

public class WifiRXActivity extends Activity implements OnClickListener, OnItemSelectedListener {

    private static final String TAG = "WifiRXActivity";
    private static final String INSMODE_RES = "insmode_result";

    // cmd Message
    private static final int INIT_TEST_STATUS = 0;
    private static final int DEINIT_TEST_STATUS = 1;
    private static final int WIFI_RX_GO = 2;
    private static final int WIFI_RX_STOP = 3;
    private static final int REFRESH_VIEW = 4;

    // UI Message
    private static final int INIT_VIEW = 0;
    private static final int DEINIT_VIEW = 1;
    private static final int GO_ALERT = 2;
    private static final int STOP_ALERT = 3;

    // setting pram key
    private static final String CHANNEL = "rx_channel";
    private static final String TEST_NUM = "rx_test_num";

    // when WifiEUT Activity insmode success(mInsmodeSuccessByEUT is
    // true),WifiRXActivity does not insmode and rmmode
    // but when when WifiEUT Activity insmode fail,WifiRXActivity will insmode
    // and rmmode
    private boolean mInsmodeSuccessByEUT = false;
    private boolean mInsmodeSuccessByRX = false;

    private WifiRXHandler mWifiRXHandler;

    private Spinner mChannel;
    private EditText mTestRXNum;
    private TextView mRXOkResult;
    private TextView mPERResult;

    private Button mGo;
    private Button mStop;

    // private ProgressDialog mProgress;
    private AlertDialog mAlertDialog;
    private AlertDialog mCmdAlertDialog;
    private SharedPreferences mPref;

    private WifiEUTHelper mWifiHelper;
    private WifiEUTHelper.WifiRX mWifiRX;

    private int mChannelPosition;

    private String mOkResult;
    private String mRXPerResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_rx);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        mInsmodeSuccessByEUT = this.getIntent().getBooleanExtra(INSMODE_RES, false);

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
        mWifiRXHandler = new WifiRXHandler(ht.getLooper());

        /*
         * mProgress = ProgressDialog.show(this, "Wifi Initing...",
         * "Please wait...", true, true); Message initStatus =
         * mWifiRXHandler.obtainMessage(INIT_TEST_STATUS);
         * mWifiRXHandler.sendMessage(initStatus);
         */
        mWifiRX = new WifiEUTHelper.WifiRX();
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
         * mWifiRXHandler.obtainMessage(DEINIT_TEST_STATUS);
         * mWifiRXHandler.sendMessage(deInitStatus); }
         */
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mWifiRX != null) {
            Editor editor = mPref.edit();
            editor.putInt(CHANNEL, mChannelPosition);
            editor.putString(TEST_NUM, mWifiRX.rxtestnum);
            editor.commit();
        }

        if (mWifiRXHandler != null) {
            Log.d(TAG, "HandlerThread has quit");
            mWifiRXHandler.getLooper().quit();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Message doMessage = null;
        // get the setting param
        getSettingParam();
        if (v == mGo) {
            doMessage = mWifiRXHandler.obtainMessage(WIFI_RX_GO);
        } else if (v == mStop) {
            doMessage = mWifiRXHandler.obtainMessage(WIFI_RX_STOP);
        }
        mWifiRXHandler.sendMessage(doMessage);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        if (mWifiRX != null) {
            if (parent == mChannel) {
                mWifiRX.channel = this.getResources().getStringArray(R.array.channel_arr)[position];
                mChannelPosition = position;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    class WifiRXHandler extends Handler {
        public WifiRXHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Message uiMessage = null;
            switch (msg.what) {
                case INIT_TEST_STATUS:
                    if (!mInsmodeSuccessByEUT) {
                        if (WifiEUTHelper.insmodeWifi()) {
                            mInsmodeSuccessByRX = true;
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
                    if (mInsmodeSuccessByRX && !WifiEUTHelper.remodeWifi()) {
                        uiMessage = mHandler.obtainMessage(DEINIT_VIEW);
                        mHandler.sendMessage(uiMessage);
                    } else {
                        mInsmodeSuccessByRX = false;
                        finish();
                    }
                    break;
                case WIFI_RX_GO:
                    if (WifiEUTHelper.wifiRXStart(mWifiRX)) {
                        uiMessage = mHandler.obtainMessage(GO_ALERT, 1, 0);
                    } else {
                        uiMessage = mHandler.obtainMessage(GO_ALERT, 0, 0);
                    }
                    mHandler.sendMessage(uiMessage);
                    break;
                case WIFI_RX_STOP:
                    analysisRXResult(WifiEUTHelper.wifiRXResult());
                    if (WifiEUTHelper.wifiRXStop()) {
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
                        mWifiRX = new WifiEUTHelper.WifiRX();
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
                case GO_ALERT:
                    if (msg.arg1 == 1) {
                        mCmdAlertDialog.setMessage("Wifi EUT RX Go Success");
                    } else {
                        mCmdAlertDialog.setMessage("Wifi EUT RX Go Fail");
                    }
                    if (!isFinishing()) {
                        mCmdAlertDialog.show();
                    }
                    break;
                case STOP_ALERT:
                    if (msg.arg1 == 1) {
                        mCmdAlertDialog.setMessage("Wifi EUT RX Stop Success");
                    } else {
                        mCmdAlertDialog.setMessage("Wifi EUT RX Stop Fail");
                    }
                    if (!isFinishing()) {
                        mCmdAlertDialog.show();
                    }
                    break;
                case REFRESH_VIEW:
                    mRXOkResult.setText(mOkResult);
                    mPERResult.setText(mRXPerResult);
                    break;
                default:
                    break;
            }
        }
    };

    private void initUI() {
        mChannel = (Spinner) findViewById(R.id.wifi_eut_channel);
        ArrayAdapter<String> channelAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                        .getStringArray(R.array.channel_arr));
        channelAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChannel.setAdapter(channelAdapter);
        mChannel.setOnItemSelectedListener(this);

        mTestRXNum = (EditText) findViewById(R.id.wifi_eut_text_rx_num);
        mTestRXNum.addTextChangedListener(new TextWatcher() {
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
                if (mWifiRX != null) {
                    mWifiRX.rxtestnum = mTestRXNum.getText().toString();
                }
            }
        });

        mRXOkResult = (TextView) findViewById(R.id.wifi_eut_rx_ok_result);
        mPERResult = (TextView) findViewById(R.id.wifi_eut_per_result);

        mGo = (Button) findViewById(R.id.wifi_rx_go);
        mStop = (Button) findViewById(R.id.wifi_rx_stop);

        mGo.setOnClickListener(this);
        mStop.setOnClickListener(this);
    }

    /**
     * when insmodeWifi fail or others,all view shoule disable
     */
    private void disabledView() {
        mChannel.setEnabled(false);
        mTestRXNum.setEnabled(false);
        mRXOkResult.setEnabled(false);
        mPERResult.setEnabled(false);
        mGo.setEnabled(false);
        mStop.setEnabled(false);
    }

    /**
     * when insmodeWifi success or others,all view shoule enable
     */

    private void enabledView() {
        mChannel.setEnabled(true);
        mTestRXNum.setEnabled(true);
        mRXOkResult.setEnabled(true);
        mPERResult.setEnabled(true);
        mGo.setEnabled(true);
        mStop.setEnabled(true);
    }

    /**
     * display the setting param which setting in the last time
     */
    private void refreshUI() {
        mChannelPosition = mPref.getInt(CHANNEL, 0);
        mChannel.setSelection(mChannelPosition);

        mTestRXNum.setText(mPref.getString(TEST_NUM, "0"));
    }

    /**
     * get setting param when click button start/go
     */
    private void getSettingParam() {
        mWifiRX.channel = this.getResources().getStringArray(R.array.channel_arr)[mChannelPosition];
        mWifiRX.rxtestnum = mTestRXNum.getText().toString();
        Log.d(TAG, "Now testing RX in WifiEUT, the setting param is: \n channel " + mWifiRX.channel
                + "\n test_num is " + mWifiRX.rxtestnum);
    }

    private void analysisRXResult(String result) {
        Message uiMessage;
        Log.d(TAG,"analysisRXResult" + result);
        if (result != null) {
            String[] str = result.split("\\:");
            if (str.length < 3) {
                Log.d(TAG, "AT Result is error");
                mOkResult = "error";
                mRXPerResult = "error";
            } else {
                String[] str1 = str[2].split("\\ ");
                for (int i = 1; i < str1.length; i++) {
                    Log.d(TAG, "str is " + str1[i]);
                    if (str1[i].contains("rx_end_count")) {
                        String[] str2 = str1[i].split("\\=");
                        mOkResult = str2[1];
                        String val1 = mTestRXNum.getText().toString();
                        /* SPRD: Fixbug454654,some vales may be null. @{ */
                        if (TextUtils.isEmpty(val1)) {
                            val1 = "0";
                        }
                        if (TextUtils.isEmpty(mOkResult.toString())) {
                            Log.d(TAG, "mOkResult is null");
                            return;
                        }
                        int a = Integer.valueOf(val1).intValue()
                                - Integer.valueOf(mOkResult.toString()).intValue();
                        int b = Integer.valueOf(val1).intValue();
                        /* @} */
                        NumberFormat numberFormat = NumberFormat.getInstance();
                        numberFormat.setMaximumFractionDigits(2);
                        mRXPerResult = numberFormat.format((float) a / (float) b * 100) + "%";
                    }
                }
            }
        } else {
            Log.d(TAG, "AT Result is null");
            mOkResult = "null";
            mRXPerResult = "null";
        }
        uiMessage = mHandler.obtainMessage(REFRESH_VIEW);
        mHandler.sendMessage(uiMessage);
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
