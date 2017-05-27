/** Created by Spreadst */
package com.sprd.settings;

import java.util.ArrayList;
import java.util.List;

import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;
import com.sprd.android.config.OptConfig;//Kalyy 20160829

public class SprdUsbSettings extends PreferenceActivity {

    private final String LOG_TAG = "SprdUsbSettings";
    private final static boolean DBG = true;

    public static final int DEFAUT_FUNCTION = 0;
    public static final int CHARGE_ONLY = DEFAUT_FUNCTION + 1;
    public static final int TETHER = DEFAUT_FUNCTION + 2;
    public static final int CDROM = DEFAUT_FUNCTION + 3;
    public static final int UMS = DEFAUT_FUNCTION + 4;
    public static final int MTP = DEFAUT_FUNCTION + 5;
    public static final int PTP = DEFAUT_FUNCTION + 6;
    public static final int MIDI = DEFAUT_FUNCTION + 7;
    public static final int INTERNET_SHARE = DEFAUT_FUNCTION + 8;

    private static final String KEY_CHARGE_ONLY = "usb_charge_only";
    private static final String KEY_TETHER = "usb_tether_settings";
    private static final String KEY_UMS = "usb_storage";
    private static final String KEY_CDROM = "usb_virtual_drive";
    private static final String KEY_REMEMBER = "remember_choice";
    private static final String KEY_MTP = "usb_mtp";
    private static final String KEY_PTP = "usb_ptp";
    private static final String KEY_MIDI = "usb_midi";
    private static final String KEY_INTERNET = "usb_pc_internet_share";

    private CheckBoxPreference mMtp;
    private CheckBoxPreference mMidi;
    private CheckBoxPreference mPtp;
    private CheckBoxPreference mUsbChargeOnly;
    private CheckBoxPreference mUsbTether;
    private CheckBoxPreference mUms;
    private CheckBoxPreference mCdrom;
    private CheckBoxPreference mRememberChoice;
    private CheckBoxPreference mInternetShare;

    private UsbManager mUsbManager = null;
    private StorageManager mStorageManager = null;
    private ConnectivityManager mConnectivityManager = null;
    private KeyguardManager mKeyguardManager = null;
    private WifiManager mWifiManager = null;

    private BroadcastReceiver mUmsReceiver = null;
    private BroadcastReceiver mTetherChangeReceiver = null;
    private BroadcastReceiver mPowerDisconnectReceiver = null;
    private BroadcastReceiver mUnlockReceiver = null;
    // private BroadcastReceiver mMtpReceiver = null;
    // private BroadcastReceiver mPtpReceiver = null;

    private ProgressDialog mDialog = null;
    private String[] mUsbRegexs;

    private boolean mUsbConnected = false;
    private boolean mLastRememberStatus = false;
    private int selectedItem = 0;
    private boolean mDelayDoUms = false;
    private boolean mUsbAccessoryMode = false;
    private int test = 0;
    private int resultCode = -1;
    private static final boolean IS_SUPPORT_USB_REVERSE_TETHER = SystemProperties.getBoolean("persist.sys.usb-pc.tethering",true);

