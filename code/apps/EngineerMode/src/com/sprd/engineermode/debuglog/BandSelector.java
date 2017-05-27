package com.sprd.engineermode.debuglog;

import com.sprd.engineermode.utils.IATUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.RadioCapbility;
import android.telephony.TelephonyManager.RadioFeatures;
import android.util.Log;

/**
 * collect radio capbility and bands in every mode
  */
public class BandSelector {
    private static final String TAG = "BandSelector";

    public static final int RADIO_MODE_MASK_NONE     = 0x00;
//    public static final int RADIO_MODE_MASK_SVLET    = 0x02;
    private static final int RADIO_MODE_MASK_TD_LTE   = 0x04;
    private static final int RADIO_MODE_MASK_LTE_FDD  = 0x08;
    private static final int RADIO_MODE_MASK_GSM      = 0x10;
    private static final int RADIO_MODE_MASK_TD_SCDMA = 0x20;
    private static final int RADIO_MODE_MASK_WCDMA    = 0x40;

    public static final int RADIO_MODE_NONE     = 0; // 0x00
//    public static final int RADIO_MODE_SVLET    = 1; // 0x02
    public static final int RADIO_MODE_TD_LTE   = 2; // 0x04
    public static final int RADIO_MODE_LTE_FDD  = 3; // 0x08
    public static final int RADIO_MODE_GSM      = 4; // 0x10
    public static final int RADIO_MODE_TD_SCDMA = 5; // 0x20
    public static final int RADIO_MODE_WCDMA    = 6; // 0x40

    private int[] mSupportRadioModes;

    private int mPhoneCount;
    private int mPhoneID;
    private int mPrimeryCard;
    private RadioCapbility mRadioCapbility;
    private RadioFeatures mRadioFeature;
    private Context mContext;
    private BandData[] mBandDatas;
    private String mChannel;
    private Handler mUiThread = new Handler();
    private boolean PrimeCardFlag; //true  primary card, false  not primary card

    public BandSelector(int phoneID, Context context, Handler threadUi) {
        final TelephonyManager tm = TelephonyManager.from(context);
        mContext = context;
        mUiThread = threadUi;
        mPhoneID = phoneID;
        mChannel = "atchannel"+mPhoneID;
        mPhoneCount = tm.getPhoneCount();
        mPrimeryCard = tm.getPrimaryCard();
        mRadioFeature = tm.getRadioFeatures(mPhoneID);
//        mPrimeryCard = Settings.Secure.getInt(mContext.getContentResolver(),
//                Settings.Secure.SERVICE_PRIMARY_CARD,-1); // 4.4

        // NONE, TDD_SVLTE, FDD_CSFB, TDD_CSFB,
        if ( mPhoneID == mPrimeryCard ) {
        	PrimeCardFlag = true;
            mRadioCapbility = TelephonyManager.getRadioCapbility();
        } else {
        	PrimeCardFlag = false;
        }

        getSupportRadioModes();
        Log.d(TAG, "Init OK:"+this.toString());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mPhoneID:").append(mPhoneID);
        sb.append(" mPhoneCount:").append(mPhoneCount);
        sb.append(" mPrimeryCard:").append(mPrimeryCard);
        sb.append(" mRadioCapbility:").append(mRadioCapbility);
        sb.append(" mRadioFeature:").append(mRadioFeature);
        sb.append(" support modes:");
        if ( mSupportRadioModes == null ) {
            sb.append("null");
        } else {
            for (int n: mSupportRadioModes) {
                sb.append(getModeName(n)+" ");
            }
        }
        return sb.toString();
    }

    public String getSetInfo() {
        String info = "";
        if ( mSupportRadioModes == null ) {
            info = "error.";
            return info;
        }

        info = "Reboot phone to enable that operation.\n\n";
        for ( int i = 0; i < mSupportRadioModes.length; i++ ) {
            info += getModeName(mSupportRadioModes[i]);
            if ( mBandDatas[i] != null && mBandDatas[i].isChanged() ) {
                if ( mBandDatas[i].successful() ) {
                    info += ":Success\n";
                } else {
                    info += ":Fail\n";
                }
            } else {
                info += ":Unchange\n";
            }
        }
        return info;
    }

    private String getModeName(int mode) {
        switch( mode ) {
            case RADIO_MODE_GSM:
                return "GSM MODE";
            case RADIO_MODE_TD_SCDMA:
                return "TD_SCDMA MODE";
            case RADIO_MODE_WCDMA:
                return "WCDMA MODE";
            case RADIO_MODE_TD_LTE:
                return "LTE_TDD MODE";
            case RADIO_MODE_LTE_FDD:
                return "LTE_FDD MODE";
            default:
                return "NONE";
        }
    }

