package com.android.dialer.calllog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import com.sprd.dialer.plugins.FdnNumberHelper;

public class FdnUpdateReceiver extends BroadcastReceiver {
    TelephonyManager mTelephonyManager;
    private static final String ACTION_FDN_STATUS_CHANGED =
            "android.intent.action.FDN_STATUS_CHANGED";
    private static final String ACTION_FDN_LIST_CHANGED =
            "android.intent.action.FDN_LIST_CHANGED";
    private static final String ACTION_SIM_STATE_CHANGED
            = "android.intent.action.SIM_STATE_CHANGED";
    private static final String INTENT_EXTRA_SUB_ID = "subid";
    private static final String INTENT_EXTRA_NUMBER =  "number";
    public static final String INTENT_VALUE_ICC_ABSENT = "ABSENT";
    public static final String INTENT_VALUE_ICC_LOADED = "LOADED";
    public static final String INTENT_KEY_ICC_STATE = "ss";
    public static final String SUBSCRIPTION_KEY  = "subscription";
    private static final int NO_SUB_ID = -1;
    private static final int INVALID_SUBSCRIPTION_ID = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ACTION_FDN_STATUS_CHANGED.equals(action)) {
            int subId = intent.getIntExtra(INTENT_EXTRA_SUB_ID, NO_SUB_ID);
            String number = intent.getStringExtra(INTENT_EXTRA_NUMBER);
            if (mTelephonyManager.getIccFdnEnabled(subId)) {
                FdnNumberHelper.getInstance(context).queryFdnList(subId, context);
            } else if (!mTelephonyManager.getIccFdnEnabled(subId)) {
                FdnNumberHelper.getInstance(context).refreshFdnListCache(subId, context);
            }
        } else if (ACTION_FDN_LIST_CHANGED.equals(action)) {
            int subId = intent.getIntExtra(INTENT_EXTRA_SUB_ID, NO_SUB_ID);
            String number = intent.getStringExtra(INTENT_EXTRA_NUMBER);
            FdnNumberHelper.getInstance(context).queryFdnList(subId, context);
        } else if (ACTION_SIM_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(INTENT_KEY_ICC_STATE);
            int subId = intent.getIntExtra(SUBSCRIPTION_KEY,
                    INVALID_SUBSCRIPTION_ID);
            if (INTENT_VALUE_ICC_LOADED.equals(state)) {
                if (mTelephonyManager.getIccFdnEnabled(subId)) {
                    FdnNumberHelper.getInstance(context).queryFdnList(
                            subId, context);
                } else {
                    FdnNumberHelper.getInstance(context).refreshFdnListCache(
                            subId, context);
                }
            } else if (INTENT_VALUE_ICC_ABSENT.equals(state)) {
                FdnNumberHelper.getInstance(context).refreshFdnListCache(
                        subId, context);
            }
        }
    }
}