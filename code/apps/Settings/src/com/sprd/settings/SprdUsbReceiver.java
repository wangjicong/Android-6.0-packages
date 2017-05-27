/** Created by Spreadst */
package com.sprd.settings;

import java.util.List;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.sprd.android.config.OptConfig;//jxl add

public class SprdUsbReceiver extends BroadcastReceiver {
    private final String LOG_TAG = "SprdUsbReceiver";

    private Context context;
    private UsbManager mUsbManager = null;
    private static boolean powerOff =false;
    private boolean mConnected = false;
    private boolean orignal_support = SystemProperties.getBoolean("ro.usb.orignal.design",false);
    @Override
    public void onReceive(Context context, Intent intent) {
        if(orignal_support){
            return ;
        }
        this.context = context;
        // TODO Auto-generated method stub
        mUsbManager = (UsbManager) context
                .getSystemService(Context.USB_SERVICE);

        String action = intent.getAction();
        Log.i(LOG_TAG, "action = " + action);
        // SPRD: In case of NullPointerException
        if (UsbManager.ACTION_USB_STATE.equals(action)) {
            if (intent.getBooleanExtra(UsbManager.USB_CONNECTED, false)) {
                mConnected = true;
                powerOff=false;

                if (SystemProperties.get("persist.sys.sprd.mtbf", "1").equals("0")) {
                    return;
                }
                /* SPRD: Bug 391387  To determine whether the boot unlock interface. @{ */
                //recoverFunction();
                ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
                List<RunningTaskInfo> info=manager.getRunningTasks(1);
                String classname = null;
                 if( null != info && !(info.isEmpty())){
                    classname=info.get(0).topActivity.getClassName();
                    // SPRD: 400416 add EmergencyDialer to avoid show usb setting when plugining usb line
                    if (classname.equals("com.android.settings.CryptKeeper") || "com.android.phone.EmergencyDialer".equals(classname)) {
                       return;
                    }else {
                        recoverFunction();
                    }
                 }else{
                        recoverFunction();
                }
              /* @} */
            }
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            if(mConnected){
                powerOff=true;
                mConnected = false;
            }
			//jxl add
			if(OptConfig.SUN_CUSTOM_C7356_XT_HVGA_SUPPORT){
            mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP);
			}

            if (Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.USB_REMEMBER_CHOICE, 0) == 0) {
					if(OptConfig.SUN_CUSTOM_C7356_XT_HVGA_SUPPORT){
						Settings.Global.putInt(context.getContentResolver(),
                    		Settings.Global.USB_CURRENT_FUNCTION, 5);//jxl modify
					}else{
                		Settings.Global.putInt(context.getContentResolver(),
                    		Settings.Global.USB_CURRENT_FUNCTION, 0);
					}
            }
        }
    }

    private void recoverFunction() {
        Intent sprdIntent = new Intent(
                "com.sprd.settings.APPLICATION_SPRD_USB_SETTINGS");
        sprdIntent.setClass(context, SprdUsbSettings.class);
        sprdIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(sprdIntent);
    }

    public String getCurrentFunction() {
        String functions = SystemProperties.get("sys.usb.config", "");
        int commaIndex = functions.indexOf(',');
        if (commaIndex > 0) {
            return functions.substring(0, commaIndex);
        } else {
            return functions;
        }
    }
    public static boolean isPowerOff() {
        return powerOff;
    }
}
