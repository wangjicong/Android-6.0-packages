/** Create by Spreadst */
package com.sprd.settings.timerpower;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert
 * activity.  Passes through Alarm ID.
 */
public class AlarmReceiver extends BroadcastReceiver {

    /** If the alarm is older than STALE_WINDOW, ignore.  It
        is probably the result of a time or timezone change */
    private final static int STALE_WINDOW = 30 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("AlarmReceiver ----------- intent = " + intent);
        if (Alarms.TIMER_POWER_SHUTDOWN_ACTION.equals(intent.getAction())) {
            // Power OFF
            Alarm alarm = null;
            // Grab the alarm from the intent. Since the remote AlarmManagerService
            // fills in the Intent to add some extra data, it must unparcel the
            // Alarm object. It throws a ClassNotFoundException when unparcelling.
            // To avoid this, do the marshalling ourselves.
            final byte[] data = intent.getByteArrayExtra(Alarms.ALARM_RAW_DATA);
            if (data != null) {
                Parcel in = Parcel.obtain();
                in.unmarshall(data, 0, data.length);
                in.setDataPosition(0);
                alarm = Alarm.CREATOR.createFromParcel(in);
            }

            if (alarm == null) {
                Log.wtf("Failed to parse the alarm from the intent");
                // Make sure we set the next alert if needed.
                Alarms.setNextAlert(context);
                return;
            }

            //if  alarm.label = on
            if(alarm.label != null && alarm.label.equals("on"))
            {
                Log.v("alarm.label = on");
                return;
            }

            // Disable the snooze alert if this alarm is the snooze.
            //Alarms.disableSnoozeAlert(context, alarm.id);
            // Disable this alarm if it does not repeat.
            if (!alarm.daysOfWeek.isRepeatSet()) {
                Alarms.enableAlarm(context, alarm.id, false);
            } else {
                // Enable the next alert if there is one. The above call to
                // enableAlarm will call setNextAlert so avoid calling it twice.
                Alarms.setNextAlert(context);
            }

            // Intentionally verbose: always log the alarm time to provide useful
            // information in bug reports.
            long now = System.currentTimeMillis();
            Log.v("Recevied alarm set for " + Log.formatTime(alarm.time));
            // Always verbose to track down time change problems.
            if (now > alarm.time + STALE_WINDOW) {
                Log.v("Ignoring stale timer power shutdown");
                return;
            }

            // Maintain a cpu wake lock
            AlarmAlertWakeLock.acquireCpuWakeLock(context);
            /* Modify at 2013-04-02 , for porting code from 4.0 to 4.1 start */
            // unlock screen
            // KeyguardManager keyguardManager =
            // (KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
            // KeyguardLock keyguardLock =
            // keyguardManager.newKeyguardLock(Log.LOGTAG);
            // keyguardLock.disableKeyguard();
            /* Modify at 2013-04-02 , for porting code from 4.0 to 4.1 end */
            // start the service
            Log.v("AlarmReceiver startService");
            Intent playAlarm = new Intent();
            playAlarm.setClass(context, AlarmKlaxon.class);
            playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
            context.startService(playAlarm);

//            Log.v("power off");
//            Intent intent1 = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
//            intent1.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
//            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent1);

            // Make sure we set the next alert if needed.
            Alarms.setNextAlert(context);
            return;
        }

    }
}