    private int getCapbility() {
    	if (PrimeCardFlag == false){
    		return RADIO_MODE_MASK_GSM;  //be set to gsm only default if not primary card 
    	}else{
        if ( mRadioCapbility == RadioCapbility.NONE) { // NONE default value is set to gsm + WCDMA
            return RADIO_MODE_MASK_GSM
            		+ RADIO_MODE_MASK_WCDMA;
        } else if ( mRadioCapbility == RadioCapbility.TDD_CSFB ) {
            return RADIO_MODE_MASK_GSM
                 + RADIO_MODE_MASK_TD_SCDMA
                 + RADIO_MODE_MASK_TD_LTE;
        } else if ( mRadioCapbility == RadioCapbility.FDD_CSFB ) {
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_WCDMA
                    + RADIO_MODE_MASK_TD_LTE
                    + RADIO_MODE_MASK_LTE_FDD;
        } else if ( mRadioCapbility == RadioCapbility.CSFB ) { // 4.4 not support
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_WCDMA
                    + RADIO_MODE_MASK_TD_SCDMA
                    + RADIO_MODE_MASK_TD_LTE
                    + RADIO_MODE_MASK_LTE_FDD;
        }
        return RADIO_MODE_MASK_NONE;
    	}
    }

    private int getRadioFeature() {
        if (mRadioFeature == RadioFeatures.SVLET) {
        } else if (mRadioFeature == RadioFeatures.TD_LTE) {
            return RADIO_MODE_MASK_TD_LTE;
        } else if (mRadioFeature == RadioFeatures.LTE_FDD) {
            return RADIO_MODE_MASK_LTE_FDD;
        } else if (mRadioFeature == RadioFeatures.TD_LTE_AND_LTE_FDD) {
            return RADIO_MODE_MASK_TD_LTE
                    + RADIO_MODE_MASK_LTE_FDD;
        } else if (mRadioFeature == RadioFeatures.LTE_FDD_AND_W_AND_GSM_CSFB) {
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_WCDMA
                    + RADIO_MODE_MASK_LTE_FDD;
        } else if (mRadioFeature == RadioFeatures.TD_LTE_AND_W_AND_GSM_CSFB) {
             return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_WCDMA
                    + RADIO_MODE_MASK_TD_LTE;
        } else if (mRadioFeature == RadioFeatures.TD_LTE_AND_LTE_FDD_AND_W_AND_GSM_CSFB) {
            return RADIO_MODE_MASK_GSM
                     + RADIO_MODE_MASK_WCDMA
                     + RADIO_MODE_MASK_TD_LTE
                     + RADIO_MODE_MASK_LTE_FDD;
        } else if (mRadioFeature == RadioFeatures.TD_LTE_AND_TD_AND_GSM_CSFB) {
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_TD_SCDMA
                    + RADIO_MODE_MASK_TD_LTE;
        } else if (mRadioFeature == RadioFeatures.TD_LTE_AND_LTE_FDD_AND_TD_AND_GSM_CSFB) {
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_TD_SCDMA
                    + RADIO_MODE_MASK_TD_LTE
                    + RADIO_MODE_MASK_LTE_FDD;
        } else if (mRadioFeature == RadioFeatures.TD_LTE_AND_LTE_FDD_AND_W_AND_TD_AND_GSM_CSFB) {
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_WCDMA
                    + RADIO_MODE_MASK_TD_SCDMA
                    + RADIO_MODE_MASK_TD_LTE
                    + RADIO_MODE_MASK_LTE_FDD;
        } else if (mRadioFeature == RadioFeatures.GSM_ONLY) {
            return RADIO_MODE_MASK_GSM;
        } else if (mRadioFeature == RadioFeatures.WCDMA_ONLY) {
            return RADIO_MODE_MASK_WCDMA;
        } else if (mRadioFeature == RadioFeatures.TD_ONLY) {
            return  RADIO_MODE_MASK_TD_SCDMA;
        } else if (mRadioFeature == RadioFeatures.TD_AND_GSM) {
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_TD_SCDMA;
        } else if (mRadioFeature == RadioFeatures.WCDMA_AND_GSM) {
            return RADIO_MODE_MASK_GSM
                    + RADIO_MODE_MASK_WCDMA;
        } else if (mRadioFeature == RadioFeatures.NONE) {
            return RADIO_MODE_MASK_GSM;
        }
        return RADIO_MODE_MASK_NONE;
    }


