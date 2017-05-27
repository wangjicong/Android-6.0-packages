package com.sprd.engineermode.debuglog;

import java.util.ArrayList;

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
import com.sprd.engineermode.telephony.TelephonyManagerSprd;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import com.sprd.engineermode.R;
import com.sprd.engineermode.engconstents;
import com.sprd.engineermode.utils.IATUtils;

public class TriBandModeSetActivity extends PreferenceActivity implements
Preference.OnPreferenceClickListener{

    private static final String TAG = "TriBandModeSetActivity";
    private static final String LOCKED_BAND = "locked_band";
    private static final String MODEM_TYPE = "modemtype";
    private static final String BAND_MODE_W = "WCDMA Band";
    private static final String ISDUALMODE = "dual_mode";
    private static final String NET_MODE = "net_mode";
    private static final String G_LOCKED_BAND = "g_locked_band";

    private static final String KEY_TD_A_F = "td_a_frequency";
    private static final String KEY_TD_F_F = "td_f_frequency";
    private static final String KEY_G_GSM = "g_gsm_frequency";
    private static final String KEY_G_DCS = "g_dcs_frequency";
    private static final String KEY_G_PCS = "g_pcs_frequency";
    private static final String KEY_GSM = "gsm_frequency";
    private static final String KEY_W1 = "w1";
    private static final String KEY_W2 ="w2";
    private static final String KEY_W5 = "w5";
    private static final String KEY_W8 = "w8";

    private static final int LOCK_TD_BAND = 1;
    private static final int LOCK_WCDMA_BAND = 2;
    private static final int LOCK_GSM_BAND = 3;

    PreferenceGroup mPreGroup = null;
    private String mLockBandRep;
    private String mLockBand;
    private int mModemType;
    private String mNetMode;
    private PreferenceCategory mPreferenceCategory;
    private CheckBoxPreference[] mGBandCapbility;
    private CheckBoxPreference[] mWBandCapbility;
    private CheckBoxPreference mTDAPref;
    private CheckBoxPreference mTDFPref;
    private Handler mUiThread = new Handler();
    private FBHandler mFBHandler;
    private ArrayList<String> mSelectBand = new ArrayList<String>();
    private ArrayList<String> mLastSelectBand = new ArrayList<String>();
    private boolean mIsDualMode = false;
    private String mGLockBand = null;
    private String mGLockBandRep = null;
    private String mWLockBand = "";
    private String mWUnLockBand = "";
    private String mDialogMessage;
    private ProgressDialog mProgressDialog;  

    protected void onCreate(Bundle savedInstanceState){
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
        mIsDualMode = extras.getBoolean(ISDUALMODE);
        mModemType = extras.getInt(MODEM_TYPE);
        mGLockBandRep = extras.getString(G_LOCKED_BAND,null);
        mLockBandRep = extras.getString(LOCKED_BAND,null);
        mNetMode = extras.getString(NET_MODE);
        if(mIsDualMode){
            PreferenceCategory prefCategory = new PreferenceCategory(this);
            prefCategory.setTitle("GSM Mode");
            mPreGroup.addPreference(prefCategory);
            mGLockBand = analyseLockBand(mGLockBandRep,"13"); 
            initGPreference();
            mPreferenceCategory = new PreferenceCategory(this);
            if(mModemType == TelephonyManagerSprd.MODEM_TYPE_TDSCDMA){
                mPreferenceCategory.setTitle("TD Mode");
            }else if(mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA){
                mPreferenceCategory.setTitle("WCDMA Mode");
            }
            mPreGroup.addPreference(mPreferenceCategory);
            if(mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA){
                mLockBand = mLockBandRep;
            }else{
                mLockBand = analyseLockBand(mLockBandRep,"15"); 
            }
            initPreference();
        }else{
            if("13".equals(mNetMode)){
                PreferenceCategory prefCategory = new PreferenceCategory(this);
                prefCategory.setTitle("GSM Mode");
                mPreGroup.addPreference(prefCategory);
                mGLockBand = analyseLockBand(mGLockBandRep,"13"); 
                initGPreference();
            }
            if("14".equals(mNetMode)){
                mPreferenceCategory = new PreferenceCategory(this);
                mPreferenceCategory.setTitle("WCDMA Mode");
                mPreGroup.addPreference(mPreferenceCategory);
                mLockBand = mLockBandRep;
                initPreference();
            }
            if("15".equals(mNetMode)){
                mPreferenceCategory = new PreferenceCategory(this);
                mPreferenceCategory.setTitle("TD Mode");
                mPreGroup.addPreference(mPreferenceCategory);
                mLockBand = analyseLockBand(mLockBandRep,"15"); 
                initPreference();
            }
        }    
    }

    @Override
    protected void onStart() {     
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        if(mFBHandler != null){
            Log.d(TAG,"HandlerThread has quit");
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
        MenuItem item =menu.findItem(R.id.frequency_set);
        if (item != null) {
            item.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu); 
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.frequency_set:{
                if(mModemType == TelephonyManagerSprd.MODEM_TYPE_TDSCDMA){
                    Message lockTD = mFBHandler.obtainMessage(LOCK_TD_BAND);         
                    mFBHandler.sendMessage(lockTD); 
                }else if(mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA){
                    mWLockBand = "";
                    mWUnLockBand = "";
                    Message lockWCDMA = mFBHandler.obtainMessage(LOCK_WCDMA_BAND);         
                    mFBHandler.sendMessage(lockWCDMA);
                }else if(mModemType == TelephonyManagerSprd.MODEM_TYPE_GSM){
                    Message lockGSM = mFBHandler.obtainMessage(LOCK_GSM_BAND);         
                    mFBHandler.sendMessage(lockGSM); 
                } 
            }      
            break;
            default:
                Log.i(TAG, "default");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private String analyseLockBand(String lockBand,String netMode){
        String result = null;
        if(lockBand != null){
            if(lockBand.contains(IATUtils.AT_OK)){
                String str[] = lockBand.split("\\:");
                result = null; 
                if(netMode.equals("15")){
                    if(str[1].contains("+")){
                        result = "3";
                    }else if(str[1].contains("F")){
                        result = "2";
                    }else if(str[1].contains("A")){
                        result = "1";
                    }
                }else{
                    String str1[] = str[1].split("\n");
                    result = str1[0].trim();
                }

            }else{
                result = null;
            }
            Log.d(TAG,"ModemType is "+mModemType+" LockBand is "+result);
            return result;
        }else{
            return null;
        }
    }

    private void checkWCDMABandSelect(){
        if(mLockBand != null){
            if(mLockBand.contains("1")){
                mWBandCapbility[0].setChecked(true);
                mLastSelectBand.add(KEY_W1);
                mSelectBand.add(KEY_W1);
            }else{
                mWBandCapbility[0].setChecked(false);    
            }
            if(mLockBand.contains("2")){
                mWBandCapbility[1].setChecked(true);
                mLastSelectBand.add(KEY_W2);
                mSelectBand.add(KEY_W2);
            }else{
                mWBandCapbility[1].setChecked(false);
            }
            if(mLockBand.contains("5")){
                mWBandCapbility[4].setChecked(true);
                mLastSelectBand.add(KEY_W5);
                mSelectBand.add(KEY_W5);
            }else{
                mWBandCapbility[4].setChecked(false);
            }
            if(mLockBand.contains("8")){
                mWBandCapbility[7].setChecked(true);
                mLastSelectBand.add(KEY_W8);
                mSelectBand.add(KEY_W8);
            }else{
                mWBandCapbility[7].setChecked(false);
            }
        }else{
            mWBandCapbility[0].setChecked(false);
            mWBandCapbility[1].setChecked(false);
            mWBandCapbility[4].setChecked(false);
            mWBandCapbility[7].setChecked(false);
        } 
    }

    private void checkGSMBandSelect(){
        if(mGLockBand != null){
            int lockType = Integer.valueOf(mGLockBand).intValue();
            switch(lockType){
                case 0:{
                    mGBandCapbility[0].setChecked(false);
                    mGBandCapbility[1].setChecked(false);
                    mGBandCapbility[2].setChecked(false);
                    mGBandCapbility[3].setChecked(true);
                    mLastSelectBand.add(KEY_G_GSM);
                    mSelectBand.add(KEY_G_GSM);
                    break;
                }
                case 1:{
                    mGBandCapbility[0].setChecked(false);
                    mGBandCapbility[1].setChecked(false);
                    mGBandCapbility[2].setChecked(true);
                    mGBandCapbility[3].setChecked(false);
                    mLastSelectBand.add(KEY_G_DCS);
                    mSelectBand.add(KEY_G_DCS);
                    break;
                }
                case 2:{
                    mGBandCapbility[0].setChecked(false);
                    mGBandCapbility[1].setChecked(true);
                    mGBandCapbility[2].setChecked(false);
                    mGBandCapbility[3].setChecked(false);
                    mLastSelectBand.add(KEY_G_PCS);
                    mSelectBand.add(KEY_G_PCS);
                    break;
                }
                case 3:{
                    mGBandCapbility[0].setChecked(true);
                    mGBandCapbility[1].setChecked(false);
                    mGBandCapbility[2].setChecked(false);
                    mGBandCapbility[3].setChecked(false);
                    mLastSelectBand.add(KEY_GSM);
                    mSelectBand.add(KEY_GSM);
                    break;
                }
                case 4:{
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
                case 5:{
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
                case 6:{
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
                case 7:{
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
                case 8:{
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
                case 9:{
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
                case 10:{
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
                case 11:{
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
                case 12:{
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
                case 13:{
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
                case 14:{
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
                default:{
                    break;
                }
            } 
        }else{
            return;
        }             
    }

    private void initGPreference(){
        mGBandCapbility = new CheckBoxPreference[4];
        for(int i=0;i<4;i++){
            mGBandCapbility[i] = new CheckBoxPreference(this);
            if(i == 0){
                mGBandCapbility[i].setTitle("GSM850");
                mGBandCapbility[i].setKey(KEY_GSM);
            }
            if(i == 1){
                mGBandCapbility[i].setTitle("PCS1900");
                mGBandCapbility[i].setKey(KEY_G_PCS);
            }
            if(i == 2){
                mGBandCapbility[i].setTitle("DCS1800");
                mGBandCapbility[i].setKey(KEY_G_DCS);
            }
            if(i == 3){
                mGBandCapbility[i].setTitle("GSM900");
                mGBandCapbility[i].setKey(KEY_G_GSM);
            }
            mGBandCapbility[i].setOnPreferenceClickListener(this);
            mPreGroup.addPreference(mGBandCapbility[i]); 
        }
        checkGSMBandSelect();
    }

    private void initPreference(){
        if(mModemType == TelephonyManagerSprd.MODEM_TYPE_TDSCDMA){            
            mTDAPref = new CheckBoxPreference(this);
            mTDAPref.setTitle(R.string.td_a_frequency);
            mTDAPref.setKey(KEY_TD_A_F);
            mTDAPref.setChecked(false);
            mTDAPref.setOnPreferenceClickListener(this);
            if(mLockBand.contains("1") || mLockBand.contains("3")){
                mTDAPref.setChecked(true); 
                mLastSelectBand.add(KEY_TD_A_F);
                mSelectBand.add(KEY_TD_A_F);           
            }  
            mPreGroup.addPreference(mTDAPref);
            mTDFPref = new CheckBoxPreference(this);
            mTDFPref.setTitle(R.string.td_f_frequency);
            mTDFPref.setKey(KEY_TD_F_F);
            mTDFPref.setChecked(false);
            mTDFPref.setOnPreferenceClickListener(this);
            if(mLockBand.contains("2") || mLockBand.contains("3")){
                mTDFPref.setChecked(true); 
                mLastSelectBand.add(KEY_TD_F_F);
                mSelectBand.add(KEY_TD_F_F);      
            }
            mPreGroup.addPreference(mTDFPref);

        }else if(mModemType == TelephonyManagerSprd.MODEM_TYPE_WCDMA){
            mWBandCapbility = new CheckBoxPreference[8];
            for(int i=8;i>0;i--){
                mWBandCapbility[i-1] = new CheckBoxPreference(this);
                mWBandCapbility[i-1].setTitle(BAND_MODE_W+i);
                mWBandCapbility[i-1].setEnabled(false);
                if(i == 1 || i == 2 || i == 5 || i == 8){
                    mWBandCapbility[i-1].setEnabled(true);
                }
                if(i == 1){
                    mWBandCapbility[i-1].setKey(KEY_W1);
                    mWBandCapbility[i-1].setOnPreferenceClickListener(this);
                }else if(i == 2){
                    mWBandCapbility[i-1].setKey(KEY_W2);
                    mWBandCapbility[i-1].setOnPreferenceClickListener(this);
                }else if(i == 5){
                    mWBandCapbility[i-1].setKey(KEY_W5);
                    mWBandCapbility[i-1].setOnPreferenceClickListener(this);
                }else if(i == 8){
                    mWBandCapbility[i-1].setKey(KEY_W8);
                    mWBandCapbility[i-1].setOnPreferenceClickListener(this);
                }
                mPreGroup.addPreference(mWBandCapbility[i-1]);             
            }
            checkWCDMABandSelect();
        }
    }



    @Override
    public boolean onPreferenceClick(Preference pref){
        String key = pref.getKey();
        CheckBoxPreference preference = (CheckBoxPreference)pref;
        if(preference.isChecked()){
            if(key.equals(KEY_TD_A_F)){
                mSelectBand.add(KEY_TD_A_F);
            }else if(key.equals(KEY_TD_F_F)){
                mSelectBand.add(KEY_TD_F_F);
            }else if(key.equals(KEY_W1)){
                mSelectBand.add(KEY_W1);
            }else if(key.equals(KEY_W2)){
                mSelectBand.add(KEY_W2);
            }else if(key.equals(KEY_W5)){
                mSelectBand.add(KEY_W5);
            }else if(key.equals(KEY_W8)){
                mSelectBand.add(KEY_W8);
            }else if(key.equals(KEY_GSM)){
                mSelectBand.add(KEY_GSM);
            }else if(key.equals(KEY_G_PCS)){
                mSelectBand.add(KEY_G_PCS);
            }else if(key.equals(KEY_G_DCS)){
                mSelectBand.add(KEY_G_DCS);
            }else if(key.equals(KEY_G_GSM)){
                mSelectBand.add(KEY_G_GSM);
            }
        }else{
            if(key.equals(KEY_TD_A_F)){
                mSelectBand.remove(KEY_TD_A_F);
            }else if(key.equals(KEY_TD_F_F)){
                mSelectBand.remove(KEY_TD_F_F);
            }else if(key.equals(KEY_W1)){
                mSelectBand.remove(KEY_W1);
            }else if(key.equals(KEY_W2)){
                mSelectBand.remove(KEY_W2);
            }else if(key.equals(KEY_W5)){
                mSelectBand.remove(KEY_W5);
            }else if(key.equals(KEY_W8)){
                mSelectBand.remove(KEY_W8);
            }else if(key.equals(KEY_GSM)){
                mSelectBand.remove(KEY_GSM);
            }else if(key.equals(KEY_G_PCS)){
                mSelectBand.remove(KEY_G_PCS);
            }else if(key.equals(KEY_G_DCS)){
                mSelectBand.remove(KEY_G_DCS);
            }else if(key.equals(KEY_G_GSM)){
                mSelectBand.remove(KEY_G_GSM);
            } 
        }
        return true;
    }

    private String checkGSMBandLock(){
        String result = null;

        /*        if(mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mLastSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS) && !mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM)){
            return null;
        }else if(!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS) && !mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM)){
            return null;
        }else if(!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM)){
            return null;
        }else if(!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mLastSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS) && !mLastSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)
                && mLastSelectBand.contains(KEY_G_GSM)){
            return null;
        }else if(mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS) && !mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM)){
            return null;
        }else if(mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM)){
            return null;
        }else if((mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)
                && mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS) && !mLastSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)
                && mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)
                && mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS) && !mLastSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)
                && mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)
                && mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((mSelectBand.contains(KEY_GSM) && mLastSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mLastSelectBand.contains(KEY_G_PCS)
                && mSelectBand.contains(KEY_G_DCS) && mLastSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)
                && mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }else if((!mSelectBand.contains(KEY_GSM) && !mLastSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mLastSelectBand.contains(KEY_G_PCS)
                && !mSelectBand.contains(KEY_G_DCS) && !mLastSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)
                && !mLastSelectBand.contains(KEY_G_GSM))){
            return null;
        }*/

        if(!mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "0";
        }else if(!mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "1";
        }else if(!mSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "2";
        }else if(mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "3";
        }else if(!mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "4";
        }else if(mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "5";
        }else if(mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "6";
        }else if(mSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "7";
        }else if(mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "8";
        }else if(mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "9";
        }else if(mSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "10";
        }else if(!mSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "11";
        }else if(mSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "12";
        }else if(!mSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "13";
        }else if(mSelectBand.contains(KEY_GSM) && mSelectBand.contains(KEY_G_PCS) && mSelectBand.contains(KEY_G_DCS) && mSelectBand.contains(KEY_G_GSM)){
            result = "14";
        }else if(!mSelectBand.contains(KEY_GSM) && !mSelectBand.contains(KEY_G_PCS) && !mSelectBand.contains(KEY_G_DCS) && !mSelectBand.contains(KEY_G_GSM)){
            result = "";
        }
        return result;
    }

    private void checkWCDMABandLockStatus(){
        if(mSelectBand.contains(KEY_W1) && (!mLastSelectBand.contains(KEY_W1))){        
            mWLockBand = "1";
        }
        if(mSelectBand.contains(KEY_W2) && (!mLastSelectBand.contains(KEY_W2))){
            mWLockBand = mWLockBand+",2";   
        }
        if(mSelectBand.contains(KEY_W5) && (!mLastSelectBand.contains(KEY_W5))){
            mWLockBand = mWLockBand+",5";     
        }
        if(mSelectBand.contains(KEY_W8) && (!mLastSelectBand.contains(KEY_W8))){
            mWLockBand = mWLockBand+",8";
        }
        Log.d(TAG,"mWLockBand is "+mWLockBand);
        if(mLastSelectBand.contains(KEY_W1) && (!mSelectBand.contains(KEY_W1))){
            mWUnLockBand = "1";
        }
        if(mLastSelectBand.contains(KEY_W2) && (!mSelectBand.contains(KEY_W2))){
            mWUnLockBand = mWUnLockBand+",2";
        }
        if(mLastSelectBand.contains(KEY_W5) && (!mSelectBand.contains(KEY_W5))){
            mWUnLockBand = mWUnLockBand+",5";
        }
        if(mLastSelectBand.contains(KEY_W8) && (!mSelectBand.contains(KEY_W8))){
            mWUnLockBand = mWUnLockBand+",8";
        }
        Log.d(TAG,"mWUnLockBand is "+mWUnLockBand);
    }

    private void resumeLastGLock(){

        if(mLastSelectBand.contains(KEY_G_GSM)){
            mGBandCapbility[3].setChecked(true);
        }else{
            mGBandCapbility[3].setChecked(false);
        }
        if(mLastSelectBand.contains(KEY_G_DCS)){
            mGBandCapbility[2].setChecked(true);        
        }else{
            mGBandCapbility[2].setChecked(false); 
        }
        if(mLastSelectBand.contains(KEY_G_PCS)){
            mGBandCapbility[1].setChecked(true);
        }else{
            mGBandCapbility[1].setChecked(false);
        }
        if(mLastSelectBand.contains(KEY_GSM)){
            mGBandCapbility[0].setChecked(true);
        }else{
            mGBandCapbility[0].setChecked(false);
        }
        mSelectBand = (ArrayList<String>)mLastSelectBand.clone();
    }

    private boolean lockWCDMABand(String band){
        String resp = null;
        if(band != null){
            if(band.contains("1")){
                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"1,1","atchannel2");
                if(resp.contains(IATUtils.AT_OK)){
                    if(band.contains("2")){
                        resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"2,1","atchannel2"); 
                        if(resp.contains(IATUtils.AT_OK)){
                            if(band.contains("5")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,1","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    if(band.contains("8")){
                                        resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                                        if(resp.contains(IATUtils.AT_OK)){
                                            return true;
                                        }else{
                                            return false;
                                        }
                                    }else{
                                        return true;
                                    }
                                }else{
                                    return false;
                                }
                            }else{
                                if(band.contains("8")){
                                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                                    if(resp.contains(IATUtils.AT_OK)){
                                        return true;
                                    }else{
                                        return false;
                                    }
                                }else{
                                    return true;
                                }
                            }
                        }else{
                            return false;
                        }
                    }else{
                        if(band.contains("5")){
                            resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,1","atchannel2");
                            if(resp.contains(IATUtils.AT_OK)){
                                if(band.contains("8")){
                                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                                    if(resp.contains(IATUtils.AT_OK)){
                                        return true;
                                    }else{
                                        return false;
                                    }
                                }else{
                                    return true;
                                }
                            }else{
                                return false;
                            }
                        }else{
                            if(band.contains("8")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    return true;
                                }else{
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        } 
                    }
                }else{
                    return false;
                }
            }else{
                if(band.contains("2")){
                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"2,1","atchannel2"); 
                    if(resp.contains(IATUtils.AT_OK)){
                        if(band.contains("5")){
                            resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,1","atchannel2");
                            if(resp.contains(IATUtils.AT_OK)){
                                if(band.contains("8")){
                                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                                    if(resp.contains(IATUtils.AT_OK)){
                                        return true;
                                    }else{
                                        return false;
                                    }
                                }else{
                                    return true;
                                }
                            }else{
                                return false;
                            }
                        }else{
                            if(band.contains("8")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    return true;
                                }else{
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        }
                    }else{
                        return false;
                    }
                }else{
                    if(band.contains("5")){
                        resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,1","atchannel2");
                        if(resp.contains(IATUtils.AT_OK)){
                            if(band.contains("8")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    return true;
                                }else{
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        }else{
                            return false;
                        }
                    }else{
                        if(band.contains("8")){
                            resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,1","atchannel2");
                            if(resp.contains(IATUtils.AT_OK)){
                                return true;
                            }else{
                                return false;
                            }
                        }else{
                            return true;
                        }
                    } 
                } 
            }
        }else{
            return false;
        }
    }

    private boolean unLockWBand(String band){
        String resp = null;
        if(band != null){
            if(band.contains("1")){
                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"1,0","atchannel2");
                if(resp.contains(IATUtils.AT_OK)){
                    if(band.contains("2")){
                        resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"2,0","atchannel2"); 
                        if(resp.contains(IATUtils.AT_OK)){
                            if(band.contains("5")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,0","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    if(band.contains("8")){
                                        resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                                        if(resp.contains(IATUtils.AT_OK)){
                                            return true;
                                        }else{
                                            return false;
                                        }
                                    }else{
                                        return true;
                                    }
                                }else{
                                    return false;
                                }
                            }else{
                                if(band.contains("8")){
                                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                                    if(resp.contains(IATUtils.AT_OK)){
                                        return true;
                                    }else{
                                        return false;
                                    }
                                }else{
                                    return true;
                                }
                            }
                        }else{
                            return false;
                        }
                    }else{
                        if(band.contains("5")){
                            resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,0","atchannel2");
                            if(resp.contains(IATUtils.AT_OK)){
                                if(band.contains("8")){
                                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                                    if(resp.contains(IATUtils.AT_OK)){
                                        return true;
                                    }else{
                                        return false;
                                    }
                                }else{
                                    return true;
                                }
                            }else{
                                return false;
                            }
                        }else{
                            if(band.contains("8")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    return true;
                                }else{
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        } 
                    }
                }else{
                    return false;
                }
            }else{
                if(band.contains("2")){
                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"2,0","atchannel2"); 
                    if(resp.contains(IATUtils.AT_OK)){
                        if(band.contains("5")){
                            resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,0","atchannel2");
                            if(resp.contains(IATUtils.AT_OK)){
                                if(band.contains("8")){
                                    resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                                    if(resp.contains(IATUtils.AT_OK)){
                                        return true;
                                    }else{
                                        return false;
                                    }
                                }else{
                                    return true;
                                }
                            }else{
                                return false;
                            }
                        }else{
                            if(band.contains("8")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    return true;
                                }else{
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        }
                    }else{
                        return false;
                    }
                }else{
                    if(band.contains("5")){
                        resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"5,0","atchannel2");
                        if(resp.contains(IATUtils.AT_OK)){
                            if(band.contains("8")){
                                resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                                if(resp.contains(IATUtils.AT_OK)){
                                    return true;
                                }else{
                                    return false;
                                }
                            }else{
                                return true;
                            }
                        }else{
                            return false;
                        }
                    }else{
                        if(band.contains("8")){
                            resp = IATUtils.sendATCmd(engconstents.ENG_AT_W_LOCK_BAND+"8,0","atchannel2");
                            if(resp.contains(IATUtils.AT_OK)){
                                return true;
                            }else{
                                return false;
                            }
                        }else{
                            return true;
                        }
                    } 
                } 
            }
        }else{
            return false;
        }
    }

    private void wProcSuccess(boolean result){
        if(result){
            dismissProgressDialog("Band Select Success");
            mLastSelectBand = (ArrayList<String>)mSelectBand.clone(); 
        }else{
            dismissProgressDialog("Band Select Fail");
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if(mLastSelectBand.contains(KEY_W1)){
                        mWBandCapbility[0].setChecked(true);
                    }else{
                        mWBandCapbility[0].setChecked(false); 
                    }
                    if(mLastSelectBand.contains(KEY_W2)){
                        mWBandCapbility[1].setChecked(true);
                    }else{
                        mWBandCapbility[1].setChecked(false);
                    }    
                    if(mLastSelectBand.contains(KEY_W5)){
                        mWBandCapbility[4].setChecked(true);
                    }else{
                        mWBandCapbility[4].setChecked(false);
                    }
                    if(mLastSelectBand.contains(KEY_W8)){
                        mWBandCapbility[7].setChecked(true);
                    }else{
                        mWBandCapbility[7].setChecked(false);
                    }
                    if(mIsDualMode){
                        resumeLastGLock();  
                    }
                }
            }); 
        }        
    }

    private void checkAndSetTDBandLock(){
        String values;
        String response = null;
        /*        if(mSelectBand.contains(KEY_TD_A_F) && mLastSelectBand.contains(KEY_TD_A_F) 
                && mSelectBand.contains(KEY_TD_F_F) && mLastSelectBand.contains(KEY_TD_F_F)){
            dismissProgressDialog();
            showResultDialog("No Band Change"); 
            return;
        }else if(!mSelectBand.contains(KEY_TD_A_F) && !mLastSelectBand.contains(KEY_TD_A_F)
                && mSelectBand.contains(KEY_TD_F_F) && mLastSelectBand.contains(KEY_TD_F_F)){
            dismissProgressDialog();
            showResultDialog("No Band Change"); 
            return;
        }else if(mSelectBand.contains(KEY_TD_A_F) && mLastSelectBand.contains(KEY_TD_A_F)
                && !mSelectBand.contains(KEY_TD_F_F) && !mLastSelectBand.contains(KEY_TD_F_F)){
            dismissProgressDialog();
            showResultDialog("No Band Change"); 
            return;            
        }else if(!mSelectBand.contains(KEY_TD_A_F) && !mLastSelectBand.contains(KEY_TD_A_F)
                && !mSelectBand.contains(KEY_TD_F_F) && !mLastSelectBand.contains(KEY_TD_F_F)){
            dismissProgressDialog();
            showResultDialog("No Band Change"); 
            return;
        }else */
        if(mSelectBand.contains(KEY_TD_A_F) && (!mSelectBand.contains(KEY_TD_F_F))){
            values = "\"A\"";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND+values,"atchannel2");
            Log.d(TAG,"<0>LOCK_TD_BAND AT is "+engconstents.ENG_AT_TD_SET_BAND+values+" Result is "+response);
        }else if(!mSelectBand.contains(KEY_TD_A_F) && mSelectBand.contains(KEY_TD_F_F)){
            values = "\"F\"";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND+values,"atchannel2");
            Log.d(TAG,"<0>LOCK_TD_BAND AT is "+engconstents.ENG_AT_TD_SET_BAND+values+" Result is "+response);
        }else if(mSelectBand.contains(KEY_TD_A_F) && mSelectBand.contains(KEY_TD_F_F)){
            values = "\"A+F\"";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND+values,"atchannel2");
            Log.d(TAG,"<0>LOCK_TD_BAND AT is "+engconstents.ENG_AT_TD_SET_BAND+values+" Result is "+response);
        }else if(!mSelectBand.contains(KEY_TD_A_F) && !mSelectBand.contains(KEY_TD_F_F)){
            values = "";
            response = IATUtils.sendATCmd(engconstents.ENG_AT_TD_SET_BAND+values,"atchannel2");
            Log.d(TAG,"<0>LOCK_TD_BAND AT is "+engconstents.ENG_AT_TD_SET_BAND+values+" Result is "+response);
        }       
        if(response != null && response.contains(IATUtils.AT_OK)){
            dismissProgressDialog("Band Select Success");
            mLastSelectBand = (ArrayList<String>)mSelectBand.clone();
        } else if(response != null && !response.contains(IATUtils.AT_OK)){
            dismissProgressDialog("Band Select Fail");
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if(mLastSelectBand.contains(KEY_TD_A_F)){
                        mTDAPref.setChecked(true);
                    }else{
                        mTDAPref.setChecked(false);
                    }
                    if(mLastSelectBand.contains(KEY_TD_F_F)){
                        mTDFPref.setChecked(true);
                    }else{
                        mTDFPref.setChecked(false);
                    }
                    mSelectBand = (ArrayList<String>)mLastSelectBand.clone(); 
                }            
            });
        }else if(response == null){
            if(!mIsDualMode){
                dismissProgressDialog(null);
            }else{
                dismissProgressDialog("Band Select Success");
                mLastSelectBand = (ArrayList<String>)mSelectBand.clone();
            }           
            return;
        }
    }

    private void setWCDMABandLock(){
        if(!mWLockBand.equals("") && lockWCDMABand(mWLockBand) && !mWUnLockBand.equals("") && unLockWBand(mWUnLockBand)){
            wProcSuccess(true);
        }else if(mWLockBand.equals("") && !mWUnLockBand.equals("") && unLockWBand(mWUnLockBand)){
            wProcSuccess(true);
        }else if(!mWLockBand.equals("") && lockWCDMABand(mWLockBand) && mWUnLockBand.equals("")){
            wProcSuccess(true);
        }else if(mWLockBand.equals("") && mWUnLockBand.equals("")){
            if(!mIsDualMode){
                dismissProgressDialog(null);
            }else{
                dismissProgressDialog("Band Select Success");
                mLastSelectBand = (ArrayList<String>)mSelectBand.clone(); 
            }           
            //showResultDialog("No Band Change"); 
            return;
        }else{
            wProcSuccess(false);
        }
    }

    class FBHandler extends Handler {

        public FBHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg){
            String values = null;
            String response;
            String atCmd;
            String dialogMessage;
            switch(msg.what){       
                case LOCK_TD_BAND:{  
                    if(!mIsDualMode){                       
                        if(mNetMode.equals("15")){
                            showProgressDialog();
                            checkAndSetTDBandLock();
                        }else if(mNetMode.equals("13")){
                            showProgressDialog();
                            values = checkGSMBandLock();
                            /*      if(checkGSMBandLock() == null){
                                dismissProgressDialog();
                                showResultDialog("No Band Change"); 
                            }else{*/
                            atCmd = engconstents.ENG_AT_SELECT_GSMBAND+values;
                            response = IATUtils.sendATCmd(atCmd,"atchannel2");
                            if(response.contains(IATUtils.AT_OK)){
                                dismissProgressDialog("Band Select Success");
                                mLastSelectBand = (ArrayList<String>)mSelectBand.clone();
                            }else{
                                dismissProgressDialog("Band Select Fail");
                                mUiThread.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        resumeLastGLock();  
                                    }
                                }); 
                            }
                            // }
                        }  
                        break;
                    }else{
                        showProgressDialog();
                        values = checkGSMBandLock();
                        /*     if(values == null){
                            checkAndSetTDBandLock();
                        }else{*/
                        atCmd = engconstents.ENG_AT_SELECT_GSMBAND+values;
                        response = IATUtils.sendATCmd(atCmd,"atchannel2");
                        Log.d(TAG,"<2>LOCK_GSM_BAND AT is "+atCmd+" Result is "+response+" mIsDualMode is "+mIsDualMode);
                        if(response.contains(IATUtils.AT_OK)){
                            checkAndSetTDBandLock();
                        }else{
                            dismissProgressDialog("Band Select Fail");
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {                                        
                                    if(mLastSelectBand.contains(KEY_TD_A_F)){
                                        mTDAPref.setChecked(true);
                                    }
                                    if(mLastSelectBand.contains(KEY_TD_F_F)){
                                        mTDFPref.setChecked(true);
                                    }  
                                    resumeLastGLock(); 
                                }
                            });
                        }
                        //}   
                        break;
                    }         
                }
                case LOCK_WCDMA_BAND:{ 
                    Log.d(TAG,"mIsDualMode is "+mIsDualMode+", mNetMode is "+mNetMode);
                    if(!mIsDualMode){
                        if(mNetMode.equals("14")){                           
                            showProgressDialog();
                            checkWCDMABandLockStatus();
                            setWCDMABandLock();
                        }else if(mNetMode.equals("13")){
                            showProgressDialog();
                            values = checkGSMBandLock();
                            /*   if(values == null){
                                dismissProgressDialog();
                                showResultDialog("No Band Change"); 
                            }else{*/
                            atCmd = engconstents.ENG_AT_SELECT_GSMBAND+values;
                            response = IATUtils.sendATCmd(atCmd,"atchannel2");
                            if(response.contains(IATUtils.AT_OK)){
                                dismissProgressDialog("Band Select Success");                              
                                mLastSelectBand = (ArrayList<String>)mSelectBand.clone();
                            }else{
                                dismissProgressDialog("Band Select Fail");
                                mUiThread.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        resumeLastGLock();  
                                    }
                                }); 
                            }
                            //}                 
                        }
                        break;
                    }else{
                        showProgressDialog();
                        values = checkGSMBandLock();
                        /*    if(values == null){
                            checkWCDMABandLockStatus();
                            setWCDMABandLock(); 
                        }else{*/
                        atCmd = engconstents.ENG_AT_SELECT_GSMBAND+values;
                        response = IATUtils.sendATCmd(atCmd,"atchannel2");
                        Log.d(TAG,"<2>LOCK_GSM_BAND AT is "+atCmd+" Result is "+response+" mIsDualMode is "+mIsDualMode);
                        if(response.contains(IATUtils.AT_OK)){
                            checkWCDMABandLockStatus();
                            setWCDMABandLock();  
                        }else{
                            dismissProgressDialog("Band Select Fail"); 
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(mLastSelectBand.contains(KEY_W1)){
                                        mWBandCapbility[0].setChecked(true);
                                    }else{
                                        mWBandCapbility[0].setChecked(false); 
                                    }
                                    if(mLastSelectBand.contains(KEY_W2)){
                                        mWBandCapbility[1].setChecked(true);
                                    }else{
                                        mWBandCapbility[1].setChecked(false);
                                    }    
                                    if(mLastSelectBand.contains(KEY_W5)){
                                        mWBandCapbility[4].setChecked(true);
                                    }else{
                                        mWBandCapbility[4].setChecked(false);
                                    }
                                    if(mLastSelectBand.contains(KEY_W8)){
                                        mWBandCapbility[7].setChecked(true);
                                    }else{
                                        mWBandCapbility[7].setChecked(false);
                                    }
                                    resumeLastGLock();
                                }
                            });
                        }  
                        //}    
                        break;
                    }
                }
                case LOCK_GSM_BAND:{
                    showProgressDialog();
                    values = checkGSMBandLock();
                    if(values != null){
                        atCmd = engconstents.ENG_AT_SELECT_GSMBAND+values;
                        response = IATUtils.sendATCmd(atCmd,"atchannel2");
                        Log.d(TAG,"<2>LOCK_GSM_BAND AT is "+atCmd+" Result is "+response);
                        if(response.contains(IATUtils.AT_OK)){
                            dismissProgressDialog("Band Select Success");
                            mLastSelectBand = (ArrayList<String>)mSelectBand.clone();
                        }else{
                            dismissProgressDialog("Band Select Fail");
                            mUiThread.post(new Runnable() {
                                @Override
                                public void run() {
                                    resumeLastGLock();                                    
                                }
                            }); 
                        }
                    }/*else{
                        dismissProgressDialog();
                        dialogMessage = "No Band Change";
                        showResultDialog(dialogMessage);  
                    }*/            
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void showResultDialog(String message){
        if(message != null){
            mDialogMessage = message;
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = new AlertDialog.Builder(TriBandModeSetActivity.this)
                    .setTitle("Band Select")
                    .setMessage(mDialogMessage)
                    .setPositiveButton(R.string.alertdialog_cancel, 
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
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

    private void showProgressDialog(){
        mUiThread.post(new Runnable() {
            @Override
            public void run() {
                mProgressDialog = ProgressDialog.show(TriBandModeSetActivity.this, "Set...", "Please wait...", true, false);
            }
        });  
    }

    private void dismissProgressDialog(String message){
        if(message != null){
            mDialogMessage = message;
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if(mProgressDialog != null){
                        mProgressDialog.dismiss();                   
                        AlertDialog alertDialog = new AlertDialog.Builder(TriBandModeSetActivity.this)
                        .setTitle("Band Select")
                        .setMessage(mDialogMessage)
                        .setPositiveButton(R.string.alertdialog_cancel, 
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).create();
                        alertDialog.show();           
                    }
                }
            }); 
        }else{
            mUiThread.post(new Runnable() {
                @Override
                public void run() {
                    if(mProgressDialog != null){
                        mProgressDialog.dismiss();                                                  
                    }
                }
            });  
        }

    }
}
