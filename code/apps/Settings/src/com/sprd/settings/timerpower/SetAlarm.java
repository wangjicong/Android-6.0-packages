/** Create by Spreadst */
package com.sprd.settings.timerpower;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.TimePicker;

import com.android.settings.R;
import com.android.settings.Settings;

/**
 * Manages each alarm
 */
public class SetAlarm extends PreferenceActivity
        implements TimePickerDialog.OnTimeSetListener,
        Preference.OnPreferenceChangeListener {

    private Preference mTimePref;
    private RepeatPreference mRepeatPref;

    private int     mId;
    private int     mHour;
    private int     mMinutes;
    private boolean mTimePickerCancelled;
    private Alarm   mOriginalAlarm;

    /* SPRD: Add for bug425245. @{ */
    private TimePicker mTimePicker;
    private int     mCurrentHour;
    private int     mCurrentMinutes;
    /* @} */

    private Toast sameAlarmToast ;
    public boolean isSametimeAlarm = false;

    private String strLabel;
    private boolean bEnable;

    private Alarm mAlarm = new Alarm();
    private static int DAYS_ERROR = -1;
    private final int ALARM_ON = 1;
    private final int ALARM_OFF = 2;
    // SPRD: ADD to mark timePickerDislog status
    private TimePickerDialog mTimePickerDialog;
    private static final String IS_TIMEPICKER_SHOWING =  "is_timepicker_showing";
    /**
     * Set an alarm.  Requires an Alarms.ALARM_ID to be passed in as an
     * extra. FIXME: Pass an Alarm object like every other Activity.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Override the default content view.
        setContentView(R.layout.set_alarm);

        addPreferencesFromResource(R.xml.alarm_prefs);

        mTimePref = findPreference("time");
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");
        mRepeatPref.setOnPreferenceChangeListener(this);

        Intent i = getIntent();
        mId = i.getIntExtra(Alarms.ALARM_ID, -1);

        Log.v("timerpower SetAlarm ============ >>>>  getIntExtra-Alarms.ALARM_ID : "+mId);

        if (Log.LOGV) {
            Log.v("In SetAlarm, alarm id = " + mId);
        }

        Alarm alarm = null;
        if (mId == -1) {
            // No alarm id means create a new alarm.
            alarm = new Alarm();
        } else {
            /* load alarm details from database */
            alarm = Alarms.getAlarm(this,getContentResolver(), mId);
            // Bad alarm, bail to avoid a NPE.
            if (alarm == null) {
                finish();
                return;
            } else if (mId == ALARM_ON) {
                this.setTitle(R.string.set_alarm_on);
            } else if (mId == ALARM_OFF) {
                this.setTitle(R.string.set_alarm_off);
            }
        }
        mOriginalAlarm = alarm;

        strLabel = alarm.label;
        bEnable = alarm.enabled;

        Log.v("timerpower SetAlarm ============ >>>>  getIntExtra-alarm.id : "+alarm.id);

        updatePrefs(mOriginalAlarm);

        // We have to do this to get the save/cancel buttons to highlight on
        // their own.
        getListView().setItemsCanFocus(true);

        // Attach actions to each button.
        Button b = (Button) findViewById(R.id.alarm_save);
        b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    boolean hasDuplicateDate = hasDuplicateAlarmDate();

                    //if there is a alarm witch has the same hour, minutes and repeat date, show timepicker again
                    if(Alarms.isSametimeAlarm(getContentResolver(), mHour, mMinutes,mId) && hasDuplicateDate){
                        Log.v("show sameAlarm Dialog");
                        sameAlarmShow(mHour, mMinutes);
                        return;
                    }
                    saveAlarm(strLabel,true);
                    popAlarmSetToast(SetAlarm.this, mHour,mMinutes,
                           mRepeatPref.getDaysOfWeek());
                    finish();
                }
        });
        final Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setEnabled(false);
        revert.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int newId = mId;
                    updatePrefs(mOriginalAlarm);
                    // "Revert" on a newly created alarm should delete it.
                    if (mOriginalAlarm.id == -1) {
//                        Alarms.deleteAlarm(SetAlarm.this, newId);
                    } else {
                        saveAlarm(strLabel,bEnable);
                    }
                    revert.setEnabled(false);
                }
        });

        // The last thing we do is pop the time picker if this is a new alarm.
        if (mId == -1) {
            // Assume the user hit cancel
            mTimePickerCancelled = true;
            showTimePicker();
        }
    }

    // Used to post runnables asynchronously.
    private static final Handler sHandler = new Handler();

    public boolean onPreferenceChange(final Preference p, Object newValue) {
        // Asynchronously save the alarm since this method is called _before_
        // the value of the preference has changed.
        if(Alarms.isSametimeAlarm(getContentResolver(), mHour, mMinutes,mId) && hasDuplicateAlarmDate()){
            // SPRD: Add for Bug 447577.
            checkChangeTimeAndDate(mHour, mMinutes);
            return true;
        }
        sHandler.post(new Runnable() {
            public void run() {
                Log.v("timerpower SetAlarm    setAlarm      onPreferenceChange");
                /* SPRD: check when date change for bug 427627 @{ */
                checkChangeTimeAndDate(mHour, mMinutes);
                /* @} */
                saveAlarmAndEnableRevert(strLabel,bEnable);
            }
        });
        return true;
    }

    private void updatePrefs(Alarm alarm) {
        mId = alarm.id;
        mHour = alarm.hour;
        mMinutes = alarm.minutes;
        mRepeatPref.setDaysOfWeek(alarm.daysOfWeek);
        updateTime();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mTimePref) {
            showTimePicker();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * reCreate timepickerdialog if it is showing when land screen
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(
            android.content.res.Configuration newConfig) {
        if (mTimePickerDialog != null && mTimePickerDialog.isShowing()) {
            /* SPRD: Save the current time of timepickerdialog. */
            mTimePicker = mTimePickerDialog.getTime();
            mCurrentHour = mTimePicker.getCurrentHour();
            mCurrentMinutes = mTimePicker.getCurrentMinute();
            mTimePickerDialog.dismiss();
            //showTimePicker();
            showCurrentTimePicker();
            /* @} */
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        // In the usual case of viewing an alarm, mTimePickerCancelled is
        // initialized to false. When creating a new alarm, this value is
        // assumed true until the user changes the time.
        /* SPRD: modify code for bug 457964. */
        boolean hasDuplicateDate = hasDuplicateAlarmDate();
        if (!mTimePickerCancelled&&!isSametimeAlarm&&!hasDuplicateDate) {
            saveAlarm(strLabel,bEnable);
        }
        /* @} */
        finish();
    }

    private void showTimePicker() {
        // SPRD: modify to mark timePickerDislog status
        mTimePickerDialog = new TimePickerDialog(this, this, mHour, mMinutes,
                DateFormat.is24HourFormat(this));
        mTimePickerDialog.show();
    }

    /**
     * SPRD: Add for bug425245. @{
     */
    private void showCurrentTimePicker() {
        mTimePickerDialog = new TimePickerDialog(this, this, mCurrentHour, mCurrentMinutes,
                DateFormat.is24HourFormat(this));
        mTimePickerDialog.show();
    }
    /* @} */

    private int getDaysCodeFromDB(){
        int code = DAYS_ERROR;
        //get code from database;
        ContentResolver cr = getContentResolver();
        if(cr != null){
            Cursor cursor = cr.query(Alarm.Columns.CONTENT_URI, null,
                    Alarm.Columns.MESSAGE + "!=" + "'" + strLabel + "'", null,null);
            if(cursor !=null){
                if(cursor.moveToFirst()){
                    code = cursor.getInt(cursor.getColumnIndex(Alarm.Columns.DAYS_OF_WEEK));
                }
                cursor.close();
            }
        }
        return code;
    }

    private boolean hasDuplicateAlarmDate(){
        int code = DAYS_ERROR, days = DAYS_ERROR;
        boolean hasDuplicateDate = false;

        code = getDaysCodeFromDB();
        //update mAlarm;
        mAlarm.daysOfWeek = mRepeatPref.getDaysOfWeek();
        mAlarm.id = mId;
        mAlarm.hour = mHour;
        mAlarm.minutes = mMinutes;
        mAlarm.label = strLabel;
        mAlarm.enabled = bEnable;
        if(mAlarm.daysOfWeek != null){
            days = mAlarm.daysOfWeek.getCoded();
            hasDuplicateDate = mAlarm.daysOfWeek.hasDuplicateDate(days, code);
            Log.v("hasDuplicateAlarm --- days =" + days +", code = " + code);
            Log.v("hasDuplicateAlarm --- hasDuplicateDate = " + hasDuplicateDate);
        }
        return hasDuplicateDate;
    }
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // onTimeSet is called when the user clicks "Set"
        mTimePickerCancelled = false;
        //if there is a alarm witch has the same hour and minutes , show timepicker again
        isSametimeAlarm = Alarms.isSametimeAlarm(getContentResolver(), hourOfDay, minute,mId);

        boolean hasDuplicateDate = hasDuplicateAlarmDate();
        if(Alarms.isSametimeAlarm(getContentResolver(), hourOfDay, minute,mId) == true && hasDuplicateDate){
            sameAlarmShow(hourOfDay, minute);
            return;
        }

        mHour = hourOfDay;
        mMinutes = minute;
        updateTime();
        /* SPRD 426908 @{ */
        //final Button revert = (Button) findViewById(R.id.alarm_revert);
        //revert.setEnabled(true);
        checkChangeTimeAndDate(hourOfDay, minute);
        /* @} */
        // SPRD: Add for Bug 442760
        saveAlarmAndEnableRevert(strLabel,bEnable);
    }
    /* SPRD 426908 @{ */
    private void checkChangeTimeAndDate(int hour , int minutes){
        final Button revert = (Button) findViewById(R.id.alarm_revert);
        if (mOriginalAlarm.hour == hour && mOriginalAlarm.minutes == minutes
                && mOriginalAlarm.daysOfWeek.toString(getApplicationContext(), true).equals(mRepeatPref.getDaysOfWeek().toString(getApplicationContext(), true))) {
            revert.setEnabled(false);
        } else {
            revert.setEnabled(true);
        }
    }
    /* @} */
    private void updateTime() {
        if (Log.LOGV) {
            Log.v("updateTime " + mId);
        }
        mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinutes,
                mRepeatPref.getDaysOfWeek()));
    }

    private long saveAlarmAndEnableRevert(String strLabel,boolean benable) {
        // Enable "Revert" to go back to the original Alarm.
        /* SPRD 426908 @{ */
/*        final Button revert = (Button) findViewById(R.id.alarm_revert);
        revert.setEnabled(true);*/
        /* @} */
        return saveAlarm(strLabel,benable);
    }

    private long saveAlarm(String label,boolean bEnable) {
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.hour = mHour;
        alarm.minutes = mMinutes;
        alarm.daysOfWeek = mRepeatPref.getDaysOfWeek();
        alarm.label = label;
        alarm.enabled = bEnable;
        // fix bug 202305 to make timing shutdown work when change TimeZone on 2013-08-22 begin
        alarm.time = Alarms.calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek).getTimeInMillis();
        // fix bug 202305 to make timing shutdown work when change TimeZone on 2013-08-22 end
        Log.v("timerpower SetAlarm =========== >>>>> saveAlarm mId "+mId);
        Log.v("timerpower SetAlarm =========== >>>>> saveAlarm mHour "+mHour);
        Log.v("timerpower SetAlarm =========== >>>>> saveAlarm mMinutes "+mMinutes);
        Log.v("timerpower SetAlarm =========== >>>>> saveAlarm alarm.label "+alarm.label);
        long time = 0;
        if (alarm.id != -1) {
            time = Alarms.setAlarm(this, alarm);
            /* Modify at 2013-02-21, for Porting from 4.0 MP barnch start */
            Alarms.enableAlarm(this, alarm.id, alarm.enabled);
            /* Modify at 2013-02-21, for Porting from 4.0 MP barnch end */
            Log.v("timerpower SetAlarm =========== >>>>> setAlarm  time = " + time);
        }
        return time;
    }

    /**
     * Display a toast that tells the user how long until the alarm
     * goes off.  This helps prevent "am/pm" mistakes.
     */
    static void popAlarmSetToast(Context context, int hour, int minute,
                                 Alarm.DaysOfWeek daysOfWeek) {
        popAlarmSetToast(context,
                Alarms.calculateAlarm(hour, minute, daysOfWeek)
                .getTimeInMillis());
    }

    static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
        ToastMaster.setToast(toast);
        toast.show();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from
     * now"
     */
    static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                    (dispHour ? 2 : 0) |
                    (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }
    /**
     * when new set time has already been set by other alarms , show timepicker again
     * @param hour
     * @param minute
     */
    private void sameAlarmShow(int hour,int minute){
        String time = null;
        boolean is24HourFormat = DateFormat.is24HourFormat(this);
        // the time is 12Hour Format.
        if (!is24HourFormat) {
            // afternoon time
            if (hour > 12) {
                time = (hour - 12) + ":" + minute +this.getResources().getString(R.string.timerpower_afternoon);
                if (minute < 10) {
                    time = (hour - 12) + ":0" + minute + this.getResources().getString(R.string.timerpower_afternoon);
                }
            } else {
                // morning time
                time = hour + ":" + minute + this.getResources().getString(R.string.timerpower_morning);
                if (minute < 10) {
                    time = hour + ":0" + minute + this.getResources().getString(R.string.timerpower_morning);
                }
            }
        } else {
            // the time is 24Hour Format.
            time = hour + ":" + minute;
            if (minute < 10) {
                time = hour + ":0" + minute;
            }
        }
        if( sameAlarmToast != null ){
          sameAlarmToast.setText(getString(R.string.alarm_alread_exist, time)) ;
          sameAlarmToast.show() ;
          showTimePicker();
          return ;

        }
        sameAlarmToast = new Toast(this).makeText(this, getString(R.string.alarm_alread_exist, time), Toast.LENGTH_LONG);
        sameAlarmToast.show() ;
        showTimePicker();
    }
}
