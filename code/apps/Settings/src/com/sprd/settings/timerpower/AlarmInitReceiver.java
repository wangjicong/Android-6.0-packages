/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/** Create by Spreadst */
package com.sprd.settings.timerpower;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class AlarmInitReceiver extends BroadcastReceiver {

    private Context mContext;
    private static HandlerThread mHanderThread;
    private static MyHandler mThreadHander;
    private boolean isBoot = false;

    /**
     * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
     * TIME_SET, TIMEZONE_CHANGED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Log.v("AlarmInitReceiver ---- intent = " + intent);
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            isBoot = true;
        }
        if (null == mHanderThread) {
            Log.v("onReceive mHanderThread is null.");
            mHanderThread = new HandlerThread("HandlerThreadAlarmInitReceiver");
            mHanderThread.start();

            Looper looper = mHanderThread.getLooper();
            mThreadHander = new MyHandler(looper);
        }

        mThreadHander.obtainMessage().sendToTarget();
    }

    /*
     * use HanderThread msgQuence to do the data.
     */
    private class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (isBoot) {
                updateAlarmEnabled(mContext);
            } else {
                updateAlarmTime(mContext);
            }

            Alarms.disableAlert(mContext);
            Alarms.setNextAlert(mContext);
        }
    }

    /*
     * update alarm time When time_set and timeZone_change receiver.
     */
    private void updateAlarmTime(Context context) {
        Cursor cursor = Alarms.getAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Alarm alarm = new Alarm(context, cursor);
                    ContentValues values = new ContentValues(1);
                    alarm.time = Alarms.calculateAlarm(alarm.hour,
                            alarm.minutes, alarm.daysOfWeek).getTimeInMillis();
                    values.put(Alarm.Columns.ALARM_TIME, alarm.time);
                    context.getContentResolver().update(
                            ContentUris.withAppendedId(
                                    Alarm.Columns.CONTENT_URI, alarm.id),
                            values, null, null);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        isBoot = false;
    }
    private void updateAlarmEnabled(Context context) {
        Cursor cursor = Alarms.getAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Alarm alarm = new Alarm(context, cursor);
                    ContentValues values = new ContentValues(1);
                    alarm.time = Alarms.calculateAlarm(alarm.hour,
                            alarm.minutes, alarm.daysOfWeek).getTimeInMillis();
                    int repete = cursor.getInt(cursor.getColumnIndex(Alarm.Columns.DAYS_OF_WEEK));
                    Log.v("repete = "+repete);
                    Calendar currentTime = Calendar.getInstance();
                    if(currentTime.before(alarm.time) && repete == 0){
                        Log.v("no use alarm");
                        alarm.enabled = false;
                    }
                    values.put(Alarm.Columns.ENABLED, alarm.enabled);
                    context.getContentResolver().update(
                            ContentUris.withAppendedId(
                                    Alarm.Columns.CONTENT_URI, alarm.id),
                            values, null, null);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        isBoot = false;
    }
}
