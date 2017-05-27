package com.sprd.engineermode.connectivity.btSettings;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;

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
import android.util.Log;
import android.widget.Toast;
import com.sprd.engineermode.debuglog.slogui.SlogAction;
import com.sprd.engineermode.debuglog.slogui.SlogUICommonControl;

import android.content.Intent;
import android.content.Context;
import com.sprd.engineermode.R;

import android.bluetooth.BluetoothAdapter;

import android.net.LocalSocketAddress;

public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener{

    private static final String TAG = "BTSettingsActivity";

    private static final String KEY_DATA_CAPTURE = "data_capture";
    private static final String KEY_DSP_LOG_CTRL = "dsp_log_control";
    private static final String KEY_CONTROLLER_BQB = "controller_bqb_mode";

    private static final String LOCAL_ADDRESS = "127.0.0.1";
    private static final String PROPERTY__HCITOOLS_SOCKET = "bluetooth.hcitools.socket";
    private static final String PROPERTY__HCITOOLS_SERVER = "bluetooth.hcitools.server";

    private static final String CONTROLLER_BQB_SOCKET = "/data/misc/.bqb_ctrl";
    private static final String COMM_CONTROLLER_ENABLE = "\r\nAT+SPBQBTEST=1\r\n";
    private static final String COMM_CONTROLLER_DISABLE = "\r\nAT+SPBQBTEST=0\r\n";
    private static final String COMM_CONTROLLER_TRIGGER = "\r\nAT+SPBQBTEST=?\r\n";

    private static final String NOTIFY_BQB_ENABLE = "\r\n+SPBQBTEST OK: ENABLED\r\n";
    private static final String NOTIFY_BQB_DISABLE = "\r\n+SPBQBTEST OK: DISABLE\r\n";
    private static final String TRIGGER_BQB_ENABLE = "\r\n+SPBQBTEST OK: BQB\r\n";
    private static final String TRIGGER_BQB_DISABLE = "\r\n+SPBQBTEST OK: AT\r\n";
    private static final String CONTROLLER_BQB_ENABLED = "controller bqb enabled";
    private static final String CONTROLLER_BQB_DISABLED = "controller bqb disabled";


    private static final int MESSAGE_GET_DSP_LOG_STAT = 0;
    private static final int MESSAGE_DSP_LOG_CONTROL = 1;
    private static final int MESSAGE_DATA_CAPTURE = 2;
    private static final int MESSAGE_CONTROLLER_BQB_ONOFF = 3;
    private static final int MESSAGE_CONTROLLER_BQB_TRIGGER = 4;

    private static final int OPCODE_GET_DSP_LOG_STAT = 0xFC8F;
    private static final int OPCODE_DSP_LOG_CONTROL = 0xFC8D;
    private static final int OPCODE_DATA_CAPTURE = 0xFC8E;

    private static final int DSP_LOG_CONTROL_DISABLE = 0x00;
    private static final int DSP_LOG_CONTROL_USB = 0x01;
    private static final int DSP_LOG_CONTROL_UART = 0x02;

    private static int socket_port = 0;
    private LocalSocket mConBqbSocket = null;
    private InputStream mConBqbInputStream = null;
    private OutputStream mConBqbOutputStream = null;

    private Handler mUiThread = new Handler();
    private BTSettingsHandler mBTSettingsHandler;

    private ListPreference mDspLogControl;
    private Preference mDataCapture;
    private SharedPreferences mSharePref;
    private Preference mControllerBqb;

    private Context mContext;

    private BluetoothAdapter mAdapter;

    private Socket client = null;
    private InputStream in = null;
    private OutputStream out = null;

    private Object sync = new Object();

    private boolean mControllerBqbState = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_bt_settings);

        mDspLogControl = (ListPreference) findPreference(KEY_DSP_LOG_CTRL);
        mSharePref = PreferenceManager.getDefaultSharedPreferences(this);
        mSharePref.registerOnSharedPreferenceChangeListener(this);
        mDataCapture = (Preference) findPreference(KEY_DATA_CAPTURE);
        mDataCapture.setOnPreferenceClickListener(this);
        mDataCapture.setEnabled(true);

        mControllerBqb = (Preference) this.findPreference(KEY_CONTROLLER_BQB);
        mControllerBqb.setOnPreferenceClickListener(this);

        mContext = this;

        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mBTSettingsHandler = new BTSettingsHandler(ht.getLooper());

        mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();

        mDspLogControl.setSummary(mDspLogControl.getEntry());
        if (mAdapter.isEnabled()) {

            mControllerBqb.setEnabled(false);

            Log.d(TAG, "read DSP LOG state");

            if (SystemProperties.get(PROPERTY__HCITOOLS_SERVER, "stopped").equals("stopped")) {
                Log.e(TAG, "HCI SERVER did not start");
            } else {
                socket_port =  Integer.parseInt(SystemProperties.get(PROPERTY__HCITOOLS_SOCKET, "0"));
                Log.d(TAG, PROPERTY__HCITOOLS_SOCKET + ": " + socket_port);
                if (socket_port == 0) {
                    Log.e(TAG, "unknow socket");
                } else {
                    Message msg = mBTSettingsHandler.obtainMessage(MESSAGE_GET_DSP_LOG_STAT);
                    mBTSettingsHandler.sendMessage(msg);
                }
            }
        } else {
            mDspLogControl.setEnabled(false);
            mDataCapture.setEnabled(false);

            mControllerBqb.setEnabled(true);
            Message msg = mBTSettingsHandler.obtainMessage(MESSAGE_CONTROLLER_BQB_TRIGGER);
            mBTSettingsHandler.sendMessage(msg);

           //uiLog("please enable bt!");
           //finish();
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        try {
            synchronized(sync) {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
                if (client != null) {
                    client.close();
                    client = null;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error: " + e);
        }
        closeConBqb();
        finish();
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        super.onBackPressed();
        try {
            synchronized(sync) {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
                if (client != null) {
                    client.close();
                    client = null;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error: " + e);
        }
        closeConBqb();
        finish();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        return true;
    }

    class BTSettingsHandler extends Handler {

        public BTSettingsHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_GET_DSP_LOG_STAT:
                sendHciCommand(OPCODE_GET_DSP_LOG_STAT, 0, 0);
                break;
            case MESSAGE_DSP_LOG_CONTROL:
                sendHciCommand(OPCODE_DSP_LOG_CONTROL, msg.arg1, msg.arg2);
              break;
            case MESSAGE_DATA_CAPTURE:
                sendHciCommand(OPCODE_DATA_CAPTURE, 0, 0);
                break;
            case MESSAGE_CONTROLLER_BQB_ONOFF:
                controllerBqbEnable(!mControllerBqbState);
                break;
            case MESSAGE_CONTROLLER_BQB_TRIGGER:
                mControllerBqbState = getControllerBqbState();
            default:
                break;
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref.getKey().equals(KEY_DATA_CAPTURE)) {
            Log.d(TAG, "press: " + KEY_DATA_CAPTURE);
            Message msg = mBTSettingsHandler.obtainMessage(MESSAGE_DATA_CAPTURE);
            msg.arg1 = 0;
            mBTSettingsHandler.sendMessage(msg);
        }else if (pref.getKey().equals(KEY_CONTROLLER_BQB)) {
            Log.d(TAG, "Controller BQB State Changed " + mControllerBqbState + " -> " + !mControllerBqbState);
            Message msg = mBTSettingsHandler.obtainMessage(MESSAGE_CONTROLLER_BQB_ONOFF);
            mBTSettingsHandler.sendMessage(msg);
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {

        if(key.equals(KEY_DSP_LOG_CTRL)){
            mDspLogControl.setSummary(mDspLogControl.getEntry());
            String value = mDspLogControl.getValue();
            int index = mDspLogControl.findIndexOfValue(value);
            Log.d(TAG, value + " index: " + index);
            Message msg = mBTSettingsHandler.obtainMessage(MESSAGE_DSP_LOG_CONTROL);
            msg.arg1 = 1;
            msg.arg2 = index;
            mBTSettingsHandler.sendMessage(msg);
        } else {
            Log.d(TAG, "onSharedPreferenceChanged: " + key);
        }
    }

    private void sendHciCommand(int opcode, int len, int para) {

        if (socket_port == 0) {
            Log.e(TAG, "unknow socket");
            return;
        }
        client = new Socket();
        try {
            InetSocketAddress localAddr = new InetSocketAddress(LOCAL_ADDRESS, socket_port);
            Log.d(TAG, "connect localAddr");
            client.connect(localAddr, 10000);
            Log.d(TAG, "get OutputStream");
            out = client.getOutputStream();
            Log.d(TAG, "write opcode: 0x" + Integer.toHexString(opcode) + " len: " + len + " para: " + Integer.toHexString(para));
            byte[] data = new byte[5];
            data[0] = (byte)((opcode >> 8) & 0xFF);
            data[1] = (byte)(opcode & 0xFF);
            data[2] = (byte)(len & 0xFF);
            data[3] = (byte)(para & 0xFF);
            Log.d(TAG, "byte: " + Integer.toHexString(data[0] & 0xFF) + Integer.toHexString(data[1] & 0xFF)
                + Integer.toHexString(data[2] & 0xFF) + Integer.toHexString(data[3] & 0xFF));
            out.write(data);
            out.flush();
            in = client.getInputStream();
            byte[] readData = new byte[128];
            int ret = in.read(readData);
            if (ret == 7
                && readData[0] == 'T'
                && readData[1] == 'I'
                && readData[2] == 'M'
                && readData[3] == 'E'
                && readData[4] == 'O'
                && readData[5] == 'U'
                && readData[6] == 'T') {
                Log.d(TAG, "hci command TIMEOUT");
                uiLog(opcode2String(opcode) + " EVT: TIMEOUT");
            } else {
                Log.d(TAG, "read len: " + ret + " byte[0]: " + Integer.toHexString(readData[0] & 0xFF));
                switch (opcode) {
                    case OPCODE_GET_DSP_LOG_STAT:
                        CharSequence[] values = mDspLogControl.getEntryValues();
                        if (readData[0] == 0) {
                            final String value = values[0].toString();
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDspLogControl.setValue(value);
                                    Toast.makeText(mContext, "DSP LOG EVT: DISABLE",
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else if (readData[0] == 1){
                            final String value = values[1].toString();
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDspLogControl.setValue(value);
                                    Toast.makeText(mContext, "DSP LOG EVT: USB",
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else if (readData[0] == 2){
                            final String value = values[2].toString();
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDspLogControl.setValue(value);
                                    Toast.makeText(mContext, "DSP LOG EVT: UART",
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            uiLog(opcode2String(opcode) + " EVT: ERROR");
                        }
                        break;

                    case OPCODE_DSP_LOG_CONTROL:
                        String state = null;
                        if (para == DSP_LOG_CONTROL_DISABLE) {
                            state = " disable";
                        } else if (para == DSP_LOG_CONTROL_USB) {
                            state = " usb";
                        } else if (para == DSP_LOG_CONTROL_UART) {
                            state = " uart";
                        } else {

                        }
                        if (readData[0] == 0) {
                            uiLog(opcode2String(opcode) + state + " EVT: OK");
                        } else {
                            uiLog(opcode2String(opcode) + state + " EVT: ERROR");
                        }
                        break;

                    case OPCODE_DATA_CAPTURE:
                        if (readData[0] == 0) {
                            uiLog(opcode2String(opcode) + " EVT: OK");
                        } else {
                            uiLog(opcode2String(opcode) + " EVT: ERROR");
                        }
                        break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error: " + e);
        } finally {
            try {
                synchronized(sync) {
                    if (in != null) {
                        in.close();
                        in = null;
                    }
                    if (out != null) {
                        out.close();
                        out = null;
                    }
                    if (client != null) {
                        client.close();
                        client = null;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "error: " + e);
            }
        }
    }

    private void uiLog(String log) {
        final String stringLog = log;
        mUiThread.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, stringLog,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String opcode2String(int opcode){
        String res = "unknown";
        switch(opcode) {
            case OPCODE_GET_DSP_LOG_STAT:
                res = "GET DSP LOG";
                break;
            case OPCODE_DSP_LOG_CONTROL:
                res = "DSP LOG";
                break;
            case OPCODE_DATA_CAPTURE:
                res = "DATA CAPTURE";
                break;
            default:
                break;
        }
        return res;
    }

    private void controllerBqbEnable(boolean enable){
        Log.d(TAG, "controllerBqbEnable: " + enable);
        try {
        Log.d(TAG, "init bqb local socket");
            LocalSocketAddress address = new LocalSocketAddress(CONTROLLER_BQB_SOCKET,
                LocalSocketAddress.Namespace.ABSTRACT);
            mConBqbSocket = new LocalSocket();
            mConBqbSocket.connect(address);
            mConBqbOutputStream = mConBqbSocket.getOutputStream();
            mConBqbInputStream = mConBqbSocket.getInputStream();

            String commandString = enable ? COMM_CONTROLLER_ENABLE : COMM_CONTROLLER_DISABLE;
            Log.d(TAG, "write: " + commandString);
            mConBqbOutputStream.write(commandString.getBytes());
            byte[] buffer = new byte[128];
            Log.d(TAG, "wait bqb response");
            int len = mConBqbInputStream.read(buffer);
            String res = new String(buffer, 0, len);
            Log.d(TAG, "Response: " + res);
            if (-1 != res.indexOf(NOTIFY_BQB_ENABLE)) {
                mControllerBqbState = true;
                mUiThread.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "CONTROLLER BQB ENABLE");
                        mControllerBqb.setSummary(CONTROLLER_BQB_ENABLED);
                    }
                });
            } else if (-1 != res.indexOf(NOTIFY_BQB_DISABLE)) {
                mControllerBqbState = false;
                mUiThread.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "CONTROLLER BQB DISABLE");
                        mControllerBqb.setSummary(CONTROLLER_BQB_DISABLED);
                    }
                });
            }

        } catch (IOException ex) {
            Log.e(TAG, "Communication error :", ex);
        } finally {
            closeConBqb();
        }
    }

    private void closeConBqb(){
        synchronized(sync) {
            try {
                if (mConBqbOutputStream != null) {
                    mConBqbOutputStream.close();
                    mConBqbOutputStream = null;
                }
                if (mConBqbInputStream != null) {
                    mConBqbInputStream.close();
                    mConBqbInputStream = null;
                }
                if (mConBqbSocket != null) {
                    mConBqbSocket.close();
                    mConBqbSocket = null;
                }
            } catch (IOException ex) {
                Log.e(TAG, "close error :", ex);
            }
        }
    }

    private boolean getControllerBqbState(){
        InputStream mInputStream = null;
        OutputStream mOutputStream = null;
        try {
        Log.d(TAG, "init bqb local socket");
            LocalSocketAddress address = new LocalSocketAddress(CONTROLLER_BQB_SOCKET,
                LocalSocketAddress.Namespace.ABSTRACT);
            mConBqbSocket = new LocalSocket();
            mConBqbSocket.connect(address);
            mOutputStream = mConBqbSocket.getOutputStream();
            mInputStream = mConBqbSocket.getInputStream();
            Log.d(TAG, "write bqb command: " + COMM_CONTROLLER_TRIGGER);
            mOutputStream.write(COMM_CONTROLLER_TRIGGER.getBytes());
            byte[] buffer = new byte[128];
            Log.d(TAG, "waite bqb response");
            int len = mInputStream.read(buffer);
            String res = new String(buffer, 0, len);
            Log.d(TAG, "Response: " + res);
            if (-1 != res.indexOf(TRIGGER_BQB_ENABLE)) {
                mUiThread.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "CONTROLLER BQB ENABLE");
                        mControllerBqb.setSummary(CONTROLLER_BQB_ENABLED);
                    }
                });
                return true;
            } else if (-1 != res.indexOf(TRIGGER_BQB_DISABLE)) {
                mUiThread.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "CONTROLLER BQB DISABLE");
                        mControllerBqb.setSummary(CONTROLLER_BQB_DISABLED);
                    }
                });
                return false;
            }
        } catch (IOException ex) {
            Log.e(TAG, "Communication error :", ex);
        } finally {
            try {
                if (mOutputStream != null) {
                    mOutputStream.close();
                    mOutputStream = null;
                }
                if (mInputStream != null) {
                    mInputStream.close();
                    mInputStream = null;
                }
                if (mConBqbSocket != null) {
                    mConBqbSocket.close();
                    mConBqbSocket = null;
                }
            } catch (IOException ex) {
                Log.e(TAG, "close error :", ex);
            }
        }
        return false;
    }

}
