/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.deskclock.alarms;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.AlarmInstance;
import android.telephony.SubscriptionManager;
import android.util.Log;

/* SPRD: for bug 511193 add alarmclock FlipSilent @{ */
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
/* @} */
import android.os.Vibrator;

/**
 * This service is in charge of starting/stopping the alarm. It will bring up and manage the
 * {@link AlarmActivity} as well as {@link AlarmKlaxon}.
 *
 * Registers a broadcast receiver to listen for snooze/dismiss intents. The broadcast receiver
 * exits early if AlarmActivity is bound to prevent double-processing of the snooze/dismiss intents.
 */
public class AlarmService extends Service {
    /**
     * AlarmActivity and AlarmService (when unbound) listen for this broadcast intent
     * so that other applications can snooze the alarm (after ALARM_ALERT_ACTION and before
     * ALARM_DONE_ACTION).
     */
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    /**
     * AlarmActivity and AlarmService listen for this broadcast intent so that other
     * applications can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    /** A public action sent by AlarmService when the alarm has started. */
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    /** A public action sent by AlarmService when the alarm has stopped for any reason. */
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    /** Private action used to start an alarm with this service. */
    public static final String START_ALARM_ACTION = "START_ALARM";

    /** Private action used to stop an alarm with this service. */
    public static final String STOP_ALARM_ACTION = "STOP_ALARM";

    /* SPRD: for bug 511193 add alarmclock FlipSilent @{ */
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float mData;
    private boolean isSupportMuteAlarms = false;
    private boolean mFirstIn = true;
    private boolean mDeviceDirBefore = false;
    private boolean mActionStart = false;
    private Context mContext;
    /* @} */

    /** Binder given to AlarmActivity */
    private final IBinder mBinder = new Binder();

    /** Whether the service is currently bound to AlarmActivity */
    private boolean mIsBound = false;

    /** Whether the receiver is currently registered */
    private boolean mIsRegistered = false;

    // SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm
    private AlarmInstance mInstance;

    @Override
    public IBinder onBind(Intent intent) {
        mIsBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mIsBound = false;
        return super.onUnbind(intent);
    }

    /**
     * Utility method to help start alarm properly. If alarm is already firing, it
     * will mark it as missed and start the new one.
     *
     * @param context application context
     * @param instance to trigger alarm
     */
    public static void startAlarm(Context context, AlarmInstance instance) {
        final Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId)
                .setAction(START_ALARM_ACTION);

