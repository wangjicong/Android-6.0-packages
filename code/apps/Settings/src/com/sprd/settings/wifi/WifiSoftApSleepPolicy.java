package com.sprd.settings.wifi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.database.ContentObserver;

public class WifiSoftApSleepPolicy extends BroadcastReceiver{

    private static final String TAG = "WifiSoftApSleepPolicy";

    public static final int WIFI_SOFT_AP_SLEEP_POLICY_NEVER = 0;
    public static final int WIFI_SOFT_AP_SLEEP_POLICY_5_MINS = 1;
    public static final int WIFI_SOFT_AP_SLEEP_POLICY_10_MINS = 2;
    public static final String WIFI_SOFT_AP_SLEEP_POLICY = "wifi_soft_ap_sleep_policy_key";

    public static final int FIVE_MINS = 300000;
    public static final int TEN_MINS = 600000;
    public static final String ALARM_FOR_HOTSPOT_IDLE_ACTION = "sprd.wifi.alarm.IDLE_HOTSPOT";

    private static WifiManager mWifiManager = null;
    private static AlarmManager mAlarmManager = null;
    private static PendingIntent mPendingIntent = null;
    private static boolean isSoftApRunning = false;
    private static boolean isAlarmRunning = false;
    private static Context mContext = null;

    public static ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.e(TAG,"change policy!");
            if (!isSoftApRunning)
                return;
            Log.e(TAG,"change policy and soft ap running");
            mAlarmManager.cancel(mPendingIntent);
            isAlarmRunning = false;
            onSoftApConnChanged();
        }
    };

    public static ContentObserver getSoftApChangeObserver(){
        return mObserver;
    }

    public static void init(Context context) {
        if (mWifiManager == null) {
            Log.e(TAG,"init");
            mWifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
            mAlarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
            mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ALARM_FOR_HOTSPOT_IDLE_ACTION), 0);
            context.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(WIFI_SOFT_AP_SLEEP_POLICY), true, mObserver);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        //check null pointer
        init(mContext);

        if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
            isSoftApRunning = true;
            int hotspotState = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_AP_STATE,
                        WifiManager.WIFI_AP_STATE_FAILED);
            if (hotspotState == WifiManager.WIFI_AP_STATE_ENABLED) {
                Log.e(TAG,"open");
                checkAndSendMsg();
            } else if (hotspotState == WifiManager.WIFI_AP_STATE_DISABLED) {
                if (mAlarmManager != null) {
                    mAlarmManager.cancel(mPendingIntent);
                    isAlarmRunning = false;
                }
                Log.e(TAG,"close");
                isSoftApRunning = false;
            }
        }else if (WifiManager.WIFI_AP_CONNECTION_CHANGED_ACTION.equals(action) || HotspotSettings.STATIONS_STATE_CHANGED_ACTION.equals(action)) {
            onSoftApConnChanged();
        } else if (ALARM_FOR_HOTSPOT_IDLE_ACTION.equals(action)) {
            mWifiManager.setWifiApEnabled(null, false);
        }
    }

    private static void  onSoftApConnChanged() {
        String mConnectedStationsStr = mWifiManager.softApGetConnectedStations();
        Log.e(TAG, "mConnectedStationsStr = " + mConnectedStationsStr);
        if (mConnectedStationsStr == null || mConnectedStationsStr.length() == 0) {
            Log.e(TAG,"no ap connected");
            if(isAlarmRunning)
                return;
            checkAndSendMsg();
        } else {
            mAlarmManager.cancel(mPendingIntent);
            isAlarmRunning = false;
            Log.e(TAG,"coming sta");
        }
    }

    private static boolean checkAndSendMsg(){
        int value = Settings.System.getInt(mContext.getContentResolver(),WIFI_SOFT_AP_SLEEP_POLICY,
                          WifiSoftApSleepPolicy.WIFI_SOFT_AP_SLEEP_POLICY_NEVER);
        if (value == WIFI_SOFT_AP_SLEEP_POLICY_NEVER) {
            Log.e(TAG,"always! and return");
            return false;
        }
        if (value == WIFI_SOFT_AP_SLEEP_POLICY_5_MINS) {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+FIVE_MINS, mPendingIntent);
            isAlarmRunning = true;
        } else if (value == WIFI_SOFT_AP_SLEEP_POLICY_10_MINS) {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+TEN_MINS, mPendingIntent);
            isAlarmRunning = true;
        }
        return true;
    }
}
