package com.android.settings;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.preference.PreferenceManager;
import android.provider.CalendarContract.CalendarAlerts;
import android.os.SystemProperties;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.CallLog.Calls;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import com.sprd.android.config.OptConfig;

public class BreathingLightService extends Service{
    private static final String TAG = "BreathingLightService";
    private static final String INTENT_CANCEL_BREATHINGING_LIGHT = "com.android.intent.action.cancel.breathinglight";
    private static final String INTENT_CHANGE_BREATHINGING_LIGHT = "com.android.intent.action.change.breathinglight";
    private static final String BREATHING_LIGHT_SERVICE_ACTION = "com.android.settings.service.breathinglight";	
    private static final String KEY_NOTIFICATIONS = "breathing_light_notifications";
    private static final String KEY_LOW_BATTERY = "breathing_light_low_battery";
    private static final String KEY_CHARGING = "breathing_light_charging";
    //Kalyy 2015-02-04
    private static final String KEY_INCOMING_CALL = "breathing_light_incoming_call";
    private static final String KEY_MEDIA = "breathing_light_media";
    private static final String KEY_IS_HAS_NOTIFICATION = "is_has_notification";
    private static final String SHARED_PREFERENCE_NAME = "com.android.settings_preferences";
    private static final String SYSTEM_PROPERTIES_LOW_BATTERY_LED_ON = "persist.sys.low.battery.led.on";
    private static final String SYSTEM_PROPERTIES_MID_BATTERY_LED_ON = "persist.sys.charg.led.on";
    private static final String SYSTEM_PROPERTIES_NOTIFICATION_LED_ON = "persist.sys.notification.led.on";
    //Kalyy 2015-02-04
    private static final String SYSTEM_PROPERTIES_INCAMING_CALL_LED_ON = "persist.sys.incall.led.on";
    private static final String SYSTEM_PROPERTIES_MEDIA_LED_ON = "persist.sys.media.led.on";
    private static final String SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_ON = "1";
    private static final String SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF = "0";
    private static final String SYSTEM_PROPERTIEX_LED_LIGHT_LEVEL = "persist.sys.led.light.level";//0/1/2/3/4/5(close)
    private static final String BREATHING_NOTIFICATIONS_CLICKED = "breathing_light_notifications_clicked";
    private static final int LIGHT_NOTIFICATION_ID = 0;
    private static final int ALERT_EXT_STATE_IGNORED = 100;
    private static final String SMS_URI = "content://mms-sms/";
    private static final String SMS_URI_INBOX = "content://sms/inbox";
    private static final String MMS_URI_INBOX = "content://mms/inbox";
    private static final String EMAIL_URI_MAILBOX = "content://com.android.email.provider/mailbox";
    private DbContentObServer missedCallContentObServer;
    private DbContentObServer sMSContentObServer;
    private DbContentObServer calendarObServer;
    private DbContentObServer emailContentObServer;
    private QueryDbRunnable queryDbRunnable;
    private PendingIntent showPendIntent;
    private PendingIntent cancelPendIntent;
    private NotificationManager mNotificationManager;
    private AlarmManager mAlarmManager;
    private BroadcastReceiver mReceiver;
    private Handler mHandler = new Handler ();
    private int color;
    private int ledOn;
    private int ledOff;
    private static boolean isContinue;
    private static boolean isNotificationSelected;
    private static boolean isLowBatterySelected;
    private static boolean isChargingSelected;
    //Kalyy 2015-02-04
    private static boolean isIncomingCallSelected;
    private static boolean isMediaSelected;
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"BreathingLightService onCreate.");
        ledOn = this.getResources().getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOn);
        ledOff = this.getResources().getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOff);
        color = this.getResources().getColor(
                com.android.internal.R.color.config_defaultNotificationColor);
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG,"BreathingLightService onStart.");
        if (intent == null) {
            return;
        }
        boolean isClickedWhickpreference = intent.getBooleanExtra(BREATHING_NOTIFICATIONS_CLICKED, true);
        Log.d(TAG,"onStart isClickedWhickpreference: "+isClickedWhickpreference);
        updateFromSharedPreference();
        Log.d(TAG,"onStart isNotificationSelected: "+isNotificationSelected);
        Log.d(TAG,"onStart isLowBatterySelected: "+isLowBatterySelected);
        Log.d(TAG,"onStart isChargingSelected: "+isChargingSelected);
        // put the value in the system properties file
        if (isLowBatterySelected) {
            SystemProperties.set(SYSTEM_PROPERTIES_LOW_BATTERY_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_ON);
        } else {
            SystemProperties.set(SYSTEM_PROPERTIES_LOW_BATTERY_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
        }
        if (isChargingSelected) {
            SystemProperties.set(SYSTEM_PROPERTIES_MID_BATTERY_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_ON);
        } else {
            SystemProperties.set(SYSTEM_PROPERTIES_MID_BATTERY_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
        }
        if (isNotificationSelected) {
            SystemProperties.set(SYSTEM_PROPERTIES_NOTIFICATION_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_ON);
        } else {
            SystemProperties.set(SYSTEM_PROPERTIES_NOTIFICATION_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
        }
        //Kalyy 2015-02-04
        if (isIncomingCallSelected) {
            SystemProperties.set(SYSTEM_PROPERTIES_INCAMING_CALL_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_ON);
        } else {
            SystemProperties.set(SYSTEM_PROPERTIES_INCAMING_CALL_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
        }
        if (isMediaSelected) {
            SystemProperties.set(SYSTEM_PROPERTIES_MEDIA_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_ON);
        } else {
            SystemProperties.set(SYSTEM_PROPERTIES_MEDIA_LED_ON, 
                    SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
        }
        isContinue = false;
        if (isNotificationSelected && isClickedWhickpreference) {
            registerContentObServer();
            notificationCtrl();
        } else if (!isNotificationSelected){
            if (queryDbRunnable != null) {
                mHandler.removeCallbacks(queryDbRunnable);
                queryDbRunnable = null;
            }
            putValueInSystem(KEY_IS_HAS_NOTIFICATION,false);
            ungisterContentObServer();
            //cancelLitghtNotification();//Kalyy 2015-02-04
			Log.d(TAG,"<<<< ljz 1 isNotificationSelected = "+isNotificationSelected);
        }
		//Kalyy 2015-02-04
		/*
        Intent breathLightIntent = new Intent(INTENT_CHANGE_BREATHINGING_LIGHT);
        this.sendBroadcast(breathLightIntent);
		*/
		//if(isLowBatterySelected)//Kalyy 20170122
		{
			SystemProperties.set(SYSTEM_PROPERTIEX_LED_LIGHT_LEVEL, "3");
	        Intent breathLightIntent = new Intent(INTENT_CHANGE_BREATHINGING_LIGHT);
	        this.sendBroadcast(breathLightIntent);
		}
		if(isNotificationSelected)
		{
			msgControl();
		}
		else
		{
			cancelLitghtNotification();
		}
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"BreathingLightService onDestroy.");
        super.onDestroy();
        putValueInSystem(KEY_IS_HAS_NOTIFICATION,false);
        Intent breathLightIntent = new Intent(INTENT_CHANGE_BREATHINGING_LIGHT);
        this.sendBroadcast(breathLightIntent);
        ungisterContentObServer();
        cancelLitghtNotification();
        updateFromSharedPreference();
        if (isNotificationSelected || isLowBatterySelected || isChargingSelected||isIncomingCallSelected||isMediaSelected) {//Kalyy 2015-02-04
            Intent sIntent = new Intent(BREATHING_LIGHT_SERVICE_ACTION);
            sIntent.setPackage(this.getPackageName());
            this.startService(sIntent);
        } else {

            /* hsj add 20151126 @{ */
            SystemProperties.set(SYSTEM_PROPERTIES_LOW_BATTERY_LED_ON, SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
            SystemProperties.set(SYSTEM_PROPERTIES_MID_BATTERY_LED_ON, SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
            SystemProperties.set(SYSTEM_PROPERTIES_NOTIFICATION_LED_ON, SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
            SystemProperties.set(SYSTEM_PROPERTIES_INCAMING_CALL_LED_ON, SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
            SystemProperties.set(SYSTEM_PROPERTIES_MEDIA_LED_ON, SYSTEM_PROPERTIEX_SHUTDOWN_CHARGING_LED_OFF);
            cancelLitghtNotification();
            /* @} */
			
            stopSelf();
        }
    }

    private void updateFromSharedPreference() {
        SharedPreferences sp = this.getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        isNotificationSelected = sp.getBoolean(KEY_NOTIFICATIONS, true);
        isLowBatterySelected = sp.getBoolean(KEY_LOW_BATTERY, true);
        isChargingSelected = sp.getBoolean(KEY_CHARGING, true);
        //Kalyy 2015-02-04
        isIncomingCallSelected = sp.getBoolean(KEY_INCOMING_CALL, true);
        isMediaSelected = sp.getBoolean(KEY_MEDIA, true);

        /*SUN:jicong.wang add for rgb start{@*/  
        if (OptConfig.SUN_RGB_SINGLE_FOR_LEDS){
            isNotificationSelected = false;
            isMediaSelected = false;           
        }
        /*SUN:jicong.wang add for rgb end @}*/        
    }

    /**
     * put the value in the Settigns db
     * @param key
     * @param value
     */
    private void putValueInSystem(String key,boolean value) {
        Settings.System.putInt(this.getContentResolver(), key, (value ? 1 : 0));
    }

    /**
     * register Db uri listener
     */
    private void registerContentObServer() {
        missedCallContentObServer = new DbContentObServer(new Handler());
        this.getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, 
                false, missedCallContentObServer);
        sMSContentObServer = new DbContentObServer(new Handler());
        this.getContentResolver().registerContentObserver(Uri.parse(SMS_URI), 
                true, sMSContentObServer);
        calendarObServer = new DbContentObServer(new Handler());
        this.getContentResolver().registerContentObserver(CalendarAlerts.CONTENT_URI, 
                false, calendarObServer);
        emailContentObServer = new DbContentObServer(new Handler());
        this.getContentResolver().registerContentObserver(Uri.parse(EMAIL_URI_MAILBOX), 
                false, emailContentObServer);
    }

    /**
     * ungister db uri listener
     */
    private void ungisterContentObServer() {
        if (missedCallContentObServer != null) {
            this.getContentResolver().unregisterContentObserver(missedCallContentObServer);
            missedCallContentObServer = null;
        }
        if (sMSContentObServer != null) {
            this.getContentResolver().unregisterContentObserver(sMSContentObServer);
            sMSContentObServer = null;
        }
        if (calendarObServer != null) {
            this.getContentResolver().unregisterContentObserver(calendarObServer);
            calendarObServer = null;
        }
        if (emailContentObServer != null) {
            this.getContentResolver().unregisterContentObserver(emailContentObServer);
            emailContentObServer = null;
        }
    }

    private int getMissedCall() {
        Cursor cursor = this.getContentResolver().query(Calls.CONTENT_URI, 
                new String[]{Calls.NUMBER,Calls.TYPE,Calls.NEW}, Calls.TYPE + " = " +
                Calls.MISSED_TYPE + " AND " + Calls.NEW + " = 1" , null, null);
        int unReadNum = 0;
        if (cursor != null) {
            try {
                unReadNum = cursor.getCount();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        return unReadNum;
    }

    private int getUnReadSms() {
        int unReadNum = 0;
        unReadNum = getUnReadMsg(SMS_URI_INBOX);
        unReadNum += getUnReadMsg(MMS_URI_INBOX);
        return unReadNum;
    }

    private int getUnReadMsg(String uri) {
        Uri msgUri = Uri.parse(uri);
        Cursor cursor = this.getContentResolver().query(msgUri, null, "read = 0", null, null);
        int unReadNum = 0;
        if (cursor != null) {
            try {
                unReadNum = cursor.getCount();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        return unReadNum;
    }
    
    private int getUnReadCalendar() {
        String selection = CalendarAlerts.STATE + "=" + CalendarAlerts.STATE_FIRED
        + " OR " + CalendarAlerts.STATE + "=" + ALERT_EXT_STATE_IGNORED;
        Cursor cursor = this.getContentResolver().query(CalendarAlerts.CONTENT_URI, null, selection, null, null);
        int unReadNum = 0;
        if (cursor != null) {
            try {
                unReadNum = cursor.getCount();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        return unReadNum;
    }

    private int getUnReadEmail() {
        Uri emailUri = Uri.parse(EMAIL_URI_MAILBOX);
        Cursor cursor = this.getContentResolver().query(emailUri, null, 
                "type = 0", null, null);
        int unReadNum = 0;
        if (cursor != null && cursor.getCount() > 0) {
            try {
                cursor.moveToFirst();
                unReadNum = cursor.getInt(cursor.getColumnIndex("unreadCount"));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        return unReadNum;
    }

		//qiuyaobo,remove light notify,20160929,begin
		/*
    private void showLightNotification() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
        }
        mNotificationManager.cancel(LIGHT_NOTIFICATION_ID);

        Notification.Builder builder = new Notification.Builder(this);  
        builder.setDefaults(Notification.DEFAULT_LIGHTS);
        builder.setSmallIcon(R.mipmap.ic_launcher_settings);
        Intent intent = new Intent();        
        PendingIntent pendIntent = PendingIntent.getActivity(this, 
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification lightNotification = builder.getNotification();
        lightNotification.setLatestEventInfo(this, null, null, pendIntent);       
        mNotificationManager.notify(LIGHT_NOTIFICATION_ID,lightNotification);       
    }
    */
		//qiuyaobo,remove light notify,20160929,end

    private void cancelLitghtNotification() {
    	  //qiuyaobo,remove light notify,20160929,begin
        //if (mNotificationManager != null) {
        //    mNotificationManager.cancel(LIGHT_NOTIFICATION_ID);
        //}
        //qiuyaobo,remove light notify,20160929,end
        
        Intent cancelLightIntent = new Intent(INTENT_CANCEL_BREATHINGING_LIGHT);
        this.sendBroadcast(cancelLightIntent);
    }

    private void startLight() {
        if (isContinue) {
            return;
        }
        isContinue = true;
        Intent breathLightIntent = new Intent(INTENT_CHANGE_BREATHINGING_LIGHT);
        this.sendBroadcast(breathLightIntent);
        
        //qiuyaobo,remove light notify,20160929,begin
        //showLightNotification();
        //qiuyaobo,remove light notify,20160929,end
    }

    private void msgControl() {
        /*//Kalyy 2015-02-04
        Log.d(TAG,"onChange isNotificationSelected: "+isNotificationSelected);
        if (!isNotificationSelected) {
            putValueInSystem(KEY_IS_HAS_NOTIFICATION,false);
            return;
        }
        */
        int light_level = Integer.parseInt(SystemProperties.get("persist.sys.led.light.level","5"));
        if(light_level<=1)
        {
        	return;
        }
        putValueInSystem(KEY_IS_HAS_NOTIFICATION,true);
        int missedCallNum = getMissedCall();
        Log.d(TAG,"onChange missedCallNum: "+missedCallNum);
        Log.d(TAG,"onChange isContinue: "+isContinue);
        if (missedCallNum > 0) {
            startLight();
            return;
        }
        int smsNum = getUnReadSms();
        Log.d(TAG,"onChange smsNum: "+smsNum);
        if (smsNum > 0) {
            startLight();
            return;
        }
        int calendarNum = getUnReadCalendar();
        Log.d(TAG,"onChange calendarNum: "+calendarNum);
        if (calendarNum > 0) {
            startLight();
            return;
        }
        int emailNum = getUnReadEmail();
        Log.d(TAG,"onChange emailNum: "+emailNum);
        if (emailNum > 0) {
            startLight();
            return;
        }
        isContinue = false;
        putValueInSystem(KEY_IS_HAS_NOTIFICATION,false);
        //Kalyy 2015-02-04
        /*
        Intent breathLightIntent = new Intent(INTENT_CHANGE_BREATHINGING_LIGHT);
        this.sendBroadcast(breathLightIntent);
        */
        cancelLitghtNotification();
    }
    
    private class QueryDbRunnable implements Runnable {

        public void run() {
            msgControl();
        }
        
    }

    private void notificationCtrl() {
        if (queryDbRunnable != null) {
            mHandler.removeCallbacks(queryDbRunnable);
            queryDbRunnable = null;
        }
        queryDbRunnable = new QueryDbRunnable();
        mHandler.postDelayed(queryDbRunnable, 500);
    }

    /**
     * Called when the db is changed
     *
     */
    private class DbContentObServer extends ContentObserver {

        public DbContentObServer(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG,"onChange.");
            notificationCtrl();
        }
        
    }
}
