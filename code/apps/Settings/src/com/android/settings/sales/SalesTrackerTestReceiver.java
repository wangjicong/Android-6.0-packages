package com.android.settings.sales;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



public class SalesTrackerTestReceiver extends BroadcastReceiver {
	public static String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";

	public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(SECRET_CODE_ACTION)) {
            Intent i = new Intent(context,SalesTrackerTestActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
