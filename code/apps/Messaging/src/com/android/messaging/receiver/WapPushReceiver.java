package com.android.messaging.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.messaging.datamodel.action.WapPushAction;
import com.android.messaging.util.PhoneUtils;

public class WapPushReceiver extends BroadcastReceiver {
    private static final String TAG = "WapPushReceiver";
    private static final String EXTRA_SUBSCRIPTION = "subscription";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "=========wap push===0==onReceive=====type: "+ intent.getType() + "  action: " + intent.getAction());
        if (PhoneUtils.getDefault().isSmsEnabled()) {
            Log.d(TAG,"==========wap push===1==onReceive=====type: "+ intent.getType() + "  action: "+ intent.getAction());
            final android.telephony.SmsMessage[] messages = SmsReceiver.getMessagesFromIntent(intent);

            // Check messages for validity
            if (messages == null || messages.length < 1) {
                Log.e(TAG,"====wap push===2==processReceivedSms: null or zero or ignored message");
                return;
            }
            // Always convert negative subIds into -1
            int subId = PhoneUtils.getDefault().getEffectiveIncomingSubIdFromSystem(intent,WapPushReceiver.EXTRA_SUBSCRIPTION);
            WapPushAction action = new WapPushAction(subId, intent);
            Log.d(TAG,"==========wap push====3====onReceive=====subId: " + subId);
            action.start();
        }
    }
}
