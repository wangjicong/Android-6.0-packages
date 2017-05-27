package com.wx.hallview;

/**
 * Created by Administrator on 16-1-20.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;

public class HallReceiver extends BroadcastReceiver{
    private static final String ACTION_LOCAL_CHANGED = "android.intent.action.LOCALE_CHANGED";
	
    private static Button button;

    public void onReceive(Context paramContext, Intent paramIntent)
    {
        String str = paramIntent.getAction();
        ContentResolver cr = paramContext.getContentResolver();
        if(System.getInt(cr,System.HOLSTER_FUNCTION, 0) != 1){
        	return;
        }
        ViewContorller  viewContorller = ViewContorller.getInstance(paramContext.getApplicationContext());
        Log.d("HallReceiver", "onReceive action = " + str);
        if("com.wx.hall".equals(str)){  

            int state = paramIntent.getIntExtra("state", 0);
            if (state == 1){
                if (button == null){
                    button = new Button(paramContext);
                }
                System.putInt(cr, "com.wx.hall.status", 1);
                viewContorller.launchRootView();

            }else if(state == 0){
                System.putInt(cr, "com.wx.hall.status", 0);
                viewContorller.hideRootView();
            }

        }else if("com.wx.hallview.launchfragment".equals(str)){
            String fragmentTag = paramIntent.getStringExtra("fragment_tag");
            boolean isOutgoing = paramIntent.getBooleanExtra("is_outgoing", false);
            if(!TextUtils.isEmpty(fragmentTag) && fragmentTag.equals("incall") ){
                viewContorller.showInCallView(isOutgoing);
            }

        }else if("android.intent.action.SCREEN_ON".equals(str)){
            viewContorller.onScreenOn();
        }else if("android.intent.action.SCREEN_OFF".equals(str)){
            viewContorller.onScreenOff();
        }else if (ACTION_LOCAL_CHANGED.equals(str)){
            viewContorller.reload();
        }
    }
}
