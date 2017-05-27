package com.android.providers.telephony.ext.mmsbackup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sprd.plat.Interface.INotify;

/**
 * Created by apuser on 16-4-27.
 */
public class BackupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        BackupLog.log("BackupReceiver", "onReceive");
        if (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
            BackupLog.log("BackupReceiver", "media ejected!");
            if (getCallback() != null) {
                getCallback().OnNotify(Defines.CMD_CANCEL_ALL, 0, 0, null, null);
            }
        } else
            {
                BackupLog.log("BackupReceiver", "===>>>Call Back is null");
            }
    }

    public static void setCallback(INotify cbf){
        mcbf = cbf;
        Log.e("BackupReceiver", "===>>>Set Call Back");
    }
    private static INotify getCallback(){ return  mcbf;}
    private  static INotify  mcbf = null;
}
