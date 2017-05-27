/** Create by Spreadst */
package com.sprd.settings.timerpower;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Parcel;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.format.DateFormat;
import java.util.ArrayList;
/**
 * The Alarms provider supplies info about Alarm Clock settings
 */
public class Alarms {

    // This action triggers the AlarmReceiver as well as the AlarmKlaxon. It
    // is a public action used in the manifest for receiving Alarm broadcasts
    // from the alarm manager.
    public static final String ALARM_ALERT_ACTION = "com.android.settings.timerpower.ALARM_ALERT";
    public static final String TIMER_POWER_SHUTDOWN_ACTION = "com.android.settings.timerpower.SHUTDOWN";

    // A public action sent by AlarmKlaxon when the alarm has stopped sounding
    // for any reason (e.g. because it has been dismissed from AlarmAlertFullScreen,
    // or killed due to an incoming phone call, etc).
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    // AlarmAlertFullScreen listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    // AlarmAlertFullScreen listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    // This is a private action used by the AlarmKlaxon to update the UI to
    // show the alarm has been killed.
    public static final String ALARM_KILLED = "alarm_killed";

    // Extra in the ALARM_KILLED intent to indicate to the user how long the
    // alarm played before being killed.
    public static final String ALARM_KILLED_TIMEOUT = "alarm_killed_timeout";

    // This string is used to indicate a silent alarm in the db.
    public static final String ALARM_ALERT_SILENT = "silent";

    // This intent is sent from the notification when the user cancels the
    // snooze alert.
    public static final String CANCEL_SNOOZE = "cancel_snooze";

    // This string is used when passing an Alarm object through an intent.
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";

    // This extra is the raw Alarm object data. It is used in the
    // AlarmManagerService to avoid a ClassNotFoundException when filling in
    // the Intent extras.
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    // This string is used to identify the alarm id passed to SetAlarm from the
    // list of alarms.
    public static final String ALARM_ID = "alarm_id";

    final static String PREF_SNOOZE_ID = "snooze_id";
    final static String PREF_SNOOZE_TIME = "snooze_time";

    private final static String DM12 = "E h:mm aa";
    private final static String DM24 = "E k:mm";

    private final static String M12 = "h:mm aa";
    // Shared with DigitalClock
    final static String M24 = "kk:mm";

    // If FIRST_ALERT equals "true",that means it's the first alert after
    // boot_complete,
    // so the first alert's "poweroff" button turns visible,and FIRST_ALERT
    // turns false
    // after the first alert
    public static boolean FIRST_ALERT = false;

    // if FIRST_KLAXON equals "false",means it's the first klaxon since
    // boot_complete,
    // and,before the Alarmreceiver set the next alert,FIRST_KLAXON turns false
    public static boolean FIRST_KLAXON = false;

    // Flag for alarm exist or not
    private static File ALARM_FLAG_FILE = new File("/productinfo/poweron_timeinmillis");
    private static boolean bPoweron = false;

    /**
     * Queries all alarms
     * @return cursor over all alarms
     */
    public static Cursor getAlarmsCursor(ContentResolver contentResolver) {
        return contentResolver.query(
                Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, null);
    }

