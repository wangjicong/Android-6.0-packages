package com.android.phone;

import com.android.ims.ImsManager;
import com.android.ims.ImsCallForwardInfo;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.sprd.phone.TeleServicePluginsHelper;
import com.sprd.phone.settings.CallForwardTimeEditPreFragement;
import com.sprd.phone.settings.callbarring.TimeConsumingPreferenceListener;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;


public class GsmUmtsCallForwardOptions extends TimeConsumingPreferenceActivity
implements CallForwardTimeEditPreFragement.Listener {
    private static final String LOG_TAG = "GsmUmtsCallForwardOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String NUM_PROJECTION[] = {
        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    private static final String BUTTON_CFU_KEY   = "button_cfu_key";
    private static final String BUTTON_CFB_KEY   = "button_cfb_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";
    // SPRD: add for callforward time
    private static final String BUTTON_CFT_KEY = "button_cft_key";

    /* SPRD: function VOLTE call forward query support. @{ */

    private static final String KEY_TOGGLE = "toggle";
    private static final String KEY_STATUS = "status";
    private static final String KEY_NUMBER = "number";

    private CallForwardEditPreference mButtonCFU;
    private CallForwardEditPreference mButtonCFB;
    private CallForwardEditPreference mButtonCFNRy;
    private CallForwardEditPreference mButtonCFNRc;

    /* SPRD: add for callforward time @{ */
    private Preference mButtonCFT;
    SharedPreferences mPrefs;
    public static final String PREF_PREFIX = "phonecalltimeforward_";
    TimeConsumingPreferenceListener tcpListener;
    Context mContext;
    private int mPhoneId = 0;
    /* @} */
    /* SPRD: add for bug 478880 @{ */
    private static final int CFU_PREF_REASON = 0;
    private static final String CFT_STATUS_ACTIVE = "1";
    /* @} */
    // SPRD: modify for bug544093
    private static final int CF_AUDIO_SERVICE_CALSS = 1;

    private final ArrayList<CallForwardEditPreference> mPreferences =
            new ArrayList<CallForwardEditPreference> ();
    private int mInitIndex= 0;

    private boolean mFirstResume;
    private Bundle mIcicle;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    /* SPRD: add for bug 527079 @{ */
    private TelephonyManager mTelephonyManager;
    /* @} */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        /* SPRD: add for bug 527079 @{ */
        mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null && mTelephonyManager.isVolteCallEnabled()) {
            mTelephonyManager.listen(sLtePhoneStateListener,
                    PhoneStateListener.LISTEN_VOLTE_STATE);
        }
        /* @} */

        /* SPRD: add for callforward time @{ */
        mContext = this.getApplicationContext();
        if (ImsManager.isVolteEnabledByPlatform(mContext)) {
            addPreferencesFromResource(R.xml.volte_callforward_options);
        } else {
            addPreferencesFromResource(R.xml.callforward_options);
        }
        /* @} */

        mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        mSubscriptionInfoHelper.setActionBarTitle(
                getActionBar(), getResources(), R.string.call_forwarding_settings_with_label);
        mPhone = mSubscriptionInfoHelper.getPhone();
        // SPRD: add for callforward time
        mPhoneId = mPhone.getPhoneId();

        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCFU = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFU_KEY);
        mButtonCFB = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFB_KEY);
        mButtonCFNRy = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRY_KEY);
        mButtonCFNRc = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRC_KEY);

        mButtonCFU.setParentActivity(this, mButtonCFU.reason);
        mButtonCFB.setParentActivity(this, mButtonCFB.reason);
        mButtonCFNRy.setParentActivity(this, mButtonCFNRy.reason);
        mButtonCFNRc.setParentActivity(this, mButtonCFNRc.reason);

        mPreferences.add(mButtonCFU);
        mPreferences.add(mButtonCFB);
        mPreferences.add(mButtonCFNRy);
        mPreferences.add(mButtonCFNRc);

        /* SPRD: add for callforward time @{ */
        if (ImsManager.isVolteEnabledByPlatform(mContext)) {
            mPrefs = mContext.getSharedPreferences(PREF_PREFIX + mPhoneId,
                    mContext.MODE_WORLD_READABLE);
            CallForwardTimeEditPreFragement.addListener(this);
            tcpListener = this;

            mButtonCFT = prefSet.findPreference(BUTTON_CFT_KEY);
        }
        /* @} */

        // we wait to do the initialization until onResume so that the
        // TimeConsumingPreferenceActivity dialog can display as it
        // relies on onResume / onPause to maintain its foreground state.

        mFirstResume = true;
        mIcicle = icicle;

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // SPRD: modify for bug539931
        refreshCFTButton();
        if (mFirstResume) {
            if (mIcicle == null) {
                if (DBG) Log.d(LOG_TAG, "start to init ");
                /* SPRD: add for bug544093 @{ */
                if (mPhone != null) {
                    mPhone.getCallForwardingOption(CommandsInterface.CF_REASON_ALL,
                            CF_AUDIO_SERVICE_CALSS, null, null);
                }
                /* @} */
                 mPreferences.get(mInitIndex).init(this, false, mPhone);
            } else {
                mInitIndex = mPreferences.size();

                 for (CallForwardEditPreference pref : mPreferences) {
                     Bundle bundle = mIcicle.getParcelable(pref.getKey());
                     pref.setToggled(bundle.getBoolean(KEY_TOGGLE));
                     CallForwardInfo cf = new CallForwardInfo();
                     cf.number = bundle.getString(KEY_NUMBER);
                     cf.status = bundle.getInt(KEY_STATUS);
                     pref.handleCallForwardResult(cf);
                     pref.init(this, true, mPhone);
                 }
            }
            mFirstResume = false;
            mIcicle = null;
        }

        /* SPRD: add for bug 478880 @{ */
        if (ImsManager.isVolteEnabledByPlatform(mContext)) {
            updateCFTSummaryText();
        }
        /* @} */
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for (CallForwardEditPreference pref : mPreferences) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_TOGGLE, pref.isToggled());
            if (pref.callForwardInfo != null) {
                bundle.putString(KEY_NUMBER, pref.callForwardInfo.number);
                bundle.putInt(KEY_STATUS, pref.callForwardInfo.status);
            }
            outState.putParcelable(pref.getKey(), bundle);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            mPreferences.get(mInitIndex).init(this, false, mPhone);
        }
        super.onFinished(preference, reading);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DBG) Log.d(LOG_TAG, "onActivityResult: done");
        if (resultCode != RESULT_OK) {
            if (DBG) Log.d(LOG_TAG, "onActivityResult: contact picker result not OK.");
            return;
        }
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(data.getData(),
                NUM_PROJECTION, null, null, null);
            if ((cursor == null) || (!cursor.moveToFirst())) {
                if (DBG) Log.d(LOG_TAG, "onActivityResult: bad contact data, no results found.");
                return;
            }

                switch (requestCode) {
                    case CommandsInterface.CF_REASON_UNCONDITIONAL:
                        mButtonCFU.onPickActivityResult(cursor.getString(0));
                        break;
                    case CommandsInterface.CF_REASON_BUSY:
                        mButtonCFB.onPickActivityResult(cursor.getString(0));
                        break;
                    case CommandsInterface.CF_REASON_NO_REPLY:
                        mButtonCFNRy.onPickActivityResult(cursor.getString(0));
                        break;
                    case CommandsInterface.CF_REASON_NOT_REACHABLE:
                        mButtonCFNRc.onPickActivityResult(cursor.getString(0));
                        break;
                    default:
                        break;
                }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            /* SPRD: add for bug544979 @{ */
            if (ImsManager.isVolteEnabledByPlatform(
                    mSubscriptionInfoHelper.getPhone().getContext())) {
                GsmUmtsAllCallForwardOptions.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
            } else {
                CallFeaturesSetting.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
            }
            /* @} */
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* SPRD: add for bug544979 @{ */
    @Override
    public void onBackPressed() {
        if (ImsManager.isVolteEnabledByPlatform(mSubscriptionInfoHelper.getPhone().getContext())) {
            GsmUmtsAllCallForwardOptions.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
        } else {
            CallFeaturesSetting.goUpToTopLevelSetting(this, mSubscriptionInfoHelper);
        }
    }
    /* @} */

    /* SPRD: add for callforward time @{ */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ImsManager.isVolteEnabledByPlatform(mContext)) {
            CallForwardTimeEditPreFragement.removeListener(this);
        }

        /* SPRD: add for bug 527079 @{ */
        if (mTelephonyManager != null && mTelephonyManager.isVolteCallEnabled()) {
            mTelephonyManager.listen(sLtePhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
        /* @} */
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mButtonCFT) {
            final Intent intent = new Intent();
            intent.setClassName("com.android.phone",
                    "com.sprd.phone.settings.CallForwardTimeEditPreference");
            intent.putExtra("phone_id", String.valueOf(mPhoneId));
            startActivity(intent);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


    @Override
    public void onCallForawrdTimeStateChanged(String number){
        mInitIndex = 0;
        mPreferences.get(mInitIndex).init(this, false, mPhone);
        /* SPRD: add for bug 478880 @{ */
        updateCFTSummaryText();
        if (number != null) {
            mButtonCFU.saveStringPrefs(
                    CallForwardEditPreference.PREF_PREFIX + mPhoneId + "_" + CFU_PREF_REASON,
                    number);
        }
        /* @} */
    }

    /**
     * SPRD: add for bug 478880 @{
     *
     */
    private void updateCFTSummaryText() {
        CharSequence mSummary;
        // SPRD: modify for bug539931
        if (CFT_STATUS_ACTIVE.equals(mPrefs.getString(PREF_PREFIX + "status_" + mPhoneId, ""))) {
            mSummary = mPrefs.getString(PREF_PREFIX + "num_" + mPhoneId, "");
        } else {
            mSummary = mContext.getText(R.string.sum_cft_disabled);
        }
        mButtonCFT.setSummary(mSummary);
    }
    /** @} */

    public void savePrefData(String key, String value) {
        Log.w(LOG_TAG, "savePrefData(" + key + ", " + value + ")");
        if (mPrefs != null) {
            try {
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putString(key, value);
                editor.apply();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception happen.");
            }
        }
    }

    @Override
    public void onEnableStatus(Preference preference, int status) {
        if (status == 1) {
            mButtonCFT.setEnabled(false);
        } else {
            mButtonCFT.setEnabled(true);
            /* SPRD: add for bug549240 @{ */
            if (CFT_STATUS_ACTIVE.equals(
                    mPrefs.getString(PREF_PREFIX + "status_" + mPhoneId, ""))) {
                mButtonCFU.setEnabled(false);
            }
            /* @} */
        }
        if (preference == mButtonCFU) {
            Log.i(LOG_TAG, "onEnableStatus...status = " + status);
            updatePrefCategoryEnabled(mButtonCFU);
        }
    }
    /* @} */

    public static boolean checkVideoCallServiceClass(int sc) {
        return (sc & CommandsInterface.SERVICE_CLASS_DATA) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_DATA_SYNC) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_DATA_ASYNC) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_PACKET) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_PAD) != 0;
    }

    public static boolean checkServiceClassSupport(int sc) {
        return (sc & CommandsInterface.SERVICE_CLASS_DATA) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_DATA_SYNC) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_DATA_ASYNC) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_PACKET) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_PAD) != 0
            ||(sc & CommandsInterface.SERVICE_CLASS_VOICE) != 0;
    }

    private void updatePrefCategoryEnabled(Preference preference) {
        if(preference == mButtonCFU){
            if(mButtonCFU.getStatus() == 1){
                mButtonCFB.setEnabled(false);
                mButtonCFNRc.setEnabled(false);
                mButtonCFNRy.setEnabled(false);
            }else{
                mButtonCFB.setEnabled(true);
                mButtonCFNRc.setEnabled(true);
                mButtonCFNRy.setEnabled(true);
            }
        }
    }
    /* SPRD: function VOLTE call forward query support. @{ */

    public void onUpdateTwinsPref(boolean toggled, int arg1, int arg2, String arg3, String arg4) {
        int serviceClass = arg1;
        int reason = arg2;
        if (reason == 0) {
                mButtonCFU.setToggled(toggled);
                // SPRD: modify for bug539931
                refreshCFTButton();
                if (toggled) {
                    mButtonCFB.setEnabled(false);
                    mButtonCFNRc.setEnabled(false);
                    mButtonCFNRy.setEnabled(false);
                } else {
                    mButtonCFB.setEnabled(true);
                    mButtonCFNRc.setEnabled(true);
                    mButtonCFNRy.setEnabled(true);
                }
                /* @} */
        }
    }

    @Override
    public void onError(Preference preference, int error) {
        Log.d(LOG_TAG, "onError, preference=" + preference.getKey() + ", error=" + error);
        if (preference instanceof CallForwardEditPreference) {
            CallForwardEditPreference pref = (CallForwardEditPreference)preference;
            if (pref != null) {
                pref.setEnabled(false);
            }
        }
        super.onError(preference,error);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.userSetLocale) {
            finish();
        }
    }
    /* @} */

    /* SPRD: add for bug 527079 @{ */
    public final PhoneStateListener sLtePhoneStateListener = new PhoneStateListener() {
        @Override
        public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
            refreshCFTButton();
        }
    };
    /* @} */
    /* SPRD: add for bug539931 @{ */
    public void refreshCFTButton() {
        if (mButtonCFT != null) {
            if ((mButtonCFU.isToggled()
                    && !CFT_STATUS_ACTIVE.equals(
                            mPrefs.getString(PREF_PREFIX + "status_" + mPhoneId, "")))
                    // SPRD: modify for bug544925 && 552776
                        || (mPhone != null && !mPhone.isImsRegistered()
                                           && !TeleServicePluginsHelper.getInstance(this).
                                                   isCallTimeForwardSupport())) {
                mButtonCFT.setEnabled(false);
            } else {
                mButtonCFT.setEnabled(true);
            }
        }
    }
    /* @} */
}
