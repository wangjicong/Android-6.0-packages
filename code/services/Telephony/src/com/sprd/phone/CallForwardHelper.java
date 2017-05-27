package com.sprd.phone;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.phone.PhoneGlobals;

public class CallForwardHelper {
    private static final String TAG = CallForwardHelper.class.getSimpleName();
    private static CallForwardHelper mInstance;
    private static final String KEY_ICC_ID = "cfu_icc_id";
    private static final String KEY_CFU_VOICE_VALUE = "cfu_voice_value";

    private PhoneGlobals mApplication;
    private SharedPreferences mPreference;
    private Editor mEditor;
    private int[] mServiceState;
    TelephonyManager mTelephonyManager;
    /* SPRD: modify for bug526598 @{ */
    //final Map<Integer, Integer> mSubInfoMap =
    //        new LinkedHashMap<Integer, Integer>(TelephonyManager.getDefault().getPhoneCount());
    private ArrayMap<Integer, PhoneStateListener> mPhoneStateListeners =
            new ArrayMap<Integer, PhoneStateListener>();
    private boolean mFirstQuery = true;
    /* @} */

    public static CallForwardHelper getInstance() {
        if (mInstance == null) {
            mInstance = new CallForwardHelper();
        }
        return mInstance;
    }

    private CallForwardHelper() {
        mApplication = PhoneGlobals.getInstance();
        int phoneCount = TelephonyManager.from(mApplication).getPhoneCount();
        mServiceState = new int[phoneCount];
        for (int i = 0; i < phoneCount; i++) {
            mServiceState[i] = ServiceState.STATE_POWER_OFF;
        }

        // SPRD: update file mode to default PRIVATE. See bug #462167.
        mPreference = mApplication.getApplicationContext().getSharedPreferences(TAG,
                mApplication.getApplicationContext().MODE_PRIVATE);
        mEditor = mPreference.edit();

        SubscriptionManager.from(mApplication).addOnSubscriptionsChangedListener(
                new OnSubscriptionsChangedListener() {
                    @Override
                    public void onSubscriptionsChanged() {
                        updatePhoneStateListeners();
                    }
                });
    }

    private boolean containsSubId(List<SubscriptionInfo> subInfos, int subId) {
        if (subInfos == null) {
            return false;
        }

        for (int i = 0; i < subInfos.size(); i++) {
            if (subInfos.get(i).getSubscriptionId() == subId) {
                return true;
            }
        }
        return false;
    }

    /* SPRD: modify for bug526598 @{
    protected boolean hasCorrectSubinfo(List<SubscriptionInfo> allSubscriptions) {
        for (SubscriptionInfo info : allSubscriptions) {
            log("hasCorrectSubinfo()        info.subId = " + info.getSubscriptionId());
            if (!mSubInfoMap.containsKey(info.getSubscriptionId())) {
                mSubInfoMap.put(info.getSubscriptionId(), 1);
                return true;
            }
        }
        return false;
    }*/

    protected void updatePhoneStateListeners() {
        // SPRD: modify for bug526598
        TelephonyManager mTelephonyManager = (TelephonyManager) mApplication.getSystemService(
                Context.TELEPHONY_SERVICE);
        List<SubscriptionInfo> subscriptions = SubscriptionManager.from(
                mApplication).getActiveSubscriptionInfoList();

        // Unregister phone listeners for inactive subscriptions.
        /* SPRD: modify for bug526598 @{ */
        // Iterator<Integer> itr = mSubInfoMap.keySet().iterator();
        Iterator<Integer> itr = mPhoneStateListeners.keySet().iterator();
        /* @} */
        while (itr.hasNext()) {
            int subId = itr.next();
            if (subscriptions == null || !containsSubId(subscriptions, subId)) {
                /* SPRD: modify for bug526598 @{ */
                log("set Listening to LISTEN_NONE and removes the listener.");
                // Hide the outstanding notifications.
                mApplication.updateMwi(subId, false);
                mApplication.updateCfi(subId, false);

                // Listening to LISTEN_NONE removes the listener.
                mTelephonyManager.listen(
                        mPhoneStateListeners.get(subId), PhoneStateListener.LISTEN_NONE);
                /* @} */
                itr.remove();
            }
        }

        if (subscriptions == null) {
            subscriptions = Collections.emptyList();
        }

        // Register new phone listeners for active subscriptions.
        /* SPRD: modify for bug526598 @{ */
        for (int i = 0; i < subscriptions.size(); i++) {
            int subId = subscriptions.get(i).getSubscriptionId();
            if (!mPhoneStateListeners.containsKey(subId)) {
                log("register listener for sub[" + subId + "]");
                PhoneStateListener listener = getPhoneStateListener(subId);
                mTelephonyManager.listen(listener,
                        PhoneStateListener.LISTEN_SERVICE_STATE
                                | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR);
                mPhoneStateListeners.put(subId, listener);
            }
        }
        /* @} */
    }

