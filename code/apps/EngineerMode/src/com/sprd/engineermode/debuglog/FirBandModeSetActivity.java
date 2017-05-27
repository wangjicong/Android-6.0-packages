package com.sprd.engineermode.debuglog;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import com.sprd.engineermode.telephony.TelephonyManagerSprd;
import android.telephony.TelephonyManager.RadioCapbility;
import android.telephony.TelephonyManager.RadioFeatures;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.os.SystemProperties;

import com.sprd.engineermode.EMSwitchPreference;
import com.sprd.engineermode.R;
import com.sprd.engineermode.engconstents;
import com.sprd.engineermode.utils.IATUtils;

public class FirBandModeSetActivity extends PreferenceActivity implements
        Preference.OnPreferenceClickListener {

    private static final String TAG = "FirBandModeSetActivity";
//    private static final int[] TDD_LTE_SUPPORT_BANDS = { 38, 39, 40, 41 };
//    private static final int[] FDD_LTE_SUPPORT_BANDS = { 3, 7 };
    private static final int LTE_BAND_SUPPORT_MAX = 1;
    private static final String KEY_SIM_INDEX = "simindex";
    private static final String LOCKED_BAND = "locked_band";
    private static final String MODEM_TYPE = "modemtype";
    private static final String BAND_MODE_W = "WCDMA Band";
    private static final String ISDUALMODE = "dual_mode";
    private static final String NET_MODE = "net_mode";
    private static final String G_LOCKED_BAND = "g_locked_band";
    private static final String LTE_LOCKED_BAND = "lte_locked_band";
    private static final String ISSUPPORTLTE = "lte_support";

    private static final String KEY_TD_A_F = "td_a_frequency";
    private static final String KEY_TD_F_F = "td_f_frequency";
    private static final String KEY_G_GSM = "g_gsm_frequency";
    private static final String KEY_G_DCS = "g_dcs_frequency";
    private static final String KEY_G_PCS = "g_pcs_frequency";
    private static final String KEY_GSM = "gsm_frequency";
    private static final String KEY_W1 = "w1";
    private static final String KEY_W2 = "w2";
    private static final String KEY_W5 = "w5";
    private static final String KEY_W8 = "w8";
    private static final String KEY_TDD_LTE = "TDD_BAND";
    private static final String KEY_FDD_LTE = "FDD_BAND";

    private static final int LOCK_TD_BAND = 1;
    private static final int LOCK_WCDMA_BAND = 2;
    private static final int LOCK_GSM_BAND = 3;
    private static final int LOCK_LTE_BAND = 4;
//    private static final int INIT_LTE_BAND = 5;
    private static final int GET_CHECKED_LTE_BAND = 5;
    private static final int GET_ORI_LTE_BAND = 6;

    PreferenceGroup mPreGroup = null;
    private String mLTELockBandRep;
    private String mLTELockBand;
    private String mLockBandRep;
    private String mLockBand;
    private int mModemType;
    private String mNetMode;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceCategory mLTEPreferenceCategory;
    private RadioCapbility mCurrentRadioCapbility;
    private RadioFeatures mCurrentRadioFeatures;
    private CheckBoxPreference[] mGBandCapbility;
    private CheckBoxPreference[] mWBandCapbility;
    private CheckBoxPreference mTDAPref;
    private CheckBoxPreference mTDFPref;
//    boolean[] isTddChecked = new boolean[TDD_LTE_SUPPORT_BANDS.length];
//    boolean[] isFddChecked = new boolean[TDD_LTE_SUPPORT_BANDS.length];

    private FBHandler mFBHandler;
    private ArrayList<String> mSelectBand = new ArrayList<String>();
    private ArrayList<String> mLastSelectBand = new ArrayList<String>();
    private boolean mIsDualMode = false;
    private String mGLockBand = null;
    private String mGLockBandRep = null;
    private String mWLockBand = "";
    private String mWUnLockBand = "";
    private String mServerName = "atchannel0";
    private int mSim = 0;
    // private String mDialogMessage;
    private ProgressDialog mProgressDialog;

    private int mLTESupportBand_TDD = -1;
    private int mLTESupportBand_FDD = -1;
    private int mLTECheckBand_TDD = 0;
    private int mLTECheckBand_FDD = 0;
    private final static String ORI_LTE_BAND_TDD = "ORI_LTE_BAND_TDD";
    private final static String ORI_LTE_BAND_FDD = "ORI_LTE_BAND_FDD";
    
    private boolean isSupportLTE = SystemProperties.get(
            "persist.radio.ssda.mode").equals("svlte")
            || SystemProperties.get("persist.radio.ssda.mode").equals(
                    "tdd-csfb")
            || SystemProperties.get("persist.radio.ssda.mode").equals(
                    "fdd-csfb")
            || SystemProperties.get("persist.radio.ssda.mode").equals("csfb");
    private Handler mUiThread = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
            case GET_ORI_LTE_BAND: {
                String response = (String) msg.obj;
                if (response != null && response.contains(IATUtils.AT_OK)) {
                    String[] result = response.split("\n");
                    String[] result2 = result[0].split("\\:");
                    String[] result3 = result2[1].split(",");
                    mLTESupportBand_TDD = ((Integer.valueOf(result3[0].trim()) << 32) & 0xFF00) + Integer.valueOf(result3[1].trim());
                    mLTESupportBand_FDD = ((Integer.valueOf(result3[2].trim()) << 32) & 0xFF00) + Integer.valueOf(result3[3].trim());
                    Log.d(TAG, " GET_ORI_LTE_BAND mLTESupportBand_TDD=" + mLTESupportBand_TDD + ",mLTESupportBand_FDD="
                            + mLTESupportBand_FDD);
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(FirBandModeSetActivity.this);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putInt(ORI_LTE_BAND_TDD, mLTESupportBand_TDD);
                    editor.putInt(ORI_LTE_BAND_FDD, mLTESupportBand_FDD);
                    editor.apply();
                    initLTEPreference();
                }
            }
                break;
            case GET_CHECKED_LTE_BAND: {
                String response = (String) msg.obj;
                if (response != null && response.contains(IATUtils.AT_OK)) {
                    String[] result = response.split("\n");
                    String[] result2 = result[0].split("\\:");
                    String[] result3 = result2[1].split(",");
                    mLTECheckBand_TDD = ((Integer.valueOf(result3[0].trim()) << 32) & 0xFF00) + Integer.valueOf(result3[1].trim());
                    mLTECheckBand_FDD = ((Integer.valueOf(result3[2].trim()) << 32) & 0xFF00) + Integer.valueOf(result3[3].trim());
                    Log.d(TAG, "GET_CHECKED_LTE_BAND mLTESupportBand_TDD=" + mLTESupportBand_TDD + ",mLTESupportBand_FDD="
                            + mLTESupportBand_FDD);
                    Log.d(TAG, "GET_CHECKED_LTE_BAND mLTECheckBand_TDD=" + mLTECheckBand_TDD + ",mLTECheckBand_FDD="
                            + mLTECheckBand_FDD);
                    CheckBoxPreference pre = null;

                    for ( int i = 0; i < 32; i++ ) {
                        if (((mLTESupportBand_FDD >> i) & 0x01) == 1 ) {
                            pre = (CheckBoxPreference) findPreference(KEY_FDD_LTE + (i+1));
                            if (((mLTECheckBand_FDD >> i) & 0x01) == 1 ) {
                                pre.setChecked(true);
                            } else {
                                pre.setChecked(false);
                            }
                        }
                    }
                    for ( int i = 0; i < 32; i++ ) {
                        if (((mLTESupportBand_TDD >> i) & 0x01) == 1 ) {
                            pre = (CheckBoxPreference) findPreference(KEY_TDD_LTE + (i+33));
                            if (((mLTECheckBand_TDD >> i) & 0x01) == 1 ) {
                                pre.setChecked(true);
                            } else {
                                pre.setChecked(false);
                            }
                        }
                    }
                  }

//                    int tmp = 32;
//                    for (int i = 0; i < TDD_LTE_SUPPORT_BANDS.length; i++) {
//                        if ((tmp & tddValues) != 0) {
//                            isTddChecked[i] = true;
//                        } else {
//                            isTddChecked[i] = false;
//                        }
//                        tmp = (tmp << 1);
//                    }
//                    if ((fddValues & 4) != 0) {
//                        isFddChecked[0] = true;
//                    } else {
//                        isFddChecked[0] = false;
//                    }
//                    if ((fddValues & 64) != 0) {
//                        isFddChecked[1] = true;
//                    } else {
//                        isFddChecked[1] = false;
//                    }
//                }
//                CheckBoxPreference pre = null;
//
//                for (int i = 0; i < TDD_LTE_SUPPORT_BANDS.length; i++) {
//                    pre = (CheckBoxPreference) findPreference(KEY_TDD_LTE
//                            + TDD_LTE_SUPPORT_BANDS[i]);
//                    pre.setChecked(isTddChecked[i]);
//                }
//                for (int i = 0; i < FDD_LTE_SUPPORT_BANDS.length; i++) {
//                    pre = (CheckBoxPreference) findPreference(KEY_FDD_LTE
//                            + FDD_LTE_SUPPORT_BANDS[i]);
//                    pre.setChecked(isFddChecked[i]);
//                }
            }
                break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this));
        mPreGroup = getPreferenceScreen();
        HandlerThread ht = new HandlerThread(TAG);
        ht.start();
        mFBHandler = new FBHandler(ht.getLooper());
        Bundle extras = this.getIntent().getExtras();
        if(extras == null){
            return;
        }
        mSim = extras.getInt(KEY_SIM_INDEX);
        mIsDualMode = extras.getBoolean(ISDUALMODE);
        mModemType = extras.getInt(MODEM_TYPE);
        mGLockBandRep = extras.getString(G_LOCKED_BAND, null);
        mLockBandRep = extras.getString(LOCKED_BAND, null);
        mNetMode = extras.getString(NET_MODE);
        mServerName = "atchannel" + mSim;
        if (mIsDualMode) {
            PreferenceCategory prefCategory = new PreferenceCategory(this);
            prefCategory.setTitle("GSM Mode");
            mPreGroup.addPreference(prefCategory);
            mGLockBand = analyseLockBand(mGLockBandRep, "13");
            initGPreference();
            mPreferenceCategory = new PreferenceCategory(this);
            if (mModemType == TelephonyManagerSprd.MODEM_TYPE_TDSCDMA) {
                mPreferenceCategory.setTitle("TD Mode");
            } else if (mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA) {
                mPreferenceCategory.setTitle("WCDMA Mode");
            }
            mPreGroup.addPreference(mPreferenceCategory);
            if (mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA) {
                mLockBand = mLockBandRep;
            } else {
                mLockBand = analyseLockBand(mLockBandRep, "15");
            }
            initPreference();
        } else {
            if ("13".equals(mNetMode)) {
                PreferenceCategory prefCategory = new PreferenceCategory(this);
                prefCategory.setTitle("GSM Mode");
                mPreGroup.addPreference(prefCategory);
                mGLockBand = analyseLockBand(mGLockBandRep, "13");
                initGPreference();
            }
            if ("14".equals(mNetMode)) {
                mPreferenceCategory = new PreferenceCategory(this);
                mPreferenceCategory.setTitle("WCDMA Mode");
                mPreGroup.addPreference(mPreferenceCategory);
                mLockBand = mLockBandRep;
                initPreference();
            }
            if ("15".equals(mNetMode)) {
                mPreferenceCategory = new PreferenceCategory(this);
                mPreferenceCategory.setTitle("TD Mode");
                mPreGroup.addPreference(mPreferenceCategory);
                mLockBand = analyseLockBand(mLockBandRep, "15");
                initPreference();
            }
        }

        if (isSupportLTE) {
            mLTEPreferenceCategory = new PreferenceCategory(this);
            mLTEPreferenceCategory.setTitle("LTE Mode");
            mPreGroup.addPreference(mLTEPreferenceCategory);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            mLTESupportBand_TDD = sp.getInt(ORI_LTE_BAND_TDD, -1);
            mLTESupportBand_FDD = sp.getInt(ORI_LTE_BAND_FDD, -1);
            if ( mLTESupportBand_TDD == -1 && mLTESupportBand_FDD == -1) {
                Message msg = mFBHandler.obtainMessage(GET_ORI_LTE_BAND);
                mFBHandler.sendMessage(msg);
            } else {
                initLTEPreference();
                Message msg = mFBHandler.obtainMessage(GET_CHECKED_LTE_BAND);
                mFBHandler.sendMessage(msg);

            }
        }
    }

    /* @} */
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if (mFBHandler != null) {
            Log.d(TAG, "HandlerThread has quit");
            mFBHandler.getLooper().quit();
        }
        mSelectBand.clear();
        mLastSelectBand.clear();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.frequency_set, menu);
        MenuItem item = menu.findItem(R.id.frequency_set);
        if (item != null) {
            item.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.frequency_set: {
            if (mModemType == TelephonyManagerSprd.MODEM_TYPE_TDSCDMA) {
                Message lockTD = mFBHandler.obtainMessage(LOCK_TD_BAND);
                mFBHandler.sendMessage(lockTD);
            } else if (mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA) {
                mWLockBand = "";
                mWUnLockBand = "";
                Message lockWCDMA = mFBHandler.obtainMessage(LOCK_WCDMA_BAND);
                mFBHandler.sendMessage(lockWCDMA);
            } else if (mModemType == TelephonyManagerSprd.MODEM_TYPE_GSM) {
                Message lockGSM = mFBHandler.obtainMessage(LOCK_GSM_BAND);
                mFBHandler.sendMessage(lockGSM);
            }

            if (isSupportLTE) {
                Message msg = mFBHandler.obtainMessage(LOCK_LTE_BAND);
                mFBHandler.sendMessage(msg);
            }
        }
            break;
        default:
            Log.i(TAG, "default");
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private String analyseLTELockBand(String lteLockBand) {
        return null;
    }

    private String analyseLockBand(String lockBand, String netMode) {
        String result = null;
        if (lockBand != null) {
            if (lockBand.contains(IATUtils.AT_OK)) {
                String str[] = lockBand.split("\\:");
                result = null;
                if (netMode.equals("15")) {
                    if (str[1].contains("+")) {
                        result = "3";
                    } else if (str[1].contains("F")) {
                        result = "2";
                    } else if (str[1].contains("A")) {
                        result = "1";
                    }
                } else if (netMode.equals("13")) {
                    String[] line = str[1].split("\n");
                    Log.d(TAG, "13" + line[0]);
                    if (line[0].contains(",")) {
                        String[] gsmBand = line[0].split(",");
                        result = gsmBand[1].trim();
                    }
                } else {
                    String str1[] = str[1].split("\n");
                    result = str1[0].trim();
                }
            } else {
                result = null;
            }
            Log.d(TAG, "ModemType is " + mModemType + " LockBand is " + result);
            return result;
        } else {
            return null;
        }
    }

    private void checkWCDMABandSelect() {
        if (mLockBand != null) {
            if (mLockBand.contains("1")) {
                mWBandCapbility[0].setChecked(true);
                mLastSelectBand.add(KEY_W1);
                mSelectBand.add(KEY_W1);
            } else {
                mWBandCapbility[0].setChecked(false);
            }
            if (mLockBand.contains("2")) {
                mWBandCapbility[1].setChecked(true);
                mLastSelectBand.add(KEY_W2);
                mSelectBand.add(KEY_W2);
            } else {
                mWBandCapbility[1].setChecked(false);
            }
            if (mLockBand.contains("5")) {
                mWBandCapbility[4].setChecked(true);
                mLastSelectBand.add(KEY_W5);
                mSelectBand.add(KEY_W5);
            } else {
                mWBandCapbility[4].setChecked(false);
            }
            if (mLockBand.contains("8")) {
                mWBandCapbility[7].setChecked(true);
                mLastSelectBand.add(KEY_W8);
                mSelectBand.add(KEY_W8);
            } else {
                mWBandCapbility[7].setChecked(false);
            }
        } else {
            mWBandCapbility[0].setChecked(false);
            mWBandCapbility[1].setChecked(false);
            mWBandCapbility[4].setChecked(false);
            mWBandCapbility[7].setChecked(false);
        }
    }

    private void checkGSMBandSelect() {
        if (mGLockBand != null) {
            int lockType = Integer.valueOf(mGLockBand).intValue();
            switch (lockType) {
            case 0: {
                mGBandCapbility[0].setChecked(false);
                mGBandCapbility[1].setChecked(false);
                mGBandCapbility[2].setChecked(false);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                break;
            }
            case 1: {
                mGBandCapbility[0].setChecked(false);
                mGBandCapbility[1].setChecked(false);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(false);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                break;
            }
            case 2: {
                mGBandCapbility[0].setChecked(false);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(false);
                mGBandCapbility[3].setChecked(false);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                break;
            }
            case 3: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(false);
                mGBandCapbility[2].setChecked(false);
                mGBandCapbility[3].setChecked(false);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                break;
            }
            case 4: {
                mGBandCapbility[0].setChecked(false);
                mGBandCapbility[1].setChecked(false);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                break;
            }
            case 5: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(false);
                mGBandCapbility[2].setChecked(false);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                break;
            }
            case 6: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(false);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(false);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                break;
            }
            case 7: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(false);
                mGBandCapbility[3].setChecked(false);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                break;
            }
            case 8: {
                mGBandCapbility[0].setChecked(false);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(false);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                break;
            }
            case 9: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(false);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                break;
            }
            case 10: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(false);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                break;
            }
            case 11: {
                mGBandCapbility[0].setChecked(false);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                break;
            }
            case 12: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(false);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                break;
            }
            case 13: {
                mGBandCapbility[0].setChecked(false);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                break;
            }
            case 14: {
                mGBandCapbility[0].setChecked(true);
                mGBandCapbility[1].setChecked(true);
                mGBandCapbility[2].setChecked(true);
                mGBandCapbility[3].setChecked(true);
                mLastSelectBand.add(KEY_GSM);
                mSelectBand.add(KEY_GSM);
                mLastSelectBand.add(KEY_G_DCS);
                mSelectBand.add(KEY_G_DCS);
                mLastSelectBand.add(KEY_G_PCS);
                mSelectBand.add(KEY_G_PCS);
                mLastSelectBand.add(KEY_G_GSM);
                mSelectBand.add(KEY_G_GSM);
                break;
            }
            default: {
                break;
            }
            }
        } else {
            return;
        }
    }

    private void initGPreference() {
        mGBandCapbility = new CheckBoxPreference[4];
        for (int i = 0; i < 4; i++) {
            mGBandCapbility[i] = new CheckBoxPreference(this);
            if (i == 0) {
                mGBandCapbility[i].setTitle("GSM850");
                mGBandCapbility[i].setKey(KEY_GSM);
            }
            if (i == 1) {
                mGBandCapbility[i].setTitle("PCS1900");
                mGBandCapbility[i].setKey(KEY_G_PCS);
            }
            if (i == 2) {
                mGBandCapbility[i].setTitle("DCS1800");
                mGBandCapbility[i].setKey(KEY_G_DCS);
            }
            if (i == 3) {
                mGBandCapbility[i].setTitle("GSM900");
                mGBandCapbility[i].setKey(KEY_G_GSM);
            }
            mGBandCapbility[i].setOnPreferenceClickListener(this);
            mPreGroup.addPreference(mGBandCapbility[i]);
        }
        checkGSMBandSelect();
    }

    private void initLTEPreference() {
        CheckBoxPreference addOne = null;
        for ( int i = 0; i < 32; i++ ) {
            if (((mLTESupportBand_FDD >> i) & 0x01) == 1) {
                addOne = new CheckBoxPreference(this);
                addOne.setTitle(KEY_FDD_LTE + (i+1));
                addOne.setKey(KEY_FDD_LTE + (i+1));
                addOne.setChecked(false);
                addOne.setOnPreferenceClickListener(this);
                mPreGroup.addPreference(addOne);
            }
        }
        for ( int i = 0; i < 32; i++ ) {
            if (((mLTESupportBand_TDD >> i) & 0x01) == 1) {
                addOne = new CheckBoxPreference(this);
                addOne.setTitle(KEY_TDD_LTE + (i+33));
                addOne.setKey(KEY_TDD_LTE + (i+33));
                addOne.setChecked(false);
                addOne.setOnPreferenceClickListener(this);
                mPreGroup.addPreference(addOne);
            }
        }
//        for (int bandIndex = 0; bandIndex < FDD_LTE_SUPPORT_BANDS.length; bandIndex++) {
//            addOne = new CheckBoxPreference(this);
//            addOne.setTitle(KEY_FDD_LTE + FDD_LTE_SUPPORT_BANDS[bandIndex]);
//            addOne.setKey(KEY_FDD_LTE + FDD_LTE_SUPPORT_BANDS[bandIndex]);
//            addOne.setChecked(false);
//            addOne.setOnPreferenceClickListener(this);
//            mPreGroup.addPreference(addOne);
//        }
//        for (int bandIndex = 0; bandIndex < TDD_LTE_SUPPORT_BANDS.length; bandIndex++) {
//            addOne = new CheckBoxPreference(this);
//            addOne.setTitle(KEY_TDD_LTE + TDD_LTE_SUPPORT_BANDS[bandIndex]);
//            addOne.setKey(KEY_TDD_LTE + TDD_LTE_SUPPORT_BANDS[bandIndex]);
//            addOne.setChecked(false);
//            addOne.setOnPreferenceClickListener(this);
//            mPreGroup.addPreference(addOne);
//        }
    }

    private void initPreference() {
        if (mModemType == TelephonyManagerSprd.MODEM_TYPE_TDSCDMA) {
            mTDAPref = new CheckBoxPreference(this);
            mTDAPref.setTitle(R.string.td_a_frequency);
            mTDAPref.setKey(KEY_TD_A_F);
            mTDAPref.setChecked(false);
            /* SPRD: fix bug344931 Perfect BandSelect @{ */
            mTDAPref.setOnPreferenceClickListener(this);
            mPreGroup.addPreference(mTDAPref);
            mTDFPref = new CheckBoxPreference(this);
            mTDFPref.setTitle(R.string.td_f_frequency);
            mTDFPref.setKey(KEY_TD_F_F);
            mTDFPref.setChecked(false);
            mTDFPref.setOnPreferenceClickListener(this);
            mPreGroup.addPreference(mTDFPref);
            if (mLockBand.contains("1")) {
                mTDAPref.setChecked(true);
                mTDFPref.setChecked(false);
                mLastSelectBand.add(KEY_TD_A_F);
                mSelectBand.add(KEY_TD_A_F);
            } else if (mLockBand.contains("2")) {
                mTDFPref.setChecked(true);
                mTDAPref.setChecked(false);
                mLastSelectBand.add(KEY_TD_F_F);
                mSelectBand.add(KEY_TD_F_F);
            } else if (mLockBand.contains("3")) {
                mTDFPref.setChecked(true);
                mTDAPref.setChecked(true);
                mLastSelectBand.add(KEY_TD_A_F);
                mSelectBand.add(KEY_TD_A_F);
                mLastSelectBand.add(KEY_TD_F_F);
                mSelectBand.add(KEY_TD_F_F);
            } else {
                mTDFPref.setChecked(false);
                mTDAPref.setChecked(false);
            }
            /* @} */
        } else if (mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA) {
            mWBandCapbility = new CheckBoxPreference[8];
            for (int i = 8; i > 0; i--) {
                mWBandCapbility[i - 1] = new CheckBoxPreference(this);
                mWBandCapbility[i - 1].setTitle(BAND_MODE_W + i);
                mWBandCapbility[i - 1].setEnabled(false);
                if (i == 1 || i == 2 || i == 5 || i == 8) {
                    mWBandCapbility[i - 1].setEnabled(true);
                }
                if (i == 1) {
                    mWBandCapbility[i - 1].setKey(KEY_W1);
                    mWBandCapbility[i - 1].setOnPreferenceClickListener(this);
                } else if (i == 2) {
                    mWBandCapbility[i - 1].setKey(KEY_W2);
                    mWBandCapbility[i - 1].setOnPreferenceClickListener(this);
                } else if (i == 5) {
                    mWBandCapbility[i - 1].setKey(KEY_W5);
                    mWBandCapbility[i - 1].setOnPreferenceClickListener(this);
                } else if (i == 8) {
                    mWBandCapbility[i - 1].setKey(KEY_W8);
                    mWBandCapbility[i - 1].setOnPreferenceClickListener(this);
                }
                mPreGroup.addPreference(mWBandCapbility[i - 1]);
            }
            checkWCDMABandSelect();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        String key = pref.getKey();
        CheckBoxPreference preference = (CheckBoxPreference) pref;
        if (preference.isChecked()) {
            if (key.equals(KEY_TD_A_F)) {
                mSelectBand.add(KEY_TD_A_F);
            } else if (key.equals(KEY_TD_F_F)) {
                mSelectBand.add(KEY_TD_F_F);
            } else if (key.equals(KEY_W1)) {
                mSelectBand.add(KEY_W1);
            } else if (key.equals(KEY_W2)) {
                mSelectBand.add(KEY_W2);
            } else if (key.equals(KEY_W5)) {
                mSelectBand.add(KEY_W5);
            } else if (key.equals(KEY_W8)) {
                mSelectBand.add(KEY_W8);
            } else if (key.equals(KEY_GSM)) {
                mSelectBand.add(KEY_GSM);
            } else if (key.equals(KEY_G_PCS)) {
                mSelectBand.add(KEY_G_PCS);
            } else if (key.equals(KEY_G_DCS)) {
                mSelectBand.add(KEY_G_DCS);
//            } else if (key.equals(KEY_TDD_LTE + 38)) {
//                isTddChecked[0] = true;
//            } else if (key.equals(KEY_TDD_LTE + 39)) {
//                isTddChecked[1] = true;
//            } else if (key.equals(KEY_TDD_LTE + 40)) {
//                isTddChecked[2] = true;
//            } else if (key.equals(KEY_TDD_LTE + 41)) {
//                isTddChecked[3] = true;
//            } else if (key.equals(KEY_FDD_LTE + 3)) {
//                isFddChecked[0] = true;
//            } else if (key.equals(KEY_FDD_LTE + 7)) {
//                isFddChecked[1] = true;
            } else if ( key.startsWith(KEY_TDD_LTE) ) {
                int n = Integer.valueOf(key.substring(KEY_TDD_LTE.length()));
                mLTECheckBand_TDD  = mLTECheckBand_TDD | (0x01 << (n-1));
            } else if ( key.startsWith(KEY_FDD_LTE) ) {
                int n = Integer.valueOf(key.substring(KEY_FDD_LTE.length()));
                mLTECheckBand_FDD  = mLTECheckBand_FDD | (0x01 << (n-1));
            }
        } else {
            if (key.equals(KEY_TD_A_F)) {
                mSelectBand.remove(KEY_TD_A_F);
            } else if (key.equals(KEY_TD_F_F)) {
                mSelectBand.remove(KEY_TD_F_F);
            } else if (key.equals(KEY_W1)) {
                mSelectBand.remove(KEY_W1);
            } else if (key.equals(KEY_W2)) {
                mSelectBand.remove(KEY_W2);
            } else if (key.equals(KEY_W5)) {
                mSelectBand.remove(KEY_W5);
            } else if (key.equals(KEY_W8)) {
                mSelectBand.remove(KEY_W8);
            } else if (key.equals(KEY_GSM)) {
                mSelectBand.remove(KEY_GSM);
            } else if (key.equals(KEY_G_PCS)) {
                mSelectBand.remove(KEY_G_PCS);
            } else if (key.equals(KEY_G_DCS)) {
                mSelectBand.remove(KEY_G_DCS);
            } else if (key.equals(KEY_G_GSM)) {
                mSelectBand.remove(KEY_G_GSM);
            } else if (key.equals(KEY_G_GSM)) {
                mSelectBand.add(KEY_G_GSM);
//            } else if (key.equals(KEY_TDD_LTE + 38)) {
//                isTddChecked[0] = false;
//            } else if (key.equals(KEY_TDD_LTE + 39)) {
//                isTddChecked[1] = false;
//            } else if (key.equals(KEY_TDD_LTE + 40)) {
//                isTddChecked[2] = false;
//            } else if (key.equals(KEY_TDD_LTE + 41)) {
//                isTddChecked[3] = false;
//            } else if (key.equals(KEY_FDD_LTE + 3)) {
//                isFddChecked[0] = false;
//            } else if (key.equals(KEY_FDD_LTE + 7)) {
//                isFddChecked[1] = false;
            } else if ( key.startsWith(KEY_TDD_LTE) ) {
                int n = Integer.valueOf(key.substring(KEY_TDD_LTE.length()));
                mLTECheckBand_TDD  = mLTECheckBand_TDD & (~(0x01 << (n-1)));
            } else if ( key.startsWith(KEY_FDD_LTE) ) {
                int n = Integer.valueOf(key.substring(KEY_FDD_LTE.length()));
                mLTECheckBand_FDD  = mLTECheckBand_FDD & (~(0x01 << (n-1)));
            }
        }
        return true;
    }

    private String checkGSMBandLock() {
        String result = null;

        /*
         * if(mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM)
         * && !mSelectBand.contains(KEY_G_PCS) &&
         * !mLastSelectBand.contains(KEY_G_PCS) &&
         * !mSelectBand.contains(KEY_G_DCS) &&
         * !mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM)){ return null; }else
         * if(!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS)
         * && mLastSelectBand.contains(KEY_G_PCS) &&
         * !mSelectBand.contains(KEY_G_DCS) &&
         * !mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM)){ return null; }else
         * if(!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) &&
         * !mSelectBand.contains(KEY_G_PCS) &&
         * !mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM)){ return null; }else
         * if(!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) &&
         * !mSelectBand.contains(KEY_G_PCS) &&
         * !mLastSelectBand.contains(KEY_G_PCS) &&
         * !mSelectBand.contains(KEY_G_DCS) &&
         * !mLastSelectBand.contains(KEY_G_DCS) &&
         * mSelectBand.contains(KEY_G_GSM) &&
         * mLastSelectBand.contains(KEY_G_GSM)){ return null; }else
         * if(mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM)
         * && mSelectBand.contains(KEY_G_PCS) &&
         * mLastSelectBand.contains(KEY_G_PCS) &&
         * !mSelectBand.contains(KEY_G_DCS) &&
         * !mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM)){ return null; }else
         * if(mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM)
         * && !mSelectBand.contains(KEY_G_PCS) &&
         * !mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM)){ return null; }else
         * if((mSelectBand.contains(KEY_GSM) &&
         * mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS)
         * && !mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * mSelectBand.contains(KEY_G_GSM) &&
         * mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS)
         * && mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS)
         * && mLastSelectBand.contains(KEY_G_PCS) &&
         * !mSelectBand.contains(KEY_G_DCS) &&
         * !mLastSelectBand.contains(KEY_G_DCS) &&
         * mSelectBand.contains(KEY_G_GSM) &&
         * mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) &&
         * !mSelectBand.contains(KEY_G_PCS) &&
         * !mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * mSelectBand.contains(KEY_G_GSM) &&
         * mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((mSelectBand.contains(KEY_GSM) &&
         * mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS)
         * && mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((mSelectBand.contains(KEY_GSM) &&
         * mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS)
         * && mLastSelectBand.contains(KEY_G_PCS) &&
         * !mSelectBand.contains(KEY_G_DCS) &&
         * !mLastSelectBand.contains(KEY_G_DCS) &&
         * mSelectBand.contains(KEY_G_GSM) &&
         * mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS)
         * && mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * mSelectBand.contains(KEY_G_GSM) &&
         * mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((mSelectBand.contains(KEY_GSM) &&
         * mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS)
         * && mLastSelectBand.contains(KEY_G_PCS) &&
         * mSelectBand.contains(KEY_G_DCS) &&
         * mLastSelectBand.contains(KEY_G_DCS) &&
         * mSelectBand.contains(KEY_G_GSM) &&
         * mLastSelectBand.contains(KEY_G_GSM))){ return null; }else
         * if((!mSelectBand.contains(KEY_GSM) &&
         * !mLastSelectBand.contains(KEY_GSM) &&
         * !mSelectBand.contains(KEY_G_PCS) &&
         * !mLastSelectBand.contains(KEY_G_PCS) &&
         * !mSelectBand.contains(KEY_G_DCS) &&
         * !mLastSelectBand.contains(KEY_G_DCS) &&
         * !mSelectBand.contains(KEY_G_GSM) &&
         * !mLastSelectBand.contains(KEY_G_GSM))){ return null; }
         */

        if (!mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "0";
        } else if (!mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            result = "1";
        } else if (!mSelectBand.contains(KEY_GSM)
                && mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            result = "2";
        } else if (mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            result = "3";
        } else if (!mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "4";
        } else if (mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "5";
        } else if (mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            result = "6";
        } else if (mSelectBand.contains(KEY_GSM)
                && mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            result = "7";
        } else if (mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "8";
        } else if (mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "9";
        } else if (mSelectBand.contains(KEY_GSM)
                && mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "10";
        } else if (!mSelectBand.contains(KEY_GSM)
                && mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            result = "11";
        } else if (mSelectBand.contains(KEY_GSM)
                && mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            result = "12";
        } else if (!mSelectBand.contains(KEY_GSM)
                && mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "13";
        } else if (mSelectBand.contains(KEY_GSM)
                && mSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS)
                && mSelectBand.contains(KEY_G_GSM)) {
            result = "14";
        } else if (!mSelectBand.contains(KEY_GSM)
                && !mSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS)
                && !mSelectBand.contains(KEY_G_GSM)) {
            // result = null;
        }
        return result;
    }

    private void checkWCDMABandLockStatus() {
        if (mSelectBand.contains(KEY_W1) && (!mLastSelectBand.contains(KEY_W1))) {
            mWLockBand = "1";
        }
        if (mSelectBand.contains(KEY_W2) && (!mLastSelectBand.contains(KEY_W2))) {
            mWLockBand = mWLockBand + ",2";
        }
        if (mSelectBand.contains(KEY_W5) && (!mLastSelectBand.contains(KEY_W5))) {
            mWLockBand = mWLockBand + ",5";
        }
        if (mSelectBand.contains(KEY_W8) && (!mLastSelectBand.contains(KEY_W8))) {
            mWLockBand = mWLockBand + ",8";
        }
        Log.d(TAG, "mWLockBand is " + mWLockBand);
        if (mLastSelectBand.contains(KEY_W1) && (!mSelectBand.contains(KEY_W1))) {
            mWUnLockBand = "1";
        }
        if (mLastSelectBand.contains(KEY_W2) && (!mSelectBand.contains(KEY_W2))) {
            mWUnLockBand = mWUnLockBand + ",2";
        }
        if (mLastSelectBand.contains(KEY_W5) && (!mSelectBand.contains(KEY_W5))) {
            mWUnLockBand = mWUnLockBand + ",5";
        }
        if (mLastSelectBand.contains(KEY_W8) && (!mSelectBand.contains(KEY_W8))) {
            mWUnLockBand = mWUnLockBand + ",8";
        }
        Log.d(TAG, "mWUnLockBand is " + mWUnLockBand);
    }

    private void resumeLastGLock() {

        if (mLastSelectBand.contains(KEY_G_GSM)) {
            mGBandCapbility[3].setChecked(true);
        } else {
            mGBandCapbility[3].setChecked(false);
        }
        if (mLastSelectBand.contains(KEY_G_DCS)) {
            mGBandCapbility[2].setChecked(true);
        } else {
            mGBandCapbility[2].setChecked(false);
        }
        if (mLastSelectBand.contains(KEY_G_PCS)) {
            mGBandCapbility[1].setChecked(true);
        } else {
            mGBandCapbility[1].setChecked(false);
        }
        if (mLastSelectBand.contains(KEY_GSM)) {
            mGBandCapbility[0].setChecked(true);
        } else {
            mGBandCapbility[0].setChecked(false);
        }
        mSelectBand = (ArrayList<String>) mLastSelectBand.clone();
    }

    private boolean lockWCDMABand(String band) {
        String resp = null;
        if (band != null) {
            if (band.contains("1")) {
                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND
                        + "1,1", mServerName);
                if (resp.contains(IATUtils.AT_OK)) {
                    if (band.contains("2")) {
                        resp = IATUtils.sendATCmd(
                                engconstents.ENG_AT_W_LOCK_BAND + "2,1",
                                mServerName);
                        if (resp.contains(IATUtils.AT_OK)) {
                            if (band.contains("5")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "5,1", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    if (band.contains("8")) {
                                        resp = IATUtils.sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,1", mServerName);
                                        if (resp.contains(IATUtils.AT_OK)) {
                                            return true;
                                        } else {
                                            return false;
                                        }
                                    } else {
                                        return true;
                                    }
                                } else {
                                    return false;
                                }
                            } else {
                                if (band.contains("8")) {
                                    resp = IATUtils.sendATCmd(
                                            engconstents.ENG_AT_W_LOCK_BAND
                                                    + "8,1", mServerName);
                                    if (resp.contains(IATUtils.AT_OK)) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                            }
                        } else {
                            return false;
                        }
                    } else {
                        if (band.contains("5")) {
                            resp = IATUtils.sendATCmd(
                                    engconstents.ENG_AT_W_LOCK_BAND + "5,1",
                                    mServerName);
                            if (resp.contains(IATUtils.AT_OK)) {
                                if (band.contains("8")) {
                                    resp = IATUtils.sendATCmd(
                                            engconstents.ENG_AT_W_LOCK_BAND
                                                    + "8,1", mServerName);
                                    if (resp.contains(IATUtils.AT_OK)) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            if (band.contains("8")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,1", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                } else {
                    return false;
                }
            } else {
                if (band.contains("2")) {
                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND
                            + "2,1", mServerName);
                    if (resp.contains(IATUtils.AT_OK)) {
                        if (band.contains("5")) {
                            resp = IATUtils.sendATCmd(
                                    engconstents.ENG_AT_W_LOCK_BAND + "5,1",
                                    mServerName);
                            if (resp.contains(IATUtils.AT_OK)) {
                                if (band.contains("8")) {
                                    resp = IATUtils.sendATCmd(
                                            engconstents.ENG_AT_W_LOCK_BAND
                                                    + "8,1", mServerName);
                                    if (resp.contains(IATUtils.AT_OK)) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            if (band.contains("8")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,1", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (band.contains("5")) {
                        resp = IATUtils.sendATCmd(
                                engconstents.ENG_AT_W_LOCK_BAND + "5,1",
                                mServerName);
                        if (resp.contains(IATUtils.AT_OK)) {
                            if (band.contains("8")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,1", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        if (band.contains("8")) {
                            resp = IATUtils.sendATCmd(
                                    engconstents.ENG_AT_W_LOCK_BAND + "8,1",
                                    mServerName);
                            if (resp.contains(IATUtils.AT_OK)) {
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean unLockWBand(String band) {
        String resp = null;
        if (band != null) {
            if (band.contains("1")) {
                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND
                        + "1,0", mServerName);
                if (resp.contains(IATUtils.AT_OK)) {
                    if (band.contains("2")) {
                        resp = IATUtils.sendATCmd(
                                engconstents.ENG_AT_W_LOCK_BAND + "2,0",
                                mServerName);
                        if (resp.contains(IATUtils.AT_OK)) {
                            if (band.contains("5")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "5,0", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    if (band.contains("8")) {
                                        resp = IATUtils.sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,0", mServerName);
                                        if (resp.contains(IATUtils.AT_OK)) {
                                            return true;
                                        } else {
                                            return false;
                                        }
                                    } else {
                                        return true;
                                    }
                                } else {
                                    return false;
                                }
                            } else {
                                if (band.contains("8")) {
                                    resp = IATUtils.sendATCmd(
                                            engconstents.ENG_AT_W_LOCK_BAND
                                                    + "8,0", mServerName);
                                    if (resp.contains(IATUtils.AT_OK)) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                            }
                        } else {
                            return false;
                        }
                    } else {
                        if (band.contains("5")) {
                            resp = IATUtils.sendATCmd(
                                    engconstents.ENG_AT_W_LOCK_BAND + "5,0",
                                    mServerName);
                            if (resp.contains(IATUtils.AT_OK)) {
                                if (band.contains("8")) {
                                    resp = IATUtils.sendATCmd(
                                            engconstents.ENG_AT_W_LOCK_BAND
                                                    + "8,0", mServerName);
                                    if (resp.contains(IATUtils.AT_OK)) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            if (band.contains("8")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,0", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        }
                    }
                } else {
                    return false;
                }
            } else {
                if (band.contains("2")) {
                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND
                            + "2,0", mServerName);
                    if (resp.contains(IATUtils.AT_OK)) {
                        if (band.contains("5")) {
                            resp = IATUtils.sendATCmd(
                                    engconstents.ENG_AT_W_LOCK_BAND + "5,0",
                                    mServerName);
                            if (resp.contains(IATUtils.AT_OK)) {
                                if (band.contains("8")) {
                                    resp = IATUtils.sendATCmd(
                                            engconstents.ENG_AT_W_LOCK_BAND
                                                    + "8,0", mServerName);
                                    if (resp.contains(IATUtils.AT_OK)) {
                                        return true;
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return true;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            if (band.contains("8")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,0", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (band.contains("5")) {
                        resp = IATUtils.sendATCmd(
                                engconstents.ENG_AT_W_LOCK_BAND + "5,0",
                                mServerName);
                        if (resp.contains(IATUtils.AT_OK)) {
                            if (band.contains("8")) {
                                resp = IATUtils
                                        .sendATCmd(
                                                engconstents.ENG_AT_W_LOCK_BAND
                                                        + "8,0", mServerName);
                                if (resp.contains(IATUtils.AT_OK)) {
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                return true;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        if (band.contains("8")) {
                            resp = IATUtils.sendATCmd(
                                    engconstents.ENG_AT_W_LOCK_BAND + "8,0",
                                    mServerName);
                            if (resp.contains(IATUtils.AT_OK)) {
                                return true;
                            } else {
                                return false;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            }
        } else {
            return false;
        }
    }

    private void wProcSuccess(boolean result) {
        if (result) {
            dismissProgressDialog("Band Select Success");
            mLastSelectBand = (ArrayList<String>) mSelectBand.clone();
        } else {
            dismissProgressDialog("Band Select Fail");
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if (mLastSelectBand.contains(KEY_W1)) {
                        mWBandCapbility[0].setChecked(true);
                    } else {
                        mWBandCapbility[0].setChecked(false);
                    }
                    if (mLastSelectBand.contains(KEY_W2)) {
                        mWBandCapbility[1].setChecked(true);
                    } else {
                        mWBandCapbility[1].setChecked(false);
                    }
                    if (mLastSelectBand.contains(KEY_W5)) {
                        mWBandCapbility[4].setChecked(true);
                    } else {
                        mWBandCapbility[4].setChecked(false);
                    }
                    if (mLastSelectBand.contains(KEY_W8)) {
                        mWBandCapbility[7].setChecked(true);
                    } else {
                        mWBandCapbility[7].setChecked(false);
                    }
                    if (mIsDualMode) {
                        resumeLastGLock();
                    }
                }
            });
        }
    }

    private void checkAndSetTDBandLock() {
        String values;
        String response = null;
        /*
         * if(mSelectBand.contains(KEY_TD_A_F) &&
         * mLastSelectBand.contains(KEY_TD_A_F) &&
         * mSelectBand.contains(KEY_TD_F_F) &&
         * mLastSelectBand.contains(KEY_TD_F_F)){ dismissProgressDialog();
         * showResultDialog("No Band Change"); return; }else
         * if(!mSelectBand.contains(KEY_TD_A_F) &&
         * !mLastSelectBand.contains(KEY_TD_A_F) &&
         * mSelectBand.contains(KEY_TD_F_F) &&
         * mLastSelectBand.contains(KEY_TD_F_F)){ dismissProgressDialog();
         * showResultDialog("No Band Change"); return; }else
         * if(mSelectBand.contains(KEY_TD_A_F) &&
         * mLastSelectBand.contains(KEY_TD_A_F) &&
         * !mSelectBand.contains(KEY_TD_F_F) &&
         * !mLastSelectBand.contains(KEY_TD_F_F)){ dismissProgressDialog();
         * showResultDialog("No Band Change"); return; }else
         * if(!mSelectBand.contains(KEY_TD_A_F) &&
         * !mLastSelectBand.contains(KEY_TD_A_F) &&
         * !mSelectBand.contains(KEY_TD_F_F) &&
         * !mLastSelectBand.contains(KEY_TD_F_F)){ dismissProgressDialog();
         * showResultDialog("No Band Change"); return; }else
         */
        if (mSelectBand.contains(KEY_TD_A_F)
                && (!mSelectBand.contains(KEY_TD_F_F))) {
            values = "\"A\"";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND
                    + values, mServerName);
            Log.d(TAG, "<0>LOCK_TD_BAND AT is "
                    + engconstents.ENG_AT_TD_SET_BAND + values + " Result is "
                    + response);
        } else if (!mSelectBand.contains(KEY_TD_A_F)
                && mSelectBand.contains(KEY_TD_F_F)) {
            values = "\"F\"";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND
                    + values, mServerName);
            Log.d(TAG, "<0>LOCK_TD_BAND AT is "
                    + engconstents.ENG_AT_TD_SET_BAND + values + " Result is "
                    + response);
        } else if (mSelectBand.contains(KEY_TD_A_F)
                && mSelectBand.contains(KEY_TD_F_F)) {
            values = "\"A+F\"";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND
                    + values, mServerName);
            Log.d(TAG, "<0>LOCK_TD_BAND AT is "
                    + engconstents.ENG_AT_TD_SET_BAND + values + " Result is "
                    + response);
        } else if (!mSelectBand.contains(KEY_TD_A_F)
                && !mSelectBand.contains(KEY_TD_F_F)) {
            values = "";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND
                    + values, mServerName);
            Log.d(TAG, "<0>LOCK_TD_BAND AT is "
                    + engconstents.ENG_AT_TD_SET_BAND + values + " Result is "
                    + response);
        }
        if (response != null && response.contains(IATUtils.AT_OK)) {
            dismissProgressDialog("Band Select Success");
            mLastSelectBand = (ArrayList<String>) mSelectBand.clone();
        } else if (response != null && !response.contains(IATUtils.AT_OK)) {
            dismissProgressDialog("Band Select Fail");
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if (mLastSelectBand.contains(KEY_TD_A_F)) {
                        mTDAPref.setChecked(true);
                    } else {
                        mTDAPref.setChecked(false);
                    }
                    if (mLastSelectBand.contains(KEY_TD_F_F)) {
                        mTDFPref.setChecked(true);
                    } else {
                        mTDFPref.setChecked(false);
                    }
                    mSelectBand = (ArrayList<String>) mLastSelectBand.clone();
                }
            });
        } else if (response == null) {
            if (!mIsDualMode) {
                dismissProgressDialog(null);
            } else {
                dismissProgressDialog("Band Select Success");
                mLastSelectBand = (ArrayList<String>) mSelectBand.clone();
            }
            return;
        }
    }

    private void setWCDMABandLock() {
        if (!mWLockBand.equals("") && lockWCDMABand(mWLockBand)
                && !mWUnLockBand.equals("") && unLockWBand(mWUnLockBand)) {
            wProcSuccess(true);
        } else if (mWLockBand.equals("") && !mWUnLockBand.equals("")
                && unLockWBand(mWUnLockBand)) {
            wProcSuccess(true);
        } else if (!mWLockBand.equals("") && lockWCDMABand(mWLockBand)
                && mWUnLockBand.equals("")) {
            wProcSuccess(true);
        } else if (mWLockBand.equals("") && mWUnLockBand.equals("")) {
            if (!mIsDualMode) {
                dismissProgressDialog(null);
            } else {
                dismissProgressDialog("Band Select Success");
                mLastSelectBand = (ArrayList<String>) mSelectBand.clone();
            }
            // showResultDialog("No Band Change");
            return;
        } else {
            wProcSuccess(false);
        }
    }

    class FBHandler extends Handler {

        public FBHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String values = null;
            String response;
            String atCmd;
            String dialogMessage;
            switch (msg.what) {
            case LOCK_TD_BAND: {
                if (!mIsDualMode) {
                    if (mNetMode.equals("15")) {
                        showProgressDialog();
                        checkAndSetTDBandLock();
                    } else if (mNetMode.equals("13")) {
                        showProgressDialog();
                        values = checkGSMBandLock();
                        /*
                         * if(checkGSMBandLock() == null){
                         * dismissProgressDialog();
                         * showResultDialog("No Band Change"); }else{
                         */
                        if (values != null) {
                            atCmd = engconstents.ENG_AT_SELECT_GSMBAND + values;
                            response = IATUtils.sendATCmd(atCmd, mServerName);

                            if (response.contains(IATUtils.AT_OK)) {
                                dismissProgressDialog("Band Select Success");
                                mLastSelectBand = (ArrayList<String>) mSelectBand
                                        .clone();
                            } else {
                                dismissProgressDialog("Band Select Fail");
                                mUiThread.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        resumeLastGLock();
                                    }
                                });
                            }
                        } else {
                            dismissProgressDialog("Band Select Fail");
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    resumeLastGLock();
                                }
                            });
                        }
                    }
                    break;
                } else if (mIsDualMode) {
                    showProgressDialog();
                    values = checkGSMBandLock();
                    /*
                     * if(values == null){ checkAndSetTDBandLock(); }else{
                     */
                    if (values != null) {
                        atCmd = engconstents.ENG_AT_SELECT_GSMBAND + values;
                        response = IATUtils.sendATCmd(atCmd, mServerName);
                        Log.d(TAG, "<0>LOCK_GSM_BAND AT is " + atCmd
                                + " Result is " + response + " mIsDualMode is "
                                + mIsDualMode);
                    } else {
                        response = "error";
                    }
                    if (response.contains(IATUtils.AT_OK)) {
                        checkAndSetTDBandLock();
                    } else {
                        dismissProgressDialog("Band Select Fail");
                        mUiThread.post(new Runnable() {
                            /* SPRD: fix bug344931 Perfect BandSelect @{ */
                            @Override
                            public void run() {
                                if (mLastSelectBand.contains(KEY_TD_A_F)
                                        && mLastSelectBand.contains(KEY_TD_F_F)) {
                                    mTDAPref.setChecked(true);
                                    mTDFPref.setChecked(true);
                                } else if (mLastSelectBand.contains(KEY_TD_A_F)
                                        && (!mLastSelectBand
                                                .contains(KEY_TD_F_F))) {
                                    mTDAPref.setChecked(true);
                                    mTDFPref.setChecked(false);
                                } else if ((!mLastSelectBand
                                        .contains(KEY_TD_A_F))
                                        && (mLastSelectBand
                                                .contains(KEY_TD_F_F))) {
                                    mTDAPref.setChecked(false);
                                    mTDFPref.setChecked(true);
                                }
                                resumeLastGLock();
                            }
                        });
                        /* @} */
                    }
                    break;
                } else {
                    break;
                }
            }
            case LOCK_WCDMA_BAND: {
                Log.d(TAG, "mIsDualMode is " + mIsDualMode + ", mNetMode is "
                        + mNetMode);
                if (!mIsDualMode) {
                    if (mNetMode.equals("14")) {
                        showProgressDialog();
                        checkWCDMABandLockStatus();
                        setWCDMABandLock();
                    } else if (mNetMode.equals("13")) {
                        showProgressDialog();
                        values = checkGSMBandLock();
                        /*
                         * if(values == null){ dismissProgressDialog();
                         * showResultDialog("No Band Change"); }else{
                         */
                        if (values != null) {
                            atCmd = engconstents.ENG_AT_SELECT_GSMBAND + values;
                            response = IATUtils.sendATCmd(atCmd, mServerName);
                            if (response.contains(IATUtils.AT_OK)) {
                                dismissProgressDialog("Band Select Success");
                                mLastSelectBand = (ArrayList<String>) mSelectBand
                                        .clone();
                            } else {
                                dismissProgressDialog("Band Select Fail");
                                mUiThread.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        resumeLastGLock();
                                    }
                                });
                            }
                        }
                    }
                    break;
                } else {
                    showProgressDialog();
                    values = checkGSMBandLock();
                    /*
                     * if(values == null){ checkWCDMABandLockStatus();
                     * setWCDMABandLock(); }else{
                     */
                    if (values != null) {
                        atCmd = engconstents.ENG_AT_SELECT_GSMBAND + values;
                        response = IATUtils.sendATCmd(atCmd, mServerName);
                        Log.d(TAG, "<0>LOCK_GSM_BAND AT is " + atCmd
                                + " Result is " + response + " mIsDualMode is "
                                + mIsDualMode);
                    } else {
                        response = IATUtils.AT_OK;
                    }
                    if (response.contains(IATUtils.AT_OK)) {
                        checkWCDMABandLockStatus();
                        setWCDMABandLock();
                    } else {
                        dismissProgressDialog("Band Select Fail");
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mLastSelectBand.contains(KEY_W1)) {
                                    mWBandCapbility[0].setChecked(true);
                                } else {
                                    mWBandCapbility[0].setChecked(false);
                                }
                                if (mLastSelectBand.contains(KEY_W2)) {
                                    mWBandCapbility[1].setChecked(true);
                                } else {
                                    mWBandCapbility[1].setChecked(false);
                                }
                                if (mLastSelectBand.contains(KEY_W5)) {
                                    mWBandCapbility[4].setChecked(true);
                                } else {
                                    mWBandCapbility[4].setChecked(false);
                                }
                                if (mLastSelectBand.contains(KEY_W8)) {
                                    mWBandCapbility[7].setChecked(true);
                                } else {
                                    mWBandCapbility[7].setChecked(false);
                                }
                                resumeLastGLock();
                            }
                        });
                    }
                    // }
                    break;
                }
            }
            case LOCK_GSM_BAND: {
                showProgressDialog();
                values = checkGSMBandLock();
                if (values != null) {
                    atCmd = engconstents.ENG_AT_SELECT_GSMBAND + values;
                    response = IATUtils.sendATCmd(atCmd, mServerName);
                    Log.d(TAG, "<0>LOCK_GSM_BAND AT is " + atCmd
                            + " Result is " + response);
                    if (response.contains(IATUtils.AT_OK)) {
                        dismissProgressDialog("Band Select Success");
                        mLastSelectBand = (ArrayList<String>) mSelectBand
                                .clone();
                    } else {
                        dismissProgressDialog("Band Select Fail");
                        mUiThread.post(new Runnable() {
                            @Override
                            public void run() {
                                resumeLastGLock();
                            }
                        });
                    }
                }/*
                  * else{ dismissProgressDialog(); dialogMessage =
                  * "No Band Change"; showResultDialog(dialogMessage); }
                  */
                break;
            }
            case GET_ORI_LTE_BAND:
            case GET_CHECKED_LTE_BAND: {
                // boolean[] isTddChecked = new
                // boolean[TDD_LTE_SUPPORT_BANDS.length];
                // boolean[] isFddChecked = new
                // boolean[TDD_LTE_SUPPORT_BANDS.length];
                showProgressDialog();
                CheckBoxPreference pre = null;
                atCmd = engconstents.ENG_AT_GET_LTE_BAND;
                response = IATUtils.sendATCmd(atCmd, mServerName);
                Log.d(TAG, "GET_CHECKED_LTE_BAND=" + response);
                Message msgLTE = mUiThread.obtainMessage(msg.what,
                        response);
                mUiThread.sendMessage(msgLTE);
                dismissProgressDialog(null);
                break;
            }
            case LOCK_LTE_BAND: {
                showProgressDialog();
                int tddatCmd_h = (mLTECheckBand_TDD>>16) & 0xFF;
                int tddatCmd_l = mLTECheckBand_TDD & 0xFF;
                int fddatCmd_h = (mLTECheckBand_FDD>>16) & 0xFF;
                int fddatCmd_l = mLTECheckBand_FDD & 0xFF;
//                if (tddatCmd == 0) {
//                    dismissProgressDialog("TDD Band select at least one");
//                } else {
                    atCmd = engconstents.ENG_AT_SET_LTE_BAND + tddatCmd_h + "," + tddatCmd_l + "," + fddatCmd_h + "," + fddatCmd_l;
                    response = IATUtils.sendATCmd(atCmd, "atchannel0");
                    if (response!=null && response.contains(IATUtils.AT_OK)) {
                        dismissProgressDialog("LTE Band Select Success");
                    } else {
                        dismissProgressDialog("LTE Band Select Fail");
                        return;
                    }
//                }
                atCmd = "AT+RESET=0";
                response = IATUtils.sendATCmd(atCmd, "atchannel0");
                if (response!=null && response.contains(IATUtils.AT_OK)) {
//                    dismissProgressDialog("LTE Band Select Success");
                } else {
                    dismissProgressDialog("LTE Band Select Fail");
                    return;
                }
                Message msgLTE = mFBHandler.obtainMessage(GET_CHECKED_LTE_BAND);
                mFBHandler.sendMessage(msgLTE);
                break;
            }
            default:
                break;
            }
        }
    }

//    private int getTddBandLockCheckboxStatus() {
//        int tmp = 32;
//        int sum = 0;
//        for (int i = 0; i < TDD_LTE_SUPPORT_BANDS.length; i++) {
//            if (isTddChecked[i]) {
//                sum = sum + tmp;
//            }
//            tmp = tmp << 1;
//        }
//        return sum;
//    }

//    private int getFddBandLockCheckboxStatus() {
//        int tmp = 32;
//        int sum = 0;
//        if (isFddChecked[0]) {
//            sum = sum + 4;
//        }
//        if (isFddChecked[1]) {
//            sum = sum + 64;
//        }
//        return sum;
//    }

    private void showResultDialog(String message) {
        if (message != null) {
            final String dialogMessage = message;
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = new AlertDialog.Builder(
                            FirBandModeSetActivity.this)
                            .setTitle("Band Select")
                            .setMessage(dialogMessage)
                            .setPositiveButton(R.string.alertdialog_cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                        }
                                    }).create();
                    alertDialog.show();
                }
            });
        }
    }

    private String addStateForBandCapbility(String str, int strLength) {
        int strLen = str.length();
        if (strLen < strLength) {
            while (strLen < strLength) {
                StringBuffer sb = new StringBuffer();
                sb.append("0").append(str);
                str = sb.toString();
                strLen = str.length();
            }
        }
        return str;
    }

    private void showProgressDialog() {
        mUiThread.post(new Runnable() {
            @Override
            public void run() {
                mProgressDialog = ProgressDialog.show(
                        FirBandModeSetActivity.this, "Set...",
                        "Please wait...", true, false);
            }
        });
    }

    private void dismissProgressDialog(String message) {
        Log.d(TAG, "input dismissProgressDialog");
        if (message != null) {
            final String dialogMessage = message;
            Log.d(TAG, "dialogMessage:" + dialogMessage);
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        AlertDialog alertDialog = new AlertDialog.Builder(
                                FirBandModeSetActivity.this)
                                .setTitle("Band Select")
                                .setMessage(dialogMessage)
                                .setPositiveButton(R.string.alertdialog_cancel,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int which) {
                                            }
                                        }).create();
                        alertDialog.show();
                    }
                }
            });
        } else {
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                }
            });
        }

    }
}