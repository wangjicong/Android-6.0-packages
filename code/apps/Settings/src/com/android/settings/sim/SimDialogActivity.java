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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;
import android.text.TextUtils;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.inputmethod.plugin.SimLangListUtils;
import com.sprd.settings.SettingsPluginsHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.util.Log;
//Kalyy add for 2G3G4G
import com.sprd.android.config.OptConfig;
import android.content.Intent;
//Kalyy add for 2G3G4G

public class SimDialogActivity extends Activity {
    private static String TAG = "SimDialogActivity";

    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    public static final int INVALID_PICK = -1;
    public static final int DATA_PICK = 0;
    public static final int CALLS_PICK = 1;
    public static final int SMS_PICK = 2;
    public static final int PREFERRED_PICK = 3;
    /* SPRD: add option for selecting primary card @{ */
    public static final int PRIMARY_PICK = 4;
    /* SPRD: add for Orange sim language @{ */
    public static final int LANG_PICK = 5;
    public static final int SHOW_APN_DIALOG = 6;
    private int mDialogType = INVALID_PICK;
    public static final String PRIMARYCARD_PICK_CANCELABLE = "show_after_boot";
    private boolean mIsForeground = false;
    private boolean mIsPrimaryCardCancelable = false;
    private PorgressDialogFragment mProgerssDialogFragment = null;
    private TelephonyManager mTelephonyManager = null;
    private Dialog mSimChooseDialog = null;
    private SubscriptionManager mSubscriptionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* SPRD: add option for selecting primary card @{ */
        mTelephonyManager =  TelephonyManager.from(SimDialogActivity.this);
        processIntent();
        /* SPRD: modify for bug508651 @{ */
        final IntentFilter intentFilter = new IntentFilter(
                TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(PhoneConstants.ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        /* @} */
        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.RADIO_OPERATION), true,
                mRadioBusyObserver);
        /* @} */
    }