    private PhoneStateListener getPhoneStateListener(int subId) {
        final int phoneId = SubscriptionManager.getPhoneId(subId);
        log("getPhoneStateListener for phone[" + phoneId + "]" + ", [subId: " + subId + "]");

        PhoneStateListener phoneStateListener = new PhoneStateListener(subId) {
            @Override
            public void onCallForwardingIndicatorChanged(boolean cfi) {
                if (mServiceState[phoneId] == ServiceState.STATE_IN_SERVICE) {
                    log("onCallForwardingIndicatorChangedByServiceClass->(phoneId:" + phoneId
                            + ") mServiceState[phoneId]:" + mServiceState[phoneId]);
                    onCfiChanged(cfi, phoneId);
                }
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                /* SPRD: add for bug 510667 @{ */
                int subId = -1;
                Phone phone = PhoneFactory.getPhone(phoneId);
                if (phone != null) {
                    subId = phone.getSubId();
                }
                /* @} */
                if (mTelephonyManager != null && !mTelephonyManager.hasIccCard(subId)) {
                    log("(phoneId" + phoneId + ") card doesn't exist");
                    return;
                }
                mServiceState[phoneId] = serviceState.getState();
                log("(phoneId" + phoneId + ") onServiceStateChanged(), state: "
                        + serviceState.getState());
                switch (serviceState.getState()) {
                    case ServiceState.STATE_OUT_OF_SERVICE:
                    case ServiceState.STATE_POWER_OFF:
                        mApplication.getNotificationMgr().updateCfiVisibility(false, phoneId);
                        break;
                    case ServiceState.STATE_IN_SERVICE:
                        // SPRD: modify for bug526598
                        if (SystemProperties.getInt("persist.sys.callforwarding", 0) == 1
                                || !mFirstQuery) {
                            queryAllCfu(phoneId);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        return phoneStateListener;
    }

    private void onCfiChanged(boolean visible, int phoneId) {
        log("onCfiChanged(): " + visible + ", phoneId = " + phoneId);
        if (phoneId < 0 || phoneId > TelephonyManager.getDefault().getPhoneCount()) {
            return;
        }
        checkIccId(phoneId);
        saveCfiToPreference(visible, phoneId);
        mApplication.getNotificationMgr().updateCfiVisibility(visible, phoneId);
        /* SPRD: modify for bug526598 @{ */
        String savedIccId = mPreference.getString(KEY_ICC_ID + phoneId, null);
        String currentIccId = getCurrentIccId(phoneId);
        if (visible && savedIccId != null && savedIccId.equalsIgnoreCase(currentIccId)) {
            mFirstQuery = false;
        }
        /* @} */
    }

    private void saveCfiToPreference(boolean visible, int phoneId) {
        log("saveCfiToPreference->visible = " + visible + ",phoneId = " + phoneId);
        mEditor.putBoolean(KEY_CFU_VOICE_VALUE + phoneId, visible);
        mEditor.apply();
    }

    private void checkIccId(int phoneId) {
        String savedIccId = mPreference.getString(KEY_ICC_ID + phoneId, null);
        String currentIccId = getCurrentIccId(phoneId);
        if (currentIccId == null) {
            log("checkIccId->currentIccId is null!");
            return;
        }
        if (savedIccId == null || !savedIccId.equalsIgnoreCase(currentIccId)) {
            log("checkIccId->current id is not saved id->savedIccId:" + savedIccId + " currentIccId:"
                    + currentIccId);
            mEditor.putString(KEY_ICC_ID + phoneId, currentIccId);
            mEditor.putBoolean(KEY_CFU_VOICE_VALUE + phoneId, false);
            mEditor.apply();
            if (mServiceState[phoneId] == ServiceState.STATE_IN_SERVICE) {
                PhoneFactory.getPhone(phoneId).getCallForwardingOption(
                        CommandsInterface.CF_REASON_UNCONDITIONAL, null);
            }
        }
    }

    private String getCurrentIccId(int phoneId) {
        String iccId = null;
        Phone phone = PhoneFactory.getPhone(phoneId);
        /* SPRD: add for bug 529444 to avoid NullPoiontException @{ */
        if (phone != null) {
            IccCard iccCard = phone.getIccCard();
            if (iccCard != null) {
                IccRecords iccRecords = iccCard.getIccRecords();
                if (iccRecords != null) {
                    iccId = iccRecords.getIccId();
                }
            }
        }
        /* @} */
        return iccId;
    }

    private void queryAllCfu(int phoneId) {
        log("queryAllCfu(),phoneId = " + phoneId);
        checkIccId(phoneId);
        boolean showVoiceCfi = mPreference.getBoolean(KEY_CFU_VOICE_VALUE + phoneId, false);
        mApplication.getNotificationMgr().updateCfiVisibility(showVoiceCfi, phoneId);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