    private int[] getSupportRadioModes() {
        /*
         * NONE, TDD_SVLTE, FDD_CSFB, TDD_CSFB, CSFB
         * 1、"persist.radio.ssda.mode" （TelephonyManager：static RadioCapbility getRadioCapbility()）
               “tdd-csfb” means 3mod （RadioCapbility.TDD_CSFB）
               “fdd-csfb”means 4mod（RadioCapbility.FDD_CSFB）
                “csfb”means 5mod （RadioCapbility.CSFB）
            3mod：GSM、TD-SCDMA、TDD-LTE
            4mod：GSM、 WCDMA、TDD-LTE、FDD-LTE
            5mod：GSM、TD- SCDMA、WCDMA、TDD-LTE、FDD-LTE
        */
        int modes1 = getCapbility();
        int modes2 = getRadioFeature();
        int modes = modes1 & modes2;
        int n = 0;
        for(int i = 0; i < 8; i++ ) {
            if ( ((modes >> i) & 0x01) == 1 ) {
                n++;
            }
        }
        Log.d(TAG, "getSupportRadioModes modes1:"+modes1+" modes2:"+modes2+" modes:"+modes+" n:"+n);
        mSupportRadioModes = new int[n];
        for(int i = 0, j = 0; i < 8; i++ ) {
            switch (modes & (1 << i)) {
                case RADIO_MODE_MASK_TD_LTE:
                    mSupportRadioModes[j] = RADIO_MODE_TD_LTE;
                    break;
                case RADIO_MODE_MASK_LTE_FDD:
                    mSupportRadioModes[j] = RADIO_MODE_LTE_FDD;
                    break;
                case RADIO_MODE_MASK_GSM:
                    mSupportRadioModes[j] = RADIO_MODE_GSM;
                    break;
                case RADIO_MODE_MASK_TD_SCDMA:
                    mSupportRadioModes[j] = RADIO_MODE_TD_SCDMA;
                    break;
                case RADIO_MODE_MASK_WCDMA:
                    mSupportRadioModes[j] = RADIO_MODE_WCDMA;
                    break;
                default:
                    continue;
            }
            j++;
        }
        return mSupportRadioModes;
    }

    public void initModes(PreferenceGroup preferenceGroup) {
        if ( mSupportRadioModes == null ) {
            Log.d(TAG, "loadBands() mRadioModes is null");
            return;
        }
        if ( mBandDatas != null ) {
            return;
        }
        mBandDatas = new BandData[mSupportRadioModes.length];
        for ( int i = 0; i < mSupportRadioModes.length; i++ ) {
            BandData data = null;
            final PreferenceCategory pref = new PreferenceCategory(mContext);
            pref.setTitle(getModeName(mSupportRadioModes[i]));
            preferenceGroup.addPreference(pref);
            switch( mSupportRadioModes[i] ) {
                case RADIO_MODE_GSM:
                    data = new GSMBandData(mContext, mPhoneID, pref);
                    break;
                case RADIO_MODE_TD_SCDMA:
                    data = new TDBandData(mContext, mPhoneID, pref);
                    break;
                case RADIO_MODE_WCDMA:
                    data = new WCDMABandData(mContext, mPhoneID, pref);
                    break;
                case RADIO_MODE_TD_LTE:
                    data = new TDDBandData(mContext, mPhoneID, pref);
                    break;
                case RADIO_MODE_LTE_FDD:
                    data = new FDDBandData(mContext, mPhoneID, pref);
                    break;
                default:
                    break;
            }

            if ( data != null && data.init() ) {
                final BandData d = data;
//                mUiThread.post(new Runnable() {
//                      public void run() {
                          CheckBoxPreference[] cbPrefs = d.addAvailableBandsToPerference();
                          for (CheckBoxPreference cb:cbPrefs ) {
                              pref.addPreference(cb);
                          }
//                      }
//                  });
                mBandDatas[i] = data;
            }
        }
    }

    public void loadBands() {
        if (mUiThread == null) return;
        mUiThread.post(new Runnable() {
            public void run() {
                for ( int i = 0; i < mBandDatas.length; i++ ) {
                    if ( mBandDatas[i] != null ) {
                        mBandDatas[i].init();
                    }
                }
            }
        });
    }

    public void saveBand() {
        for ( BandData bands: mBandDatas ) {
            if ( bands != null && bands.isChanged() ) {
                bands.setSelectedBandToModem();
            }
        }
//        String response = IATUtils.sendATCmd("AT+SBAND=5", mChannel);
//        Log.d(TAG, "<"+mChannel+"> AT+RESET=0 Result:" + response );
//
//        response = IATUtils.sendATCmd("AT+SBAND=4", mChannel);
//        Log.d(TAG, "<"+mChannel+"> AT+RESET=4 Result:" + response );
    }

    public boolean isCheckOneOrMore() {
        for ( int i = 0; i < mBandDatas.length; i++ ) {
            if ( mBandDatas[i] != null && !mBandDatas[i].isCheckOneOrMore() ) {
                return false;
            }
        }
        return true;
    }
}
