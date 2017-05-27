package com.android.settings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import com.sprd.android.config.OptConfig;

public class BreathingLightBootReceiver extends BroadcastReceiver{
    private static final String TAG = "BreathingLightBootReceiver";
    private static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";
    private static final String BREATHING_LIGHT_SERVICE_ACTION = "com.android.settings.service.breathinglight";
    private static final String KEY_NOTIFICATIONS = "breathing_light_notifications";
    private static final String KEY_LOW_BATTERY = "breathing_light_low_battery";
    private static final String KEY_CHARGING = "breathing_light_charging";
    //Kalyy 2015-02-04
    private static final String KEY_INCOMING_CALL = "breathing_light_incoming_call";
    private static final String KEY_MEDIA = "breathing_light_media";
    private Context mContext;
    private int isLowBatterySelected;
    private int isChargingSelected;
    private int isNotificationSelected;
    //Kalyy 2015-02-04
    private int isIncomingCallSelected;
    private int isMediaSelected;
    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        loadValuesFromSettingDb(context);
        if (intent != null) {
            if (!OptConfig.SUN_BREATHINGLIGHT){
                return;
            }

            String action = intent.getAction();
            Log.d(TAG,"onReceive action: "+action);
            if (action.equals(ACTION_BOOT)) {
                if (isStartService()) {
                    Intent sintent = new Intent(BREATHING_LIGHT_SERVICE_ACTION);
                    sintent.setPackage(context.getPackageName());
                    context.startService(sintent);
                }
            }
        }
    }

    private void loadValuesFromSettingDb(Context context) {
        try {
            isNotificationSelected = Settings.System.getInt(mContext.getContentResolver(),KEY_NOTIFICATIONS,
                context.getResources().getInteger(com.android.internal.R.integer.config_breathinglight_notification_onoff));
            isLowBatterySelected = Settings.System.getInt(mContext.getContentResolver(),KEY_LOW_BATTERY,
                context.getResources().getInteger(com.android.internal.R.integer.config_breathinglight_lowbattery));
            isChargingSelected = Settings.System.getInt(mContext.getContentResolver(),KEY_CHARGING,
                context.getResources().getInteger(com.android.internal.R.integer.config_breathinglight_charging));
            
            //Kalyy 2015-02-04
            isIncomingCallSelected = Settings.System.getInt(mContext.getContentResolver(),KEY_INCOMING_CALL,
                 context.getResources().getInteger(com.android.internal.R.integer.config_breathinglight_incoming));
            isMediaSelected = Settings.System.getInt(mContext.getContentResolver(),KEY_MEDIA,
                context.getResources().getInteger(com.android.internal.R.integer.config_breathinglight_media));
            
            Log.d(TAG, "loadValuesFromSettingDb isNotificationSelected: " + isNotificationSelected);
            Log.v(TAG, "loadValuesFromSettingDb isLowBatterySelected: " + isLowBatterySelected);
            Log.v(TAG, "loadValuesFromSettingDb isChargingSelected: " + isChargingSelected);
            Log.d(TAG, "loadValuesFromSettingDb isIncomingCallSelected: " + isIncomingCallSelected);
            Log.v(TAG, "loadValuesFromSettingDb isMediaSelected: " + isMediaSelected);
            
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private boolean isStartService() {
        if (isNotificationSelected == 0 && isLowBatterySelected == 0
                && isChargingSelected == 0 && isIncomingCallSelected == 0 && isMediaSelected == 0) {//Kalyy 2015-02-04
            return false;
        }
        return true;
    }
}
