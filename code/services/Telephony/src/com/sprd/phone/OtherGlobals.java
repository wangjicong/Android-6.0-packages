
package com.sprd.phone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.internal.telephony.TelephonyIntents;
import com.android.services.telephony.Log;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;
import com.android.services.telephony.Log;
import com.android.internal.telephony.policy.RadioTaskManager;
import com.android.phone.R;
import com.sprd.phone.settings.apnconfig.ApnConfigService;
import com.sprd.android.config.OptConfig;

/**
 * It is a convenient class for other feature support. We had better put this class in the same
 * package with {@link PhoneGlobals}, so we can touch all the package permission variables and
 * methods in it.
 */
public class OtherGlobals extends ContextWrapper {
    private static final String TAG = "OtherGlobals";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private static OtherGlobals mInstance;
    private Context mContext;

    /** SPRD: sim lock begin @{ */
    private SimLockManager mSimLockManager;
    /** @} */

    /** Add for third part application support. */
    public static final int TYPE_CALL_FIRE_WALL = 0;
    public static final int TYPE_VOICE_SEARCH = 1;

    private static final int APP_NOT_SUPPORT = 0;
    private static final int APP_SUPPORT = 1;
    private static HashMap<Integer, Intent> sSupportAppIntent = new HashMap<Integer, Intent>();
    private HashMap<Integer, Integer> mSupportApp;
    static {
        Intent intent = new Intent("com.sprd.blacklist.action");
        sSupportAppIntent.put(TYPE_CALL_FIRE_WALL, intent);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        sSupportAppIntent.put(TYPE_VOICE_SEARCH, intent);
    }
    /** end */

    private static byte mSimStateChangedFlag = 0;
    /* SPRD: add by fix bug 426448 @{ */
    private static boolean[] mAllSimLoaded = {
            false, false
    };
    /* @} */

    private static final int EVENT_SIM_STATE_CHANGED = 1;

    private TelephonyManager mTelephonyManager;
    private RadioTaskManager mRadioTaskManager;

    private boolean hasSubscriptionInfosUpdated = false;
    public static final String ICCIDS_PREFS_NAME = "iccid.msms.info";
    private int mPhoneCount;
    public static final String SIM_ICC_ID = "icc_id";

    private static final String SHOW_SELECT_PRIMARY_CARD_DIALOG = "android.intent.action.SHOW_SELECT_PRIMARY_CARD_DIALOG";
    //SPRD: Bug 537083 phone is the encryption process,don't display primary card dialog
    private static final String DECRYPT_STATE = "trigger_restart_framework";//the state indicate that decrypt the encrypted phone and trigger restart framework

    public static OtherGlobals getInstance() {
        return mInstance;
    }

    public OtherGlobals(Context context) {
        super(context);
        mInstance = this;
        mContext = context;
    }

    public void onCreate() {
        /** Add for Contacts application support @{ */
        Log.d(TAG, "Send Broadcast ACTION_PHONE_START");
        Intent intent = new Intent(TelephonyIntents.ACTION_PHONE_START);
        sendBroadcast(intent);
        /** @} */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);

        /** SPRD: add for simlock feture @{ */
        IntentFilter simlockIntentFilter = new IntentFilter();
        simlockIntentFilter.addAction(TelephonyIntents.SHOW_SIMLOCK_UNLOCK_SCREEN_ACTION);
        simlockIntentFilter.addAction(TelephonyIntents.SHOW_SIMLOCK_UNLOCK_SCREEN_BYNV_ACTION);
        registerReceiver(mUnlockScreenReceiver, simlockIntentFilter);
        /** @} */

        mTelephonyManager = (TelephonyManager) TelephonyManager.from(mContext);
        mPhoneCount = mTelephonyManager.getPhoneCount();
        // Initialize instance of RadioTaskManager
        mRadioTaskManager = new RadioTaskManager(mContext);
        mRadioTaskManager.onCreate(R.string.feature_create_policy);

