
package com.android.phone;

import java.util.ArrayList;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.sprd.phone.OtherGlobals;
import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.VoLteServiceState;
import android.telephony.TelephonyManager;
//import android.telephony.TelephonyManagerSprd;
import android.util.AttributeSet;
import android.util.Log;
import android.preference.PreferenceActivity;

//import com.sprd.phone.settings.callbarring.TimeConsumingPreferenceListener;
import com.sprd.videophone.vtmanager.VTManager;
import com.android.phone.GsmUmtsAdditionalCallOptions;
import android.preference.PreferenceScreen;

import com.android.phone.PhoneUtils;
import com.android.phone.R;
import com.android.phone.PhoneGlobals;
import com.android.phone.TimeConsumingPreferenceActivity;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import com.android.ims.ImsManager;
import com.android.ims.ImsConfig;
import com.android.ims.ImsConfigListener;

public class ImsCallAvailabilityPreference extends TimeConsumingPreferenceActivity implements
                Preference.OnPreferenceChangeListener{
    private static final String LOG_TAG = "ImsCallAvailabilityPreference";
    private static final String BUTTON_IC_RESOLUTION = "vt_resolution_set_key";// SPRD:add for VOLTE

    public static final String VT_RESOLUTION = "vt_resolution";

    private static final String PROPERTY_VOLTE_ENABLE = "persist.sys.volte.enable";

    public static final int RESOLUTION_720P = 0;
    public static final int RESOLUTION_VGA_15 = 1;
    public static final int RESOLUTION_VGA_30 = 2;
    public static final int RESOLUTION_QVGA_15 = 3;
    public static final int RESOLUTION_QVGA_30 = 4;
    public static final int RESOLUTION_CIF = 5;
    public static final int RESOLUTION_QCIF = 6;

    private final ArrayList<Preference> mPreferences = new ArrayList<Preference>();
    private ListPreference mPreferredSetResolution;
    private Preference imsVoLteProvisionedButton;
    private SharedPreferences mSharePref;
    private Phone mPhone;
    ImsManager mImsManager;
    ImsConfig  mImsConfig;
    ImsConfigListenerProxy mImsConfigListenerProxy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "..oncreate");
        /* SPRD: add for bug523256 @{ */
        int subId = this.getIntent().getIntExtra("sub_id", 0);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
            Log.i(LOG_TAG, "phoneId is invalid");
            return;
        }
        mImsManager = ImsManager.getInstance(this,phoneId);
        /* @} */
        if(mImsManager != null){
           try {
              mImsConfig = mImsManager.getConfigInterface();
             }catch (Exception ie) {
              Log.d(LOG_TAG, "Get ImsConfig occour exception =" + ie);
             }
        }
        mImsConfigListenerProxy = new ImsConfigListenerProxy();
        addPreferencesFromResource(R.xml.ims_service_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        mPreferredSetResolution = (ListPreference)prefSet.findPreference(BUTTON_IC_RESOLUTION);
        mPreferredSetResolution.setEnabled(true);
        mPreferredSetResolution.setOnPreferenceChangeListener(this);
        mSharePref = PreferenceManager.getDefaultSharedPreferences(this);
       try {
             mImsConfig.getVideoQuality(mImsConfigListenerProxy);
         }catch (Exception ie) {
             Log.d(LOG_TAG, "getVideoQuality occour exception ="+ ie);
         }
        // SPRD: modify for bug523256
        //int phoneId = this.getIntent().getIntExtra("phone_id", 0);
        mPhone = PhoneFactory.getPhone(phoneId);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mPreferredSetResolution) {
            Log.d(LOG_TAG, "onPreferenceChange: getValue=" + objValue);
            try {
                mImsConfig.setVideoQuality(Integer.parseInt((String) objValue) + 1,
                        mImsConfigListenerProxy);// SPRD:modify for bug534346
             }catch (Exception ie) {

             }
            mPreferredSetResolution.setValueIndex(Integer.parseInt((String)objValue));
            mPreferredSetResolution.setSummary(mPreferredSetResolution.getEntry());
        }
        return true;
   }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        super.onFinished(preference, reading);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private class ImsConfigListenerProxy extends ImsConfigListener.Stub {

        @Override
        public void onGetFeatureResponse(int feature, int network, int value, int status){
        }

        @Override
        public void onSetFeatureResponse(int feature, int network, int value, int status){
        }

        @Override
        public void onGetVideoQuality(int status, int quality){
            mPreferredSetResolution.setValueIndex(quality - 1);// SPRD:modify for bug534346
            mPreferredSetResolution.setSummary(mPreferredSetResolution.getEntry());
        }

        @Override
        public void onSetVideoQuality(int status){
        }
    }
}
