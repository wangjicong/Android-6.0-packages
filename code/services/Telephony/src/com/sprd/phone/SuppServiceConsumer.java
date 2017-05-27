package com.sprd.phone;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.SuppServiceNotification;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class SuppServiceConsumer extends Handler {
    private static final int MO_CALL = 0;
    private static final int MT_CALL = 1;

    protected static final int EVENT_SSN = 1;

    private Context mContext;
    private static SuppServiceConsumer mInstance;

    // TODO Register this handler to gsmphone.
    private SuppServiceConsumer(Context context) {
        mContext = context;
    }

    public static SuppServiceConsumer getInstance(Context context, Phone phone) {
        if (mInstance == null) {
            mInstance = new SuppServiceConsumer(context);
        }
        phone.registerForSuppServiceNotification(mInstance, EVENT_SSN, null);
        // it can not handle the old one still or the activity will be not
        // release
        mInstance.setContext(context);
        return mInstance;
    }

    private void setContext(Context context) {
        mContext = context;
    }

    @Override
    public void handleMessage(Message msg) {
        // TODO Auto-generated method stub
        AsyncResult ar;
        switch (msg.what) {
        case EVENT_SSN:
            ar = (AsyncResult)msg.obj;
            CharSequence cs = null;
            SuppServiceNotification not = (SuppServiceNotification) ar.result;
            if (not.notificationType == MO_CALL) {
                switch(not.code) {
                    case SuppServiceNotification.MO_CODE_UNCONDITIONAL_CF_ACTIVE:
                    cs = mContext.getString(com.android.internal.R.string.ActiveUnconCf);
                    break;
                    case SuppServiceNotification.MO_CODE_SOME_CF_ACTIVE:
                    cs = mContext.getString(com.android.internal.R.string.ActiveConCf);
                    break;
                    case SuppServiceNotification.MO_CODE_CALL_FORWARDED:
                    cs = mContext.getString(com.android.internal.R.string.CallForwarded);
                    break;
                    case SuppServiceNotification.MO_CODE_CALL_IS_WAITING:
                    cs = mContext.getString(com.android.internal.R.string.CallWaiting);
                    break;
                    case SuppServiceNotification.MO_CODE_OUTGOING_CALLS_BARRED:
                    cs = mContext.getString(com.android.internal.R.string.OutCallBarred);
                    break;
                    case SuppServiceNotification.MO_CODE_INCOMING_CALLS_BARRED:
                    cs = mContext.getString(com.android.internal.R.string.InCallBarred);
                    break;
                    //case SuppServiceNotification.MO_CODE_CLIR_SUPPRESSION_REJECTED:
                    //cs = mContext.getText(com.android.internal.R.string.ClirRejected);
                    //break;
                }
            } else if (not.notificationType == MT_CALL) {
                switch(not.code) {
                    case SuppServiceNotification.MT_CODE_FORWARDED_CALL:
                    cs = mContext.getString(com.android.internal.R.string.ForwardedCall);
                    break;
                    /* case SuppServiceNotification.MT_CODE_CUG_CALL:
                    cs = mContext.getText(com.android.internal.R.string.CugCall);
                    break;*/
                    //Fix Bug 4182 phone_01
                    case SuppServiceNotification.MT_CODE_CALL_ON_HOLD:
                    cs = mContext.getString(com.android.internal.R.string.CallHold);
                    break;
                    case SuppServiceNotification.MT_CODE_CALL_RETRIEVED:
                    cs = mContext.getString(com.android.internal.R.string.CallRetrieved);
                    break;
                    /*case SuppServiceNotification.MT_CODE_MULTI_PARTY_CALL:
                    cs = mContext.getText(com.android.internal.R.string.MultiCall);
                    break;
                    case SuppServiceNotification.MT_CODE_ON_HOLD_CALL_RELEASED:
                    cs = mContext.getText(com.android.internal.R.string.HoldCallReleased);
                    break;
                    case SuppServiceNotification.MT_CODE_CALL_CONNECTING_ECT:
                    cs = mContext.getText(com.android.internal.R.string.ConnectingEct);
                    break;
                    case SuppServiceNotification.MT_CODE_CALL_CONNECTED_ECT:
                    cs = mContext.getText(com.android.internal.R.string.ConnectedEct);
                    break;*/
                    case SuppServiceNotification.MT_CODE_ADDITIONAL_CALL_FORWARDED:
                    cs = mContext.getString(com.android.internal.R.string.IncomingCallForwarded);
                    break;
                }
            }
            if (cs!=null) {
                Toast.makeText(mContext, cs, Toast.LENGTH_LONG).show();
            }
        }
        super.handleMessage(msg);
    }
}
