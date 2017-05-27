package com.sprd.dialer;
import com.android.dialer.util.DialerUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.dialer.util.DialerUtils;

public class SubinfoUpdateReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

        DialerUtils.getActiveSubscriptionInfo(context, -1, true);
    }

}
