package com.android.server.telecom;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.CarrierConfigManager;

public class InCallScreenOffController extends CallsManagerListenerBase {
    private final Context mContext;
    private final CallsManager mCallsManager;
    private CarrierConfigManager mConfigManager;
    private static final int MSG_ACTIVE = 9;
    private static final int DELAY_BEFORE_SENDING_MSEC = 5000;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ACTIVE:
                    goToSleepForActiveCallState();
                    break;
            }
        }
    };

    InCallScreenOffController(Context context, CallsManager callsManager) {
        mContext = context;
        mCallsManager = callsManager;
        mConfigManager = (CarrierConfigManager) mContext
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);

        if (mConfigManager.getConfigForDefaultPhone() != null
                && !isProximitySensorAvailable()
                && mConfigManager.getConfigForDefaultPhone().getBoolean(
                        CarrierConfigManager.KEY_SCREEN_OFF_IN_ACTIVE_CALL_STATE_BOOL)) {
            mCallsManager.addListener(this);
        }
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        Call activeCall = mCallsManager.getActiveCall();
        if (((oldState == CallState.RINGING || oldState == CallState.DIALING || oldState == CallState.ON_HOLD)
                    && newState == CallState.ACTIVE)
                || (activeCall != null
                    && oldState == CallState.RINGING
                    && newState == CallState.DISCONNECTED)) {
            handleScreenOff();
        }
    }

    private boolean isProximitySensorAvailable() {
        SensorManager sensorManager = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        return proximitySensor != null;
    }

    private void goToSleepForActiveCallState() {
        PowerManager powerManager = (PowerManager) mContext
                .getSystemService(Context.POWER_SERVICE);
        powerManager.goToSleep(SystemClock.uptimeMillis());
    }

    private void handleScreenOff() {
        mHandler.removeMessages(MSG_ACTIVE);
        final Message msg = mHandler.obtainMessage(MSG_ACTIVE);
        mHandler.sendMessageDelayed(msg, DELAY_BEFORE_SENDING_MSEC);
        Log.i(this, "turn off the screen 5s after the call in active state");
    }
}