        // Maintain a cpu wake lock until the service can get it
        AlarmAlertWakeLock.acquireCpuWakeLock(context);
        context.startService(intent);
    }

    /**
     * Utility method to help stop an alarm properly. Nothing will happen, if alarm is not firing
     * or using a different instance.
     *
     * @param context application context
     * @param instance you are trying to stop
     */
    public static void stopAlarm(Context context, AlarmInstance instance) {
        final Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId)
                .setAction(STOP_ALARM_ACTION);

        // We don't need a wake lock here, since we are trying to kill an alarm
        context.startService(intent);
    }

    private TelephonyManager mTelephonyManager[];
    private int mInitialCallState[];
    private AlarmInstance mCurrentAlarm = null;
    private PhoneStateListener mPhoneStateListener[];

    /* SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm @{
     * @orig
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            // The user might already be in a call when the alarm fires. When
            // we register onCallStateChanged, we get the initial in-call state
            // which kills the alarm. Check against the initial call state so
            // we don't kill the alarm during a call.
            if (state != TelephonyManager.CALL_STATE_IDLE && state != mInitialCallState) {
                sendBroadcast(AlarmStateManager.createStateChangeIntent(AlarmService.this,
                        "AlarmService", mCurrentAlarm, AlarmInstance.MISSED_STATE));
            }
        }
    };
    */
    private PhoneStateListener getPhoneStateListener(final int phoneId) {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String ignored) {
                state = TelephonyManager.getDefault().getCallState(SubscriptionManager.getSubId(phoneId)[0]);
                Log.i(LogUtils.LOGTAG, "PhoneStateListener,onCallStateChanged phoneId:" + phoneId + ", state:"
                        + state + ", mInitialCallState[phoneId]" + mInitialCallState[phoneId]);
                if (state == mInitialCallState[phoneId]) {
                    return;
                } else if (mInitialCallState[phoneId] != TelephonyManager.CALL_STATE_IDLE
                        && state != TelephonyManager.CALL_STATE_IDLE) {
                    mInitialCallState[phoneId] = state;
                    return;
                } else {
                    mInitialCallState[phoneId] = state;
                }
                if (state != TelephonyManager.CALL_STATE_IDLE) {
                    Log.i(LogUtils.LOGTAG, "PhoneStateListener, onCallStateChanged(), state != TelephonyManager.CALL_STATE_IDLE, mInstance = "
                            + mInstance);
                    AlarmStateManager.setMissedState(AlarmService.this, mInstance);
                } else if (mInstance != null) {
                    Log.i(LogUtils.LOGTAG, "PhoneStateListener, onCallStateChanged(), startAlarm(), mInstance = " +
                             mInstance);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // SPRD: Bug 565700 Alarm does not notify(not ringing )during active call
                    AlarmKlaxon.start(AlarmService.this, mInstance);
                }
            }
        };
        return phoneStateListener;
    }
    /* @} */

    private void startAlarm(AlarmInstance instance) {
        LogUtils.v("AlarmService.start with instance: " + instance.mId);
        /* SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm @{ */
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            mInitialCallState[i] = TelephonyManager.getDefault().getCallState(SubscriptionManager.getSubId(i)[0]);
            mTelephonyManager[i].listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_CALL_STATE);
        }
        /* @} */
        if (mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(this, mCurrentAlarm);
            stopCurrentAlarm();
        }

        AlarmAlertWakeLock.acquireCpuWakeLock(this);

        Events.sendEvent(R.string.category_alarm, R.string.action_fire, 0);

        mCurrentAlarm = instance;
        AlarmNotifications.showAlarmNotification(this, mCurrentAlarm);
        /* SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm @{
         * @orig
        mInitialCallState = mTelephonyManager.getCallState();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        /* @} */
        /* SPRD: Bug 565700 Alarm does not notify(not ringing )during active call @{ */
        if (!isInCall()) {
            AlarmKlaxon.start(this, mCurrentAlarm);
        } else {
            Vibrator vibrator = (Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
            vibrator.vibrate(100);
        }
        /* @} */
        sendBroadcast(new Intent(ALARM_ALERT_ACTION));
    }

    private void stopCurrentAlarm() {
        if (mCurrentAlarm == null) {
            LogUtils.v("There is no current alarm to stop");
            return;
        }

        LogUtils.v("AlarmService.stop with instance: %s", (Object) mCurrentAlarm.mId);
        AlarmKlaxon.stop(this);
        /* SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm @{
         * @orig
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        /* @} */
        sendBroadcast(new Intent(ALARM_DONE_ACTION));

        mCurrentAlarm = null;
        AlarmAlertWakeLock.releaseCpuLock();
    }

    private final BroadcastReceiver mActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtils.i("AlarmService received intent %s", action);
            if (mCurrentAlarm == null || mCurrentAlarm.mAlarmState != AlarmInstance.FIRED_STATE) {
                LogUtils.i("No valid firing alarm");
                return;
            }

            if (mIsBound) {
                LogUtils.i("AlarmActivity bound; AlarmService no-op");
                return;
            }

            switch (action) {
                case ALARM_SNOOZE_ACTION:
                    // Set the alarm state to snoozed.
                    // If this broadcast receiver is handling the snooze intent then AlarmActivity
                    // must not be showing, so always show snooze toast.
                    AlarmStateManager.setSnoozeState(context, mCurrentAlarm, true /* showToast */);
                    Events.sendAlarmEvent(R.string.action_snooze, R.string.label_intent);
                    break;
                case ALARM_DISMISS_ACTION:
                    // Set the alarm state to dismissed.
                    AlarmStateManager.setDismissState(context, mCurrentAlarm);
                    Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_intent);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        /* SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm @{
         * @orig
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        */
        mTelephonyManager = new TelephonyManager [TelephonyManager.getDefault().getPhoneCount()];
        mPhoneStateListener = new PhoneStateListener [TelephonyManager.getDefault().getPhoneCount()] ;
        for(int i = 0 ; i < TelephonyManager.getDefault().getPhoneCount() ; i++){
            mTelephonyManager [i] = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            mPhoneStateListener [i] = getPhoneStateListener(i);
            mInitialCallState = new int [TelephonyManager.getDefault().getPhoneCount()];
        }
        /* @} */

        // Register the broadcast receiver
        final IntentFilter filter = new IntentFilter(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mActionsReceiver, filter);
        mIsRegistered = true;
        /* SPRD: for bug 511193 add alarmclock FlipSilent @{ */
        registerSensorEventListener();
        /* @} */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.v("AlarmService.onStartCommand() with %s", intent);

        /* SPRD: Bug 513394 java.lang.NullPointerException @{ */
        if(intent != null) {
            final long instanceId = AlarmInstance.getId(intent.getData());
            switch (intent.getAction()) {
                case START_ALARM_ACTION:
                    final ContentResolver cr = this.getContentResolver();
                    mInstance = AlarmInstance.getInstance(cr, instanceId);
                    if (mInstance == null) {
                        LogUtils.e("No instance found to start alarm: %d", instanceId);
                        if (mCurrentAlarm != null) {
                            // Only release lock if we are not firing alarm
                            AlarmAlertWakeLock.releaseCpuLock();
                        }
                        break;
                    }

                    if (mCurrentAlarm != null && mCurrentAlarm.mId == instanceId) {
                        LogUtils.e("Alarm already started for instance: %d", instanceId);
                        break;
                    }
                    startAlarm(mInstance);
                    break;
                case STOP_ALARM_ACTION:
                    if (mCurrentAlarm != null && mCurrentAlarm.mId != instanceId) {
                        LogUtils.e("Can't stop alarm for instance: %d because current alarm is: %d",
                                instanceId, mCurrentAlarm.mId);
                        break;
                    }
                    stopCurrentAlarm();
                    stopSelf();
            }
        }
        /* @} */

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LogUtils.v("AlarmService.onDestroy() called");
        super.onDestroy();
        /* SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm @{ */
        for(int i = 0 ; i < TelephonyManager.getDefault().getPhoneCount() ; i++){
            mTelephonyManager [i].listen(mPhoneStateListener [i], PhoneStateListener.LISTEN_NONE);
        }
        /* @} */

        /* SPRD: for bug 511193 add alarmclock FlipSilent @{ */
        unregisterSensorEventListener();
        /* @} */
        if (mCurrentAlarm != null) {
            stopCurrentAlarm();
        }

        if (mIsRegistered) {
            unregisterReceiver(mActionsReceiver);
            mIsRegistered = false;
        }
    }
    /* SPRD: Bug 502135 multi-sim PhoneStateListener for the alarm @{ */
    private boolean isInCall() {
        boolean isInCall = false;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (TelephonyManager.getDefault().getCallState(SubscriptionManager.getSubId(i)[0]) != TelephonyManager.CALL_STATE_IDLE) {
                isInCall = true;
                break;
            }
        }
        return isInCall;
    }
    /* @} */

    /* SPRD: for bug 511193 add alarmclock FlipSilent @{ */
    private final SensorEventListener mSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            mData = event.values[SensorManager.DATA_X];
            LogUtils.d("SensorEventListener mData is:" + mData);
            if (mFirstIn) {
                mFirstIn = false;
                mDeviceDirBefore = (mData == 0 ? true : false);
            }

            boolean nowDir = false;
            nowDir = (mData == 0 ? true : false);
            LogUtils.d("SensorEventListener nowDir is:" + nowDir + " mDeviceDirBefore = " + mDeviceDirBefore
                    + " mActionStart = " + mActionStart);
            if (mDeviceDirBefore != nowDir) {
                if (!mActionStart) {
                    mActionStart = true;
                    LogUtils.d("SensorEventListener AlarmKlaxon.stop");
                    if (mContext != null) {
                        AlarmKlaxon.stop(mContext);
                    }
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void registerSensorEventListener() {
        isSupportMuteAlarms = Settings.Global.getInt(getContentResolver(), Settings.Global.MUTE_ALARMS, 0) != 0;
        LogUtils.v("isSupportMuteAlarms:" + isSupportMuteAlarms);
        mContext = this;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SPRDHUB_FACE_UP_DOWN);
            if (mSensor != null && isSupportMuteAlarms) {
                mSensorManager.registerListener(mSensorListener, mSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    public void unregisterSensorEventListener() {
        if (mSensorManager != null) {
            LogUtils.d("SensorEventListener mSensorManager != null unregisterListener");
            mSensorManager.unregisterListener(mSensorListener);
            mContext = null;
        }
    }
    /* @} */
}
