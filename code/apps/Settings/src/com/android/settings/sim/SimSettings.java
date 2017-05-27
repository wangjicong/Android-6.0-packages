/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.sim;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.SearchIndexableResource;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.provider.Settings;

import android.os.UserHandle;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.R;
import android.os.SystemProperties;

import com.android.internal.telephony.TeleUtils;
import com.android.internal.telephony.TelephonyProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import com.sprd.android.config.OptConfig;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = false;

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    private static final String STANDBY_DIALOG_TAG = "standby_dialog";
    private static final String DATA_DIALOG_TAG = "data_dialog";
    private static final String PROGRESS_DIALOG_TAG = "progress_dialog";
    private static final String KEY_PRIMARY_CARD = "sim_primary_card";
    // SPRD: add new feature for data switch on/off
    private static final String KEY_ACTIVITIES = "sim_activities";
    public static final String EXTRA_SLOT_ID = "slot_id";

    /**
     * By UX design we use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     * mSelectableSubInfos is the list of SubInfos that a user can select for data, calls, and SMS.
     */
    //SPRD: modify for bug497338
    //private List<SubscriptionInfo> mAvailableSubInfos = null;
    private List<SubscriptionInfo> mSubInfoList = null;
    private List<SubscriptionInfo> mSelectableSubInfos = null;
    //SPRD: modify for bug497338
    private List<SubscriptionInfo> mAvailableSubInfoList = null;
    private PreferenceScreen mSimCards = null;
    // SPRD: add new feature for data switch on/off
    private DataPreference mDataPreference;
    private SubscriptionManager mSubscriptionManager;
    // SPRD: add option to enable/disable sim card
    private TelephonyManager mTelephonyManager = null;
    private int mNumSlots;
    private Context mContext;

    /* SPRD: add option to enable/disable sim card @{ */
    private boolean mNeedPromptDataChange = false;
    private PorgressDialogFragment mProgressDialogFragment = null;
    private DialogFragment mAlertDialogFragment = null;
    private FragmentManager mFragmentManager;
    /* @} */

    /* SPRD:  modify for bug 514144 @{ */
    private int mProgressShow = 1;
    private boolean mhasProgressShow = false;
    /* @} */

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SIM;
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        mContext = getActivity();

        mSubscriptionManager = SubscriptionManager.from(getActivity());
        /* SPRD: add option to enable/disable sim card @{ */
        mFragmentManager = getFragmentManager();
        mTelephonyManager = TelephonyManager.from(getActivity());
        /* @} */
        // SPRD: add new feature for data switch on/off
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        addPreferencesFromResource(R.xml.sim_settings);

        mNumSlots = tm.getSimCount();
        mSimCards = (PreferenceScreen)findPreference(SIM_CARD_CATEGORY);
        //SPRD: modify for bug497338
        //mAvailableSubInfos = new ArrayList<SubscriptionInfo>(mNumSlots);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        //SPRD: modify for bug497338
        mAvailableSubInfoList = getActiveSubInfoList();
        /* SPRD: add new feature for data switch on/off @{ */
        PreferenceCategory simPreferenceCatergory = (PreferenceCategory )findPreference(KEY_ACTIVITIES);
        mDataPreference = (DataPreference)new DataPreference(getActivity());
        mDataPreference.setOrder(0);
        mDataPreference.setKey(KEY_CELLULAR_DATA);
        simPreferenceCatergory.addPreference(mDataPreference);
        /* @} */
        SimSelectNotification.cancelNotification(getActivity());
        /* SPRD: add option remember default SMS/Voice sub id @{ */
        //SPRD: modify for bug497338
        if (mAvailableSubInfoList.size() > 1) {
            initSimManagerSharedPreferences();
        }
        /* @} */
        /* SPRD: modify for bug513637 @{ */
        if (TeleUtils.isSuppSetDataAndPrimaryCardBind()) {
            simPreferenceCatergory.removePreference(findPreference(KEY_PRIMARY_CARD));
        }
        /* @} */
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged:");
            //SPRD: modify for bug497338
            mAvailableSubInfoList = getActiveSubInfoList();
            updateSubscriptions();
        }
    };

    private void updateSubscriptions() {
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        for (int i = 0; i < mNumSlots; ++i) {
            Preference pref = mSimCards.findPreference("sim" + i);
            if (pref instanceof SimPreference) {
                mSimCards.removePreference(pref);
            }
        }
        //SPRD: modify for bug497338
        //mAvailableSubInfos.clear();
        mSelectableSubInfos.clear();

        for (int i = 0; i < mNumSlots; ++i) {
            final SubscriptionInfo sir = mSubscriptionManager
                    .getActiveSubscriptionInfoForSimSlotIndex(i);
            SimPreference simPreference = new SimPreference(mContext, sir, i);
            simPreference.setOrder(i-mNumSlots);
            mSimCards.addPreference(simPreference);
            //SPRD: modify for bug497338
            //mAvailableSubInfos.add(sir);
            if (sir != null) {
                mSelectableSubInfos.add(sir);
            }
        }
        updateAllOptions();
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        final int prefSize = mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference)pref).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
     // SPRD: add option of selecting primary card
        /* SPRD: modify for bug513637 @{ */
        if (!TeleUtils.isSuppSetDataAndPrimaryCardBind()) {
            updatePrimaryCardValues();
        }
        /* @} */
    }

    private void updateSmsValues() {
        final Preference simPref = findPreference(KEY_SMS);
        final SubscriptionInfo sir = mSubscriptionManager.getDefaultSmsSubscriptionInfo();
        simPref.setTitle(R.string.sms_messages_title);
        if (DBG) log("[updateSmsValues] mSubInfoList=" + mSubInfoList);

        if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
        } else if (sir == null) {
            if(OptConfig.SUN_SMS_ALWAYS_CHOOSE){
	            /* SPRD: add option 'Always prompt' for SMS PICK @{ */
	            if (SubscriptionManager.MAX_SUBSCRIPTION_ID_VALUE ==
	                    SubscriptionManager.getDefaultSmsSubId()) {
	                simPref.setSummary(R.string.sim_calls_ask_first_prefs_title); //yanghua add for SMS default Ask every time
	            } else {
	                simPref.setSummary(R.string.sim_selection_required_pref);
	            }
	            /* @} */
            }
            else
            {
	            simPref.setSummary(R.string.sim_selection_required_pref);
            }
        }
        /* SPRD: add option to enable/disable sim card @{ */
        //simPref.setEnabled(mSelectableSubInfos.size() >= 1);
        //SPRD: modify for bug497338
        simPref.setEnabled(mAvailableSubInfoList.size() > 1);
        /* @} */
    }

    /* SPRD: add new feature for data switch on/off @{ */
    private void updateCellularDataValues() {
        mDataPreference.update();
    }
    /* @} */

    private void updateCallValues() {
        final Preference simPref = findPreference(KEY_CALLS);
        final TelecomManager telecomManager = TelecomManager.from(mContext);
        final PhoneAccountHandle phoneAccount =
            telecomManager.getUserSelectedOutgoingPhoneAccount();
        final List<PhoneAccountHandle> allPhoneAccounts =
            telecomManager.getCallCapablePhoneAccounts();
        //SPRD: modify for bug529724
        PhoneAccount pa = telecomManager.getPhoneAccount(phoneAccount);
        /* SPRD: modify for bug494106 @{ */
        final boolean isPhoneAccountAvialable = (phoneAccount != null) && (pa != null);
        simPref.setTitle(R.string.calls_title);
        simPref.setSummary(!isPhoneAccountAvialable
                ? mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                : (String)pa.getLabel());
        /* @} */
        /* SPRD: add option to enable/disable sim card @{ */
        //simPref.setEnabled(allPhoneAccounts.size() > 1);
        //SPRD: modify for bug497338
        simPref.setEnabled(mAvailableSubInfoList.size() > 1);
        /* @} */
    }

    /* SPRD: add option of selecting primary card @{ */
    private void updatePrimaryCardValues() {
        final Preference simPref = findPreference(KEY_PRIMARY_CARD);
        final int primaryCard = mTelephonyManager.getPrimaryCard();
        final SubscriptionInfo sir = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(primaryCard);
        simPref.setTitle(R.string.select_primary_card);
        if (DBG) log("[updatePrimaryCardValues] mSubInfoList=" + mSubInfoList);

        if (sir != null) {
            simPref.setSummary(sir.getDisplayName().toString().trim().isEmpty()?
                    "SIM"+(sir.getSimSlotIndex()+1):sir.getDisplayName());
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
        }
        /* SPRD: modify by bug459921@{ */
        if (mSubscriptionManager.getActiveSubscriptionInfoCount() <= 1 || OptConfig.SUN_SUBCUSTOM_C7356_TYC_S417_WVGA) { //yanghua for C7356_TYC_S417
            simPref.setEnabled(false);
        } else {
            // SPRD: [Lastest:bug484116 History:bug429579] Not allowed to switch primary card if any sim card have been disabled.
            //SPRD: modify for bug497338
            simPref.setEnabled(mAvailableSubInfoList.size() > 1);
        }
    }
    /* @} */

    @Override
    public void onResume() {
        super.onResume();
        /* SPRD:  modify for bug 493220 @{ */
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.RADIO_OPERATION), true,
                mRadioBusyObserver, UserHandle.USER_OWNER);   // SPRD:  modify for bug 508104
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.MOBILE_DATA), true,
                mMobileDataObserver,UserHandle.USER_OWNER);   // SPRD:  modify for bug 508104
        /*}@*/

        /* SPRD:  modify for bug 514144 @{ */
        if(mProgressDialogFragment != null){
            HasShowProgress();
        }
        /*}@*/
        // SPRD: add option to enable/disable sim card
        mNeedPromptDataChange = false;
        updatePreferencesState();
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        //SPRD: modify for bug497338
        //updateSubscriptions();
    }

    /* SPRD: add option to enable/disable sim card @{ */
    private void showStandbyAlertDialog(final int phoneId, final boolean onOff) {
        StandbyAlertDialogFragment.show(SimSettings.this, phoneId, onOff);
    }

    private void showProgressDialog() {
        Log.d(TAG, "show progressing dialog...");
        //FragmentManager fm = getFragmentManager();
        if (getActivity() != null && getActivity().isResumed()) {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            mProgressDialogFragment = new PorgressDialogFragment();
            mProgressDialogFragment.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
            mProgressDialogFragment.setCancelable(false);
            /* SPRD: modify for bug493042 @{ */
            mProgressDialogFragment.setTargetFragment(this, 0);
            /* @} */
            mProgressDialogFragment.show(
                    transaction, PROGRESS_DIALOG_TAG);
        }
    }

    /* SPRD: modify for bug493042 @{ */
    private void resetProgressDialogFragment(PorgressDialogFragment dialogFragment) {
        mProgressDialogFragment = dialogFragment;
    }
    /* @} */
    /* SPRD: modify for bug492873 @{ */
    private void resetAlertDialogFragment(DialogFragment dialogFragment) {
        mAlertDialogFragment = dialogFragment;
    }
    /* @} */

    private void showDataAlertDialog(String msg) {
        DataAlertDialogFragment.show(SimSettings.this, msg);
    }
    /* @} */

    @Override
    public void onPause() {
        super.onPause();
        /* SPRD:  modify for bug 493220 @{ */
        getContentResolver().unregisterContentObserver(mRadioBusyObserver);
        // SPRD: add new feature for data switch on/off
        getContentResolver().unregisterContentObserver(mMobileDataObserver);
        /*}@*/
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mHandler.removeMessages(mProgressShow);
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        // Disable Sim selection for Data when voice call is going on as changing the default data
        // sim causes a modem reset currently and call gets disconnected
        // ToDo : Add subtext on disabled preference to let user know that default data sim cannot
        // be changed while call is going on
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);
             final Preference pref = findPreference(KEY_CELLULAR_DATA);
            if (pref != null) {
                final boolean ecbMode = SystemProperties.getBoolean(
                        TelephonyProperties.PROPERTY_INECM_MODE, false);
                /* SPRD: Modify the Bug 492127 @{ */
                if (mSubscriptionManager.getActiveSubscriptionIdList().length <= 1) {
                    pref.setEnabled(false);
                } else {
                    //SPRD: modify for bug497338
                    pref.setEnabled((state == TelephonyManager.CALL_STATE_IDLE) && !ecbMode && mAvailableSubInfoList.size() > 0);
                }
                /* @} */
            }
        }
    };

    @Override
    public void onDestroy() {// SPRD: add option to enable/disable sim card
        super.onDestroy();
        //updatePreferencesState();
    };

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        final Context context = mContext;
        Intent intent = new Intent(context, SimDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (preference instanceof SimPreference) {
            // SPRD: modify for bug500268
            SimFragmentDialog.show(SimSettings.this, ((SimPreference) preference).getSlotId());
        } else if (preference instanceof DataPreference) {
            // SPRD: add new feature for data switch on/off
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_CALLS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.CALLS_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_SMS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.SMS_PICK);
            context.startActivity(intent);
        }
        /* SPRD: add option of selecting primary card @{ */
        else if (findPreference(KEY_PRIMARY_CARD) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.PRIMARY_PICK);
            context.startActivity(intent);
        }
        /* @} */

        return true;
    }

    /* SPRD: add option to enable/disable sim card @{ */
    private ContentObserver mRadioBusyObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() != null) {
                updateSubscriptions();
                updatePreferencesState();
                if (!mTelephonyManager.isRadioBusy() && mNeedPromptDataChange) {
                    mNeedPromptDataChange = false;
                    /* SPRD: modify for bug497338 @{ */
                    if (mAvailableSubInfoList.size() == 1) {
                        int availableSubId = mAvailableSubInfoList.get(0).getSubscriptionId();
                    /*}@*/
                        String msg = getString(R.string.toggle_data_change_message,
                        SubscriptionManager.getPhoneId(availableSubId)+ 1);
                        showDataAlertDialog(msg);
                    }
                }
            }
        }
    };

    private void updatePreferencesState() {
        if (!mTelephonyManager.isRadioBusy() && mhasProgressShow) {  // SPRD:  modify for bug 514144
            if (mProgressDialogFragment != null) {
                mProgressDialogFragment.dismissAllowingStateLoss(); // SPRD: modify for bug500791
                mProgressDialogFragment = null;
                mhasProgressShow = false;
            }
        }

        if (mTelephonyManager.isAirplaneModeOn()) {
            if(mAlertDialogFragment != null) {
                //getFragmentManager().get
                mAlertDialogFragment.dismissAllowingStateLoss(); // SPRD: modify for bug500791
                mAlertDialogFragment = null;
            }
            if (mProgressDialogFragment != null) {
                mProgressDialogFragment.dismissAllowingStateLoss(); // SPRD: modify for bug500791
                mProgressDialogFragment = null;
            }
        }
        getPreferenceScreen().setEnabled(
                !mTelephonyManager.isRadioBusy() && !mTelephonyManager.isAirplaneModeOn());
    }

    /* SPRD: set current default voice/sms sub id */
    /* as multi sim active default voice/sms sub id  @{*/
    private void initSimManagerSharedPreferences() {
        SubscriptionManager subscriptionManager
                = SubscriptionManager.from(getActivity());
        long smsSubId = mTelephonyManager.getMultiSimActiveDefaultSmsSubId();
        long voiceSubId = mTelephonyManager.getMultiSimActiveDefaultVoiceSubId();
        Log.d(TAG, "initSimManagerSharedPreferences, smsSubId: " + smsSubId + ",  voiceSubId: "
                + voiceSubId);
        if (smsSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mTelephonyManager.setMultiSimActiveDefaultSmsSubId(subscriptionManager
                    .getDefaultSmsSubId());
        }
        if (voiceSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            mTelephonyManager.setMultiSimActiveDefaultVoiceSubId(subscriptionManager
                    .getDefaultVoiceSubId());
        }
    }
    /* @} */

    private class SimPreference extends Preference {
        private SubscriptionInfo mSubInfoRecord;
        private int mSlotId;
        Context mContext;
        // SPRD: add option to enable/disable sim card
        private Switch mSwitch;

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);
            // SPRD: use custom layout: add Switch to enable/disable sim
            setLayoutResource(R.layout.sim_preference_ex);

            mContext = context;
            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
        }

        /* SPRD: add option to enable/disable sim card @{ */
        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            mSwitch = (Switch) view.findViewById(R.id.universal_switch);
            mSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    boolean standby = mTelephonyManager.isSimStandby(mSlotId);
                    if (standby != isChecked) {
                        showStandbyAlertDialog(mSlotId, isChecked);
                    }
                }
            });
            updateStandbyState();
        }

        private void updateStandbyState() {
            if (mSwitch != null) {
                if (mSubInfoRecord != null) {
                    boolean standby = mTelephonyManager.isSimStandby(mSlotId);
                    mSwitch.setChecked(standby);
                    boolean canSetSimStandby = (mTelephonyManager.getSimState(
                            mSlotId) == TelephonyManager.SIM_STATE_READY || !standby)
                            && !mTelephonyManager.isRadioBusy()
                            && !mTelephonyManager.isAirplaneModeOn();
                    mSwitch.setEnabled(canSetSimStandby);
                    mSwitch.setVisibility(View.VISIBLE);
                } else {
                    mSwitch.setVisibility(View.GONE);
                }
            }
        }
        /* @} */


        public void update() {
            //SPRD: modify for Bug494140
            if (!isAdded()) {
                return;
            }

            final Resources res = mContext.getResources();

			/*wangxing 20160628 modify for rename sim1 sim2 start @{ */
			String s = String.format(getResources().getString(R.string.sim_editor_title), (mSlotId + 1));
            s = s.replace("1","M");
            s = s.replace("2","S");
            if(OptConfig.SUNVOV_CUSTOM_C7301_XLL_FWVGA){
            	setTitle(s);
            }else{
                setTitle(String.format(mContext.getResources()
                    .getString(R.string.sim_editor_title), (mSlotId + 1)));
            }
			/*wangxing 20160628 modify for rename sim1 sim2 end @} */
			
            if (mSubInfoRecord != null) {// SPRD: add option to enable/disable sim card
                if (!mTelephonyManager.isSimStandby(mSlotId)) {
                    setSummary(R.string.not_stand_by);
                    setFragment(null);
                    setEnabled(false);
                } else {
                    if (TextUtils.isEmpty(getPhoneNumber(mSubInfoRecord))) {
                        setSummary(mSubInfoRecord.getDisplayName());
                    } else {
                        setSummary(mSubInfoRecord.getDisplayName() + " - " +
                                getPhoneNumber(mSubInfoRecord));
                    }
                    setEnabled(!mTelephonyManager.isRadioBusy()
                            && !mTelephonyManager.isAirplaneModeOn()
                            // SPRD: modify the bug494142
                            && mTelephonyManager.getSimState(mSubInfoRecord
                                    .getSimSlotIndex()) == TelephonyManager.SIM_STATE_READY);
                }
                setIcon(new BitmapDrawable(res, (mSubInfoRecord.createIconBitmap(mContext))));
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
            // SPRD: add option to enable/disable sim card
            updateStandbyState();
        }

        private int getSlotId() {
            return mSlotId;
        }
    }

    /* SPRD: add new feature for data switch on/off @{ */
    public class DataPreference extends Preference {
        Context mContext;
        Switch mDataSwitch;
        public DataPreference(Context context) {
            super(context);
            mContext = context;
            setLayoutResource(R.layout.sim_preference_ex);
            update();
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            mDataSwitch = (Switch) view.findViewById(R.id.universal_switch);
            mDataSwitch.setVisibility(View.VISIBLE);
            final int dataSubId = SubscriptionManager.getDefaultDataSubId();
            mDataSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    boolean isDataEnable = mTelephonyManager.getDataEnabled(dataSubId);
                    if (isDataEnable != isChecked) {
                        // SPRD: [bug475942] Remain data status set by user and restore it even when sim changed.
                        Settings.Global.putInt(mContext.getContentResolver(),
                                Settings.Global.DATA_REMAIN_UNCHANGED, isDataEnable ? 0 : 1);
                        mTelephonyManager.setDataEnabled(dataSubId, isChecked);
                    }
                }
            });
            updateDataSwitch(dataSubId);
        }

        public void updateDataSwitch(int subId) {
            Log.d(TAG,"mDataSwitch updateDataSwitch subId" + subId);
            if (mDataSwitch != null) {
                boolean isDataEnable = mTelephonyManager.getDataEnabled(subId);
                int phoneId = SubscriptionManager.getPhoneId(subId);
                mDataSwitch.setChecked(isDataEnable);
                boolean canSetDataEnable = (SubscriptionManager
                        .getSimStateForSlotIdx(phoneId) == TelephonyManager.SIM_STATE_READY)
                        && !mTelephonyManager.isRadioBusy()
                        && !mTelephonyManager.isAirplaneModeOn()
                        && mTelephonyManager.isSimStandby(SubscriptionManager.getSlotId(subId));
                mDataSwitch.setEnabled(canSetDataEnable);
            }
        }

        public void update() {
            final  SubscriptionInfo sir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
            setTitle(R.string.cellular_data_title);
            if (DBG) log("[update DataPreference] mSubInfoList=" + mSubInfoList);

            if (sir != null) {
                setSummary(sir.getDisplayName().toString().trim().isEmpty()?
                        "SIM"+(sir.getSimSlotIndex()+1):sir.getDisplayName());
                updateDataSwitch(sir.getSubscriptionId());
            } else if (sir == null) {
                setSummary(R.string.sim_selection_required_pref);
            }
            if (mSubscriptionManager.getActiveSubscriptionInfoCount() <= 1) {
                setEnabled(false);
            } else {
                //SPRD: modify for bug497338
                setEnabled(mAvailableSubInfoList.size() > 0);
            }
        }
    }

    private ContentObserver mMobileDataObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateCellularDataValues();
        }

    };
    /* @} */


    // Returns the line1Number. Line1number should always be read from TelephonyManager since it can
    // be overridden for display purposes.
    private String getPhoneNumber(SubscriptionInfo info) {
        final TelephonyManager tm =
            (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1NumberForSubscriber(info.getSubscriptionId());
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    if (Utils.showSimCardTile(context)) {
                        SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.sim_settings;
                        result.add(sir);
                    }

                    return result;
                }
            };

    /* SPRD: add option to enable/disable sim card @{ */
    private List<SubscriptionInfo> getActiveSubInfoList() {
        /* SPRD: modify for avoid null point exception @{ */
        if (mSubscriptionManager == null) {
            return new ArrayList<SubscriptionInfo>();
        }
        /* @} */
        List<SubscriptionInfo> availableSubInfoList = mSubscriptionManager
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

    public static class PorgressDialogFragment extends DialogFragment {
        View v;
        TextView mMessageView;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            //return super.onCreateView(inflater, container, savedInstanceState);
            v = inflater.inflate(R.layout.progress_dialog_fragment_ex, container, false);
            //ProgressBar mProgress = (ProgressBar) v.findViewById(R.id.progress);
            mMessageView = (TextView) v.findViewById(R.id.message);
            mMessageView.setText(getResources().getString(R.string.primary_card_switching));
            //setView(view);
            return v;
        }

        /* SPRD: modify for bug493042 @{ */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (getTargetFragment() != null) {
                ((SimSettings) getTargetFragment()).resetProgressDialogFragment(this);
            }
            return super.onCreateDialog(savedInstanceState);
        }
        /* @} */

    }

    /* SPRD:  modify for bug 514144 @{ */
    Handler mHandler = new Handler() {
                      public void handleMessage(Message msg) {
                          mhasProgressShow = true;
                          updatePreferencesState();
                      };
       };
    private void HasShowProgress(){
        mHandler.sendEmptyMessageDelayed(mProgressShow, 5000);
    }
    /* @} */

    public static class StandbyAlertDialogFragment extends DialogFragment {
        private static final String SAVE_PHONE_ID = "phoneId";
        private static final String SAVE_ON_OFF = "onOff";
        private int mPhoneId;
        private boolean mOnOff;
        private boolean mProgressShow = false;

        public static void show(SimSettings parent, int phoneId, boolean onOff) {
            if (!parent.isAdded()) return;
            StandbyAlertDialogFragment dialog = new StandbyAlertDialogFragment();
            dialog.mPhoneId = phoneId;
            dialog.mOnOff = onOff;
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), STANDBY_DIALOG_TAG);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mPhoneId = savedInstanceState.getInt(SAVE_PHONE_ID);
                mOnOff = savedInstanceState.getBoolean(SAVE_ON_OFF);
            }
            final SimSettings sft = (SimSettings) getTargetFragment();
            /* SPRD: modify for bug492873 @{ */
            if (sft == null) {
                Log.d(TAG, "StandbyAlertDialogFragment getTargetFragment failure!!!");
                return super.onCreateDialog(savedInstanceState);
            }
            sft.resetAlertDialogFragment(this);
            /* @} */
            final TelephonyManager telephonyManager = TelephonyManager.from(getActivity());
            final SubscriptionManager subscriptionManager = SubscriptionManager.from(getActivity());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.proxy_error);
            builder.setMessage(R.string.stand_by_set_changed_prompt);
            builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    telephonyManager.setSimStandby(mPhoneId, mOnOff);
                    if ((!mOnOff && mPhoneId == subscriptionManager.getDefaultDataPhoneId()
                            || mOnOff && !telephonyManager
                            .isSimStandby(subscriptionManager.getDefaultDataPhoneId()))
                            && telephonyManager.getDataEnabled()) {
                        sft.mNeedPromptDataChange = true;
                    } else {
                        sft.mNeedPromptDataChange = false;
                    }
                    mProgressShow = true;
                    sft.showProgressDialog();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            /* SPRD: modify for bug492873 @{ */
            if (getTargetFragment() != null) {
                ((SimSettings) getTargetFragment()).updateSimSlotValues();
                /* SPRD:  modify for bug 514144 @{ */
                if(mProgressShow){
                    ((SimSettings) getTargetFragment()).HasShowProgress();
                }
                /* @} */
            }
            /* @} */
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(SAVE_PHONE_ID, mPhoneId);
            outState.putBoolean(SAVE_ON_OFF, mOnOff);
        }
    }

    public static class DataAlertDialogFragment extends DialogFragment {
        private static final String SAVE_MSG = "msg";
        private String mMsg;

        public static void show(SimSettings parent, String msg) {
            if (!parent.isAdded()) return;
            //FragmentTransaction transaction = getFragmentManager().beginTransaction();
            DataAlertDialogFragment dialog = new DataAlertDialogFragment();
            dialog.mMsg = msg;
            dialog.setTargetFragment(parent, 0);
            dialog.showAllowingStateLoss(parent.getFragmentManager(), DATA_DIALOG_TAG);//SPRD: modify for bug544907
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mMsg = savedInstanceState.getCharSequence(SAVE_MSG).toString();
            }
            /* SPRD: modify for bug492873 @{ */
            if (getTargetFragment() != null) {
                ((SimSettings) getTargetFragment()).resetAlertDialogFragment(this);
                ((SimSettings) getTargetFragment()).mNeedPromptDataChange = false;
            }
            /* @} */
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.proxy_error);
            builder.setMessage(mMsg);
            builder.setNegativeButton(R.string.okay, null);
            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putCharSequence(SAVE_MSG, (CharSequence)mMsg);
        }
    }
    /* @} */

}