    // Private method to get a more limited set of alarms from the database.
    private static Cursor getFilteredAlarmsCursor(
            ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI,
                Alarm.Columns.ALARM_QUERY_COLUMNS, Alarm.Columns.WHERE_ENABLED,
                null, null);
    }

    private static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(8);
        // Set the alarm_time value if this alarm does not repeat. This will be
        // used later to disable expire alarms.
        long time = 0;
        if (!alarm.daysOfWeek.isRepeatSet()) {
            time = calculateAlarm(alarm);
        }

        values.put(Alarm.Columns.ENABLED, alarm.enabled ? 1 : 0);
        values.put(Alarm.Columns.HOUR, alarm.hour);
        values.put(Alarm.Columns.MINUTES, alarm.minutes);
        values.put(Alarm.Columns.ALARM_TIME, alarm.time);
        values.put(Alarm.Columns.DAYS_OF_WEEK, alarm.daysOfWeek.getCoded());
        values.put(Alarm.Columns.VIBRATE, alarm.vibrate);
        values.put(Alarm.Columns.MESSAGE, alarm.label);

        // A null alert Uri indicates a silent alarm.
        values.put(Alarm.Columns.ALERT, alarm.alert == null ? ALARM_ALERT_SILENT
                : alarm.alert.toString());

        return values;
    }

    /**
     * Return an Alarm object representing the alarm id in the database.
     * Returns null if no alarm exists.
     */
    public static synchronized Alarm getAlarm(Context context,ContentResolver contentResolver, int alarmId) {
        if(alarmId < 1){
            return null;
        }
        Log.v("timerpower Alarms ========== >>>>>>>>>>> Enter getAlarm "+alarmId);
        Log.v("timerpower Alarms ========== >>>>>>>>>>> Enter getAlarm "+Alarm.Columns.ALARM_QUERY_COLUMNS);
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId),
                Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, null);
        Alarm alarm = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                alarm = new Alarm(context,cursor);
            }
            cursor.close();
        }
        return alarm;
    }


    /**
     * A convenience method to set an alarm in the Alarms
     * content provider.
     * @return Time when the alarm will fire.
     */
    public static long setAlarm(Context context, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        ContentResolver resolver = context.getContentResolver();

        Log.v("timerpower Alarms setAlarm Update : "+alarm.id);
        resolver.update(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id),
                values, null, null);

        long timeInMillis = calculateAlarm(alarm);
        Log.v("timerpower Alarms setAlarm Update : "+alarm.enabled);
        setNextAlert(context);
        Log.v("timerpower Alarms setAlarm Update timeInMillis: "+timeInMillis);

        return timeInMillis;
    }

    /**
     * A convenience method to enable or disable an alarm.
     *
     * @param id             corresponds to the _id column
     * @param enabled        corresponds to the ENABLED column
     */

    public static void enableAlarm(
            final Context context, final int id, boolean enabled) {

        Log.v("timerpower Alarms ========== >>>>> Enter enableAlarm  enabled = " + enabled);

        enableAlarmInternal(context, id, enabled);
        setNextAlert(context);
    }

    private static void enableAlarmInternal(final Context context,
            final int id, boolean enabled) {

        Log.v("timerpower Alarms ========== >>>>> Enter enableAlarmInternal");
        enableAlarmInternal(context, getAlarm(context,context.getContentResolver(), id),
                enabled);
    }

    private static void enableAlarmInternal(final Context context,
            final Alarm alarm, boolean enabled) {
        if (alarm == null) {
            return;
        }

        Log.v("timerpower Alarms ========== >>>>> Enter enableAlarmInternal enabled = " + enabled);

        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues(2);
        values.put(Alarm.Columns.ENABLED, enabled ? 1 : 0);

        // If we are enabling the alarm, calculate alarm time since the time
        // value in Alarm may be old.
        if (enabled) {
            // fix bug 208077  dead local store on 2013-08-27 begin
            /*long time = 0;
            if (!alarm.daysOfWeek.isRepeatSet()) {
                time = calculateAlarm(alarm);
            }*/
            // SPRD:ADD calculate the alarm time.
            alarm.time = calculateAlarm(alarm);
            // fix bug 208077 dead local store on 2013-08-27 end
            // fix bug 202305 to make timing shutdown work when change TimeZone on 2013-08-22 begin
            values.put(Alarm.Columns.ALARM_TIME, alarm.time);
            // fix bug 202305 to make timing shutdown work when change TimeZone on 2013-08-22 end
        } else {
            // Clear the snooze if the id matches.
            //disableSnoozeAlert(context, alarm.id);
        }
        resolver.update(ContentUris.withAppendedId(
                Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
    }

    public static Alarm calculateNextAlert(final Context context) {
        Alarm alarm = null;
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        Cursor cursor = getFilteredAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Alarm a = new Alarm(context,cursor);
                    // A time of 0 indicates this is a repeating alarm, so
                    // calculate the time to get the next alert.
                    if (a.time == 0) {
                        a.time = calculateAlarm(a);
                    } else if (a.time < now) {
                        Log.v("Disabling expired alarm set for " +
                              Log.formatTime(a.time));
                        // Expired alarm, disable it and move along.
                        enableAlarmInternal(context, a, false);
                        continue;
                    }
                    if (a.time < minTime) {
                        minTime = a.time;
                        alarm = a;
                        Log.v("set minTime = " + minTime);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return alarm;
    }

    // Modify for fix bug 146374 start
    // get all enable alerts
    public static List<Alarm> calculateNextAlerts(final Context context) {
        List<Alarm> alarms = new ArrayList<Alarm>();
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        Cursor cursor = getFilteredAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Alarm a = new Alarm(context, cursor);
                    // A time of 0 indicates this is a repeating alarm, so
                    // calculate the time to get the next alert.
                    if (a.time == 0) {
                        a.time = calculateAlarm(a);
                    } else if (a.time < now) {
                        Log.v("Disabling expired alarm set for " + Log.formatTime(a.time));
                        // Expired alarm, disable it and move along.
                        // SPRD:ADD check the repeat set enable.
                        enableAlarmInternal(context, a, a.daysOfWeek.isRepeatSet());
                        continue;
                    }
                    if (a.time < minTime) {
                        // minTime = a.time;
                        alarms.add(a);
                        // alarm = a;
                        Log.v("set minTime = " + minTime);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return alarms;
    }

    // Modify for fix bug 146374 end
    /**
     * Disables non-repeating alarms that have passed.  Called at
     * boot.
     */
    public static void disableExpiredAlarms(final Context context) {
        Cursor cur = getFilteredAlarmsCursor(context.getContentResolver());
        long now = System.currentTimeMillis();

        if (cur.moveToFirst()) {
            do {
                Alarm alarm = new Alarm(context,cur);
                // A time of 0 means this alarm repeats. If the time is
                // non-zero, check if the time is before now.
                if (alarm.time != 0 && alarm.time < now) {
                    Log.v("Disabling expired alarm set for " +
                          Log.formatTime(alarm.time));
                    enableAlarmInternal(context, alarm, false);
                }
            } while (cur.moveToNext());
        }
        cur.close();
    }

    /**
     * Called at system startup, on time/timezone change, and whenever
     * the user changes alarm settings.  Activates snooze if set,
     * otherwise loads all alarms, activates next alert.
     */
    public static void setNextAlert(final Context context) {
        Log.v("wanghaiying setNextAlert");
        // Alarm alarm = calculateNextAlert(context);
        // Modify for fix bug 146374 start
        List<Alarm> alarms = calculateNextAlerts(context);
        Log.v("call disableAlert");
        disableAlert(context);
        for (Alarm alarm : alarms) {
            if (alarm != null) {
                Log.v("enableAlert alarm.time:" + alarm.time);
                enableAlert(context, alarm, alarm.time);
            }
        }
    }

    /**
     * Sets alert in AlarmManger and StatusBar.  This is what will
     * actually launch the alert when the alarm triggers.
     *
     * @param alarm Alarm.
     * @param atTimeInMillis milliseconds since epoch
     */
    private static void enableAlert(Context context, final Alarm alarm,
            final long atTimeInMillis) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        if (Log.LOGV) {
            Log.v("** setAlert id " + alarm.id + " atTime " + atTimeInMillis);
            Log.v("** setAlert lable[" + alarm.label + "]");
        }

        if(alarm.label != null && alarm.label.equals("on")) {
            Log.v("Alarms enableAlert power on need write files");
            bPoweron = true;
            alarm_flag_setup(atTimeInMillis);

            Intent intent = new Intent(ALARM_ALERT_ACTION);
            Parcel out = Parcel.obtain();
            alarm.writeToParcel(out, 0);
            out.setDataPosition(0);
            intent.putExtra(ALARM_RAW_DATA, out.marshall());
            PendingIntent sender = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            /* Modify at 2013-3-21, for fix bug 140131 start */
            am.cancelAlarm(sender);
            //SPRD : For bug 447224
            am.set(AlarmManager.POWER_ON_WAKEUP, atTimeInMillis, sender);
        } else {
            Log.v("Alarms enableAlert power off");
            bPoweron =false;
            Intent intent = new Intent(TIMER_POWER_SHUTDOWN_ACTION);
            Parcel out = Parcel.obtain();
            alarm.writeToParcel(out, 0);
            out.setDataPosition(0);
            intent.putExtra(ALARM_RAW_DATA, out.marshall());
            PendingIntent sender = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        }
    }

    /**
     * Disables alert in AlarmManger and StatusBar.
     *
     * @param id Alarm ID.
     */
    static void disableAlert(Context context) {
        Log.v("timerpower Alarms-----------disableAlert");
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, new Intent(ALARM_ALERT_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancelAlarm(sender);
        PendingIntent sender1 = PendingIntent.getBroadcast(
                context, 0, new Intent(TIMER_POWER_SHUTDOWN_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancelAlarm(sender1);
        alarm_flag_cancel();
    }

    private static long calculateAlarm(Alarm alarm) {
        return calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek)
                .getTimeInMillis();
    }

    /**
     * Given an alarm in hours and minutes, return a time suitable for
     * setting in AlarmManager.
     */
    static Calendar calculateAlarm(int hour, int minute,
            Alarm.DaysOfWeek daysOfWeek) {

        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);
        int nowSecond = c.get(Calendar.SECOND);

        Log.v("timerpower Alarms ========== >>>>> Enter calculateAlarm  ");

        // if alarm is behind current time, advance one day for power off clock
        if (hour < nowHour || hour == nowHour && minute < nowMinute || hour == nowHour
                && minute == nowMinute && 30 < nowSecond && Alarms.FIRST_KLAXON
                && SystemProperties.get("ro.bootmode", "unknown").equals("alarm")
                || hour == nowHour && minute == nowMinute && !Alarms.FIRST_KLAXON) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }

        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = daysOfWeek.getNextAlarm(c);
        if (addDays > 0) c.add(Calendar.DAY_OF_WEEK, addDays);
        return c;
    }

    static String formatTime(final Context context, int hour, int minute,
                             Alarm.DaysOfWeek daysOfWeek) {
        Calendar c = calculateAlarm(hour, minute, daysOfWeek);
        return formatTime(context, c);
    }

    /* used by AlarmAlert */
    static String formatTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? M24 : M12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * Shows day and time -- used for lock screen
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? DM24 : DM12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * Save time of the next alarm, as a formatted string, into the system
     * settings so those who care can make use of it.
     */
    static void saveNextAlarm(final Context context, String timeString) {
        Settings.System.putString(context.getContentResolver(),
                                  Settings.System.NEXT_ALARM_FORMATTED,
                                  timeString);
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    /**
     * check if there is a alarm which has the same hour and minute
     *
     * @param cr
     * @param hour
     * @param minute
     * @param alarmId
     * @return
     */
    public static boolean isSametimeAlarm(ContentResolver cr, int hour, int minute, int alarmId) {
        boolean flag = false;
        Cursor cursor = cr.query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                Alarm.Columns.HOUR + " = " + new Integer(hour).toString() + " AND "
                        + Alarm.Columns.MINUTES + " = " + new Integer(minute).toString()
                        + " AND _id != " + alarmId, null, Alarm.Columns.DEFAULT_SORT_ORDER);
        flag = cursor.getCount() > 0;
        cursor.close();
        return flag;
    }

    /**
     * function to set up alarm flag
     *
     * @return
     */
    private static void alarm_flag_setup(final long alarmTimeInMillis) {
        Calendar c = Calendar.getInstance();
        c.set(2012, 0, 1, 0, 0, 0);//jiazhenl modify from 2012 to 2016 //Kalyy modify
        Calendar to = Calendar.getInstance();
        to.setTimeInMillis(alarmTimeInMillis);
        TimeZone zone = c.getTimeZone();
        long dstOffset = zone.getOffset(alarmTimeInMillis);
        long startTimeInMillis = c.getTimeInMillis();
        long dstAlarmTimeInMillis = alarmTimeInMillis - dstOffset;
        long timeDiffInMillis = dstAlarmTimeInMillis - startTimeInMillis;
        long timeDiffInSecs = timeDiffInMillis/1000;

        Log.v("write " + String.valueOf(timeDiffInSecs) + " to" + ALARM_FLAG_FILE);

        if (ALARM_FLAG_FILE.exists()) {
            Log.v(ALARM_FLAG_FILE + " already exist, delete it");
            try {
                ALARM_FLAG_FILE.delete();
                Log.v(ALARM_FLAG_FILE + " delete before write success");
            } catch (Exception e) {
                Log.v(ALARM_FLAG_FILE + " delete before write failed");
            }
        }

        FileOutputStream command = null;
        try {
            command = new FileOutputStream(ALARM_FLAG_FILE);
            command.write(String.valueOf(timeDiffInSecs).getBytes());
            command.write("\n".getBytes());
            command.write(String.valueOf(alarmTimeInMillis / 1000).getBytes());
            command.write("\n".getBytes());

            command.flush();
            command.getFD().sync();
            command.close();
            Log.v(ALARM_FLAG_FILE + " write done");
            command = null;
        } catch (Exception e) {
            Log.v(ALARM_FLAG_FILE + " write error : " + e.getMessage());
        } finally {
            if (null != command) {
                try {
                    command.close();
                } catch (IOException e) {
                    Log.v("FileOutputStream close error : " + e.getMessage());
                }
            }
        }
    }

    /**
     * function to cancel alarm flag
     *
     * @return
     */
    private static void alarm_flag_cancel() {
        if (ALARM_FLAG_FILE.exists()&& bPoweron) {
            Log.v(ALARM_FLAG_FILE + " exist");
            try {
                ALARM_FLAG_FILE.delete();
                Log.v(ALARM_FLAG_FILE + " delete success");
            } catch (Exception e) {
                Log.v(ALARM_FLAG_FILE + " delete failed");
            }
        } else {
            Log.v(ALARM_FLAG_FILE + " already delete");
        }
    }
}