    /* SPRD: modify for bug526139 @{ */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = getIntent().getExtras();
        int OldDialogType = extras.getInt(DIALOG_TYPE_KEY, INVALID_PICK);
        /* SPRD:modify for Bug555948,the primary card dialog was dismissed after SMS dialog create.@{ */
        mIsPrimaryCardCancelable = extras.getBoolean(PRIMARYCARD_PICK_CANCELABLE);
        setIntent(intent);
        extras = getIntent().getExtras();
        int dialogType = extras.getInt(DIALOG_TYPE_KEY, INVALID_PICK);
        if (dialogType != OldDialogType && mSimChooseDialog != null && !mIsPrimaryCardCancelable) {
            /* @} */
            mSimChooseDialog.dismiss();
            processIntent();
        }
    }

    private void processIntent() {
        final Bundle extras = getIntent().getExtras();
        /*SPRD: add for 492893, fuzz test @{*/
        if (extras == null) {
            Log.e(TAG, "invalid extras null");
            finish();
            return;
        }
        /*@}*/

        final int dialogType = extras.getInt(DIALOG_TYPE_KEY, INVALID_PICK);
        mIsPrimaryCardCancelable = extras.getBoolean(PRIMARYCARD_PICK_CANCELABLE);
        switch (dialogType) {
            case DATA_PICK:
            case CALLS_PICK:
            case SMS_PICK:
            /* SPRD: add option for selecting primary card @{ */
            case PRIMARY_PICK:
                mDialogType = dialogType;
                mSimChooseDialog = createDialog(this, mDialogType);
                mSimChooseDialog.show();
                break;
            /* @} */
            /* SPRD: add for Orange sim language @{ */
            case LANG_PICK:
                mDialogType = dialogType;
                break;
            /* @} */
            case PREFERRED_PICK:
                displayPreferredDialog(extras.getInt(PREFERRED_SIM));
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }
    }
    /* @} */

    private void displayPreferredDialog(final int slotId) {
        final Resources res = getResources();
        final Context context = getApplicationContext();
        final SubscriptionInfo sir = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoForSimSlotIndex(slotId);

        if (sir != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.sim_preferred_title);
            alertDialogBuilder.setMessage(res.getString(
                        R.string.sim_preferred_message, sir.getDisplayName()));

            alertDialogBuilder.setPositiveButton(R.string.yes, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    final int subId = sir.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandle =
                            subscriptionIdToPhoneAccountHandle(subId);
                    setDefaultDataSubId(context, subId);
                    setDefaultSmsSubId(context, subId);
                    setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                    finish();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.no, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    finish();
                }
            });
            alertDialogBuilder.create().show();
        } else {
            finish();
        }
    }

    private static void setDefaultDataSubId(final Context context, final int subId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultDataSubId(subId);
        /* SPRD: add new feature for data switch on/off @{ */
        TelephonyManager teleMgr = TelephonyManager.from(context);
        if (!teleMgr.getDataEnabled(subId)) {
            teleMgr.setDataEnabled(subId,true);
        }
        /* @} */
        Toast.makeText(context, R.string.data_switch_started, Toast.LENGTH_LONG).show();
    }

    /**
    * SPRD: add new feature for data switch on/off
    */
    private  void disableDataForOtherSubscriptions(Context context,int subId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        TelephonyManager teleMgr = TelephonyManager.from(context);
        List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (subInfo.getSubscriptionId() != subId) {
                    teleMgr.setDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    }


    private static void setDefaultSmsSubId(final Context context, final int subId) {
        /* SPRD: add option set multi sim active default sms sub id @{ */
        //final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        //subscriptionManager.setDefaultSmsSubId(subId);
        TelephonyManager tm = TelephonyManager.from(context);
        tm.setMultiSimActiveDefaultSmsSubId(subId);
        /* @} */
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        final TelephonyManager telephonyManager = TelephonyManager.from(this);
        final Iterator<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == telephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }

        return null;
    }

    public Dialog createDialog(final Context context, final int id) {
        dismissSimChooseDialog();
        final ArrayList<String> list = new ArrayList<String>();
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final List<SubscriptionInfo> subInfoList =
            subscriptionManager.getActiveSubscriptionInfoList();
        final int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();
        final StatusBarManager statusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {
                        // SPRD: modify by add radioButton on set defult sub id
                        setDefaltSubIdByDialogId(context, id, value, subInfoList);
                    }
                };

        Dialog.OnKeyListener keyListener = new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                    /* SPRD: add option for selecting primary card @{ */
                    if (keyCode == KeyEvent.KEYCODE_BACK &&
                            !mIsPrimaryCardCancelable) {
                        finish();
                    }
                    /* @} */
                    return true;
                }
            };

        ArrayList<SubscriptionInfo> callsSubInfoList = new ArrayList<SubscriptionInfo>();
        if (id == CALLS_PICK) {
            final TelecomManager telecomManager = TelecomManager.from(context);
            final TelephonyManager telephonyManager = TelephonyManager.from(context);
            final Iterator<PhoneAccountHandle> phoneAccounts =
                    telecomManager.getCallCapablePhoneAccounts().listIterator();

            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            callsSubInfoList.add(null);			
            /* @} */
            while (phoneAccounts.hasNext()) {
                final PhoneAccount phoneAccount =
                        telecomManager.getPhoneAccount(phoneAccounts.next());
                list.add((String)phoneAccount.getLabel());
                int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    final SubscriptionInfo sir = SubscriptionManager.from(context)
                            .getActiveSubscriptionInfo(subId);
                    callsSubInfoList.add(sir);
                } else {
                    callsSubInfoList.add(null);
                }
            }
        } else {
            for (int i = 0; i < selectableSubInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                CharSequence displayName = sir.getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        }

        if(OptConfig.SUN_SMS_ALWAYS_CHOOSE){
        /* SPRD: add option 'Always prompt' for SMS PICK @{ */
        if (id == SMS_PICK) { //yanghua add for SMS default Ask every time
            /* add "always promit" only there are more than one sim card @{ */
            if (subInfoList.size() > 1) {
                list.add(0, getResources().getString(R.string.sim_calls_ask_first_prefs_title));
                subInfoList.add(0, null);
            }
            /* @} */
        }
        /* @} */
        }

        String[] arr = list.toArray(new String[0]);

        // SPRD: modify for bug459003
        AlertDialog.Builder builder = new AlertDialog.Builder(context,
                android.R.style.Theme_Material_Light_Dialog_NoActionBar);

        ListAdapter adapter = new SelectAccountListAdapter(
                id == CALLS_PICK ? callsSubInfoList : subInfoList,
                builder.getContext(),
                R.layout.select_account_list_item,
                arr, id);

        switch (id) {
            case DATA_PICK:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case CALLS_PICK:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case SMS_PICK:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            /* SPRD: add option of selecting primary card @{ */
            case PRIMARY_PICK:
                /* SPRD: add for bug 543820 @{ */
                View titleView = LayoutInflater.from(this).inflate(
                        R.layout.select_primary_card_title, null);
                TextView textview = (TextView) titleView
                        .findViewById(R.id.multi_mode_slot_introduce);
                 if (TelephonyManager.isDeviceSupportLte()) {
                textview.setText(getString(R.string.select_primary_slot_description_4g));
                 }
                textview.setTextColor(Color.BLACK);
                builder.setCustomTitle(titleView);
                /* @} */
                break;
            /* @} */
            /* SPRD: add for Orange sim language @{ */
            case LANG_PICK:
                SimLangListUtils.getInstance(this).setTitle(builder,R.string.sim_card_select_title);
                break;
            /* @} */
            default:
                throw new IllegalArgumentException("Invalid dialog type "
                        + id + " in SIM dialog.");
        }

        Dialog dialog = builder.setAdapter(adapter, selectionListener).create();
        dialog.setOnKeyListener(keyListener);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });

        /* SPRD: add option of selecting primary card @{ */
        if (mIsPrimaryCardCancelable) {
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    statusBarManager.disable(StatusBarManager.DISABLE_NONE);
                }
            });
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            dialog.setCanceledOnTouchOutside(false);
            statusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
        }
        /* @} */

        return dialog;

    }

    /* SPRD: add option of selecting primary card @{ */
    private void showAlertDialog(final int phoneId) {
        dismissSimChooseDialog();
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.attention)
                .setMessage(R.string.whether_switch_primary_card)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mTelephonyManager.setPrimaryCard(phoneId);
                                showProgressingDialog();
                                //Kalyy add for 2G3G4G
                                if(OptConfig.SUN_2G3G4G_SUPPORT){
                                    Intent intent = new Intent("com.android.system.network");
                                    getApplicationContext().sendBroadcast(intent);
                                }
                                //Kalyy add for 2G3G4G
                            }
                        })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                }).create();
         /* SPRD: modify for bug503957 @{ */
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });
        /* }@ */
        alertDialog.show();
    }

    private void showProgressingDialog() {
        Log.d(TAG, "show progressing dialog...");
        dismissProgressDialog();
        FragmentTransaction tr = getFragmentManager().beginTransaction();
        tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        mProgerssDialogFragment = new PorgressDialogFragment();
        mProgerssDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
        mProgerssDialogFragment.setCancelable(false);
        mProgerssDialogFragment.show(tr, "progress_dialog");
    }

    private void dismissProgressDialog() {
        if (mProgerssDialogFragment != null && mProgerssDialogFragment.isVisible() && mIsForeground) {
            Log.d(TAG, "dismiss progressing dialog...");
            mProgerssDialogFragment.dismiss();
            finish();
        }
    }

    /**
    * SPRD: add for set default SMS/Voice/Data sub id by dialog id
    */
    private void setDefaltSubIdByDialogId(
            final Context context, int dialogId, int chooseId, List<SubscriptionInfo> subInfoList){
        final SubscriptionInfo sir;
        mDialogType = INVALID_PICK;

        switch (dialogId) {
            case DATA_PICK:
                sir = subInfoList.get(chooseId);
                 /* SPRD: add new feature for data switch on/off @{ */
                 int currentDataSubId = SubscriptionManager.getDefaultDataSubId();
                 Log.d(TAG,"subId = " + sir.getSubscriptionId()
                        + ",currentDataSubId = " + currentDataSubId);
                 if ( sir.getSubscriptionId() != currentDataSubId ) {
                     /* SPRD: add for Reliance feature of display data confirm dialog @{ */
                     if (SettingsPluginsHelper.getInstance().displayConfirmDataDialog(context,sir,currentDataSubId,this)) {
                         return;
                     }
                     /* @} */
                     Log.d(TAG,"set defalut data connection");
                     setDefaultDataSubId(context, sir.getSubscriptionId());
                     disableDataForOtherSubscriptions(context,sir.getSubscriptionId());
                 }
                 /* @} */
                break;
            case CALLS_PICK:
                final TelecomManager telecomManager =
                        TelecomManager.from(context);
                final List<PhoneAccountHandle> phoneAccountsList =
                        telecomManager.getCallCapablePhoneAccounts();
                setUserSelectedOutgoingPhoneAccount(
                        chooseId < 1 ? null : phoneAccountsList.get(chooseId - 1));
                break;
            case SMS_PICK:
                sir = subInfoList.get(chooseId);
                if(OptConfig.SUN_SMS_ALWAYS_CHOOSE){				
                // SPRD: add option 'Always prompt' for SMS PICK
                setDefaultSmsSubId(context, sir != null ? sir.getSubscriptionId()
                        : SubscriptionManager.MAX_SUBSCRIPTION_ID_VALUE); //yanghua modify for SMS default Ask every time
                }
                else
                {
                setDefaultSmsSubId(context, sir.getSubscriptionId());
                }
                break;
            /* SPRD: add option of selecting primary card @{ */
            case PRIMARY_PICK:
                sir = subInfoList.get(chooseId);
                int selectPrimaryCard = sir.getSimSlotIndex();
                TelephonyManager tm = TelephonyManager.from(SimDialogActivity.this);
                Log.d(TAG, "PRIMARY_PICK lastPrimaryCard = " + tm.getPrimaryCard()
                        + " selectPrimaryCard = " + selectPrimaryCard);

                if (mIsPrimaryCardCancelable) {
                        mTelephonyManager.setPrimaryCard(selectPrimaryCard);
                } else if (selectPrimaryCard != tm.getPrimaryCard()) {
                    Log.d(TAG, "selectPrimaryCard != tm.getPrimaryCard()");
                    showAlertDialog(selectPrimaryCard);
                    return;
                }
            break;
            /* @} */
            /* SPRD: add for Orange sim language @{ */
            case LANG_PICK:
                SimLangListUtils.getInstance(this).startSimLangListActivity(this,chooseId,subInfoList);
                break;
            /* @} */
            default:
                throw new IllegalArgumentException("Invalid dialog type "
                        + dialogId + " in SIM dialog.");
        }

        finish();

    }

    private void dismissSimChooseDialog() {
        if (mSimChooseDialog != null && mSimChooseDialog.isShowing() && mIsForeground) {
            mSimChooseDialog.dismiss();
        }
    }

    private ContentObserver mRadioBusyObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (!mTelephonyManager.isRadioBusy()) {
                dismissProgressDialog();
            } else if (mTelephonyManager.isAirplaneModeOn() && !mIsPrimaryCardCancelable) {
                finish();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mSubscriptionManager = SubscriptionManager.from(this);
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);

        mIsForeground = true;
        if (mDialogType != INVALID_PICK) {
            if (mIsPrimaryCardCancelable) {
                mSimChooseDialog = createDialog(this.getApplicationContext(), mDialogType);
            } else {
                mSimChooseDialog = createDialog(this, mDialogType);
            }
            mSimChooseDialog.show();
        }

        if (!mTelephonyManager.isRadioBusy()) {
            dismissProgressDialog();
        }
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            List<SubscriptionInfo> availableSubInfoList = SubscriptionManager.from(getApplicationContext()).getActiveSubscriptionInfoList();
            if(availableSubInfoList == null || availableSubInfoList.size()<2)
            {
                finish();
            }
        }
    };

    @Override
    protected void onPause() {
        mIsForeground = false;
        super.onPause();
        mSubscriptionManager.from(this).removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            getContentResolver().unregisterContentObserver(mRadioBusyObserver);
            //SPRD: modify for bug508651
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            Log.d(TAG, "onDestroy,Exception = " + e);
        }
    };

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private int mResId;
        private int mDialogId;
        private final float OPACITY = 0.54f;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList,
                Context context, int resource, String[] arr, int dialogId) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
            mDialogId = dialogId;
            mSubInfoList = subInfoList;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView;
            final ViewHolder holder;
            SubscriptionManager subscriptionManager = SubscriptionManager.from(mContext);
            TelecomManager telecomManager = TelecomManager.from(mContext);

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                holder.defaultSubscription =
                        (RadioButton) rowView.findViewById(R.id.default_subscription_off);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            final SubscriptionInfo sir = mSubInfoList.get(position);
            PhoneAccountHandle phoneAccount =
                    telecomManager.getUserSelectedOutgoingPhoneAccount();
            // SPRD: add option to enable/disable sim card
            String summary = "";
            if (sir == null) {
                holder.title.setText(getItem(position));
                // SPRD: add option to enable/disable sim card
                //holder.summary.setText("");
                holder.icon.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_live_help));
                holder.icon.setAlpha(OPACITY);
                /* SPRD: modify by add radioButton on set defult sub id @{ */
                holder.defaultSubscription.setChecked(false);
                switch (mDialogId) {
                    case DATA_PICK:
                        break;
                    case CALLS_PICK:
                        holder.defaultSubscription.setChecked(0 == position
                                &&  phoneAccount == null);
                        break;
                    case SMS_PICK:
                        holder.defaultSubscription.setChecked(0 == position
                                && subscriptionManager.getDefaultSmsSubId()
                                == SubscriptionManager.MAX_SUBSCRIPTION_ID_VALUE);
                        break;
                    case PRIMARY_PICK:
                        break;
                /* SPRD: add for Orange sim language @{ */
                case LANG_PICK:
                    break;
                    /* @} */
                    default:
                        throw new IllegalArgumentException("Invalid dialog type "
                        + mDialogId + " in SIM dialog.");
                        }
                        /* @} */

            } else {
                holder.title.setText(sir.getDisplayName());
                /* SPRD: add option to enable/disable sim card @{ */
                //holder.summary.setText(sir.getNumber());
                summary = sir.getNumber();
                /* @} */
                holder.icon.setImageBitmap(sir.createIconBitmap(mContext));
                 /* SPRD: modify by add radioButton on set defult sub id @{ */
                switch (mDialogId) {
                    case DATA_PICK:
                        holder.defaultSubscription.setChecked(
                                subscriptionManager.getDefaultDataSubId() == sir.getSubscriptionId());
                        break;
                    case CALLS_PICK:
                        holder.defaultSubscription.setChecked(phoneAccount != null
                                && subscriptionManager.getDefaultVoiceSubId() == sir.getSubscriptionId());
                        break;
                    case SMS_PICK:
                        holder.defaultSubscription.setChecked(
                                subscriptionManager.getDefaultSmsSubId() == sir.getSubscriptionId());
                        break;
                    case PRIMARY_PICK:
                        holder.defaultSubscription.setChecked(
                                mTelephonyManager.getPrimaryCard() == sir.getSimSlotIndex());
                        break;
                    /* SPRD: Porting bug#430157 @{ */
                    case LANG_PICK:
                        holder.defaultSubscription.setVisibility(View.GONE);
                        break;
                    /* @} */
                    default:
                        throw new IllegalArgumentException("Invalid dialog type " + mDialogId + " in SIM dialog.");
                }
            }
            /* SPRD: add option to enable/disable sim card @{ */
            holder.defaultSubscription.setVisibility(mIsPrimaryCardCancelable ? View.GONE : View.VISIBLE);

            /* SPRD: modify for bug494887 @{ */
            if (mDialogId == DATA_PICK) {
                holder.defaultSubscription.setEnabled(isEnabled(position));
            }
            /* @} */

            holder.summary.setText(summary);
            if (TextUtils.isEmpty(summary)) {
                holder.summary.setVisibility(View.GONE);
            } else {
                holder.summary.setVisibility(View.VISIBLE);
            }
            /* @} */

            final boolean isSubIdChecked = holder.defaultSubscription.isChecked();
            holder.defaultSubscription.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onCheckedChanged isSubIdChecked = " + isSubIdChecked);
                    if (!isSubIdChecked) {
                        setDefaltSubIdByDialogId(mContext, mDialogId, position, mSubInfoList);
                    } else {
                        finish();
                    }
                }
            });
            /* @} */
            return rowView;
        }

        /* SPRD: modify for bug494887 @{ */
        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mDialogId == DATA_PICK) {
                final SubscriptionInfo sir = mSubInfoList.get(position);
                boolean isSimReady = mTelephonyManager
                        .getSimState(sir.getSimSlotIndex()) == TelephonyManager.SIM_STATE_READY;
                boolean isSimStandby = mTelephonyManager.isSimStandby(sir.getSimSlotIndex());
                if (!isSimStandby || !isSimReady) {
                    return false;
                }
            }
            return true;
        }
        /* @} */

        private class ViewHolder {
            TextView title;
            TextView summary;
            ImageView icon;
            // SPRD: modify by add radioButton on set defult sub id
            RadioButton defaultSubscription;
        }
    }

    public static class PorgressDialogFragment extends DialogFragment {
        View v;
        TextView mMessageView;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            v = inflater.inflate(R.layout.progress_dialog_fragment_ex, container, false);
            mMessageView = (TextView) v.findViewById(R.id.message);
            mMessageView.setText(getResources().getString(R.string.primary_card_switching));
            //setView(view);
            return v;
        }
    }

    /* SPRD: modify for bug508651 @{ */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                if (stateExtra != null && IccCardConstants.INTENT_VALUE_ICC_ABSENT .equals(stateExtra)) {
                    dismissSimChooseDialog();
                    finish();
                }
            } else if (PhoneConstants.ACTION_SUBSCRIPTION_PHONE_STATE_CHANGED.equals(action)) {
                /* SPRD: [bug522030] If call incoming while primary card selection dialog is showing. Just
                   dismiss the dialog and make sure all radios powered on if allowed @{ */
                if (mIsPrimaryCardCancelable) {
                    int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, 0);
                    int phoneId = SubscriptionManager.getPhoneId(subId);
                    mTelephonyManager.setPrimaryCard(phoneId);
                    if (mSimChooseDialog != null) {
                        mSimChooseDialog.dismiss();
                    }
                    finish();
                }
                /* @} */
            }
        }
    };
    /* @} */
}