    private boolean SUPPORT_CTA = SystemProperties.getBoolean("ro.usb.support_cta", false);

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (mKeyguardManager.isKeyguardLocked()) {
                if (DBG)
                    Log.d(LOG_TAG, "keyguard locked and do nothing.");
                return;
            }
            if (DBG)
                Log.d(LOG_TAG, "msg.what = " + msg.what + ", msg.arg1 = "
                        + msg.arg1);
            switch (msg.what) {
            case CHARGE_ONLY:
                if (msg.arg1 == 1) {
                    if (SUPPORT_CTA) {
                        mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_NONE);
                    } else {
                        mUsbManager.setCurrentFunction(null);
                    }
                    mUsbManager.setUsbDataUnlocked(false);
                } else {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE);
                    mUsbManager.setUsbDataUnlocked(true);
                }
                break;
            case TETHER:
                test = mConnectivityManager.setUsbTethering(msg.arg1 == 1);
                if (test != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                    Toast.makeText(SprdUsbSettings.this,
                            R.string.usb_tethering_errored_subtext,
                            Toast.LENGTH_SHORT).show();
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE);
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION,
                            DEFAUT_FUNCTION);
                }
                break;
            case UMS:
                if (msg.arg1 == 1 && umsSuccess()) {
                    updateUI();
                    break;
                }

                if ((mDialog == null || !mDialog.isShowing())
                        && SprdUsbSettings.this.isResumed()) {
                    mDialog = new ProgressDialog(SprdUsbSettings.this);
                    mDialog.setTitle(R.string.ums_settings);
                    mDialog.setCancelable(false);
                    mDialog.setIndeterminate(true);
                    if (msg.arg1 == 1) {
                        mDialog.setMessage(getText(R.string.ums_mounted_setting));
                    } else {
                        mDialog.setMessage(getText(R.string.ums_unmounted_setting));
                    }
                    mDialog.show();
                }

                // Sprd: waiting for ums avaible.
                if (msg.arg1 == 1 && !isUmsAvailable()) {
                    if (!UsbManager.USB_FUNCTION_MASS_STORAGE
                            .equals(getCurrentFunction())) {
                        mUsbManager.setCurrentFunction(
                                UsbManager.USB_FUNCTION_MASS_STORAGE);
                        mUsbTether.setChecked(false);
                    }
                    removeMessages(UMS);
                    Message message = new Message();
                    message.what = UMS;
                    message.arg1 = 1;
                    sendMessageDelayed(message, 300);
                    mDelayDoUms = true;
                    return;
                }
                if (msg.arg1 == 1) {
                    new Thread() {
                        public void run() {
                            mStorageManager.enableUsbMassStorage();
                        }
                    }.start();
                } else {
                    new Thread() {
                        public void run() {
                            mStorageManager.disableUsbMassStorage();
                        }
                    }.start();
                }
                break;
            case CDROM:
                if (msg.arg1 == 1) {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_CDROM);
                } else {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE);
                }
                break;

            case MTP:
                if (msg.arg1 == 1) {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP);
                } else {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE);
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION,
                            DEFAUT_FUNCTION);
                }
                mUsbManager.setUsbDataUnlocked(true);
                break;
            case PTP:
                if (msg.arg1 == 1) {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_PTP);
                } else {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE);
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION,
                            DEFAUT_FUNCTION);
                }
                mUsbManager.setUsbDataUnlocked(true);
                break;

            case MIDI:
                if (msg.arg1 == 1) {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MIDI);
                } else {
                    mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MASS_STORAGE);
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION,
                            DEFAUT_FUNCTION);
                }
                mUsbManager.setUsbDataUnlocked(true);
                break;

            default:
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DBG)
            Log.d(LOG_TAG, "on Create");
        setContentView(R.layout.sprd_usb_screen);
        addPreferencesFromResource(R.xml.sprd_usb_settings);

        mUsbChargeOnly = (CheckBoxPreference) findPreference(KEY_CHARGE_ONLY);
        mUsbTether = (CheckBoxPreference) findPreference(KEY_TETHER);
        mInternetShare = (CheckBoxPreference) findPreference(KEY_INTERNET);
        if (!IS_SUPPORT_USB_REVERSE_TETHER && mInternetShare != null) {
            getPreferenceScreen().removePreference(mInternetShare);
        }
        mUms = (CheckBoxPreference) findPreference(KEY_UMS);
        mCdrom = (CheckBoxPreference) findPreference(KEY_CDROM);
        mRememberChoice = (CheckBoxPreference) findPreference(KEY_REMEMBER);

        mMtp = (CheckBoxPreference) findPreference(KEY_MTP);
        mPtp = (CheckBoxPreference) findPreference(KEY_PTP);
        mMidi = (CheckBoxPreference) findPreference(KEY_MIDI);

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbRegexs = mConnectivityManager.getTetherableUsbRegexs();

        String device = SystemProperties.get("ro.device.support.cdrom");
        if (null != device && device.equals("0")) {
            getPreferenceScreen().removePreference(mCdrom);
        }

        boolean isFileTransferRestricted = ((UserManager) getSystemService(Context.USER_SERVICE))
                .hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER);
        if (isFileTransferRestricted) {
            getPreferenceScreen().removePreference(mMtp);
            getPreferenceScreen().removePreference(mPtp);
        }
        if(OptConfig.SUN_C7359_C5D_FWVGA_CHERRY){//Kalyy 20160829
            getPreferenceScreen().removePreference(mMidi);
        }
        if(OptConfig.SUN_CUSTOM_C7356_XT_HVGA_SUPPORT){//jxl add
            getPreferenceScreen().removePreference(mMidi);
            getPreferenceScreen().removePreference(mUsbTether);
            getPreferenceScreen().removePreference(mInternetShare);
            getPreferenceScreen().removePreference(mUms);
            getPreferenceScreen().removePreference(mCdrom);
            getPreferenceScreen().removePreference(mRememberChoice);
        }

        mPowerDisconnectReceiver = new PowerDisconnectReceiver();
        registerReceiver(mPowerDisconnectReceiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));

        mUnlockReceiver = new UnlockReceiver();
        registerReceiver(mUnlockReceiver, new IntentFilter(
                Intent.ACTION_USER_PRESENT));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DBG)
            Log.d(LOG_TAG, "on onStart");
        // String mCurrentfunction = getCurrentFunction();
        // if (UsbManager.USB_FUNCTION_MTP.equals(mCurrentfunction)
        // || UsbManager.USB_FUNCTION_PTP.equals(mCurrentfunction)) {
        // return;
        // }
        if (Settings.Global.getInt(getContentResolver(),
                Settings.Global.USB_REMEMBER_CHOICE, 0) != 0) {
            selectedItem = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
            if (selectedItem == UMS && !isSdcardAvailable()
                    && !isInternalSdcardAvailable()) {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
                return;
            } else if(selectedItem == INTERNET_SHARE) {
                String ip = Settings.Global.getString(getContentResolver(),"InternetShareIP");
                setInternetShare(true, ip.equals("null") ? null:ip);
                return;
            }
            Message msg = Message.obtain(mHandler, selectedItem);
            msg.arg1 = 1;
            mHandler.sendMessageDelayed(msg, 0);
        }

        // SPRD: Close the showing system dialog when start SprdUsbSettings.
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DBG)
            Log.d(LOG_TAG, "on Resume");

        selectedItem = Settings.Global.getInt(getContentResolver(),
                Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
        if (selectedItem == UMS && !isSdcardAvailable()
                && !isInternalSdcardAvailable()) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
            selectedItem = DEFAUT_FUNCTION;
        }

        // Dialog should dismiss after share/unshare is done if not receive
        // broadcast.
        if (mDialog != null
                && mDialog.isShowing()
                && ((selectedItem == UMS && umsSuccess()) || (selectedItem == DEFAUT_FUNCTION && !umsSuccess()))) {
            mDialog.dismiss();
            mDialog = null;
        }

        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(
                ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(UsbManager.ACTION_USB_STATE);
        filter.addAction(ConnectivityManager.ACTION_PC_SHARE_CHANGED);
        registerReceiver(mTetherChangeReceiver, filter);

        mUmsReceiver = new UmsReceiver();
        IntentFilter ums_filter = new IntentFilter();
        ums_filter.addAction(Intent.ACTION_MEDIA_SHARED);
        ums_filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        ums_filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        ums_filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        ums_filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        ums_filter.addAction(Intent.ACTION_MEDIA_NOFS);
        ums_filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        ums_filter.addDataScheme("file");
        registerReceiver(mUmsReceiver, ums_filter);

        updateUI();

        if (resultCode == 1) {
            String ip = getIntent().getStringExtra("ip");
            setInternetShare(true, ip);
        } else if (resultCode == 0) {
            mInternetShare.setChecked(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mTetherChangeReceiver);
        unregisterReceiver(mUmsReceiver);
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPowerDisconnectReceiver);
        unregisterReceiver(mUnlockReceiver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (preference != mRememberChoice) {
            mHandler.removeMessages(CHARGE_ONLY);
            mHandler.removeMessages(TETHER);
            mHandler.removeMessages(UMS);
            mHandler.removeMessages(CDROM);
            mHandler.removeMessages(MTP);
            mHandler.removeMessages(PTP);
            mHandler.removeMessages(MIDI);
            mHandler.removeMessages(INTERNET_SHARE);
            disableAllUI();
        }

        if (preference == mRememberChoice) {
            mLastRememberStatus = mRememberChoice.isChecked();
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.USB_REMEMBER_CHOICE,
                    mLastRememberStatus ? 1 : 0);
            return true;
        } else if (preference == mUsbChargeOnly) {
            boolean mUsbCharged = mUsbChargeOnly.isChecked();
            Message msg = Message.obtain(mHandler, CHARGE_ONLY);
            msg.arg1 = mUsbCharged ? 1 : 0;
            mHandler.sendMessageDelayed(msg, 0);
            Log.d(LOG_TAG, "mUsbCharged = " + mUsbCharged + ", "
                    + mUsbChargeOnly.isChecked() + ", msg.arg1 = " + msg.arg1);
            selectedItem = mUsbCharged ? CHARGE_ONLY : DEFAUT_FUNCTION;
        } else if (preference == mUsbTether) {
            boolean mUsbTethered = mUsbTether.isChecked();
            Message msg = Message.obtain(mHandler, TETHER);
            msg.arg1 = mUsbTethered ? 1 : 0;
            mHandler.sendMessageDelayed(msg, mUsbTethered ? 300 : 0);
            selectedItem = mUsbTethered ? TETHER : DEFAUT_FUNCTION;
        } else if (preference == mInternetShare) {
            boolean mInternetShared = mInternetShare.isChecked();
            Log.d(LOG_TAG, "onPreferenceTreeClick mInternetShared="+mInternetShared);
            if (mInternetShared) {
                Intent intent = new Intent(SprdUsbSettings.this,SprdChooseOS.class);
                startActivity(intent);
            } else {
                setInternetShare(false ,null);
            }
            return true;
        } else if (preference == mUms) {
            boolean mUmsSelected = mUms.isChecked();
            Message msg = Message.obtain(mHandler, UMS);
            msg.arg1 = mUmsSelected ? 1 : 0;
            mHandler.sendMessageDelayed(msg, 0);
            Log.d(LOG_TAG, "UMS = " + mUmsSelected + ", " + mUms.isChecked()
                    + ", msg.arg1 = " + msg.arg1);
            selectedItem = mUmsSelected ? UMS : DEFAUT_FUNCTION;
        } else if (preference == mCdrom) {
            boolean mCdromSelected = mCdrom.isChecked();
            Message msg = Message.obtain(mHandler, CDROM);
            msg.arg1 = mCdromSelected ? 1 : 0;
            mHandler.sendMessageDelayed(msg, mCdromSelected ? 500 : 0);
            selectedItem = mCdromSelected ? CDROM : DEFAUT_FUNCTION;
        } else if (preference == mMtp) {
            boolean mMtpSelected = mMtp.isChecked();
            Message msg = Message.obtain(mHandler, MTP);
            msg.arg1 = mMtpSelected ? 1 : 0;
            mHandler.sendMessageDelayed(msg, mMtpSelected ? 500 :0);
            selectedItem = mMtpSelected ? MTP : DEFAUT_FUNCTION;
        } else if (preference == mPtp) {
            boolean mPtpSelected = mPtp.isChecked();
            Message msg = Message.obtain(mHandler, PTP);
            msg.arg1 = mPtpSelected ? 1 : 0;
            mHandler.sendMessageDelayed(msg, mPtpSelected ? 500 :0);
            selectedItem = mPtpSelected ? PTP : DEFAUT_FUNCTION;
        } else if (preference == mMidi) {
            boolean mMidiSelected = mMidi.isChecked();
            Message msg = Message.obtain(mHandler, MIDI);
            msg.arg1 = mMidiSelected ? 1 : 0;
            mHandler.sendMessageDelayed(msg, 0);
            selectedItem = mMidiSelected ? MIDI : DEFAUT_FUNCTION;
        }
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.USB_CURRENT_FUNCTION, selectedItem);
        return true;
    }

    private boolean stateOK(int state, int []stateArray) {
        for (int st:stateArray) {
            if (state == st) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllPublicVolumesStateOK(int []states) {
        boolean stateOK = true;
        int publicCount = 0;
        List<VolumeInfo> allVolumes = mStorageManager.getVolumes();
        for(VolumeInfo vol:allVolumes) {
            if (vol.type == VolumeInfo.TYPE_PUBLIC) {
                publicCount ++;
                if (!stateOK(vol.state, states)) {
                    stateOK = false;
                    break;
                }
            }
        }
        if (publicCount == 0) {
            return false;
        }
        return stateOK;
    }

    private boolean isAllPublicVolumesShared() {
        int []states = new int[1];
        states[0] = VolumeInfo.STATE_SHARED;
        return isAllPublicVolumesStateOK(states);
    }

    private boolean isAllPublicVolumesUnshared() {
        int []states = new int[2];
        states[0] = VolumeInfo.STATE_MOUNTED;
        states[1] = VolumeInfo.STATE_UNMOUNTED;
        return isAllPublicVolumesStateOK(states);
    }

    private boolean umsSuccess() {
        if (isAllPublicVolumesShared()) {
            return true;
        }
        String mSdcardState = getSdcardState();
        if (!Environment.MEDIA_REMOVED.equals(mSdcardState)
                && !Environment.MEDIA_BAD_REMOVAL.equals(mSdcardState)
                && !Environment.MEDIA_NOFS.equals(mSdcardState)
                && !Environment.MEDIA_SHARED.equals(mSdcardState)
                && !Environment.MEDIA_UNKNOWN.equals(mSdcardState)) {
            return false;
        } else if (isInternalSdcardAvailable()
                && !Environment.MEDIA_SHARED.equals(getInternalSdcardState())) {
            return false;
        }
        return true;
    }

    /**
     * SPRD: check the storage storage.
     *
     * @return true if all mountable storages are mounted.
     */
    private boolean mountSuccess() {
        if (isAllPublicVolumesUnshared()) {
            return true;
        }
        String mSdcardState = getSdcardState();
        if (!Environment.MEDIA_REMOVED.equals(mSdcardState)
                && !Environment.MEDIA_BAD_REMOVAL.equals(mSdcardState)
                && !Environment.MEDIA_MOUNTED.equals(mSdcardState)
                && !Environment.MEDIA_UNKNOWN.equals(mSdcardState)) {
            return false;
        } else if (!Environment.MEDIA_MOUNTED.equals(getInternalSdcardState())) {
            return false;
        }
        return true;
    }

    private boolean isMtpPtpAvailable() {
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER)
                || mUsbAccessoryMode) {
            return false;
        }
        return true;
    }

    /**
     * mounted and unmounted are available states for ums. and other states are
     * not states for ums
     *
     * @return
     */
    private boolean isUmsAvailable() {
        if (!mUsbConnected) {
            return false;
        }

        if (isSdcardAvailable()) {
            String mSdcardState = getSdcardState();
            if (!Environment.MEDIA_MOUNTED.equals(mSdcardState)
                    && !Environment.MEDIA_UNMOUNTED.equals(mSdcardState)) {
                return false;
            }
        } else if (isInternalSdcardAvailable()) {
            String mInternalSdcardState = getInternalSdcardState();
            if (!Environment.MEDIA_MOUNTED.equals(mInternalSdcardState)
                    && !Environment.MEDIA_UNMOUNTED
                            .equals(mInternalSdcardState)) {
                return false;
            }
        } else {
            // Both sdcard and internal sdcard are unavailable.
            return false;
        }

        if (!UsbManager.USB_FUNCTION_MASS_STORAGE.equals(getCurrentFunction())) {
            return false;
        }
        return true;
    }

    private boolean isSdcardAvailable() {
        String mSdcardState = getSdcardState();
        if (!Environment.MEDIA_REMOVED.equals(mSdcardState)
                && !Environment.MEDIA_BAD_REMOVAL.equals(mSdcardState)
                && !Environment.MEDIA_NOFS.equals(mSdcardState)
                && !Environment.MEDIA_UNMOUNTABLE.equals(mSdcardState)
                && !Environment.MEDIA_UNKNOWN.equals(mSdcardState)) {
            return true;
        }

        return false;
    }

    private String getSdcardState() {
        return Environment.getExternalStoragePathState();
    }
    /* SPRD: add for internal T card  @{ */
    private String getInternalSdcardState() {
        return Environment.getInternalStoragePathState();
    }
    private boolean isInternalSdcardAvailable() {
        String interSdcardState = getInternalSdcardState();

        if (DBG) {
            Log.d(LOG_TAG, "interSdcardState = " + interSdcardState
                    + ", isEmulated = " + Environment.internalIsEmulated());
        }
        if (!Environment.MEDIA_REMOVED.equals(interSdcardState)
                && !Environment.MEDIA_BAD_REMOVAL.equals(interSdcardState)
                && !Environment.MEDIA_NOFS.equals(interSdcardState)
                && !Environment.MEDIA_UNMOUNTABLE.equals(interSdcardState)
                && !Environment.internalIsEmulated()
                && !Environment.MEDIA_UNKNOWN.equals(interSdcardState)) {
            return true;
        }

        return false;
    }

    /* @} */

    private void updateUI() {
        if (SprdUsbReceiver.isPowerOff()) {
            finish();
            return;
        }

        String mCurrentfunction = getCurrentFunction();
        if (Settings.Global.getInt(getContentResolver(),
                Settings.Global.USB_CURRENT_FUNCTION, 0) == 0) {
            mCurrentfunction = "none";
        }

        Log.i(LOG_TAG, "mCurrentfunction = " + mCurrentfunction
                + ", selectedItem = " + selectedItem);

        /*if (UsbManager.USB_FUNCTION_MTP.equals(mCurrentfunction)) {
            disableAllUI();
            mMtp.setEnabled(true);
            mMtp.setChecked(true);
            return;
        } else if (UsbManager.USB_FUNCTION_PTP.equals(mCurrentfunction)) {
            disableAllUI();
            mPtp.setEnabled(true);
            mPtp.setChecked(true);
            return;
        }*/

        if (CHARGE_ONLY == selectedItem
                && (UsbManager.USB_FUNCTION_NONE.equals(mCurrentfunction) || !SUPPORT_CTA)) {
            mUsbChargeOnly.setEnabled(true);
            mUsbChargeOnly.setChecked(true);
            mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            mUsbTether.setChecked(false);
            mUsbTether.setEnabled(false);
            mUms.setChecked(false);
            mUms.setEnabled(false);
            mCdrom.setChecked(false);
            mCdrom.setEnabled(false);
            mMtp.setEnabled(false);
            mMtp.setChecked(false);
            mPtp.setEnabled(false);
            mPtp.setChecked(false);
            mMidi.setEnabled(false);
            mMidi.setChecked(false);
            mInternetShare.setChecked(false);
            mInternetShare.setEnabled(false);
            mInternetShare.setSummary(R.string.usb_pc_internet_share_available_subtext);
        } else if (mUsbConnected) {
            mLastRememberStatus = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.USB_REMEMBER_CHOICE, 0) != 0;
            mRememberChoice.setChecked(mLastRememberStatus);
            switch (selectedItem) {
            case DEFAUT_FUNCTION:
                mUsbChargeOnly.setEnabled(true);
                mUsbChargeOnly.setChecked(false);
                mUsbChargeOnly.setSummary(R.string.usb_charge_only_summary);
                mUsbTether.setEnabled(true);
                mUsbTether.setChecked(false);
                mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
                mCdrom.setEnabled(true);
                mCdrom.setChecked(false);
                mCdrom.setSummary(R.string.usb_virtual_drive_summary);
                mMtp.setEnabled(true);
                mMtp.setChecked(false);
                mMtp.setSummary(R.string.usb_mtp_summary);
                mPtp.setEnabled(true);
                mPtp.setChecked(false);
                mPtp.setSummary(R.string.usb_ptp_summary);
                mMidi.setEnabled(true);
                mMidi.setChecked(false);
                mMidi.setSummary(R.string.usb_midi_summary);
                mInternetShare.setChecked(false);
                mInternetShare.setEnabled(true);
                mInternetShare.setSummary(R.string.usb_pc_internet_share_available_subtext);
                if (!isSdcardAvailable() && !isInternalSdcardAvailable()) {
                    mUms.setEnabled(false);
                } else {
                    mUms.setEnabled(/*Environment.MEDIA_MOUNTED
                            .equals(getSdcardState())
                            || Environment.MEDIA_MOUNTED
                                    .equals(getInternalSdcardState())
                            || */isAllPublicVolumesUnshared());
                }
                mUms.setChecked(false);
                mUms.setSummary(R.string.usb_storage_summary);
                break;
            case TETHER:
                mUsbTether.setEnabled(true);
                mUsbTether.setChecked(true);
                mUsbChargeOnly.setChecked(false);
                mUsbChargeOnly.setEnabled(false);
                mUms.setChecked(false);
                mUms.setEnabled(false);
                mCdrom.setChecked(false);
                mCdrom.setEnabled(false);
                mMtp.setEnabled(false);
                mMtp.setChecked(false);
                mPtp.setEnabled(false);
                mPtp.setChecked(false);
                mMidi.setEnabled(false);
                mMidi.setChecked(false);
                mInternetShare.setChecked(false);
                mInternetShare.setEnabled(false);
                break;
            case INTERNET_SHARE:
                mInternetShare.setChecked(true);
                mInternetShare.setEnabled(true);
                mUsbTether.setEnabled(false);
                mUsbTether.setChecked(false);
                mUsbChargeOnly.setChecked(false);
                mUsbChargeOnly.setEnabled(false);
                mUms.setChecked(false);
                mUms.setEnabled(false);
                mCdrom.setChecked(false);
                mCdrom.setEnabled(false);
                mMtp.setEnabled(false);
                mMtp.setChecked(false);
                mPtp.setEnabled(false);
                mPtp.setChecked(false);
                mMidi.setEnabled(false);
                mMidi.setChecked(false);
                break;
            case CDROM:
                mCdrom.setEnabled(true);
                mCdrom.setChecked(true);
                mUsbTether.setEnabled(false);
                mUsbTether.setEnabled(false);
                mUsbChargeOnly.setChecked(false);
                mUsbChargeOnly.setEnabled(false);
                mUms.setChecked(false);
                mUms.setEnabled(false);
                mMtp.setEnabled(false);
                mMtp.setChecked(false);
                mPtp.setEnabled(false);
                mPtp.setChecked(false);
                mMidi.setEnabled(false);
                mMidi.setChecked(false);
                mInternetShare.setChecked(false);
                mInternetShare.setEnabled(false);
                break;
            case UMS:
                mRememberChoice.setEnabled(true);
                if (!isSdcardAvailable() && !isInternalSdcardAvailable()) {
                    mUms.setEnabled(false);
                    mUms.setChecked(false);
                } else {
                    Log.i(LOG_TAG, "umsSuccess() : " + umsSuccess());
                    mUms.setEnabled(true);
                    mUms.setChecked(umsSuccess());
                }
                if (mUms.isChecked()) {
                    mUsbChargeOnly.setChecked(false);
                    mUsbChargeOnly.setEnabled(false);
                    mUsbChargeOnly
                            .setSummary(R.string.usb_charge_only_storage_active_subtext);
                    mUsbTether.setChecked(false);
                    mUsbTether.setEnabled(false);
                    mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
                    mCdrom.setChecked(false);
                    mCdrom.setEnabled(false);
                    mMtp.setEnabled(false);
                    mMtp.setChecked(false);
                    mPtp.setEnabled(false);
                    mPtp.setChecked(false);
                    mInternetShare.setChecked(false);
                    mInternetShare.setEnabled(false);
                    mInternetShare.setSummary(R.string.usb_pc_internet_share_storage_active_subtext);
                }
                break;

            case MTP:
                if (isMtpPtpAvailable()) {
                    mMtp.setEnabled(true);
                    mMtp.setChecked(true);
                } else {
                    mMtp.setEnabled(false);
                    mMtp.setChecked(false);
                }

                mPtp.setEnabled(false);
                mPtp.setChecked(false);
                mCdrom.setEnabled(false);
                mCdrom.setChecked(false);
                mUsbTether.setEnabled(false);
                mUsbTether.setEnabled(false);
                mUsbChargeOnly.setChecked(false);
                mUsbChargeOnly.setEnabled(false);
                mUms.setChecked(false);
                mUms.setEnabled(false);
                mMidi.setEnabled(false);
                mMidi.setChecked(false);
                mInternetShare.setChecked(false);
                mInternetShare.setEnabled(false);
                mInternetShare.setSummary(R.string.usb_pc_internet_share_storage_active_subtext);
                break;
            case PTP:
                if (isMtpPtpAvailable()) {
                    mPtp.setEnabled(true);
                    mPtp.setChecked(true);
                } else {
                    mPtp.setEnabled(false);
                    mPtp.setChecked(false);
                }
                mMtp.setEnabled(false);
                mMtp.setChecked(false);
                mCdrom.setEnabled(false);
                mCdrom.setChecked(false);
                mUsbTether.setEnabled(false);
                mUsbTether.setEnabled(false);
                mUsbChargeOnly.setChecked(false);
                mUsbChargeOnly.setEnabled(false);
                mMidi.setEnabled(false);
                mMidi.setChecked(false);
                mInternetShare.setChecked(false);
                mInternetShare.setEnabled(false);
                mInternetShare.setSummary(R.string.usb_pc_internet_share_storage_active_subtext);
                break;
            case MIDI:
                mMtp.setEnabled(false);
                mMtp.setChecked(false);
                mCdrom.setEnabled(false);
                mCdrom.setChecked(false);
                mUsbTether.setEnabled(false);
                mUsbTether.setEnabled(false);
                mUsbChargeOnly.setChecked(false);
                mUsbChargeOnly.setEnabled(false);
                mMidi.setEnabled(true);
                mMidi.setChecked(true);
                mInternetShare.setChecked(false);
                mInternetShare.setEnabled(false);
                break;
            }
        } else {
            disableAllUI();
        }
    }

    private void updateStatus() {
        String[] available = mConnectivityManager.getTetherableIfaces();
        String[] tethered = mConnectivityManager.getTetheredIfaces();
        String[] errored = mConnectivityManager.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    /**
     ** update charge_only && tethering && usb_mass_storage
     */
    private void updateState(Object[] available, Object[] tethered,
            Object[] errored) {
        if (DBG)
            Log.d(LOG_TAG, "updateStatus");
        if (mUsbRegexs.length == 0)
            mUsbRegexs = mConnectivityManager.getTetherableUsbRegexs();
        boolean mMassStorageActive = umsSuccess();
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        selectedItem = Settings.Global.getInt(getContentResolver(),Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
        for (Object o : available) {
            String s = (String) o;
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    usbAvailable = true;
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = mConnectivityManager.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (Object o : tethered) {
            String s = (String) o;
            for (String regex : mUsbRegexs) {
                if (s.matches(regex))
                    usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (Object o : errored) {
            String s = (String) o;
            for (String regex : mUsbRegexs) {
                if (s.matches(regex))
                    usbErrored = true;
            }
        }

        if (DBG) {
            Log.d(LOG_TAG, "updateStatus usbTethered = " + usbTethered
                    + ", usbAvailable = " + usbAvailable + ", usbErrored = "
                    + usbErrored + ", mMassStorageActive = "
                    + mMassStorageActive);
        }

        if (usbTethered) {
            mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
        } else if (usbAvailable) {
            if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
        } else if (usbErrored) {
            if (selectedItem == TETHER) {
                Toast.makeText(this, R.string.usb_tethering_errored_subtext,
                        Toast.LENGTH_SHORT).show();
            } else if (selectedItem == INTERNET_SHARE) {
                Toast.makeText(this, R.string.usb_pc_internet_share_errored_subtext,
                        Toast.LENGTH_SHORT).show();
                mInternetShare.setSummary(R.string.usb_pc_internet_share_errored_subtext);
            }
            mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
        } else {
            mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
        }
        if (usbTethered) {
            if ((Settings.Global.getInt(getContentResolver(),
                    Settings.Global.USB_CURRENT_FUNCTION, 0) != TETHER)
                    && UsbManager.USB_FUNCTION_RNDIS
                            .equals(getCurrentFunction())) {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.USB_CURRENT_FUNCTION, TETHER);
            }
            updateUI();
        } else if ((usbErrored || UsbManager.USB_FUNCTION_MASS_STORAGE
                .equals(getCurrentFunction()))
                && Settings.Global.getInt(getContentResolver(),
                        Settings.Global.USB_CURRENT_FUNCTION, 0) == TETHER) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
            updateUI();
        }
    }

    /**
     ** when checked or unchecked one,firstly disable all in the UI,Prevent to
     * operator others before enable or disable function succeed or fail
     */
    private void disableAllUI() {
        mUsbChargeOnly.setEnabled(false);
        mUsbTether.setEnabled(false);
        mUms.setEnabled(false);
        mCdrom.setEnabled(false);
        mMtp.setEnabled(false);
        mPtp.setEnabled(false);
        mMidi.setEnabled(false);
        mInternetShare.setEnabled(false);
    }

    public String getCurrentFunction() {
        String functions = SystemProperties.get("sys.usb.config", "");
        int commaIndex = functions.indexOf(',');
        if (commaIndex > 0) {
            return functions.substring(0, commaIndex);
        } else {
            return functions;
        }
    }

    private class UmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "ums action : " + action);
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED) ) {
                if (mDelayDoUms) {
                    return;
                } else {
                    if (!mountSuccess()) {
                        return;
                    }
                    if ((Settings.Global.getInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION,
                            DEFAUT_FUNCTION) == UMS)
                            && (Settings.Global.getInt(getContentResolver(),
                                    Settings.Global.USB_REMEMBER_CHOICE, 0) == 0)) {
                        Settings.Global.putInt(getContentResolver(),
                                Settings.Global.USB_CURRENT_FUNCTION,
                                DEFAUT_FUNCTION);
                    }
                }
            } else {
                if (mDelayDoUms) {
                    mHandler.removeMessages(UMS);
                    mDelayDoUms = false;
                }
                if (action.equals(Intent.ACTION_MEDIA_SHARED)
                        || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                    if (!umsSuccess()) {
                        return;
                    }
                }
            }

            if (DBG)
                Log.d(LOG_TAG, "mDialog = " + mDialog);
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
            if (!isSdcardAvailable()
                    && !isInternalSdcardAvailable()
                    && (Settings.Global.getInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION,
                            DEFAUT_FUNCTION) == UMS)) {
                Settings.Global.putInt(getContentResolver(),
                        Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
            }
            updateUI();
        }
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Log.i(LOG_TAG, "action is: " + action);
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent
                        .getStringArrayListExtra(ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent
                        .getStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent
                        .getStringArrayListExtra(ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateState(available.toArray(new String[available.size()]),
                        active.toArray(new String[active.size()]),
                        errored.toArray(new String[errored.size()]));
            } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(
                        UsbManager.USB_CONNECTED, false);
                mUsbAccessoryMode = intent.getBooleanExtra(
                        UsbManager.USB_FUNCTION_ACCESSORY, false);
                if (DBG) {
                    Log.d(LOG_TAG, "mUsbConnected = " + mUsbConnected
                            + ", mUsbAccessoryMode = " + mUsbAccessoryMode);
                }

                if (!mUsbConnected) {
                    if (mDialog != null && mDialog.isShowing() && !mDelayDoUms) {
                        if (DBG)
                            Log.d(LOG_TAG, "mDialog = " + mDialog);
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }
                updateUI();
            } else if (action.equals(ConnectivityManager.ACTION_PC_SHARE_CHANGED)) {
                if (intent.getBooleanExtra(ConnectivityManager.ACTION_PC_SHARE_CHANGED, false) == true) {
                    mInternetShare.setSummary(R.string.usb_pc_internet_share_active_subtext);
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION, INTERNET_SHARE);
                } else {
                    if (INTERNET_SHARE == Settings.Global.getInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION, 0)) {
                        mInternetShare.setSummary(R.string.usb_pc_internet_share_available_subtext);
                        Settings.Global.putInt(getContentResolver(),
                                Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
                    }
                }
                updateUI();
            }
        }
    }

    private class PowerDisconnectReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            if (DBG)
                Log.d(LOG_TAG, "plugType = " + plugType);
            if (plugType == 0) {
                if ((Settings.Global.getInt(getContentResolver(),
                        Settings.Global.USB_REMEMBER_CHOICE, 0) == 0) || (selectedItem == 0)) {
                    Settings.Global.putInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION, DEFAUT_FUNCTION);
                }
                disableAllUI();
                SprdUsbSettings.this.finish();
            }
        }
    }

    private class UnlockReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_PRESENT)) {
                Log.i(LOG_TAG, "ACTION_USER_PRESENT is received");
                if (Settings.Global.getInt(getContentResolver(),
                        Settings.Global.USB_REMEMBER_CHOICE, 0) != 0) {
                    selectedItem = Settings.Global.getInt(getContentResolver(),
                            Settings.Global.USB_CURRENT_FUNCTION,
                            DEFAUT_FUNCTION);
                    if (selectedItem == UMS && !isSdcardAvailable()
                            && !isInternalSdcardAvailable()) {
                        Settings.Global.putInt(getContentResolver(),
                                Settings.Global.USB_CURRENT_FUNCTION,
                                DEFAUT_FUNCTION);
                        return;
                    }
                    Message msg = Message.obtain(mHandler, selectedItem);
                    msg.arg1 = 1;
                    mHandler.sendMessageDelayed(msg, 0);
                }
            }
        }
    }

    private void setInternetShare(boolean isShared, String ip) {
        int result = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        if(isShared) {
            mWifiManager.setWifiEnabled(false);
            result = mConnectivityManager.enableTetherPCInternet(ip);
        } else {
            result = mConnectivityManager.disableTetherPCInternet();
        }

        selectedItem = isShared ? INTERNET_SHARE : DEFAUT_FUNCTION;

        Settings.Global.putInt(getContentResolver(),
                 Settings.Global.USB_CURRENT_FUNCTION, selectedItem);
         Settings.Global.putString(getContentResolver(),
                 "InternetShareIP", ip==null ? "null":ip);
         mInternetShare.setChecked(isShared);
    }

    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        resultCode = intent.getIntExtra("callback", -1);
        setIntent(intent);
    }
}
