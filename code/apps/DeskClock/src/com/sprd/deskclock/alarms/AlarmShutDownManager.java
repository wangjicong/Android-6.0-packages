package com.sprd.deskclock.alarms;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;

import com.sprd.deskclock.AplicationSupportChange;
import com.sprd.deskclock.LogSprd;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.AlarmInstance;
import android.os.SystemProperties;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class AlarmShutDownManager {

    /*
     * if FIRST_KLAXON equals "false",means it's the first klaxon
     * sinceboot_complete, and before the Alarmreceiver set the next
     * alert,FIRST_KLAXON turns false
     */
    public static boolean FIRST_KLAXON = false;
    // Flag for alarm exist or not
    private static File ALARM_FLAG_FILE = new File("/productinfo/alarm_flag");

    public static Handler mWriteShutDownFileHandler;

    public static HandlerThread mWriteShutDownFileHandlerThread;

    public static final String WRITE_SHUTDOWN_FILE = "WriteShutDownFile";
    // Buffer time in seconds to fire alarm instead of marking it missed.
    public static final int ALARM_FIRE_BUFFER = 15;
    public static final String RTC_ACTION = "com.sprd.deskclock.rtc_state";
    private static final String ALARM_GLOBAL_ID_EXTRA = "intent.extra.alarm.global.id";
    public static final String ALARM_STATE_EXTRA = "intent.extra.alarm.state";
    private static final boolean  SUPPORT_SHUTDOWNALARM = AplicationSupportChange.SUPPORT_SHUTDOWN_ALARM;
    public static String ALARM_MANAGER_TAG = "ALARM_MANAGER";

    /**
     * function to set up alarm flag
     */
    public static void alarmFlagSetup(Context context,AlarmInstance alarm) {
        long timeDiffInSecs = 0;
        try {
            InputStreamReader inputReader = null;
            File fileName = new File("/sys/class/rtc/rtc0/default_time");
            if(fileName.exists()) {
                try{
                    inputReader = new FileReader(fileName);
                    char tmp[] = new char[12];
                    int numRead = inputReader.read(tmp);
                    if( numRead > 0) {
                        String default_time = new String(tmp).substring(0, numRead - 1);
                        long ltime = Long.parseLong(default_time);
                        timeDiffInSecs = alarm.getAlarmTime().getTimeInMillis()/1000 - ltime;
                    }
                } catch(FileNotFoundException e) {
                    LogSprd.e("/sys/class/rtc/rtc0/default_time.");
                } catch(NumberFormatException e) {
                    LogSprd.e("!!!!!!!number format error.!!!!!!!!");
                    e.printStackTrace();
                }finally {
                    inputReader.close();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        if (ALARM_FLAG_FILE.exists()) {
            try {
                boolean deletesuccess = ALARM_FLAG_FILE.delete();
            } catch (Exception e) {
                LogSprd.v(ALARM_FLAG_FILE + " delete before write failed");
            }
        }

        FileOutputStream command = null;
        try {
            command = new FileOutputStream(ALARM_FLAG_FILE);
            command.write(String.valueOf(timeDiffInSecs).getBytes());
            command.write("\n".getBytes());
            command.write(String.valueOf(alarm.getAlarmTime().getTimeInMillis() / 1000).getBytes());
            command.write("\n".getBytes());
            alarmDurationSetup(context, command, alarm);
            // add 12m,or 24m
            boolean timeDM = get24HourMode(context);
            if (timeDM == true) {
                // 24m
                command.write(String.valueOf(24).getBytes());
                command.write("\n".getBytes());
            } else {
                command.write(String.valueOf(12).getBytes());
                command.write("\n".getBytes());
            }
            alarmPathSetup(getAlarmPath(context, alarm), command);

        } catch (Exception e) {
            LogSprd.v(ALARM_FLAG_FILE + "write error");
        } finally {
            try {
                command.flush();
                command.getFD().sync();
                LogSprd.v(ALARM_FLAG_FILE + " write sync ");
                command.close();
                LogSprd.v(ALARM_FLAG_FILE + " write done");
            } catch (Exception e2) {

            }
        }
    }


    /**
     * function to set up alarm flag
     */
    private  static void alarmDurationSetup(Context context, FileOutputStream command, AlarmInstance alarm) {

        final String auto_slience =
                PreferenceManager.getDefaultSharedPreferences(context)
                .getString(alarm.mAlarmId + "",
                        "10");
        final String snooze_interval =
            PreferenceManager.getDefaultSharedPreferences(context)
            .getString(SettingsActivity.KEY_ALARM_SNOOZE,
                    "10");
        try {
                command.write(auto_slience.getBytes());
                command.write("\n".getBytes());

                command.write(snooze_interval.getBytes());
                command.write("\n".getBytes());
        } catch (Exception e) {
            LogSprd.v(ALARM_FLAG_FILE + "write error");
        }finally{

        }
    }


    /**
     * function to set up alarm flag
     */
    private static  void alarmPathSetup(String alarmPath,FileOutputStream command) {
        try {
            command.write(alarmPath.getBytes());
            command.write("\n".getBytes());
        } catch (Exception e) {
            LogSprd.v(ALARM_FLAG_FILE + "write error");
        } finally {

            LogSprd.v(ALARM_FLAG_FILE + " write done");
        }
    }

    /**
     * function to cancel alarm flag
     */
    public static void alarmFlagCancel() {
        if (ALARM_FLAG_FILE.exists()) {
            try {
                boolean deletesuccess = ALARM_FLAG_FILE.delete();
                if(deletesuccess){
                    LogSprd.v(ALARM_FLAG_FILE + " delete success");
                }
            } catch (Exception e) {
                LogSprd.v(ALARM_FLAG_FILE + " delete failed");
            }
        } else {
            LogSprd.v(ALARM_FLAG_FILE + " already delete");
        }
    }

    private static String getAlarmPath(Context context,AlarmInstance alarm){
        String alert = alarm.mRingtone.toString();
        Uri alertUri = null;
        if(alert.contains("alarm_alert")){
            String value = Settings.System.getString(context.getContentResolver(), "alarm_alert");
            alertUri = Uri.parse(value);
        }else{
            alertUri = alarm.mRingtone;
        }
        String [] project = {
                "_data"
        };
        String path = "";
        Cursor cursor = context.getContentResolver().query(alertUri, project, null, null, null);
        try{
            if(cursor != null && cursor.moveToFirst()){
                    path = cursor.getString(0);
            }
        } catch (Exception ex){

        }finally{
            if(cursor != null){
                cursor.close();
                cursor = null;
            }
        }
        return path;
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    public static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    public static void createOrUpdateShutDownFile(final Context context,
            final AlarmInstance alarmInstance) {
        if (!SUPPORT_SHUTDOWNALARM) {
            return;
        }
        if (alarmInstance == null) {
            return;
        }
        if (mWriteShutDownFileHandlerThread == null) {
            mWriteShutDownFileHandlerThread = new HandlerThread(WRITE_SHUTDOWN_FILE);
            mWriteShutDownFileHandlerThread.start();
        }
        if (mWriteShutDownFileHandler == null) {
            mWriteShutDownFileHandler = new Handler(mWriteShutDownFileHandlerThread.getLooper());
        }
        if (mWriteShutDownFileHandler != null) {
            mWriteShutDownFileHandler.post(new Runnable() {
                @Override
                public void run() {
                    alarmFlagSetup(context, alarmInstance);
                }
            });
        }
    }

    public static void createOrUpdateShutDownFile(final Context context,
            final AlarmInstance alarmInstance, AlarmManager am, int newState ,boolean isKitKatOrLater) {
        if (alarmInstance == null) {
            return;
        }
        if (!SUPPORT_SHUTDOWNALARM) {
            return;
        }
        Intent rtcIntent = AlarmShutDownManager.createRTCIntent(context, ALARM_MANAGER_TAG,
                alarmInstance,
                newState);
        PendingIntent RTCPendingIntent = PendingIntent.getBroadcast(context,
                alarmInstance.hashCode(),
                rtcIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        LogSprd.v("AlarmStateManager AlarmManager set");
        boolean isPowerOff = android.os.SystemProperties.get(
                "ro.poweroffalarm", "default").equals("enabled");
        LogSprd.v("ro.poweroffalarm:"+android.os.SystemProperties.get("ro.poweroffalarm", "default"));
        int powerOffType;
        if (isPowerOff) {
            powerOffType = AlarmManager.POWER_OFF_WAKEUP;
        } else {
            powerOffType = AlarmManager.POWER_OFF_ALARM;
        }
        if (isKitKatOrLater) {
            am.setExact(powerOffType, alarmInstance.getAlarmTime()
                    .getTimeInMillis(), RTCPendingIntent);
        } else {
            am.set(powerOffType, alarmInstance.getAlarmTime()
                    .getTimeInMillis(), RTCPendingIntent);
        }
        if (mWriteShutDownFileHandlerThread == null) {
            mWriteShutDownFileHandlerThread = new HandlerThread(WRITE_SHUTDOWN_FILE);
            mWriteShutDownFileHandlerThread.start();
        }
        if (mWriteShutDownFileHandler == null) {
            mWriteShutDownFileHandler = new Handler(mWriteShutDownFileHandlerThread.getLooper());
        }
        if (mWriteShutDownFileHandler != null) {
            mWriteShutDownFileHandler.post(new Runnable() {
                @Override
                public void run() {
                    alarmFlagSetup(context, alarmInstance);
                }
            });
        }
    }

    public static void deleteShutDownFile() {
        if (!SUPPORT_SHUTDOWNALARM) {
            return;
        }
        if (mWriteShutDownFileHandlerThread == null) {
            mWriteShutDownFileHandlerThread = new HandlerThread(WRITE_SHUTDOWN_FILE);
            mWriteShutDownFileHandlerThread.start();
        }
        if (mWriteShutDownFileHandler == null) {
            mWriteShutDownFileHandler = new Handler(mWriteShutDownFileHandlerThread.getLooper());
        }
        if (mWriteShutDownFileHandler != null) {
            mWriteShutDownFileHandler.post(new Runnable() {
                @Override
                public void run() {
                    alarmFlagCancel();
                }
            });
        }
    }

    private static Intent createRTCIntent(Context context, String tag,
            AlarmInstance instance, Integer state) {
        Intent intent = AlarmInstance.createIntent(context, AlarmStateManager.class, instance.mId);
        intent.setAction(RTC_ACTION);
        intent.addCategory(tag);
        intent.putExtra(ALARM_GLOBAL_ID_EXTRA, getGlobalIntentId(context));
        if (state != null) {
            intent.putExtra(ALARM_STATE_EXTRA, state.intValue());
        }
        return intent;
    }

    private static int getGlobalIntentId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(ALARM_GLOBAL_ID_EXTRA, -1);
    }
}
