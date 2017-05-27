package com.android.settings;

import static com.android.internal.telephony.TelephonyIntents.SECRET_CODE_ACTION;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;


public class TestingSettingsBroadcastReceiver extends BroadcastReceiver {
  
    public TestingSettingsBroadcastReceiver() {
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        /* SPRD: modified for 522781 */
        if (SECRET_CODE_ACTION.equals(intent.getAction())) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClass(context, TestingSettings.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
