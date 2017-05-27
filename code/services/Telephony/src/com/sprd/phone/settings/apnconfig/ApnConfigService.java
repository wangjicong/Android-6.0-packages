package com.sprd.phone.settings.apnconfig;

import java.util.List;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import com.android.phone.R;

public class ApnConfigService extends Service {

    private static final String TAG = "ApnConfigService";
    private static final boolean DBG = true;
    private static final String SET_PRIMARYCARD_COMPLETE =
            "android.intent.action.SET_PRIMARYCARD_COMPLETE";
    private static final String POP_FILE = "data_pop_show";
    private static final String FIRST_REBOOT = "first_reboot";

    private boolean mIsPopUpShowing = false;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private Context mContext;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // the service started and receive the broadcast of set primary card complete
            // we should show apn config popup
            if (SET_PRIMARYCARD_COMPLETE.equals(intent.getAction())) {
                log("OnReceive: " + SET_PRIMARYCARD_COMPLETE);
                if (needShowDataOrApnPopUp()) {
                    if (!mIsPopUpShowing) {
                        mIsPopUpShowing = true;
                        showApnConfigPopUp();
                    }
                } else {
                    stopSelf();
                }
            }
        }
    };

    public ApnConfigService() {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        mContext = this;
        mTelephonyManager = TelephonyManager.from(this);
        mSubscriptionManager = SubscriptionManager.from(this);
        final IntentFilter intentFilter = new IntentFilter(
                SET_PRIMARYCARD_COMPLETE);
        registerReceiver(mReceiver, intentFilter);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /* SPRD: Add for feature of APN and DATA Pop up.BugId: 509845. @{ */
    private boolean needShowDataOrApnPopUp() {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) getApplicationContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (carrierConfigManager != null
                && carrierConfigManager.getConfig() != null) {
            // get the switch of apn config popup
            boolean needShowApnAndData = carrierConfigManager
                    .getConfigForDefaultPhone()
                    .getBoolean(CarrierConfigManager.KEY_FEATURE_DATA_AND_APN_POP_BOOL);
            log("needShowApnAndData = " + needShowApnAndData);
            if (needShowApnAndData) {
                List<SubscriptionInfo> subscriptionInfoList = mSubscriptionManager
                        .getActiveSubscriptionInfoList();
                // some oprator would not show apn config popup we can set in carrier config
                // for example:23430,23433
                String operator = carrierConfigManager
                        .getConfig()
                        .getString(
                                CarrierConfigManager.KEY_FEATURE_DATA_AND_APN_POP_OPERATOR_STRING);
                log("operator = " + operator);
                String[] operators = operator.split(",");
                int activeSubCount = mSubscriptionManager.getActiveSubscriptionInfoCount();
                if (activeSubCount > 0) {
                    for (SubscriptionInfo info : subscriptionInfoList) {
                        int subId = info.getSubscriptionId();
                        int phoneId = info.getSimSlotIndex();
                        String mccmnc = mTelephonyManager.getSimOperator(subId);
                        log("Check the sim, mccmnc = " + mccmnc);
                        for (String str : operators) {
                            // if there are two sim card and one of them is PIN locked,return false
                            if (isSimLocked(phoneId)
                                    || str.equals(mccmnc)) {
                                return false;
                            }
                        }
                    }
                } else {
                    return false;
                }
            }
            return needShowApnAndData;
        }
        return false;
    }

    private void showDataSwitchPopUp() {
        log("show DATA pop up");
        AlertDialog.Builder dataServiceDialog = new AlertDialog.Builder(this);
        dataServiceDialog
                .setCancelable(false)
                .setTitle(R.string.data_popup_title)
                .setMessage(R.string.data_popup_summary)
                .setPositiveButton(R.string.data_popup_enable,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mTelephonyManager.setDataEnabled(
                                        SubscriptionManager.getDefaultDataSubId(), true);
                            }
                        })
                .setNegativeButton(R.string.data_popup_disable,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mTelephonyManager.setDataEnabled(
                                        SubscriptionManager.getDefaultDataSubId(), false);
                            }
                        });
        AlertDialog dataDialog = dataServiceDialog.create();
        dataDialog.setCanceledOnTouchOutside(false);
        dataDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dataDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SharedPreferences sp = mContext.getSharedPreferences(
                        POP_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(FIRST_REBOOT, false);
                editor.apply();
                mIsPopUpShowing = false;
                stopSelf();
            }
        });
        dataDialog.show();
    }

    private void showApnConfigPopUp() {
        Log.d(TAG, "show APN pop up");
        AlertDialog.Builder apnConfigDialog = new AlertDialog.Builder(this);
        apnConfigDialog.setCancelable(false)
                .setTitle(R.string.apn_popup_title)
                .setMessage(R.string.apn_popup_summary)
                .setPositiveButton(R.string.apn_popup_change,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.phone",
                                "com.android.phone.MobileNetworkSettings");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.apn_popup_close,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog apnDialog = apnConfigDialog.create();
        apnDialog.setCanceledOnTouchOutside(false);
        apnDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        apnDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                SharedPreferences sp = mContext.getSharedPreferences(POP_FILE,
                        Context.MODE_PRIVATE);
                // whether the first boot or restore factory settings
                boolean needShowData = sp.getBoolean(FIRST_REBOOT, true);
                log("Need show data: " + needShowData);
                if (needShowData) {
                    showDataSwitchPopUp();
                } else {
                    mIsPopUpShowing = false;
                    stopSelf();
                }
            }
        });
        apnDialog.show();
    }
    /* @} */

    private boolean isSimLocked(int phoneId) {
        int simState = mTelephonyManager.getSimState(phoneId);
        if (simState == TelephonyManager.SIM_STATE_PIN_REQUIRED
                || simState == TelephonyManager.SIM_STATE_PUK_REQUIRED
                || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
                || simState == TelephonyManager.SIM_STATE_NETWORKSUBSET_LOCKED
                || simState == TelephonyManager.SIM_STATE_SERVICEPROVIDER_LOCKED
                || simState == TelephonyManager.SIM_STATE_CORPORATE_LOCKED
                || simState == TelephonyManager.SIM_STATE_SIM_LOCKED
                || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED_PUK
                || simState == TelephonyManager.SIM_STATE_NETWORK_SUBSET_LOCKED_PUK
                || simState == TelephonyManager.SIM_STATE_SERVICE_PROVIDER_LOCKED_PUK
                || simState == TelephonyManager.SIM_STATE_CORPORATE_LOCKED_PUK
                || simState == TelephonyManager.SIM_STATE_SIM_LOCKED_PUK
                || simState == TelephonyManager.SIM_STATE_SIM_LOCKED_FOREVER) {
            return true;
        }
        return false;
    }
    private void log(String msg) {
        if (DBG) Log.d(TAG, msg);
    }
}