        /** Add for third part application support @{ */
        mSupportApp = new HashMap<Integer, Integer>();
        Set<Integer> set = sSupportAppIntent.keySet();
        Iterator<Integer> iterator = set.iterator();
        while (iterator.hasNext()) {
            int type = iterator.next().intValue();
            Intent typeIntent = sSupportAppIntent.get(type);
            if (typeIntent != null) {
                List<ResolveInfo> infoList = getPackageManager().queryIntentActivities(typeIntent, 0);
                boolean support = (infoList != null && infoList.size() > 0);
                mSupportApp.put(type, support ? APP_SUPPORT : APP_NOT_SUPPORT);
                continue;
            }
            mSupportApp.put(type, APP_NOT_SUPPORT);
        }
        /** @} */

        //SPRD: MODIFY FOR BUG 497932
    mContext.getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED), false,
                        mSetupWizardCompleteObserver);

        /*SPRD: Bug 474688 Add for SIM hot plug feature @{*/
        mSimInside = new boolean[mPhoneCount];
        /*@}*/

        //SPRD:add for sim lock
        mSimLockManager = SimLockManager.getInstance(mContext, R.string.feature_support_simlock);
        if (SystemProperties.getBoolean("ro.simlock.unlock.autoshow", true)
                //&& !SystemProperties.getBoolean("ro.simlock.onekey.lock", false) //yanghua manual lock need simlock ui
                && !SystemProperties.getBoolean("ro.simlock.unlock.bynv", false)) {
            mSimLockManager.registerForSimLocked(mContext, mHandler);
        }
    }

    /**
     * Add for third part application support.
     * @param type
     * @return
     */
    public boolean isSupportApplication(int type) {
        Integer value = mSupportApp.get(type);
        if (value != null) {
            int result = value.intValue();
            return result == APP_SUPPORT;
        }
        Log.d(TAG, "error, do not support type : " + type);
        return false;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            ContentResolver cr = getContentResolver();

            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                Log.d(TAG, "action===" + action + ",state" + state);
                if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(state)
                        || IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(state)
                        || IccCardConstants.INTENT_VALUE_ICC_UNKNOWN.equals(state)
                        || IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(state)) {
                    /*SPRD: Bug 474688 Add for SIM hot plug feature @{*/
                    if(handleByHotPlug(phoneId,state)){
                        Log.d(TAG, "hot plug, return");
                        String newIccId = getIccId(phoneId);
                        SharedPreferences preferences = mContext.getSharedPreferences(
                                ICCIDS_PREFS_NAME, 0);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(SIM_ICC_ID + phoneId, newIccId);
                        editor.commit();
                        return;
                    }
                    /*@}*/

                    /*SPRD: Bug 537083 phone is the encryption process,don't display primary card dialog @{*/
                    String decryptState = SystemProperties.get("vold.decrypt");
                    Log.d(TAG,"decrypt state --> " + decryptState);
                    if (!TextUtils.isEmpty(decryptState) && !DECRYPT_STATE.equals(decryptState)) {
                        return;
                    }
                    /*@}*/

                    mSimStateChangedFlag |= (1 << phoneId);
                    /* SPRD: add by fix bug 426448 @{ */
                    if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(state)) {
                        mAllSimLoaded[phoneId] = true;
                    } else {
                        mAllSimLoaded[phoneId] = false;
                    }
                    /* @} */
                    Message simMsg = new Message();
                    simMsg.what = EVENT_SIM_STATE_CHANGED;
                    /* SPRD: add by fix bug 460758 @{ */
                    simMsg.arg1 = mSimStateChangedFlag;
                    /* @} */
                    mHandler.sendMessage(simMsg);
                }
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {

                Log.d(TAG, "onReceive : ACTION_AIRPLANE_MODE_CHANGED");

//                if (!mTelephonyManager.isMultiSimEnabled()) {
//                    return;
//                }
                boolean enabled = !intent.getBooleanExtra("state", false);
                boolean isEmergenceModePowerOn = intent.getBooleanExtra("isEmergencyCallEnable",
                        false);
                if (DBG)
                    Log.d(TAG, "==> enabled: " + enabled + ", isEmergenceModePowerOn : "
                            + isEmergenceModePowerOn);
                if (enabled) {
                    // Emergency modify start
                    if (!isEmergenceModePowerOn) {
                        mRadioTaskManager.setAirplaneMode(false);
                    }
                    // Emergency modify end
                } else {
                    // if (!PhoneUtils.isVideoCall()) {
                    // PhoneUtils.hangup(PhoneGlobals.getInstance().mCM);
                    // }
                    mRadioTaskManager.setAirplaneMode(true);
                }
            }
        }
    };

    /** SPRD: add for simlock feture @{ */
    private BroadcastReceiver mUnlockScreenReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.SHOW_SIMLOCK_UNLOCK_SCREEN_ACTION)) {
                int simlockSlotFlag = intent.getIntExtra(TelephonyIntents.EXTRA_SIMLOCK_UNLOCK, 0);
                if (simlockSlotFlag == 0) return;
                int phoneCount = mTelephonyManager.getPhoneCount();
                int allFlag = (1<<phoneCount)-1;
                for (int i=0; i<phoneCount; i++ ) {
                    if (((1 << i) & simlockSlotFlag) != 0) {
                        int simState = mTelephonyManager.getSimState(i);
                        Log.d(TAG, "simState[" + i + "] = " + simState);
                        Message msg = mSimLockManager.decodeMessage(simState, i);
                        if (msg != null && msg.what != 0) {
                            mHandler.sendMessage(msg);
                        }
                    }
                }
            } else if (action.equals(TelephonyIntents.SHOW_SIMLOCK_UNLOCK_SCREEN_BYNV_ACTION)) {
                mSimLockManager.showPanelForUnlockByNv(mContext);
            }
        }
    };
    /** @} */

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            switch (msg.what) {

                case EVENT_SIM_STATE_CHANGED:
                    /* SPRD: add by fix bug 460758 @{ */
                    int SimStateChangedFlag = msg.arg1;
                    Log.d(TAG, "handleMessage : EVENT_SIM_STATE_CHANGED  mSimStateChangedFlag = " + SimStateChangedFlag +
                            " hasSubscriptionInfosUpdated = " + hasSubscriptionInfosUpdated);

                    if (SimStateChangedFlag == ((1<<phoneCount)-1)) {
                    /* @} */
                        Log.d(TAG,
                                "isPrimaryCardNeedManualSet = "
                                        + mRadioTaskManager.isPrimaryCardNeedManualSet());

                        SharedPreferences preferences = mContext.getSharedPreferences(
                                ICCIDS_PREFS_NAME, 0);

                        boolean isSimsChanged = false;
                        boolean setTestmodeForNetworkType = TelephonyManager.isDeviceSupportLte();
                        for (int i = 0; i < mPhoneCount; i++) {
                            String lastIccId = preferences.getString(SIM_ICC_ID + i, null);
                            String newIccId = getIccId(i);
                            Log.d(TAG, "lastIccId = " + lastIccId + " newIccId = "
                                    + newIccId);
                            //SPRD: modify for bug506988
                            if (TextUtils.isEmpty(newIccId) || !newIccId.equalsIgnoreCase(lastIccId)) {
                                if(setTestmodeForNetworkType
                                        && TextUtils.isEmpty(newIccId) && TextUtils.isEmpty(lastIccId)) {
                                    Log.d(TAG, "volte version, lastIccId and newIccId are all empty!");
                                } else {
                                    isSimsChanged = true;
                                }
                                if (!mRadioTaskManager.hasSimLocked()) {
                                    Log.d(TAG, "save sim iccid");
                                    mTelephonyManager.setPrimaryCard(SubscriptionManager.INVALID_PHONE_INDEX);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString(SIM_ICC_ID + i, newIccId);
                                    editor.commit();
                                }
                            }
                        }
                        /* SPRD: add by fix bug 424938 to set default sms/voice sub id @{ */
                        autoSetDefaultSmsVoiceSubId();
                        /* @} */

                        Log.d(TAG, "isSimsChanged = " + isSimsChanged);
                        /*yanghua add 20170504 begin*/
                        boolean is_sim1_insert = mTelephonyManager.hasIccCard(0);        
                        boolean is_sim2_insert = mTelephonyManager.hasIccCard(1);
                        boolean isNeedShowMultiPop = true;
                        if (OptConfig.SUN_SUBCUSTOM_C7356_TYC_S417_WVGA){
                            if (is_sim1_insert==true && is_sim2_insert==true){
                                isSimsChanged = false;
                            }
                            isNeedShowMultiPop = false;
                        }
                        /*yanghua add 20170504 end*/
                        if (isSimsChanged) {
                            mRadioTaskManager.savePreferredNetworkType(TelephonyManager.NT_UNKNOWN);
                            setMultiModeSlotAccordingToPolicy();
                        } else {
                            int primaryCard = mTelephonyManager.getPrimaryCard();
                            Log.d(TAG, "primaryCard = " + primaryCard);
                            if (!SubscriptionManager.isValidPhoneId(primaryCard) && isNeedShowMultiPop) { //yanghua add isNeedShowMultiPop
                                setMultiModeSlotAccordingToPolicy();
                            } else {
                                /*yanghua add 20170504 begin*/
                                if (OptConfig.SUN_SUBCUSTOM_C7356_TYC_S417_WVGA){
                                    if (is_sim1_insert==false && is_sim2_insert==true){
                                        primaryCard = 1;
                                    }else{
                                        primaryCard = 0;
                                    }
                                    mRadioTaskManager.setDefaultDataPhoneIdNeedUpdate(true);
                                    mRadioTaskManager.manualSetPrimaryCard(primaryCard);
                                }else{
                                    mRadioTaskManager.setDefaultDataPhoneIdNeedUpdate(false);
                                    mRadioTaskManager.manualSetPrimaryCard(primaryCard);
                                }
                                /*yanghua add 20170504 end*/		
                            }
                        }

                    }
                    break;
            }
            //SPRD: add for sim lock
            if(msg.what != EVENT_SIM_STATE_CHANGED) {
                mSimLockManager.showPanel(mContext, msg);
            }
        }
    };

    private String getIccId(int phoneId) {
        String iccId = "";
        SubscriptionManager subManager = SubscriptionManager.from(mContext);
        List<SubscriptionInfo> subInfoRecord = subManager.getActiveSubscriptionInfoList();
        if (subInfoRecord != null) {
            for (int i = 0; i < subInfoRecord.size(); i++) {
                if (subInfoRecord.get(i).getSimSlotIndex() == phoneId) {
                    return subInfoRecord.get(i).getIccId();
                }
            }
        }
        return iccId;
    }

    /* SPRD: auto set default sms/voice sub id @{ */
    /*
     * Set the default voice/sms sub to reasonable values if the user hasn't selected a sub or the
     * user selected sub is not present. This method won't change user preference.
     */
    private void autoSetDefaultSmsVoiceSubId() {
        SubscriptionManager subManager = SubscriptionManager.from(mContext);
        List<SubscriptionInfo> activeSubInfoList = getActiveSubInfoList();
        int defaultVoiceSubId = mTelephonyManager.getMultiSimActiveDefaultVoiceSubId();
        Log.d(TAG, "autoSetDefaultPhones: defaultVoiceSubId = " + defaultVoiceSubId);
        if (defaultVoiceSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            defaultVoiceSubId = SubscriptionManager.getDefaultVoiceSubId();
        }
        if (defaultVoiceSubId == SubscriptionManager.MAX_SUBSCRIPTION_ID_VALUE
                && activeSubInfoList.size() < 2
                || !isSubIdActive(defaultVoiceSubId)) {
            if (activeSubInfoList.size() > 1) {
                final TelecomManager telecomManager = TelecomManager.from(this);
                telecomManager.setUserSelectedOutgoingPhoneAccount(null);
                // SPRD: [Bug535715] Set a valid default voice subId when first boot.
                int defaultVoicePhoneId = SubscriptionManager.getPhoneId(defaultVoiceSubId);
                if (!SubscriptionManager.isValidPhoneId(defaultVoicePhoneId)) {
                    subManager.setDefaultVoiceSubId(activeSubInfoList.get(0).getSubscriptionId());
                }
            } else if (activeSubInfoList.size() == 1) {
                int subId = activeSubInfoList.get(0).getSubscriptionId();
                setDefaultVoiceSubId(subId);
            }
        } else {
            setDefaultVoiceSubId(defaultVoiceSubId);
        }

        int defaultSmsSubId = mTelephonyManager.getMultiSimActiveDefaultSmsSubId();
        Log.d(TAG, "autoSetDefaultPhones: defaultSmsSubId = " + defaultSmsSubId);
        if (defaultSmsSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            defaultSmsSubId = SubscriptionManager.getDefaultSmsSubId();
        }
        if (!isSubIdActive(defaultSmsSubId)&& activeSubInfoList.size() >0) {
            subManager.setDefaultSmsSubId(activeSubInfoList.get(0).getSubscriptionId());
        } else {
            subManager.setDefaultSmsSubId(defaultSmsSubId);
        }
    }

    /* SPRD: MODIFY FOR BUG 497932:SMS sending set value exception @{ */
    private ContentObserver mSetupWizardCompleteObserver = new ContentObserver(mHandler) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            boolean isDeviceProvisioned = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED, 0) != 0;
            Log.d(TAG, "mSetupWizardCompleteObserver onChange : isDeviceProvisioned = "
                    + isDeviceProvisioned);
            if (isDeviceProvisioned && SubscriptionManager.getDefaultSmsSubId() < 1) {
                autoSetDefaultSmsVoiceSubId();
            }
        };
    };

    /* @} */

    private void setMultiModeSlotAccordingToPolicy() {
        mTelephonyManager.setPrimaryCard(SubscriptionManager.INVALID_PHONE_INDEX);

        boolean isPrimaryCardNeedManualSet = mRadioTaskManager.isPrimaryCardNeedManualSet();
        SubscriptionManager subManager = SubscriptionManager.from(mContext);
        // SPRD: Add for Apn and Data pop up. Bug:534300
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubId();
        Log.d(TAG, "isPrimaryCardNeedManualSet = " + isPrimaryCardNeedManualSet);
        /* SPRD: add by fix bug 426448 @{ */
        if (isPrimaryCardNeedManualSet && subManager.getActiveSubscriptionInfoCount() > 1) {
            prepareForMultiModeSlotChooseDialog();
        } else {
            mRadioTaskManager.autoSetPrimaryCardAccordingToPolicy();
        }
        /* @} */
        /* SPRD: Add for Apn and Data pop up. Bug:534300 @{ */
        List<SubscriptionInfo> subList = subManager.getActiveSubscriptionInfoList();
        // start the service to show apn config popup if sims changed
        Intent intent = new Intent(OtherGlobals.this, ApnConfigService.class);
        startService(intent);
        /* @} */
    }

    private boolean isSubIdActive(long subId) {
        if (subId == SubscriptionManager.MAX_SUBSCRIPTION_ID_VALUE) {
            return true;
        }

        List<SubscriptionInfo> activeSubInfoList = getActiveSubInfoList();

        for (SubscriptionInfo subInfo : activeSubInfoList) {
            if (subInfo.getSubscriptionId() == subId) {
                return true;
            }
        }
        return false;
    }

    private void setDefaultVoiceSubId(int subId) {
        Log.d(TAG, "setDefaultVoiceSubId" + subId);
        TelecomManager telecomManager = TelecomManager.from(mContext);
        PhoneAccountHandle phoneAccountHandle =
                subscriptionIdToPhoneAccountHandle(subId);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(mContext);
        final Iterator<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            final int phoneAccountSubId = PhoneUtils.getSubIdForPhoneAccountHandle(phoneAccountHandle);

            if (phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
                    && phoneAccountSubId == subId) {
                return phoneAccountHandle;
            }
        }

        return null;
    }

    private void prepareForMultiModeSlotChooseDialog() {
        /* SPRD: add by fix bug 426448 @{ */
        Log.d(TAG, "prepareForMultiModeSlotChooseDialog mAllSimLoaded[0] = "
                + mAllSimLoaded[0] + " mAllSimLoaded[1] = " + mAllSimLoaded[1]);
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        if (mAllSimLoaded[0] == true && mAllSimLoaded[1] == true) {
            Intent intent = new Intent(SHOW_SELECT_PRIMARY_CARD_DIALOG);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sendBroadcast(intent);
        }
        /* @} */
    }

    private List<SubscriptionInfo> getActiveSubInfoList() {
        SubscriptionManager subManager = SubscriptionManager.from(mContext);
        List<SubscriptionInfo> availableSubInfoList = subManager
                .getActiveSubscriptionInfoList();
        if (availableSubInfoList == null) {
            return new ArrayList<SubscriptionInfo>();
        }
        Iterator<SubscriptionInfo> iterator = availableSubInfoList.iterator();
        while (iterator.hasNext()) {
            SubscriptionInfo subInfo = iterator.next();
            int phoneId = subInfo.getSimSlotIndex();
            boolean isSimReady = mTelephonyManager.getSimState(phoneId) == TelephonyManager.SIM_STATE_READY;
            boolean isSimStandby = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.SIM_STANDBY + phoneId, 1) == 1;
            if (!isSimStandby || !isSimReady) {
                iterator.remove();
            }
        }
        return availableSubInfoList;
    }

    /*SPRD: Bug 474688 Add for SIM hot plug feature @{*/
    private boolean mSimInside[];

    private boolean handleByHotPlug(int phoneId, String state) {
        if(getResources().getBoolean(com.android.internal.R.bool.config_hotswapCapable) == false){
            Log.d(TAG, "not support hot swap");
            return false;
        }

        if(mSimStateChangedFlag == ((1 << mPhoneCount) - 1) && IccCardConstants.INTENT_VALUE_ICC_UNKNOWN.equals(state)){
            return true;
        }

        boolean simInside = mSimInside[phoneId];
        mSimInside[phoneId] = !IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(state);
        Log.d(TAG, "simInside = " + simInside + ", mSimInside[" + phoneId + "] = " + mSimInside[ phoneId] + ", mSimStateChangedFlag = " + mSimStateChangedFlag);

        boolean plugIn = !simInside && mSimInside[phoneId];
        boolean plugOut = simInside && !mSimInside[phoneId];

        if ((mSimStateChangedFlag == ((1 << mPhoneCount) - 1)) && (plugIn || plugOut)) {
            /*SPRD: No need powering on/off radio when hot plug under single sim mode @{*/
            if(mPhoneCount == 1){
                Log.d(TAG,"single sim mode");
                return true;
            }
            /*@}*/
            // SPRD: Add for bug 453401
            autoSetDefaultSmsVoiceSubId();
            if (mSimInside[phoneId]) {
                Log.d(TAG,"has sim card in slot" + phoneId );
                boolean onlyOneSim = true;
                for (int i = 0; i < mPhoneCount; i++) {
                    if (i != phoneId &&  mSimInside[i] == true) {
                        onlyOneSim = false;
                    }
                }
                if(onlyOneSim){
                    /* SPRD: Modify for bug 453401 @{ */
                    Log.d(TAG, "only has one card, must set to primary card");
                    mRadioTaskManager.updateSimPrioritiesHotPlug();
                    mTelephonyManager.setPrimaryCard(SubscriptionManager.INVALID_PHONE_INDEX);
                    mRadioTaskManager.manualSetPrimaryCard(phoneId);
                    /* @} */
                    return true;
                }
                ContentResolver cr = mContext.getContentResolver();
                boolean enabled = Settings.Global.getInt(cr,
                        Settings.Global.AIRPLANE_MODE_ON, 0) == 0;
                Log.d(TAG,
                        "handleMessage : EVENT_SIM_STATE_CHANGED  enabled = "
                                + enabled);

                int simState = TelephonyManager.from(mContext).getSimState(
                        phoneId);
                Log.d(TAG, "simState = " + simState);

                if (enabled
                        && (simState == TelephonyManager.SIM_STATE_READY || simState == TelephonyManager.SIM_STATE_UNKNOWN)) {
                    Log.d(TAG, "Hot plug in, power on it");
                    mRadioTaskManager.handleHotPlug(phoneId, true);
                }
            } else {
                Log.d(TAG,"no sim card in slot" + phoneId );
                if(phoneId == mTelephonyManager.getPrimaryCard()){
                    Log.d(TAG,"primary card removed");
                    for(int i = 0; i < mPhoneCount; i++){
                        if (i != phoneId && TelephonyManager.from(mContext).getSimState(i) != TelephonyManager.SIM_STATE_ABSENT) {
                            Log.d(TAG,"set new primary card to sim" + i);
                            mRadioTaskManager.updateSimPrioritiesHotPlug();
                            mRadioTaskManager.manualSetPrimaryCard(i);
                            return true;
                        }
                    }
                    /* @} */
                    return false;
                }
                Log.d(TAG,"antoher radio is on so turn off the radio for slot" + phoneId);
                mRadioTaskManager.handleHotPlug(phoneId, false);
            }
            return true;
        }
        return false;
    }
    /*@}*/
}
