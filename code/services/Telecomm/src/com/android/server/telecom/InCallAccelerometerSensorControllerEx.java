package com.android.server.telecom;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import com.android.server.telecom.CallState;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

/**
 * SPRD: Flip to silence from incoming calls. See bug#473877
 * Handle registering and unregistering accelerometer sensor relating to call state.
 */
public class InCallAccelerometerSensorControllerEx extends CallsManagerListenerBase
        implements SensorEventListener {

    private Context mContext;
    private CallsManager mCallsManager;
    private Ringer mRinger;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    static InCallAccelerometerSensorControllerEx mInstance;

    private static final float DOWN_ANGLE =  -5.0f;
    private static final float UP_ANGLE =  5.0f;
    private static final int FLAG_UP = 0;
    private static final int FLAG_DOWN = 1;
    private static final int FLAG_UNKNOW = -1;
    private int mFlagOfZAxis = FLAG_UNKNOW;

    public InCallAccelerometerSensorControllerEx() {
    }

    public void init(Context context, CallsManager callsManager, Ringer ringer) {
        mContext = context;
        mCallsManager = callsManager;
        mRinger = ringer;

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        callsManager.addListener(this);
    }

    public static InCallAccelerometerSensorControllerEx getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new InCallAccelerometerSensorControllerEx();
        }
        return mInstance;
    }

    @Override
    public void onCallAdded(Call call) {
        if (isFlippingToSilence() && isFeatrueFlipToSilenceEnabled()) {
            Call ringingCall = mCallsManager.getRingingCall();

            if (mSensor != null && ringingCall != null) {
                mSensorManager.registerListener(this, mSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        if (mSensor != null && oldState == CallState.RINGING && newState != CallState.RINGING) {
            mSensorManager.unregisterListener(this, mSensor);
            mFlagOfZAxis = FLAG_UNKNOW;
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        if (mSensor != null) {
            mSensorManager.unregisterListener(this, mSensor);
            mFlagOfZAxis = FLAG_UNKNOW;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[2] >= UP_ANGLE) {
            mFlagOfZAxis = FLAG_UP;
        } else if (event.values[2] <= DOWN_ANGLE && mFlagOfZAxis == FLAG_UP) {
            mFlagOfZAxis = FLAG_DOWN;
            if (mCallsManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
                mRinger.silence();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private boolean isFlippingToSilence() {
        boolean isChecked = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.FLIPPING_SILENCE_DATA, 0) != 0;
        return isChecked;
     }

    /**
     * Check if configured filp to silent incoming call alerting feature. for bug473877 @{
     */
    private boolean isFeatrueFlipToSilenceEnabled() {
        boolean FlipToSilentIncomingCallEnable = false;
        CarrierConfigManager configManager = (CarrierConfigManager) mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        if(null != configManager.getConfigForDefaultPhone()) {
            FlipToSilentIncomingCallEnable = configManager.getConfigForDefaultPhone().getBoolean(
                                CarrierConfigManager.KEY_FEATURE_FLIP_SILENT_INCOMING_CALL_ENABLED_BOOL);
        }
        return FlipToSilentIncomingCallEnable;
    }
}
