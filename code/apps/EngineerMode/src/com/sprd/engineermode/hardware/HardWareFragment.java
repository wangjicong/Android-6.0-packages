
package com.sprd.engineermode.hardware;

import java.io.File;
import java.io.IOException;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.view.ViewGroup;
import android.preference.ListPreference;
import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import android.content.SharedPreferences.Editor;

import com.sprd.engineermode.R;

public class HardWareFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "HardWareFragment";
    private static final String KEY_CAMERA_API = "camera_api";
    private static final String CAMERA_API = "camera";
    private static final String CAMERA_API_SWITCH = "api_switch";

    private static final String KEY_USB_CHARGE = "usb_charge";

    private static final int MSG_GET_USB_CHARGE = 0;
    private static final int MSG_OPEN_USB_CHARGE = 1;
    private static final int MSG_CLOSE_USB_CHARGE = 2;

    private TwoStatePreference mUsbCharge;

    private Preference mHashValue;
    private Handler mUiThread = new Handler();
    private HardwareHandler mHWHandler;
    private SharedPreferences mSharePref;
    private SwitchPreference mCameraApi;
    private Preference mRootCheck;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_hardwaretab);
        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mHWHandler = new HardwareHandler(ht.getLooper());
        mSharePref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Preference deSensePll = (Preference) findPreference("de_sense_pll");
        mHashValue = (Preference) findPreference("hash_value");
        mHashValue.setSummary(R.string.feature_not_support);
        mUsbCharge = (TwoStatePreference) findPreference(KEY_USB_CHARGE);
        mUsbCharge.setOnPreferenceChangeListener(this);
        mRootCheck = (Preference) findPreference("root_check");
        boolean isUser = SystemProperties.get("ro.build.type").equalsIgnoreCase("user");
        Log.d(TAG, "isUser = " + isUser);
        if (isUser) {
            deSensePll.setEnabled(false);
            deSensePll.setSummary(R.string.feature_not_support_by_user_version);

        }
        mCameraApi = (SwitchPreference) findPreference(KEY_CAMERA_API);
        mCameraApi.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        if (mHashValue != null && mHashValue.isEnabled()) {
            if (hashValueWrited()) {
                mHashValue.setSummary(R.string.hash_value_writed);
            } else {
                mHashValue.setSummary(R.string.hash_value_not_writed);
            }
        } 
        if (mUsbCharge != null && mUsbCharge.isEnabled()) {
            boolean usbCharge = mSharePref.getBoolean("usb_charge",true);
            int message;
            if (usbCharge) {
                mUsbCharge.setChecked(true);
                message = MSG_OPEN_USB_CHARGE;
            } else {
                mUsbCharge.setChecked(false);
                message = MSG_CLOSE_USB_CHARGE;
            }
            Message setUsbChargeState = mHWHandler.obtainMessage(message);
            mHWHandler.sendMessage(setUsbChargeState);
        }
        if (mCameraApi != null) {
            mCameraApi.setChecked(SystemProperties.getBoolean("persist.sys.camera.camera_api",true));
        }
        
        if(mRootCheck!= null) {
        	if (get_rootflag() == 1) {
        		mRootCheck.setSummary(R.string.rooted);
            } else {
            	mRootCheck.setSummary(R.string.no_rooted);
            }
        }
        super.onStart();
    }

    boolean checkFileExists() {
    	return new File("/system/bin/su").exists() || new File("/system/app/Superuser.apk").exists();
    }
    native private int get_rootflag();
    
	@Override
    public boolean onPreferenceChange(Preference pref, Object objValue) {
        if (pref == mUsbCharge) {
            if (mUsbCharge.isChecked()) {
                Message closeUsbChargeState = mHWHandler.obtainMessage(MSG_CLOSE_USB_CHARGE);
                mHWHandler.sendMessage(closeUsbChargeState);
            } else {
                Message openUsbChargeState = mHWHandler.obtainMessage(MSG_OPEN_USB_CHARGE);
                mHWHandler.sendMessage(openUsbChargeState);
            }
        } else if (pref == mCameraApi) {
            if (mCameraApi.isChecked()) {
                SystemProperties.set("persist.sys.camera.camera_api","0");
                mCameraApi.setChecked(false);
            } else {
                SystemProperties.set("persist.sys.camera.camera_api","1");
                mCameraApi.setChecked(true);
            }
        }
        return true;
    }

    native private boolean hashValueWrited();

    static {
        System.loadLibrary("jni_engineermode");
    }

    class HardwareHandler extends Handler {
        public HardwareHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_USB_CHARGE:
                    String chargeStatus = readFile("/sys/class/power_supply/battery/stop_charge");
                    Log.d(TAG,"usb charge status is "+chargeStatus);
                    if (!"readError".equals(chargeStatus) && chargeStatus != null) {
                        if (chargeStatus.contains("1")) {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mUsbCharge.setChecked(false);
                                }
                            });
                        } else if (chargeStatus.contains("0")) {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mUsbCharge.setChecked(true);
                                }
                            });
                        } else {
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    mUsbCharge.setEnabled(false);
                                    mUsbCharge.setSummary(R.string.feature_abnormal);
                                }
                            });
                        }
                    } else {
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                mUsbCharge.setEnabled(false);
                                mUsbCharge.setSummary(R.string.feature_abnormal);
                            }
                        });
                    }
                    break;
                case MSG_OPEN_USB_CHARGE:{
                    String res = execShellStr("echo 0 > /sys/class/power_supply/battery/stop_charge");
                    Log.d(TAG, "open usb charge result is " + res);
                    mUiThread.post(new Runnable() {
                      @Override
                      public void run() {
                          mUsbCharge.setChecked(true);
                          Editor editor = mSharePref.edit();
                          editor.putBoolean("usb_charge",true);
                          editor.commit();
                      }
                  });
                }
                    break;
                case MSG_CLOSE_USB_CHARGE:{
                    String res = execShellStr("echo 1 > /sys/class/power_supply/battery/stop_charge");
                    mUiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            mUsbCharge.setChecked(false);
                            Editor editor = mSharePref.edit();
                            editor.putBoolean("usb_charge",false);
                            editor.commit();
                        }
                    });
                }
                    break;
                default:
                    break;
            }
        }
    }

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

    public String execShellStr(String cmd) {
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

            while ((null != (line = stdout.readLine())) || (null != (line = stderr.readLine()))) {
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
